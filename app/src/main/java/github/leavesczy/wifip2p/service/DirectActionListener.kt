package github.leavesczy.wifip2p.service

import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager.ChannelListener

/**
 * @Author: leavesCZY
 * @Desc:
 */
interface DirectActionListener : ChannelListener {
    fun wifiP2pEnabled(enabled: Boolean)
    fun onConnectionInfoAvailable(wifiP2pInfo: WifiP2pInfo)
    fun onDisconnection()
    fun onSelfDeviceAvailable(wifiP2pDevice: WifiP2pDevice?)
    fun onPeersAvailable(wifiP2pDeviceList: Collection<WifiP2pDevice>)
}