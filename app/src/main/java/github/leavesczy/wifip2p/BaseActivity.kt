package github.leavesczy.wifip2p

import android.app.ProgressDialog
import android.app.Service
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * @Author: leavesCZY
 * @Desc:
 */
open class BaseActivity : AppCompatActivity() {

    private var loadingDialog: ProgressDialog? = null

    protected fun setTitle(title: String?) {
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.title = title
        }
    }

    protected fun showLoadingDialog(message: String?) {
        loadingDialog?.dismiss()
        loadingDialog = ProgressDialog(this)
        loadingDialog?.setTitle(message)
        loadingDialog?.show()
    }

    protected fun dismissLoadingDialog() {
        loadingDialog?.dismiss()
    }

    protected fun log(any: Any?) {
        Log.e(javaClass.simpleName, any?.toString() ?: "null")
    }

    protected fun <T : Service> startService(tClass: Class<T>) {
        startService(Intent(this, tClass))
    }

    protected fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}