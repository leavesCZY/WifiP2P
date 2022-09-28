package github.leavesczy.wifip2p

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager

/**
 * @Author: CZY
 * @Date: 2022/9/28 14:24
 * @Desc:
 */
interface DirectActionListener : WifiP2pManager.ChannelListener {

    fun wifiP2pEnabled(enabled: Boolean)

    fun onConnectionInfoAvailable(wifiP2pInfo: WifiP2pInfo)

    fun onDisconnection()

    fun onSelfDeviceAvailable(wifiP2pDevice: WifiP2pDevice)

    fun onPeersAvailable(wifiP2pDeviceList: Collection<WifiP2pDevice>)

}

class DirectBroadcastReceiver(
    private val wifiP2pManager: WifiP2pManager,
    private val wifiP2pChannel: WifiP2pManager.Channel,
    private val directActionListener: DirectActionListener
) : BroadcastReceiver() {

    companion object {

        fun getIntentFilter(): IntentFilter {
            val intentFilter = IntentFilter()
            intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
            return intentFilter
        }

    }

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                val enabled = intent.getIntExtra(
                    WifiP2pManager.EXTRA_WIFI_STATE,
                    -1
                ) == WifiP2pManager.WIFI_P2P_STATE_ENABLED
                directActionListener.wifiP2pEnabled(enabled)
                if (!enabled) {
                    directActionListener.onPeersAvailable(emptyList())
                }
                Logger.log("WIFI_P2P_STATE_CHANGED_ACTION： $enabled")
            }

            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                Logger.log("WIFI_P2P_PEERS_CHANGED_ACTION")
                wifiP2pManager.requestPeers(wifiP2pChannel) { peers ->
                    directActionListener.onPeersAvailable(
                        peers.deviceList
                    )
                }
            }

            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                val networkInfo =
                    intent.getParcelableExtra<NetworkInfo>(WifiP2pManager.EXTRA_NETWORK_INFO)

                Logger.log("WIFI_P2P_CONNECTION_CHANGED_ACTION ： " + networkInfo?.isConnected)

                if (networkInfo != null && networkInfo.isConnected) {
                    wifiP2pManager.requestConnectionInfo(wifiP2pChannel) { info ->
                        directActionListener.onConnectionInfoAvailable(info)
                    }
                    Logger.log("已连接 P2P 设备")
                } else {
                    directActionListener.onDisconnection()
                    Logger.log("与 P2P 设备已断开连接")
                }
            }

            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                val wifiP2pDevice =
                    intent.getParcelableExtra<WifiP2pDevice>(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)
                if (wifiP2pDevice != null) {
                    directActionListener.onSelfDeviceAvailable(wifiP2pDevice)
                }
                Logger.log("WIFI_P2P_THIS_DEVICE_CHANGED_ACTION ： ${wifiP2pDevice.toString()}")
            }
        }
    }

}