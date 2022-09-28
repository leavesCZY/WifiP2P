package github.leavesczy.wifip2p.sender

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import github.leavesczy.wifip2p.BaseActivity
import github.leavesczy.wifip2p.DeviceAdapter
import github.leavesczy.wifip2p.DirectActionListener
import github.leavesczy.wifip2p.DirectBroadcastReceiver
import github.leavesczy.wifip2p.Logger
import github.leavesczy.wifip2p.OnItemClickListener
import github.leavesczy.wifip2p.R
import github.leavesczy.wifip2p.utils.WifiP2pUtils

/**
 * @Author: leavesCZY
 * @Desc:
 */
@SuppressLint("NotifyDataSetChanged")
class FileSenderActivity : BaseActivity() {

    private val tvMyDeviceName by lazy {
        findViewById<TextView>(R.id.tvMyDeviceName)
    }

    private val tvMyDeviceAddress by lazy {
        findViewById<TextView>(R.id.tvMyDeviceAddress)
    }

    private val tvMyDeviceStatus by lazy {
        findViewById<TextView>(R.id.tvMyDeviceStatus)
    }

    private val tvStatus by lazy {
        findViewById<TextView>(R.id.tvStatus)
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

    private val btnDirectDiscover by lazy {
        findViewById<Button>(R.id.btnDirectDiscover)
    }

    private val fileSenderViewModel by viewModels<FileSenderViewModel>()

    private val getContentLaunch = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { imageUri ->
        if (imageUri != null) {
            val ipAddress = wifiP2pInfo?.groupOwnerAddress?.hostAddress
            Logger.log("getContentLaunch $imageUri $ipAddress")
            if (!ipAddress.isNullOrBlank()) {
                fileSenderViewModel.send(ipAddress = ipAddress, fileUri = imageUri)
            }
        }
    }

    private val wifiP2pDeviceList = mutableListOf<WifiP2pDevice>()

    private val deviceAdapter = DeviceAdapter(wifiP2pDeviceList)

    private var broadcastReceiver: BroadcastReceiver? = null

    private lateinit var wifiP2pManager: WifiP2pManager

    private lateinit var wifiP2pChannel: WifiP2pManager.Channel

    private var wifiP2pInfo: WifiP2pInfo? = null

    private var wifiP2pEnabled = false

    private val directActionListener = object : DirectActionListener {

        override fun wifiP2pEnabled(enabled: Boolean) {
            wifiP2pEnabled = enabled
        }

        override fun onConnectionInfoAvailable(wifiP2pInfo: WifiP2pInfo) {
            dismissLoadingDialog()
            wifiP2pDeviceList.clear()
            deviceAdapter.notifyDataSetChanged()
            btnDisconnect.isEnabled = true
            btnChooseFile.isEnabled = true
            Logger.log("onConnectionInfoAvailable")
            Logger.log("onConnectionInfoAvailable groupFormed: " + wifiP2pInfo.groupFormed)
            Logger.log("onConnectionInfoAvailable isGroupOwner: " + wifiP2pInfo.isGroupOwner)
            Logger.log(
                "onConnectionInfoAvailable getHostAddress: " + wifiP2pInfo.groupOwnerAddress.hostAddress
            )
            val stringBuilder = StringBuilder()
            stringBuilder.append("\n")
            stringBuilder.append("是否群主：")
            stringBuilder.append(if (wifiP2pInfo.isGroupOwner) "是群主" else "非群主")
            stringBuilder.append("\n")
            stringBuilder.append("群主IP地址：")
            stringBuilder.append(wifiP2pInfo.groupOwnerAddress.hostAddress)
            tvStatus.text = stringBuilder
            if (wifiP2pInfo.groupFormed && !wifiP2pInfo.isGroupOwner) {
                this@FileSenderActivity.wifiP2pInfo = wifiP2pInfo
            }
        }

        override fun onDisconnection() {
            Logger.log("onDisconnection")
            btnDisconnect.isEnabled = false
            btnChooseFile.isEnabled = false
            showToast("处于非连接状态")
            wifiP2pDeviceList.clear()
            deviceAdapter.notifyDataSetChanged()
            tvStatus.text = null
            wifiP2pInfo = null
        }

        override fun onSelfDeviceAvailable(wifiP2pDevice: WifiP2pDevice) {
            Logger.log("onSelfDeviceAvailable")
            Logger.log("DeviceName: " + wifiP2pDevice.deviceName)
            Logger.log("DeviceAddress: " + wifiP2pDevice.deviceAddress)
            Logger.log("Status: " + wifiP2pDevice.status)
            tvMyDeviceName.text = wifiP2pDevice.deviceName
            tvMyDeviceAddress.text = wifiP2pDevice.deviceAddress
            tvMyDeviceStatus.text = WifiP2pUtils.getDeviceStatus(wifiP2pDevice.status)
        }

        override fun onPeersAvailable(wifiP2pDeviceList: Collection<WifiP2pDevice>) {
            Logger.log("onPeersAvailable :" + wifiP2pDeviceList.size)
            this@FileSenderActivity.wifiP2pDeviceList.clear()
            this@FileSenderActivity.wifiP2pDeviceList.addAll(wifiP2pDeviceList)
            deviceAdapter.notifyDataSetChanged()
            dismissLoadingDialog()
        }

        override fun onChannelDisconnected() {
            Logger.log("onChannelDisconnected")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_sender)
        initView()
        initDevice()
    }

    @SuppressLint("MissingPermission")
    private fun initView() {
        supportActionBar?.title = "文件发送端"
        btnDisconnect.setOnClickListener {
            disconnect()
        }
        btnChooseFile.setOnClickListener {
            getContentLaunch.launch("image/*")
        }
        btnDirectDiscover.setOnClickListener {
            if (!wifiP2pEnabled) {
                showToast("需要先打开Wifi")
                return@setOnClickListener
            }
            showLoadingDialog(message = "正在搜索附近设备")
            wifiP2pDeviceList.clear()
            deviceAdapter.notifyDataSetChanged()
            wifiP2pManager.discoverPeers(wifiP2pChannel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    showToast("discoverPeers Success")
                    dismissLoadingDialog()
                }

                override fun onFailure(reasonCode: Int) {
                    showToast("discoverPeers Failure：$reasonCode")
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
        rvDeviceList.adapter = deviceAdapter
        rvDeviceList.layoutManager = LinearLayoutManager(this)
    }

    private fun initDevice() {
        val mWifiP2pManager = getSystemService(WIFI_P2P_SERVICE) as? WifiP2pManager
        if (mWifiP2pManager == null) {
            finish()
            return
        }
        wifiP2pManager = mWifiP2pManager
        wifiP2pChannel = mWifiP2pManager.initialize(this, mainLooper, directActionListener)
        broadcastReceiver =
            DirectBroadcastReceiver(mWifiP2pManager, wifiP2pChannel, directActionListener)
        registerReceiver(broadcastReceiver, DirectBroadcastReceiver.getIntentFilter())
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
        showToast("正在连接，deviceName: " + wifiP2pDevice.deviceName)
        wifiP2pManager.connect(wifiP2pChannel, wifiP2pConfig,
            object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Logger.log("connect onSuccess")
                }

                override fun onFailure(reason: Int) {
                    showToast("连接失败 $reason")
                    dismissLoadingDialog()
                }
            })
    }

    private fun disconnect() {
        wifiP2pManager.removeGroup(wifiP2pChannel, object : WifiP2pManager.ActionListener {
            override fun onFailure(reasonCode: Int) {
                Logger.log("disconnect onFailure:$reasonCode")
            }

            override fun onSuccess() {
                Logger.log("disconnect onSuccess")
                tvStatus.text = null
                btnDisconnect.isEnabled = false
                btnChooseFile.isEnabled = false
            }
        })
    }

}