package github.leavesczy.wifip2p

import android.net.wifi.p2p.WifiP2pDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import github.leavesczy.wifip2p.utils.WifiP2pUtils

/**
 * @Author: leavesCZY
 * @Date: 2024/4/1 11:27
 * @Desc:
 */
interface OnItemClickListener {

    fun onItemClick(position: Int)

}

class DeviceAdapter(private val wifiP2pDeviceList: List<WifiP2pDevice>) :
    RecyclerView.Adapter<DeviceAdapter.ViewHolder>() {

    var onItemClickListener: OnItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_device, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val device = wifiP2pDeviceList[position]
        holder.tvDeviceName.text = device.deviceName
        holder.tvDeviceAddress.text = device.deviceAddress
        holder.tvDeviceDetails.text = WifiP2pUtils.getDeviceStatus(deviceStatus = device.status)
        holder.itemView.setOnClickListener {
            onItemClickListener?.onItemClick(position = position)
        }
    }

    override fun getItemCount(): Int {
        return wifiP2pDeviceList.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val tvDeviceName: TextView

        val tvDeviceAddress: TextView

        val tvDeviceDetails: TextView

        init {
            tvDeviceName = itemView.findViewById(R.id.tvDeviceName)
            tvDeviceAddress = itemView.findViewById(R.id.tvDeviceAddress)
            tvDeviceDetails = itemView.findViewById(R.id.tvDeviceDetails)
        }
    }

}