package github.leavesczy.wifip2p

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import github.leavesczy.wifip2p.service.DirectActionListener
import github.leavesczy.wifip2p.service.DirectBroadcastReceiver
import github.leavesczy.wifip2p.service.WifiClientTask
import github.leavesczy.wifip2p.utils.WifiP2pUtils

/**
 * @Author: leavesCZY
 * @Desc:
 */
class SendFileActivity : BaseActivity() {

    private val getContentLaunch = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { imageUri ->
        if (imageUri != null) {
            log("文件路径：$imageUri")
            if (wifiP2pInfo != null) {
                WifiClientTask(this).execute(
                    wifiP2pInfo!!.groupOwnerAddress.hostAddress,
                    imageUri
                )
            }
        }
    }

    private lateinit var wifiP2pManager: WifiP2pManager

    private var channel: WifiP2pManager.Channel? = null

    private var wifiP2pInfo: WifiP2pInfo? = null

    private var wifiP2pEnabled = false

    private val wifiP2pDeviceList = mutableListOf<WifiP2pDevice>()

    private lateinit var deviceAdapter: DeviceAdapter

    private lateinit var tvMyDeviceName: TextView

    private lateinit var tvMyDeviceAddress: TextView

    private lateinit var tvMyDeviceStatus: TextView

    private lateinit var tvStatus: TextView

    private lateinit var btnDisconnect: Button

    private lateinit var btnChooseFile: Button

    private var broadcastReceiver: BroadcastReceiver? = null

    private var mWifiP2pDevice: WifiP2pDevice? = null

    private val directActionListener: DirectActionListener = object : DirectActionListener {

        override fun wifiP2pEnabled(enabled: Boolean) {
            wifiP2pEnabled = enabled
        }

        override fun onConnectionInfoAvailable(wifiP2pInfo: WifiP2pInfo) {
            dismissLoadingDialog()
            wifiP2pDeviceList.clear()
            deviceAdapter.notifyDataSetChanged()
            btnDisconnect.isEnabled = true
            btnChooseFile.isEnabled = true
            log("onConnectionInfoAvailable")
            log("onConnectionInfoAvailable groupFormed: " + wifiP2pInfo.groupFormed)
            log("onConnectionInfoAvailable isGroupOwner: " + wifiP2pInfo.isGroupOwner)
            log(
                "onConnectionInfoAvailable getHostAddress: " + wifiP2pInfo.groupOwnerAddress.hostAddress
            )
            val stringBuilder = StringBuilder()
            if (mWifiP2pDevice != null) {
                stringBuilder.append("连接的设备名：")
                stringBuilder.append(mWifiP2pDevice!!.deviceName)
                stringBuilder.append("\n")
                stringBuilder.append("连接的设备的地址：")
                stringBuilder.append(mWifiP2pDevice!!.deviceAddress)
            }
            stringBuilder.append("\n")
            stringBuilder.append("是否群主：")
            stringBuilder.append(if (wifiP2pInfo.isGroupOwner) "是群主" else "非群主")
            stringBuilder.append("\n")
            stringBuilder.append("群主IP地址：")
            stringBuilder.append(wifiP2pInfo.groupOwnerAddress.hostAddress)
            tvStatus.text = stringBuilder
            if (wifiP2pInfo.groupFormed && !wifiP2pInfo.isGroupOwner) {
                this@SendFileActivity.wifiP2pInfo = wifiP2pInfo
            }
        }

        override fun onDisconnection() {
            log("onDisconnection")
            btnDisconnect.isEnabled = false
            btnChooseFile.isEnabled = false
            showToast("处于非连接状态")
            wifiP2pDeviceList.clear()
            deviceAdapter.notifyDataSetChanged()
            tvStatus.text = null
            wifiP2pInfo = null
        }

        override fun onSelfDeviceAvailable(wifiP2pDevice: WifiP2pDevice?) {
            log("onSelfDeviceAvailable")
            log("DeviceName: " + wifiP2pDevice!!.deviceName)
            log("DeviceAddress: " + wifiP2pDevice.deviceAddress)
            log("Status: " + wifiP2pDevice.status)
            tvMyDeviceName.text = wifiP2pDevice.deviceName
            tvMyDeviceAddress.text = wifiP2pDevice.deviceAddress
            tvMyDeviceStatus.text = WifiP2pUtils.getDeviceStatus(wifiP2pDevice.status)
        }

        override fun onPeersAvailable(wifiP2pDeviceList: Collection<WifiP2pDevice>) {
            log("onPeersAvailable :" + wifiP2pDeviceList.size)
            this@SendFileActivity.wifiP2pDeviceList.clear()
            this@SendFileActivity.wifiP2pDeviceList.addAll(wifiP2pDeviceList)
            deviceAdapter.notifyDataSetChanged()
            dismissLoadingDialog()
        }

        override fun onChannelDisconnected() {
            log("onChannelDisconnected")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_file)
        initView()
        initEvent()
    }

    private fun initEvent() {
        val mWifiP2pManager = getSystemService(WIFI_P2P_SERVICE) as? WifiP2pManager
        if (mWifiP2pManager == null) {
            finish()
            return
        }
        wifiP2pManager = mWifiP2pManager
        channel = mWifiP2pManager.initialize(this, mainLooper, directActionListener)
        broadcastReceiver = DirectBroadcastReceiver(mWifiP2pManager, channel, directActionListener)
        registerReceiver(broadcastReceiver, DirectBroadcastReceiver.intentFilter)
    }

    private fun initView() {
        val clickListener = View.OnClickListener { v: View ->
            val id = v.id
            if (id == R.id.btnDisconnect) {
                disconnect()
            } else if (id == R.id.btnChooseFile) {
                getContentLaunch.launch("image/*")
            }
        }
        setTitle("发送文件")
        tvMyDeviceName = findViewById(R.id.tvMyDeviceName)
        tvMyDeviceAddress = findViewById(R.id.tvMyDeviceAddress)
        tvMyDeviceStatus = findViewById(R.id.tvMyDeviceStatus)
        tvStatus = findViewById(R.id.tvStatus)
        btnDisconnect = findViewById(R.id.btnDisconnect)
        btnChooseFile = findViewById(R.id.btnChooseFile)
        btnDisconnect.setOnClickListener(clickListener)
        btnChooseFile.setOnClickListener(clickListener)
        val rvDeviceList = findViewById<RecyclerView>(R.id.rvDeviceList)
        deviceAdapter = DeviceAdapter(wifiP2pDeviceList)
        deviceAdapter.setClickListener(object : DeviceAdapter.OnClickListener {
            override fun onItemClick(position: Int) {
                mWifiP2pDevice = wifiP2pDeviceList[position]
                showToast(mWifiP2pDevice!!.deviceName)
                connect()
            }
        })
        rvDeviceList.adapter = deviceAdapter
        rvDeviceList.layoutManager = LinearLayoutManager(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (broadcastReceiver != null) {
            unregisterReceiver(broadcastReceiver)
        }
    }

    private fun connect() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            showToast("请先授予位置权限")
            return
        }
        val config = WifiP2pConfig()
        if (config.deviceAddress != null && mWifiP2pDevice != null) {
            config.deviceAddress = mWifiP2pDevice!!.deviceAddress
            config.wps.setup = WpsInfo.PBC
            showLoadingDialog("正在连接 " + mWifiP2pDevice!!.deviceName)
            wifiP2pManager.connect(channel, config, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    log("connect onSuccess")
                }

                override fun onFailure(reason: Int) {
                    showToast("连接失败 $reason")
                    dismissLoadingDialog()
                }
            })
        }
    }

    private fun disconnect() {
        wifiP2pManager.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onFailure(reasonCode: Int) {
                log("disconnect onFailure:$reasonCode")
            }

            override fun onSuccess() {
                log("disconnect onSuccess")
                tvStatus.text = null
                btnDisconnect.isEnabled = false
                btnChooseFile.isEnabled = false
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.action, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.menuDirectEnable) {
            if (wifiP2pManager != null && channel != null) {
                startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
            } else {
                showToast("当前设备不支持Wifi Direct")
            }
            return true
        } else if (id == R.id.menuDirectDiscover) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                showToast("请先授予位置权限")
                return true
            }
            if (!wifiP2pEnabled) {
                showToast("需要先打开Wifi")
                return true
            }
            showLoadingDialog("正在搜索附近设备")
            wifiP2pDeviceList.clear()
            deviceAdapter.notifyDataSetChanged()
            //搜寻附近带有 Wi-Fi P2P 的设备
            wifiP2pManager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    showToast("Success")
                }

                override fun onFailure(reasonCode: Int) {
                    showToast("Failure")
                    dismissLoadingDialog()
                }
            })
            return true
        }
        return true
    }

}