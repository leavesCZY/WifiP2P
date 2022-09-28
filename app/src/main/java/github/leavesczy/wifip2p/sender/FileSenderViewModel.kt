package github.leavesczy.wifip2p.sender

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.core.net.toFile
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import github.leavesczy.wifip2p.Constants
import github.leavesczy.wifip2p.models.FileTransfer
import github.leavesczy.wifip2p.models.ViewState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectOutputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.random.Random

/**
 * @Author: CZY
 * @Date: 2022/9/26 10:38
 * @Desc:
 */
class FileSenderViewModel(context: Application) :
    AndroidViewModel(context) {

    private val _viewState = MutableSharedFlow<ViewState>()

    val viewState: SharedFlow<ViewState> = _viewState

    private val _log = MutableSharedFlow<String>()

    val log: SharedFlow<String> = _log

    private var job: Job? = null

    fun send(ipAddress: String, fileUri: Uri) {
        if (job != null) {
            return
        }
        job = viewModelScope.launch {
            withContext(context = Dispatchers.IO) {
                _viewState.emit(value = ViewState.Idle)

                var socket: Socket? = null
                var outputStream: OutputStream? = null
                var objectOutputStream: ObjectOutputStream? = null
                var fileInputStream: FileInputStream? = null
                try {
                    val cacheFile =
                        saveFileToCacheDir(context = getApplication(), fileUri = fileUri)
                    val fileTransfer = FileTransfer(fileName = cacheFile.name)

                    _viewState.emit(value = ViewState.Connecting)
                    _log.emit(value = "待发送的文件: $fileTransfer")
                    _log.emit(value = "开启 Socket")

                    socket = Socket()
                    socket.bind(null)

                    _log.emit(value = "socket connect，如果三十秒内未连接成功则放弃")

                    socket.connect(InetSocketAddress(ipAddress, Constants.PORT), 30000)

                    _viewState.emit(value = ViewState.Receiving)
                    _log.emit(value = "连接成功，开始传输文件")

                    outputStream = socket.getOutputStream()
                    objectOutputStream = ObjectOutputStream(outputStream)
                    objectOutputStream.writeObject(fileTransfer)
                    fileInputStream = FileInputStream(cacheFile)
                    val buffer = ByteArray(1024 * 100)
                    var length: Int
                    while (true) {
                        length = fileInputStream.read(buffer)
                        if (length > 0) {
                            outputStream.write(buffer, 0, length)
                        } else {
                            break
                        }
                        _log.emit(value = "正在传输文件，length : $length")
                    }
                    _log.emit(value = "文件发送成功")
                    _viewState.emit(value = ViewState.Success(file = cacheFile))
                } catch (e: Throwable) {
                    e.printStackTrace()
                    _log.emit(value = "异常: " + e.message)
                    _viewState.emit(value = ViewState.Failed(throwable = e))
                } finally {
                    fileInputStream?.close()
                    outputStream?.close()
                    objectOutputStream?.close()
                    socket?.close()
                }
            }
        }
        job?.invokeOnCompletion {
            job = null
        }
    }

    private suspend fun saveFileToCacheDir(context: Context, fileUri: Uri): File {
        return withContext(context = Dispatchers.IO) {
            val documentFile = DocumentFile.fromSingleUri(context, fileUri)
                ?: throw NullPointerException("fileName for given input Uri is null")
            val fileName = documentFile.name
            val outputFile = File(
                context.cacheDir, Random.nextInt(
                    1,
                    200
                ).toString() + "_" + fileName
            )
            if (outputFile.exists()) {
                outputFile.delete()
            }
            outputFile.createNewFile()
            val outputFileUri = Uri.fromFile(outputFile)
            copyFile(context, fileUri, outputFileUri)
            return@withContext outputFile
        }
    }

    private suspend fun copyFile(context: Context, inputUri: Uri, outputUri: Uri) {
        withContext(context = Dispatchers.IO) {
            val inputStream = context.contentResolver.openInputStream(inputUri)
                ?: throw NullPointerException("InputStream for given input Uri is null")
            val outputStream = FileOutputStream(outputUri.toFile())
            val buffer = ByteArray(1024)
            var length: Int
            while (true) {
                length = inputStream.read(buffer)
                if (length > 0) {
                    outputStream.write(buffer, 0, length)
                } else {
                    break
                }
            }
            inputStream.close()
            outputStream.close()
        }
    }

}