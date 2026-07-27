package com.kaeru.app.tracking

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kaeru.app.BuildConfig
import com.kaeru.app.tracking.database.AppDatabase
import com.kaeru.app.tracking.utils.isDeliveredStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import retrofit2.HttpException
import java.io.IOException

class TrackingWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val MAX_CONCURRENT_REQUESTS = 3
        private const val REQUEST_TIMEOUT_MS = 30_000L
    }

    private sealed class ItemResult {
        object Success : ItemResult()
        object TransientFailure : ItemResult()
        object PermanentFailure : ItemResult()
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val notificationHelper = NotificationHelper(applicationContext)

        if (!isNetworkAvailable(applicationContext)) {
            return@withContext Result.success()
        }

        val isFakeTest = inputData.getBoolean("is_fake_test", false)
        if (isFakeTest && BuildConfig.DEBUG) {
            notificationHelper.showNotification("Teste", "Teste", "Entregue")
            return@withContext Result.success()
        }

        val database = AppDatabase.getDatabase(applicationContext)
        val dao = database.trackingDao()
        val repository = TrackingRepository(applicationContext)

        try {
            val encomendasSalvas = dao.getAllTracking().first()
            val encomendasPendentes = encomendasSalvas.filter {
                it.lastStatus?.isDeliveredStatus() != true && it.notificationsEnabled
            }

            if (encomendasPendentes.isEmpty()) {
                return@withContext Result.success()
            }

            val semaphore = Semaphore(permits = MAX_CONCURRENT_REQUESTS)

            val resultados = coroutineScope {
                encomendasPendentes.map { encomenda ->
                    async {
                        semaphore.withPermit {
                            processarEncomenda(encomenda, repository, dao, notificationHelper)
                        }
                    }
                }.awaitAll()
            }

            val temFalhaPermanente = resultados.any { it is ItemResult.PermanentFailure }
            val temFalhaTransitoria = resultados.any { it is ItemResult.TransientFailure }

            return@withContext when {
                temFalhaTransitoria -> Result.retry()
                temFalhaPermanente -> Result.success()
                else -> Result.success()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private suspend fun processarEncomenda(
        encomenda: com.kaeru.app.tracking.database.TrackingEntity,
        repository: TrackingRepository,
        dao: com.kaeru.app.tracking.database.TrackingDao,
        notificationHelper: NotificationHelper
    ): ItemResult {
        return try {
            val response = withTimeoutOrNull(REQUEST_TIMEOUT_MS) {
                repository.trackPackage(encomenda.code, forceRefresh = true, cpf = encomenda.cpf)
            } ?: return ItemResult.TransientFailure

            val eventoMaisRecente = response.events?.firstOrNull()
            val statusNovo = eventoMaisRecente?.status
            val dataHoraNovo = "${eventoMaisRecente?.date ?: ""} ${eventoMaisRecente?.time ?: ""}".trim()

            if (statusNovo != null && (statusNovo != encomenda.lastStatus || dataHoraNovo != encomenda.lastDate)) {
                val nomeExibicao = if (encomenda.description.isNotBlank() && encomenda.description != "Encomenda Sem Nome") {
                    encomenda.description
                } else {
                    encomenda.code
                }

                val updatedItem = encomenda.copy(
                    lastStatus = statusNovo,
                    lastDate = dataHoraNovo,
                    savedAt = System.currentTimeMillis()
                )

                dao.insertTracking(updatedItem)

                try {
                    notificationHelper.showNotification(nomeExibicao, encomenda.code, statusNovo)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            ItemResult.Success
        } catch (e: HttpException) {
            e.printStackTrace()
            if (e.code() in 400..499) ItemResult.PermanentFailure else ItemResult.TransientFailure
        } catch (e: IOException) {
            e.printStackTrace()
            ItemResult.TransientFailure
        } catch (e: Exception) {
            e.printStackTrace()
            ItemResult.TransientFailure
        }
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}