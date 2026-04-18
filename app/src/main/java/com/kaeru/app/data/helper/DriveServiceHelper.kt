package com.kaeru.app.data.helper

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.Collections

class GoogleDriveHelper(private val context: Context) {

    private var driveService: Drive? = null

    fun initializeDrive(account: GoogleSignInAccount) {
        val credential = GoogleAccountCredential.usingOAuth2(
            context, Collections.singleton(DriveScopes.DRIVE_APPDATA)
        ).apply { selectedAccount = account.account }

        driveService = Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("Kaeru Track").build()
    }

    suspend fun listBackups(): List<File> = withContext(Dispatchers.IO) {
        driveService?.files()?.list()?.apply {
            spaces = "appDataFolder"
            fields = "files(id, name, modifiedTime)"
            orderBy = "modifiedTime desc"
        }?.execute()?.files ?: emptyList()
    }

    suspend fun uploadBackup(jsonContent: String, fileIdToOverwrite: String? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            val metadata = File().apply {
                name = "${System.currentTimeMillis()}.json"
            }
            val content = ByteArrayContent.fromString("application/json", jsonContent)

            if (fileIdToOverwrite != null) {
                driveService?.files()?.update(fileIdToOverwrite, metadata, content)?.execute()
            } else {
                metadata.parents = listOf("appDataFolder")
                driveService?.files()?.create(metadata, content)?.execute()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun restoreBackup(fileId: String): String? = withContext(Dispatchers.IO) {
        try {
            val outputStream = ByteArrayOutputStream()
            driveService?.files()?.get(fileId)?.executeMediaAndDownloadTo(outputStream)
            outputStream.toString("UTF-8")
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun deleteFile(fileId: String) = withContext(Dispatchers.IO) {
        driveService?.files()?.delete(fileId)?.execute()
    }

    fun clearDrive() {
        driveService = null
    }
}