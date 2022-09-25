package github.leavesczy.wifip2p.utils

import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.security.MessageDigest

/**
 * @Author: leavesCZY
 * @Desc:
 */
object Md5Util {

    fun getMd5(file: File): String? {
        var inputStream: InputStream? = null
        val buffer = ByteArray(2048)
        var numRead: Int
        val md5: MessageDigest
        return try {
            inputStream = FileInputStream(file)
            md5 = MessageDigest.getInstance("MD5")
            while (inputStream.read(buffer).also { numRead = it } > 0) {
                md5.update(buffer, 0, numRead)
            }
            inputStream.close()
            inputStream = null
            md5ToString(md5.digest())
        } catch (e: Throwable) {
            null
        } finally {
            inputStream?.close()
        }
    }

    private fun md5ToString(md5Bytes: ByteArray): String {
        val hexValue = StringBuilder()
        for (b in md5Bytes) {
            val value = b.toInt() and 0xff
            if (value < 16) {
                hexValue.append("0")
            }
            hexValue.append(Integer.toHexString(value))
        }
        return hexValue.toString()
    }
}