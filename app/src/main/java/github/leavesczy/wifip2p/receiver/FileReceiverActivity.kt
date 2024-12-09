package github.leavesczy.wifip2p.receiver

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.graphics.BitmapFactory
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import github.leavesczy.wifip2p.BaseActivity
import github.leavesczy.wifip2p.DirectActionListener
import github.leavesczy.wifip2p.DirectBroadcastReceiver
import github.leavesczy.wifip2p.R
import github.leavesczy.wifip2p.common.FileTransferViewState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

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

    private val wifiP2pManager: WifiP2pManager by lazy {
        getSystemService(WIFI_P2P_SERVICE) as WifiP2pManager
    }

    private lateinit var wifiP2pChannel: WifiP2pManager.Channel

    private var broadcastReceiver: BroadcastReceiver? = null

    private val directActionListener = object : DirectActionListener {
        override fun wifiP2pEnabled(enabled: Boolean) {
            log(log = "wifiP2pEnabled: $enabled")
        }

        override fun onConnectionInfoAvailable(wifiP2pInfo: WifiP2pInfo) {
            log(
                log = "onConnectionInfoAvailable " + "\n"
                        + "isGroupOwner: " + wifiP2pInfo.isGroupOwner + "\n"
                        + "groupFormed: " + wifiP2pInfo.groupFormed + "\n"
                        + "groupOwnerAddress: " + wifiP2pInfo.groupOwnerAddress.toString()
            )
        }

        override fun onDisconnection() {
            log(log = "onDisconnection")
        }

        override fun onSelfDeviceAvailable(device: WifiP2pDevice) {
            log(log = "onSelfDeviceAvailable: $device")
        }

        override fun onPeersAvailable(devices: List<WifiP2pDevice>) {
            log(log = "onPeersAvailable, size:" + devices.size)
            for (wifiP2pDevice in devices) {
                log(log = "wifiP2pDevice: $wifiP2pDevice")
            }
        }

        override fun onChannelDisconnected() {
            log(log = "onChannelDisconnected")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_receiver)
        initView()
        initDevice()
        initEvent()
        onBackPressedObserver()
    }

    private fun initView() {
        supportActionBar?.title = "文件接收端"
        btnCreateGroup.setOnClickListener {
            createGroup()
        }
        btnRemoveGroup.setOnClickListener {
            lifecycleScope.launch {
                removeGroupIfNeed()
            }
        }
        btnStartReceive.setOnClickListener {
            fileReceiverViewModel.startListener()
        }
    }

    private fun initDevice() {
        wifiP2pChannel = wifiP2pManager.initialize(this, mainLooper, directActionListener)
        broadcastReceiver = DirectBroadcastReceiver(
            wifiP2pManager = wifiP2pManager,
            wifiP2pChannel = wifiP2pChannel,
            directActionListener = directActionListener
        )
        ContextCompat.registerReceiver(
            this,
            broadcastReceiver,
            DirectBroadcastReceiver.getIntentFilter(),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private fun initEvent() {
        lifecycleScope.launch {
            launch {
                fileReceiverViewModel.fileTransferViewState.collect {
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
                            showImage(file = it.file)
                        }

                        is FileTransferViewState.Failed -> {
                            dismissLoadingDialog()
                        }
                    }
                }
            }
            launch {
                fileReceiverViewModel.log.collect {
                    log(log = it)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun createGroup() {
        lifecycleScope.launch {
            removeGroupIfNeed()
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
    }

    @SuppressLint("MissingPermission")
    private suspend fun removeGroupIfNeed() {
        return suspendCancellableCoroutine { continuation ->
            wifiP2pManager.requestGroupInfo(wifiP2pChannel) { group ->
                if (group == null) {
                    continuation.resume(value = Unit)
                } else {
                    wifiP2pManager.removeGroup(
                        wifiP2pChannel,
                        object : WifiP2pManager.ActionListener {
                            override fun onSuccess() {
                                val log = "removeGroup onSuccess"
                                log(log = log)
                                showToast(message = log)
                                continuation.resume(value = Unit)
                            }

                            override fun onFailure(reason: Int) {
                                val log = "removeGroup onFailure: $reason"
                                log(log = log)
                                showToast(message = log)
                                continuation.resume(value = Unit)
                            }
                        })
                }
            }
        }
    }

    private fun log(log: String) {
        tvLog.append(log)
        tvLog.append("\n")
    }

    private fun clearLog() {
        tvLog.text = ""
    }

    private fun showImage(file: File?) {
        if (file == null) {
            ivImage.setImageBitmap(null)
            ivImage.visibility = View.GONE
        } else {
            lifecycleScope.launch {
                val bitmap = withContext(context = Dispatchers.IO) {
                    BitmapFactory.decodeFile(file.absolutePath)
                }
                ivImage.setImageBitmap(bitmap)
                ivImage.visibility = View.VISIBLE
            }
        }
    }

    private fun onBackPressedObserver() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                lifecycleScope.launch {
                    if (broadcastReceiver != null) {
                        unregisterReceiver(broadcastReceiver)
                    }
                    removeGroupIfNeed()
                    finish()
                }
            }
        })
    }

}