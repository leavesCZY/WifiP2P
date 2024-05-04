package github.leavesczy.wifip2p.receiver

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import github.leavesczy.wifip2p.common.Constants
import github.leavesczy.wifip2p.common.FileTransfer
import github.leavesczy.wifip2p.common.FileTransferViewState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.ObjectInputStream
import java.net.InetSocketAddress
import java.net.ServerSocket

/**
 * @Author: CZY
 * @Date: 2022/9/26 14:18
 * @Desc:
 */
class FileReceiverViewModel(context: Application) : AndroidViewModel(context) {

    private val _fileTransferViewState = MutableSharedFlow<FileTransferViewState>()

    val fileTransferViewState: SharedFlow<FileTransferViewState>
        get() = _fileTransferViewState

    private val _log = MutableSharedFlow<String>()

    val log: SharedFlow<String>
        get() = _log

    private var job: Job? = null

    fun startListener() {
        if (job != null) {
            return
        }
        job = viewModelScope.launch(context = Dispatchers.IO) {
            _fileTransferViewState.emit(value = FileTransferViewState.Idle)

            var serverSocket: ServerSocket? = null
            var clientInputStream: InputStream? = null
            var objectInputStream: ObjectInputStream? = null
            var fileOutputStream: FileOutputStream? = null
            try {
                _fileTransferViewState.emit(value = FileTransferViewState.Connecting)
                log(log = "开启 Socket")

                serverSocket = ServerSocket()
                serverSocket.bind(InetSocketAddress(Constants.PORT))
                serverSocket.reuseAddress = true
                serverSocket.soTimeout = 30000

                log(log = "socket accept，三十秒内如果未成功则断开链接")

                val client = serverSocket.accept()

                _fileTransferViewState.emit(value = FileTransferViewState.Receiving)

                clientInputStream = client.getInputStream()
                objectInputStream = ObjectInputStream(clientInputStream)
                val fileTransfer = objectInputStream.readObject() as FileTransfer
                val file = File(getCacheDir(context = getApplication()), fileTransfer.fileName)

                log(log = "连接成功，待接收的文件: $fileTransfer")
                log(log = "文件将保存到: $file")
                log(log = "开始传输文件")

                fileOutputStream = FileOutputStream(file)
                val buffer = ByteArray(1024 * 100)
                while (true) {
                    val length = clientInputStream.read(buffer)
                    if (length > 0) {
                        fileOutputStream.write(buffer, 0, length)
                    } else {
                        break
                    }
                    log(log = "正在传输文件，length : $length")
                }
                _fileTransferViewState.emit(value = FileTransferViewState.Success(file = file))
                log(log = "文件接收成功")
            } catch (e: Throwable) {
                log(log = "异常: " + e.message)
                _fileTransferViewState.emit(value = FileTransferViewState.Failed(throwable = e))
            } finally {
                serverSocket?.close()
                clientInputStream?.close()
                objectInputStream?.close()
                fileOutputStream?.close()
            }
        }
        job?.invokeOnCompletion {
            job = null
        }
    }

    private fun getCacheDir(context: Context): File {
        val cacheDir = File(context.cacheDir, "FileTransfer")
        cacheDir.mkdirs()
        return cacheDir
    }

    private suspend fun log(log: String) {
        _log.emit(value = log)
    }

}