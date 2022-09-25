package github.leavesczy.wifip2p

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts

/**
 * @Author: leavesCZY
 * @Desc:
 */
class MainActivity : BaseActivity() {

    private val requestPermissionLaunch = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { it ->
        if (it.all { it.value }) {
            showToast("已获得权限")
        } else {
            showToast("缺少权限，请先授予权限")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<View>(R.id.btnCheckPermission).setOnClickListener { v: View? ->
            requestPermissionLaunch.launch(
                arrayOf(
                    Manifest.permission.CHANGE_NETWORK_STATE,
                    Manifest.permission.ACCESS_NETWORK_STATE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_STATE,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }
        findViewById<View>(R.id.btnSender).setOnClickListener {
            startActivity(Intent(this@MainActivity, SendFileActivity::class.java))
        }
        findViewById<View>(R.id.btnReceiver).setOnClickListener {
            startActivity(Intent(this@MainActivity, ReceiveFileActivity::class.java))
        }
    }

}