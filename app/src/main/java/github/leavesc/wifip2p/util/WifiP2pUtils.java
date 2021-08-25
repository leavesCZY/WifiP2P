package github.leavesc.wifip2p.util;

import android.net.wifi.p2p.WifiP2pDevice;

/**
 * @Author: leavesC
 * @Date: 2021/8/25 10:42
 * @Desc:
 * @Github：https://github.com/leavesC
 */
public class WifiP2pUtils {

    public static String getDeviceStatus(int deviceStatus) {
        switch (deviceStatus) {
            case WifiP2pDevice.AVAILABLE:
                return "可用的";
            case WifiP2pDevice.INVITED:
                return "邀请中";
            case WifiP2pDevice.CONNECTED:
                return "已连接";
            case WifiP2pDevice.FAILED:
                return "失败的";
            case WifiP2pDevice.UNAVAILABLE:
                return "不可用的";
            default:
                return "未知";
        }
    }

}
