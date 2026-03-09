package com.kaeru.app.tracking

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kaeru.app.tracking.database.AppDatabase
import kotlinx.coroutines.flow.first

class TrackingWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val notificationHelper = NotificationHelper(applicationContext)

        val database = AppDatabase.getDatabase(applicationContext)
        val dao = database.trackingDao()

        val repository = TrackingRepository(applicationContext)

        val isFakeTest = inputData.getBoolean("is_fake_test", false)
        if (isFakeTest) {
            notificationHelper.showNotification(
                trackingCode = "xxx",
                newStatus = "xxx"
            )
            return Result.success()
        }

        try {
            val encomendasSalvas = dao.getAllTracking().first()

            for (encomenda in encomendasSalvas) {
                val isAlreadyDelivered = encomenda.lastStatus.contains("Entregue", ignoreCase = true) ||
                        encomenda.lastStatus.contains("Delivered", ignoreCase = true)
                if (isAlreadyDelivered) continue

                val response = repository.trackPackage(encomenda.code, forceRefresh = true)
                val eventoMaisRecente = response?.events?.firstOrNull()
                val statusNovo = eventoMaisRecente?.status

                if (statusNovo != null && statusNovo != encomenda.lastStatus) {
                    val nomeExibicao = if (encomenda.description.isNotBlank() && encomenda.description != "Encomenda Sem Nome") {
                        encomenda.description
                    } else {
                        encomenda.code
                    }

                    notificationHelper.showNotification(nomeExibicao, statusNovo)

                    val updatedItem = encomenda.copy(
                        lastStatus = statusNovo,
                        lastDate = "${eventoMaisRecente.date ?: ""} ${eventoMaisRecente.time ?: ""}".trim(),
                        savedAt = System.currentTimeMillis()
                    )
                    dao.insertTracking(updatedItem)
                }
            }
            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
    }
}