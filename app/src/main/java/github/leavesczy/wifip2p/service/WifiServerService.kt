package github.leavesczy.wifip2p.service

import android.app.IntentService
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import github.leavesczy.wifip2p.utils.Md5Util
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.ObjectInputStream
import java.net.InetSocketAddress
import java.net.ServerSocket

/**
 * @Author: leavesCZY
 * @Desc: 服务器端接收文件
 */
class WifiServerService : IntentService("WifiServerService") {

    companion object {
        private const val TAG = "WifiServerService"
    }

    private var serverSocket: ServerSocket? = null

    private var inputStream: InputStream? = null

    private var objectInputStream: ObjectInputStream? = null

    private var fileOutputStream: FileOutputStream? = null

    private var progressChangListener: OnProgressChangListener? = null

    @Deprecated("Deprecated in Java")
    override fun onBind(intent: Intent): IBinder {
        return WifiServerBinder()
    }

    @Deprecated("Deprecated in Java")
    override fun onHandleIntent(intent: Intent?) {
        clean()
        var file: File? = null
        try {
            serverSocket = ServerSocket()
            serverSocket!!.reuseAddress = true
            serverSocket!!.bind(InetSocketAddress(Constants.PORT))
            val client = serverSocket!!.accept()
            Log.e(TAG, "客户端IP地址 : " + client.inetAddress.hostAddress)
            inputStream = client.getInputStream()
            objectInputStream = ObjectInputStream(inputStream)
            val fileTransfer = objectInputStream!!.readObject() as FileTransfer
            Log.e(TAG, "待接收的文件: $fileTransfer")
            val name = fileTransfer.fileName
            //将文件存储至指定位置
            file = File(cacheDir, name)
            fileOutputStream = FileOutputStream(file)
            val buf = ByteArray(1024)
            var len: Int
            var total: Long = 0
            var progress: Int
            while (inputStream!!.read(buf).also { len = it } != -1) {
                fileOutputStream!!.write(buf, 0, len)
                total += len.toLong()
                progress = (total * 100 / fileTransfer.fileLength).toInt()
                Log.e(TAG, "文件接收进度: $progress")
                progressChangListener?.onProgressChanged(fileTransfer, progress)
            }
            serverSocket?.close()
            inputStream?.close()
            objectInputStream?.close()
            fileOutputStream?.close()
            serverSocket = null
            inputStream = null
            objectInputStream = null
            fileOutputStream = null
            Log.e(TAG, "文件接收成功，文件的MD5码是：" + Md5Util.getMd5(file))
        } catch (e: Exception) {
            Log.e(TAG, "文件接收 Exception: " + e.message)
        } finally {
            clean()
            progressChangListener?.onTransferFinished(file)
            //再次启动服务，等待客户端下次连接
            startService(Intent(this, WifiServerService::class.java))
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onDestroy() {
        super.onDestroy()
        clean()
    }

    fun setProgressChangListener(progressChangListener: OnProgressChangListener?) {
        this.progressChangListener = progressChangListener
    }

    private fun clean() {
        serverSocket?.close()
        serverSocket = null
        inputStream?.close()
        inputStream = null
        objectInputStream?.close()
        objectInputStream = null
        fileOutputStream?.close()
        fileOutputStream = null
    }

    interface OnProgressChangListener {
        //当传输进度发生变化时
        fun onProgressChanged(fileTransfer: FileTransfer, progress: Int)

        //当传输结束时
        fun onTransferFinished(file: File?)
    }

    inner class WifiServerBinder : Binder() {
        val service: WifiServerService
            get() = this@WifiServerService
    }
}