package com.kaeru.app.tracking

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kaeru.app.tracking.database.AppDatabase
import com.kaeru.app.tracking.utils.isDeliveredStatus
import kotlinx.coroutines.flow.first

class TrackingWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val notificationHelper = NotificationHelper(applicationContext)
        if (!isNetworkAvailable(applicationContext)) {
            return Result.retry()
        }
        val isFakeTest = inputData.getBoolean("is_fake_test", false)
        if (isFakeTest) {
            notificationHelper.showNotification("xxx", "xxx", "xxx")
            return Result.success()
        }
        val database = AppDatabase.getDatabase(applicationContext)
        val dao = database.trackingDao()
        val repository = TrackingRepository(applicationContext)
        var hasFailedRequests = false

        try {
            val encomendasSalvas = dao.getAllTracking().first()

            for (encomenda in encomendasSalvas) {
                if (encomenda.lastStatus?.isDeliveredStatus() == true) continue
                try {
                    val response = repository.trackPackage(encomenda.code, forceRefresh = true)
                    val eventoMaisRecente = response?.events?.firstOrNull()
                    val statusNovo = eventoMaisRecente?.status

                    if (statusNovo != null && statusNovo != encomenda.lastStatus) {

                        val nomeExibicao = if (encomenda.description.isNotBlank() && encomenda.description != "Encomenda Sem Nome") {
                            encomenda.description
                        } else {
                            encomenda.code
                        }
                        val updatedItem = encomenda.copy(
                            lastStatus = statusNovo,
                            lastDate = "${eventoMaisRecente.date ?: ""} ${eventoMaisRecente.time ?: ""}".trim(),
                            savedAt = System.currentTimeMillis()
                        )
                        dao.insertTracking(updatedItem)
                        notificationHelper.showNotification(nomeExibicao, encomenda.code, statusNovo)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    hasFailedRequests = true
                }
            }
            return if (hasFailedRequests) Result.retry() else Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
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