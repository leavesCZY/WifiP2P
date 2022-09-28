package github.leavesczy.wifip2p

import android.util.Log

/**
 * @Author: CZY
 * @Date: 2022/9/27 16:52
 * @Desc:
 */
object Logger {

    fun log(any: Any?) {
        Log.e("WifiP2P", any?.toString() ?: "null")
    }

}