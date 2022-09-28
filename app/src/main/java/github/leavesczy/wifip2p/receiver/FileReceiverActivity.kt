package github.leavesczy.wifip2p.receiver

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import coil.load
import github.leavesczy.wifip2p.BaseActivity
import github.leavesczy.wifip2p.DirectActionListener
import github.leavesczy.wifip2p.DirectBroadcastReceiver
import github.leavesczy.wifip2p.R
import github.leavesczy.wifip2p.models.ViewState
import kotlinx.coroutines.launch

/**
 * @Author: leavesCZY
 * @Desc:
 */
class FileReceiverActivity : BaseActivity() {

    private val ivImage by lazy {
        findViewById<ImageView>(R.id.ivImage)
    }

    private val tvLog by lazy {
        findViewById<TextView>(R.id.tvLog)
    }

    private val btnCreateGroup by lazy {
        findViewById<Button>(R.id.btnCreateGroup)
    }

    private val btnRemoveGroup by lazy {
        findViewById<Button>(R.id.btnRemoveGroup)
    }

    private val btnStartReceive by lazy {
        findViewById<Button>(R.id.btnStartReceive)
    }

    private val fileReceiverViewModel by viewModels<FileReceiverViewModel>()

    private lateinit var wifiP2pManager: WifiP2pManager

    private lateinit var wifiP2pChannel: WifiP2pManager.Channel

    private var connectionInfoAvailable = false

    private var broadcastReceiver: BroadcastReceiver? = null

    private val directActionListener = object : DirectActionListener {
        override fun wifiP2pEnabled(enabled: Boolean) {
            log("wifiP2pEnabled: $enabled")
        }

        override fun onConnectionInfoAvailable(wifiP2pInfo: WifiP2pInfo) {
            log("onConnectionInfoAvailable")
            log("isGroupOwner：" + wifiP2pInfo.isGroupOwner)
            log("groupFormed：" + wifiP2pInfo.groupFormed)
            if (wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner) {
                connectionInfoAvailable = true
            }
        }

        override fun onDisconnection() {
            connectionInfoAvailable = false
            log("onDisconnection")
        }

        override fun onSelfDeviceAvailable(wifiP2pDevice: WifiP2pDevice) {
            log("onSelfDeviceAvailable: \n$wifiP2pDevice")
        }

        override fun onPeersAvailable(wifiP2pDeviceList: Collection<WifiP2pDevice>) {
            log("onPeersAvailable , size:" + wifiP2pDeviceList.size)
            for (wifiP2pDevice in wifiP2pDeviceList) {
                log("wifiP2pDevice: $wifiP2pDevice")
            }
        }

        override fun onChannelDisconnected() {
            log("onChannelDisconnected")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_receiver)
        initView()
        initDevice()
        initEvent()
    }

    private fun initView() {
        supportActionBar?.title = "文件接收端"
        btnCreateGroup.setOnClickListener {
            createGroup()
        }
        btnRemoveGroup.setOnClickListener {
            removeGroup()
        }
        btnStartReceive.setOnClickListener {
            fileReceiverViewModel.startListener()
        }
    }

    private fun initDevice() {
        val mWifiP2pManager = getSystemService(WIFI_P2P_SERVICE) as? WifiP2pManager
        if (mWifiP2pManager == null) {
            finish()
            return
        }
        wifiP2pManager = mWifiP2pManager
        wifiP2pChannel = wifiP2pManager.initialize(this, mainLooper, directActionListener)
        broadcastReceiver = DirectBroadcastReceiver(
            wifiP2pManager = wifiP2pManager,
            wifiP2pChannel = wifiP2pChannel,
            directActionListener = directActionListener
        )
        registerReceiver(broadcastReceiver, DirectBroadcastReceiver.getIntentFilter())
    }

    private fun initEvent() {
        lifecycleScope.launch {
            fileReceiverViewModel.viewState.collect {
                when (it) {
                    ViewState.Idle -> {
                        tvLog.text = ""
                        dismissLoadingDialog()
                    }

                    ViewState.Connecting -> {
                        showLoadingDialog(message = "")
                    }

                    is ViewState.Receiving -> {
                        showLoadingDialog(message = "")
                    }

                    is ViewState.Success -> {
                        dismissLoadingDialog()
                        ivImage.load(data = it.file)
                    }

                    is ViewState.Failed -> {
                        dismissLoadingDialog()
                    }
                }
            }
        }
        lifecycleScope.launch {
            fileReceiverViewModel.log.collect {
                log(it)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (broadcastReceiver != null) {
            unregisterReceiver(broadcastReceiver)
        }
        wifiP2pManager.removeGroup(wifiP2pChannel, null)
    }

    @SuppressLint("MissingPermission")
    private fun createGroup() {
        wifiP2pManager.createGroup(wifiP2pChannel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                val log = "createGroup onSuccess"
                log(log = log)
                showToast(message = log)
            }

            override fun onFailure(reason: Int) {
                val log = "createGroup onFailure: $reason"
                log(log = log)
                showToast(message = log)
            }
        })
    }

    private fun removeGroup() {
        wifiP2pManager.removeGroup(wifiP2pChannel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                val log = "removeGroup onSuccess"
                log(log = log)
                showToast(message = log)
            }

            override fun onFailure(reason: Int) {
                val log = "removeGroup onFailure: $reason"
                log(log = log)
                showToast(message = log)
            }
        })
    }

    private fun log(log: String) {
        tvLog.append(log)
        tvLog.append("\n\n")
    }

}