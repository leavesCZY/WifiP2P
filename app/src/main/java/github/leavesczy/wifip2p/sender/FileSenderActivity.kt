package github.leavesczy.wifip2p.sender

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import github.leavesczy.wifip2p.BaseActivity
import github.leavesczy.wifip2p.DeviceAdapter
import github.leavesczy.wifip2p.DirectActionListener
import github.leavesczy.wifip2p.DirectBroadcastReceiver
import github.leavesczy.wifip2p.OnItemClickListener
import github.leavesczy.wifip2p.R
import github.leavesczy.wifip2p.common.FileTransferViewState
import github.leavesczy.wifip2p.utils.WifiP2pUtils
import kotlinx.coroutines.launch

/**
 * @Author: leavesCZY
 * @Desc:
 */
@SuppressLint("NotifyDataSetChanged")
class FileSenderActivity : BaseActivity() {

    private val tvDeviceState by lazy {
        findViewById<TextView>(R.id.tvDeviceState)
    }

    private val tvConnectionStatus by lazy {
        findViewById<TextView>(R.id.tvConnectionStatus)
    }

    private val btnDisconnect by lazy {
        findViewById<Button>(R.id.btnDisconnect)
    }

    private val btnChooseFile by lazy {
        findViewById<Button>(R.id.btnChooseFile)
    }

    private val rvDeviceList by lazy {
        findViewById<RecyclerView>(R.id.rvDeviceList)
    }

    private val tvLog by lazy {
        findViewById<TextView>(R.id.tvLog)
    }

    private val btnDirectDiscover by lazy {
        findViewById<Button>(R.id.btnDirectDiscover)
    }

    private val fileSenderViewModel by viewModels<FileSenderViewModel>()

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { imageUri ->
            if (imageUri != null) {
                val ipAddress = wifiP2pInfo?.groupOwnerAddress?.hostAddress
                log(log = "getContentLaunch $imageUri $ipAddress")
                if (!ipAddress.isNullOrBlank()) {
                    fileSenderViewModel.send(ipAddress = ipAddress, fileUri = imageUri)
                }
            }
        }

    private val wifiP2pDeviceList = mutableListOf<WifiP2pDevice>()

    private val deviceAdapter = DeviceAdapter(wifiP2pDeviceList)

    private var broadcastReceiver: BroadcastReceiver? = null

    private val wifiP2pManager: WifiP2pManager by lazy {
        getSystemService(WIFI_P2P_SERVICE) as WifiP2pManager
    }

    private lateinit var wifiP2pChannel: WifiP2pManager.Channel

    private var wifiP2pInfo: WifiP2pInfo? = null

    private var wifiP2pEnabled = false

    private val directActionListener = object : DirectActionListener {

        override fun wifiP2pEnabled(enabled: Boolean) {
            wifiP2pEnabled = enabled
        }

        override fun onConnectionInfoAvailable(wifiP2pInfo: WifiP2pInfo) {
            dismissLoadingDialog()
            showDeviceList(devices = emptyList())
            btnDisconnect.isEnabled = true
            btnChooseFile.isEnabled = true
            log(log = buildString {
                append("onConnectionInfoAvailable")
                append("\n")
                append("groupFormed: " + wifiP2pInfo.groupFormed)
                append("\n")
                append("isGroupOwner: " + wifiP2pInfo.isGroupOwner)
                append("\n")
                append("groupOwnerAddress hostAddress: " + wifiP2pInfo.groupOwnerAddress.hostAddress)
            })
            tvConnectionStatus.text = buildString {
                append("isGroupOwner: " + wifiP2pInfo.isGroupOwner)
                append("\n")
                append("groupOwnerAddress hostAddress: " + wifiP2pInfo.groupOwnerAddress.hostAddress)
            }
            if (wifiP2pInfo.groupFormed && !wifiP2pInfo.isGroupOwner) {
                this@FileSenderActivity.wifiP2pInfo = wifiP2pInfo
            }
        }

        override fun onDisconnection() {
            log(log = "onDisconnection")
            btnDisconnect.isEnabled = false
            btnChooseFile.isEnabled = false
            showDeviceList(devices = emptyList())
            tvConnectionStatus.text = null
            wifiP2pInfo = null
            showToast(message = "处于非连接状态")
        }

        override fun onSelfDeviceAvailable(device: WifiP2pDevice) {
            val log = buildString {
                append("deviceName: " + device.deviceName)
                append("\n")
                append("deviceAddress: " + device.deviceAddress)
                append("\n")
                append(
                    "status: " + device.status + " " + WifiP2pUtils.getDeviceStatus(
                        device.status
                    )
                )
            }
            log(log = log)
            tvDeviceState.text = log
        }

        override fun onPeersAvailable(devices: List<WifiP2pDevice>) {
            log(log = "onPeersAvailable: " + devices.size)
            showDeviceList(devices = devices)
            dismissLoadingDialog()
        }

        override fun onChannelDisconnected() {
            log(log = "onChannelDisconnected")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_sender)
        initView()
        initDevice()
        initEvent()
    }

    @SuppressLint("MissingPermission")
    private fun initView() {
        supportActionBar?.title = "文件发送端"
        btnDisconnect.setOnClickListener {
            disconnect()
        }
        btnChooseFile.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }
        btnDirectDiscover.setOnClickListener {
            if (!wifiP2pEnabled) {
                showToast(message = "需要先打开Wifi")
                return@setOnClickListener
            }
            showLoadingDialog(message = "正在搜索附近设备")
            showDeviceList(devices = emptyList())
            wifiP2pManager.discoverPeers(wifiP2pChannel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    showToast(message = "discoverPeers Success")
                    dismissLoadingDialog()
                }

                override fun onFailure(reasonCode: Int) {
                    showToast(message = "discoverPeers Failure：$reasonCode")
                    dismissLoadingDialog()
                }
            })
        }
        deviceAdapter.onItemClickListener = object : OnItemClickListener {
            override fun onItemClick(position: Int) {
                val wifiP2pDevice = wifiP2pDeviceList.getOrNull(position)
                if (wifiP2pDevice != null) {
                    connect(wifiP2pDevice = wifiP2pDevice)
                }
            }
        }
        rvDeviceList.layoutManager = LinearLayoutManager(this)
        rvDeviceList.adapter = deviceAdapter
    }

    private fun showDeviceList(devices: List<WifiP2pDevice>) {
        wifiP2pDeviceList.clear()
        if (devices.isEmpty()) {
            deviceAdapter.notifyDataSetChanged()
            rvDeviceList.visibility = View.GONE
        } else {
            wifiP2pDeviceList.addAll(elements = devices)
            deviceAdapter.notifyDataSetChanged()
            rvDeviceList.visibility = View.VISIBLE
        }
    }

    private fun initDevice() {
        wifiP2pChannel = wifiP2pManager.initialize(this, mainLooper, directActionListener)
        broadcastReceiver =
            DirectBroadcastReceiver(wifiP2pManager, wifiP2pChannel, directActionListener)
        ContextCompat.registerReceiver(
            this,
            broadcastReceiver,
            DirectBroadcastReceiver.getIntentFilter(),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private fun initEvent() {
        lifecycleScope.launch {
            fileSenderViewModel.fileTransferViewState.collect {
                when (it) {
                    FileTransferViewState.Idle -> {
                        clearLog()
                        dismissLoadingDialog()
                    }

                    FileTransferViewState.Connecting -> {
                        showLoadingDialog(message = "")
                    }

                    is FileTransferViewState.Receiving -> {
                        showLoadingDialog(message = "")
                    }

                    is FileTransferViewState.Success -> {
                        dismissLoadingDialog()
                    }

                    is FileTransferViewState.Failed -> {
                        dismissLoadingDialog()
                    }
                }
            }
        }
        lifecycleScope.launch {
            fileSenderViewModel.log.collect {
                log(log = it)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (broadcastReceiver != null) {
            unregisterReceiver(broadcastReceiver)
        }
    }

    @SuppressLint("MissingPermission")
    private fun connect(wifiP2pDevice: WifiP2pDevice) {
        val wifiP2pConfig = WifiP2pConfig()
        wifiP2pConfig.deviceAddress = wifiP2pDevice.deviceAddress
        wifiP2pConfig.wps.setup = WpsInfo.PBC
        showLoadingDialog(message = "正在连接，deviceName: " + wifiP2pDevice.deviceName)
        showToast(message = "正在连接，deviceName: " + wifiP2pDevice.deviceName)
        wifiP2pManager.connect(
            wifiP2pChannel, wifiP2pConfig,
            object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    log(log = "connect onSuccess")
                }

                override fun onFailure(reason: Int) {
                    showToast(message = "连接失败 $reason")
                    dismissLoadingDialog()
                }
            }
        )
    }

    private fun disconnect() {
        wifiP2pManager.cancelConnect(wifiP2pChannel, object : WifiP2pManager.ActionListener {
            override fun onFailure(reasonCode: Int) {
                log(log = "cancelConnect onFailure:$reasonCode")
            }

            override fun onSuccess() {
                log(log = "cancelConnect onSuccess")
                tvConnectionStatus.text = null
                btnDisconnect.isEnabled = false
                btnChooseFile.isEnabled = false
            }
        })
        wifiP2pManager.removeGroup(wifiP2pChannel, null)
    }

    private fun log(log: String) {
        tvLog.append(log)
        tvLog.append("\n")
    }

    private fun clearLog() {
        tvLog.text = ""
    }

}