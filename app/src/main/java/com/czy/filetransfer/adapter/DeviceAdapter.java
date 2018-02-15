package com.czy.filetransfer.adapter;

import android.net.wifi.p2p.WifiP2pDevice;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.czy.filetransfer.MainActivity;
import com.czy.filetransfer.R;

import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.ViewHolder> {

    private List<WifiP2pDevice> wifiP2pDeviceList;

    private OnClickListener clickListener;

    public interface OnClickListener {

        void onItemClick(int position);

    }

    public DeviceAdapter(List<WifiP2pDevice> wifiP2pDeviceList) {
        this.wifiP2pDeviceList = wifiP2pDeviceList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_device, parent, false);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (clickListener != null) {
                    clickListener.onItemClick((Integer) v.getTag());
                }
            }
        });
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.tv_deviceName.setText(wifiP2pDeviceList.get(position).deviceName);
        holder.tv_deviceAddress.setText(wifiP2pDeviceList.get(position).deviceAddress);
        holder.tv_deviceDetails.setText(MainActivity.getDeviceStatus(wifiP2pDeviceList.get(position).status));
        holder.itemView.setTag(position);
    }

    @Override
    public int getItemCount() {
        return wifiP2pDeviceList.size();
    }

    public void setClickListener(OnClickListener clickListener) {
        this.clickListener = clickListener;
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private TextView tv_deviceName;

        private TextView tv_deviceAddress;

        private TextView tv_deviceDetails;

        ViewHolder(View itemView) {
            super(itemView);
            tv_deviceName = (TextView) itemView.findViewById(R.id.tv_deviceName);
            tv_deviceAddress = (TextView) itemView.findViewById(R.id.tv_deviceAddress);
            tv_deviceDetails = (TextView) itemView.findViewById(R.id.tv_deviceDetails);
        }

    }

}
