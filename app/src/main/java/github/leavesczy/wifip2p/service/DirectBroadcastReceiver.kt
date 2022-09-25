package github.leavesczy.wifip2p.service

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log
import androidx.core.app.ActivityCompat

/**
 * @Author: leavesCZY
 * @Desc:
 */
class DirectBroadcastReceiver(
    private val mWifiP2pManager: WifiP2pManager,
    private val mChannel: WifiP2pManager.Channel?,
    private val mDirectActionListener: DirectActionListener
) : BroadcastReceiver() {

    companion object {

        private const val TAG = "DirectBroadcastReceiver"

        val intentFilter: IntentFilter
            get() {
                val intentFilter = IntentFilter()
                intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
                intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
                intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
                intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
                return intentFilter
            }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != null) {
            when (action) {
                WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                    val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -100)
                    if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                        mDirectActionListener.wifiP2pEnabled(true)
                    } else {
                        mDirectActionListener.wifiP2pEnabled(false)
                        val wifiP2pDeviceList: List<WifiP2pDevice> = ArrayList()
                        mDirectActionListener.onPeersAvailable(wifiP2pDeviceList)
                    }
                }

                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    if (ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return
                    }
                    mWifiP2pManager.requestPeers(mChannel) { peers: WifiP2pDeviceList ->
                        mDirectActionListener.onPeersAvailable(
                            peers.deviceList
                        )
                    }
                }

                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    val networkInfo =
                        intent.getParcelableExtra<NetworkInfo>(WifiP2pManager.EXTRA_NETWORK_INFO)
                    if (networkInfo != null && networkInfo.isConnected) {
                        mWifiP2pManager.requestConnectionInfo(mChannel) { info ->
                            mDirectActionListener.onConnectionInfoAvailable(
                                info
                            )
                        }
                        Log.e(TAG, "已连接p2p设备")
                    } else {
                        mDirectActionListener.onDisconnection()
                        Log.e(TAG, "与p2p设备已断开连接")
                    }
                }

                WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                    val wifiP2pDevice =
                        intent.getParcelableExtra<WifiP2pDevice>(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)
                    mDirectActionListener.onSelfDeviceAvailable(wifiP2pDevice)
                }
            }
        }
    }

}