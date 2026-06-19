package com.ventgui.app.data.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.ventgui.app.BuildConfig
import com.ventgui.app.data.network.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.ktor.client.HttpClient
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class AppUpdateInfo(
    val version_code: Int,
    val version_name: String,
    val apk_url: String,
    val release_notes: String? = null,
    val is_mandatory: Boolean = false,
    val created_at: String? = null
)

sealed class UpdateState {
    object Idle : UpdateState()
    data class UpdateAvailable(val info: AppUpdateInfo) : UpdateState()
    data class Downloading(val progress: Float) : UpdateState()
    data class ReadyToInstall(val apkFile: File) : UpdateState()
    data class Error(val message: String) : UpdateState()
}

object UpdateManager {
    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState

    suspend fun checkForUpdates() {
        withContext(Dispatchers.IO) {
            try {
                // Procurar a atualização com maior versionCode no Supabase
                val updates = SupabaseClient.client.postgrest.from("app_updates")
                    .select()
                    .decodeList<AppUpdateInfo>()
                
                val latestUpdate = updates.maxByOrNull { it.version_code }
                
                val currentVersionCode = BuildConfig.VERSION_CODE
                if (latestUpdate != null && latestUpdate.version_code > currentVersionCode) {
                    _updateState.value = UpdateState.UpdateAvailable(latestUpdate)
                } else {
                    _updateState.value = UpdateState.Idle
                }
            } catch (e: Exception) {
                // Log safely without stack trace in production
                // Fail silently for update checks
            }
        }
    }

    suspend fun downloadAndInstallUpdate(context: Context, info: AppUpdateInfo) {
        withContext(Dispatchers.IO) {
            var client: HttpClient? = null
            try {
                _updateState.value = UpdateState.Downloading(0f)
                
                val destinationFile = File(context.cacheDir, "update_${info.version_name}.apk")
                if (destinationFile.exists()) {
                    destinationFile.delete()
                }

                client = HttpClient()
                val response = client.get(info.apk_url) {
                    onDownload { bytesSentTotal, contentLength ->
                        val total = contentLength ?: -1L
                        if (total > 0L) {
                            val progress = bytesSentTotal.toFloat() / total.toFloat()
                            _updateState.value = UpdateState.Downloading(progress)
                        }
                    }
                }

                val bytes = response.bodyAsBytes()
                destinationFile.writeBytes(bytes)

                _updateState.value = UpdateState.ReadyToInstall(destinationFile)
                installApk(context, destinationFile)
            } catch (e: Exception) {
                android.util.Log.e("CantanhedeHub", "Erro no download da atualizacao", e)
                _updateState.value = UpdateState.Error("Erro ao descarregar atualização: ${e.localizedMessage ?: e.message ?: e.toString()}")
            } finally {
                client?.close()
            }
        }
    }

    fun installApk(context: Context, file: File) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val apkUri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(context, authority, file)
            } else {
                Uri.fromFile(file)
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            _updateState.value = UpdateState.Error("Erro ao iniciar instalação.")
        }
    }
}
