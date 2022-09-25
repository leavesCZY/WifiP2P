package github.leavesczy.wifip2p.service

import java.io.Serializable

/**
 * @Author: leavesCZY
 * @Desc:
 */
class FileTransfer : Serializable {
    var fileName: String? = null
    var fileLength: Long = 0
    var md5: String? = null
}