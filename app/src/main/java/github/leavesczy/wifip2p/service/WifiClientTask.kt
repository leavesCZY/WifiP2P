package github.leavesczy.wifip2p.service

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Context
import android.net.Uri
import android.os.AsyncTask
import android.util.Log
import github.leavesczy.wifip2p.utils.Md5Util
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.ObjectOutputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.Random

/**
 * @Author: leavesCZY
 * @Desc: 客户端发送文件
 */
class WifiClientTask(context: Context) : AsyncTask<Any, Int, Boolean>() {

    companion object {
        private const val TAG = "WifiClientTask"
    }

    private val progressDialog: ProgressDialog

    @SuppressLint("StaticFieldLeak")
    private val context: Context

    init {
        this.context = context.applicationContext
        progressDialog = ProgressDialog(context)
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
        progressDialog.setCancelable(false)
        progressDialog.setCanceledOnTouchOutside(false)
        progressDialog.setTitle("正在发送文件")
        progressDialog.max = 100
    }

    override fun onPreExecute() {
        progressDialog.show()
    }

    @Throws(Exception::class)
    private fun getOutputFilePath(fileUri: Uri): String {
        val outputFilePath = context.externalCacheDir!!.absolutePath +
                File.separatorChar + Random().nextInt(10000) +
                Random().nextInt(10000) + ".jpg"
        val outputFile = File(outputFilePath)
        if (!outputFile.exists()) {
            outputFile.mkdirs()
            outputFile.createNewFile()
        }
        val outputFileUri = Uri.fromFile(outputFile)
        copyFile(context, fileUri, outputFileUri)
        return outputFilePath
    }

    @Deprecated("Deprecated in Java")
    override fun doInBackground(vararg params: Any?): Boolean {
        var socket: Socket? = null
        var outputStream: OutputStream? = null
        var objectOutputStream: ObjectOutputStream? = null
        var inputStream: InputStream? = null
        try {
            val hostAddress = params[0].toString()
            val imageUri = Uri.parse(params[1].toString())
            val outputFilePath = getOutputFilePath(imageUri)
            val outputFile = File(outputFilePath)
            val fileTransfer = FileTransfer()
            val fileName = outputFile.name
            val fileMa5 = Md5Util.getMd5(outputFile)
            val fileLength = outputFile.length()
            fileTransfer.fileName = fileName
            fileTransfer.md5 = fileMa5
            fileTransfer.fileLength = fileLength
            Log.e(TAG, "文件的MD5码值是：" + fileTransfer.md5)
            socket = Socket()
            socket.bind(null)
            socket.connect(InetSocketAddress(hostAddress, Constants.PORT), 10000)
            outputStream = socket.getOutputStream()
            objectOutputStream = ObjectOutputStream(outputStream)
            objectOutputStream.writeObject(fileTransfer)
            inputStream = FileInputStream(outputFile)
            val fileSize = fileTransfer.fileLength
            var total: Long = 0
            val buf = ByteArray(1024)
            var len: Int
            while (inputStream.read(buf).also { len = it } != -1) {
                outputStream.write(buf, 0, len)
                total += len.toLong()
                val progress = (total * 100 / fileSize).toInt()
                publishProgress(progress)
                Log.e(TAG, "文件发送进度：$progress")
            }
            socket.close()
            inputStream.close()
            outputStream.close()
            objectOutputStream.close()
            socket = null
            inputStream = null
            outputStream = null
            objectOutputStream = null
            Log.e(TAG, "文件发送成功")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "文件发送异常 Exception: " + e.message)
        } finally {
            socket?.close()
            inputStream?.close()
            outputStream?.close()
            objectOutputStream?.close()
        }
        return false
    }

    @Throws(NullPointerException::class, IOException::class)
    private fun copyFile(context: Context, inputUri: Uri, outputUri: Uri) {
        context.contentResolver.openInputStream(inputUri).use { inputStream ->
            FileOutputStream(outputUri.path).use { outputStream ->
                if (inputStream == null) {
                    throw NullPointerException("InputStream for given input Uri is null")
                }
                val buffer = ByteArray(1024)
                var length: Int
                while (inputStream.read(buffer).also { length = it } > 0) {
                    outputStream.write(buffer, 0, length)
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onProgressUpdate(vararg values: Int?) {
        progressDialog.progress = values.getOrNull(0) ?: 0
    }

    @Deprecated("Deprecated in Java")
    override fun onPostExecute(aBoolean: Boolean) {
        progressDialog.cancel()
        Log.e(TAG, "onPostExecute: $aBoolean")
    }

}