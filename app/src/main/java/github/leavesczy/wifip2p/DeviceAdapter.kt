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
 * @Desc:
 */
class DeviceAdapter(private val wifiP2pDeviceList: List<WifiP2pDevice>) :
    RecyclerView.Adapter<DeviceAdapter.ViewHolder>() {

    private var clickListener: OnClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_device, parent, false)
        view.setOnClickListener { v: View ->
            clickListener?.onItemClick(v.tag as Int)
        }
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.tvDeviceName.text = wifiP2pDeviceList[position].deviceName
        holder.tvDeviceAddress.text = wifiP2pDeviceList[position].deviceAddress
        holder.tvDeviceDetails.text =
            WifiP2pUtils.getDeviceStatus(wifiP2pDeviceList[position].status)
        holder.itemView.tag = position
    }

    override fun getItemCount(): Int {
        return wifiP2pDeviceList.size
    }

    fun setClickListener(clickListener: OnClickListener) {
        this.clickListener = clickListener
    }

    interface OnClickListener {
        fun onItemClick(position: Int)
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