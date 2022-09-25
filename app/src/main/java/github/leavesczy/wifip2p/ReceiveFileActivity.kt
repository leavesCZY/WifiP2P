package github.leavesczy.wifip2p

import android.Manifest
import android.app.ProgressDialog
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.ActivityCompat
import com.bumptech.glide.Glide
import github.leavesczy.wifip2p.service.DirectActionListener
import github.leavesczy.wifip2p.service.DirectBroadcastReceiver
import github.leavesczy.wifip2p.service.FileTransfer
import github.leavesczy.wifip2p.service.WifiServerService
import github.leavesczy.wifip2p.service.WifiServerService.OnProgressChangListener
import github.leavesczy.wifip2p.service.WifiServerService.WifiServerBinder
import java.io.File

/**
 * @Author: leavesCZY
 * @Desc:
 */
class ReceiveFileActivity : BaseActivity() {

    private lateinit var ivImage: ImageView

    private lateinit var tvLog: TextView

    private lateinit var progressDialog: ProgressDialog

    private val progressChangListener: OnProgressChangListener = object : OnProgressChangListener {
        override fun onProgressChanged(fileTransfer: FileTransfer, progress: Int) {
            runOnUiThread {
                progressDialog.setMessage("文件名： " + fileTransfer.fileName)
                progressDialog.progress = progress
                progressDialog.show()
            }
        }

        override fun onTransferFinished(file: File?) {
            runOnUiThread {
                progressDialog.cancel()
                if (file != null && file.exists()) {
                    Glide.with(this@ReceiveFileActivity).load(file.path).into(ivImage)
                }
            }
        }
    }

    private lateinit var wifiP2pManager: WifiP2pManager

    private var channel: WifiP2pManager.Channel? = null

    private var connectionInfoAvailable = false

    private var broadcastReceiver: BroadcastReceiver? = null

    private var wifiServerService: WifiServerService? = null

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as WifiServerBinder
            wifiServerService = binder.service
            wifiServerService!!.setProgressChangListener(progressChangListener)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            if (wifiServerService != null) {
                wifiServerService!!.setProgressChangListener(null)
                wifiServerService = null
            }
            bindService()
        }
    }
    private val directActionListener: DirectActionListener = object : DirectActionListener {
        override fun wifiP2pEnabled(enabled: Boolean) {
            log("wifiP2pEnabled: $enabled")
        }

        override fun onConnectionInfoAvailable(wifiP2pInfo: WifiP2pInfo) {
            log("onConnectionInfoAvailable")
            log("isGroupOwner：" + wifiP2pInfo.isGroupOwner)
            log("groupFormed：" + wifiP2pInfo.groupFormed)
            if (wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner) {
                connectionInfoAvailable = true
                if (wifiServerService != null) {
                    startService(WifiServerService::class.java)
                }
            }
        }

        override fun onDisconnection() {
            connectionInfoAvailable = false
            log("onDisconnection")
        }

        override fun onSelfDeviceAvailable(wifiP2pDevice: WifiP2pDevice?) {
            log("onSelfDeviceAvailable")
            log(wifiP2pDevice.toString())
        }

        override fun onPeersAvailable(wifiP2pDeviceList: Collection<WifiP2pDevice>) {
            log("onPeersAvailable,size:" + wifiP2pDeviceList.size)
            for (wifiP2pDevice in wifiP2pDeviceList) {
                log(wifiP2pDevice.toString())
            }
        }

        override fun onChannelDisconnected() {
            log("onChannelDisconnected")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receive_file)
        initView()
        val mWifiP2pManager = getSystemService(WIFI_P2P_SERVICE) as? WifiP2pManager
        if (mWifiP2pManager == null) {
            finish()
            return
        }
        wifiP2pManager = mWifiP2pManager
        channel = wifiP2pManager.initialize(this, mainLooper, directActionListener)
        broadcastReceiver = DirectBroadcastReceiver(wifiP2pManager, channel, directActionListener)
        registerReceiver(broadcastReceiver, DirectBroadcastReceiver.intentFilter)
        bindService()
    }

    private fun initView() {
        setTitle("接收文件")
        ivImage = findViewById(R.id.ivImage)
        tvLog = findViewById(R.id.tvLog)
        findViewById<View>(R.id.btnCreateGroup).setOnClickListener {
            if (ActivityCompat.checkSelfPermission(
                    this@ReceiveFileActivity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return@setOnClickListener
            }
            wifiP2pManager.createGroup(channel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    log("createGroup onSuccess")
                    dismissLoadingDialog()
                    showToast("onSuccess")
                }

                override fun onFailure(reason: Int) {
                    log("createGroup onFailure: $reason")
                    dismissLoadingDialog()
                    showToast("onFailure")
                }
            })
        }
        findViewById<View>(R.id.btnRemoveGroup).setOnClickListener {
            removeGroup()
        }
        progressDialog = ProgressDialog(this).apply {
            setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            setCancelable(false)
            setCanceledOnTouchOutside(false)
            max = 100
            setTitle("正在接收文件")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (wifiServerService != null) {
            wifiServerService?.setProgressChangListener(null)
            unbindService(serviceConnection)
        }
        if (broadcastReceiver != null) {
            unregisterReceiver(broadcastReceiver)
        }
        stopService(Intent(this, WifiServerService::class.java))
        if (connectionInfoAvailable) {
            removeGroup()
        }
    }

    private fun removeGroup() {
        wifiP2pManager.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                log("removeGroup onSuccess")
                showToast("onSuccess")
            }

            override fun onFailure(reason: Int) {
                log("removeGroup onFailure")
                showToast("onFailure")
            }
        })
    }

    private fun log(log: String) {
        tvLog.append(log)
        tvLog.append("----------")
    }

    private fun bindService() {
        val intent = Intent(this@ReceiveFileActivity, WifiServerService::class.java)
        bindService(intent, serviceConnection, BIND_AUTO_CREATE)
    }
}