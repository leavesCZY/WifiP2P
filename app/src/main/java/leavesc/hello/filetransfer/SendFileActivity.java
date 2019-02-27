package leavesc.hello.filetransfer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import leavesc.hello.filetransfer.adapter.DeviceAdapter;
import leavesc.hello.filetransfer.broadcast.DirectBroadcastReceiver;
import leavesc.hello.filetransfer.callback.DirectActionListener;
import leavesc.hello.filetransfer.common.LoadingDialog;
import leavesc.hello.filetransfer.model.FileTransfer;
import leavesc.hello.filetransfer.task.WifiClientTask;

/**
 * 作者：leavesC
 * 时间：2019/2/27 23:52
 * 描述：
 * GitHub：https://github.com/leavesC
 * Blog：https://www.jianshu.com/u/9df45b87cfdf
 */
public class SendFileActivity extends BaseActivity implements DirectActionListener {

    public static final String TAG = "SendFileActivity";

    private WifiP2pManager mWifiP2pManager;

    private boolean mWifiP2pEnabled = false;

    private WifiP2pManager.Channel mChannel;

    private TextView tv_myDeviceName;

    private TextView tv_myDeviceAddress;

    private TextView tv_myDeviceStatus;

    private TextView tv_status;

    private TextView tv_fileList;

    private List<WifiP2pDevice> wifiP2pDeviceList;

    private DeviceAdapter deviceAdapter;

    private Button btn_disconnect;

    private Button btn_chooseFile;

    private LoadingDialog loadingDialog;

    private BroadcastReceiver broadcastReceiver;

    private WifiP2pDevice mWifiP2pDevice;

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_disconnect: {
                    disconnect();
                    break;
                }
                case R.id.btn_chooseFile: {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("*/*");
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    startActivityForResult(intent, 1);
                    break;
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_file);
        initView();
        mWifiP2pManager = (WifiP2pManager) getSystemService(WIFI_P2P_SERVICE);
        mChannel = mWifiP2pManager.initialize(this, getMainLooper(), this);
        broadcastReceiver = new DirectBroadcastReceiver(mWifiP2pManager, mChannel, this);
        registerReceiver(broadcastReceiver, DirectBroadcastReceiver.getIntentFilter());
    }

    private void initView() {
        tv_myDeviceName = findViewById(R.id.tv_myDeviceName);
        tv_myDeviceAddress = findViewById(R.id.tv_myDeviceAddress);
        tv_myDeviceStatus = findViewById(R.id.tv_myDeviceStatus);
        tv_status = findViewById(R.id.tv_status);
        tv_fileList = findViewById(R.id.tv_fileList);
        btn_disconnect = findViewById(R.id.btn_disconnect);
        btn_chooseFile = findViewById(R.id.btn_chooseFile);
        btn_disconnect.setOnClickListener(clickListener);
        btn_chooseFile.setOnClickListener(clickListener);
        loadingDialog = new LoadingDialog(this);
        RecyclerView rv_deviceList = findViewById(R.id.rv_deviceList);
        wifiP2pDeviceList = new ArrayList<>();
        deviceAdapter = new DeviceAdapter(wifiP2pDeviceList);
        deviceAdapter.setClickListener(new DeviceAdapter.OnClickListener() {
            @Override
            public void onItemClick(int position) {
                mWifiP2pDevice = wifiP2pDeviceList.get(position);
                showToast(wifiP2pDeviceList.get(position).deviceName);
                connect();
            }
        });
        rv_deviceList.setAdapter(deviceAdapter);
        rv_deviceList.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                Uri uri = data.getData();
                if (uri != null) {
                    String path = getPath(this, uri);
                    if (path != null) {
                        File file = new File(path);
                        if (file.exists() && wifiP2pInfo != null) {
                            FileTransfer fileTransfer = new FileTransfer(file.getPath(), file.length());
                            Log.e(TAG, "待发送的文件：" + fileTransfer);
                            new WifiClientTask(this, fileTransfer).execute(wifiP2pInfo.groupOwnerAddress.getHostAddress());
                        }
                    }
                }
            }
        }
    }

    private void connect() {
        WifiP2pConfig config = new WifiP2pConfig();
        if (config.deviceAddress != null && mWifiP2pDevice != null) {
            config.deviceAddress = mWifiP2pDevice.deviceAddress;
            config.wps.setup = WpsInfo.PBC;
            showLoadingDialog("正在连接 " + mWifiP2pDevice.deviceName);
            mWifiP2pManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.e(TAG, "connect onSuccess");
                }

                @Override
                public void onFailure(int reason) {
                    showToast("连接失败 " + reason);
                    dismissLoadingDialog();
                }
            });
        }
    }

    private void disconnect() {
        mWifiP2pManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onFailure(int reasonCode) {
                Log.e(TAG, "disconnect onFailure:" + reasonCode);
            }

            @Override
            public void onSuccess() {
                Log.e(TAG, "disconnect onSuccess");
                tv_status.setText(null);
                btn_disconnect.setEnabled(false);
                btn_chooseFile.setEnabled(false);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.action, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuDirectEnable: {
                if (mWifiP2pManager != null && mChannel != null) {
                    startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));
                } else {
                    showToast("当前设备不支持Wifi Direct");
                }
                return true;
            }
            case R.id.menuDirectDiscover: {
                if (!mWifiP2pEnabled) {
                    showToast("需要先打开Wifi");
                    return true;
                }
                loadingDialog.show("正在搜索附近设备", true, false);
                wifiP2pDeviceList.clear();
                deviceAdapter.notifyDataSetChanged();
                //搜寻附近带有 Wi-Fi P2P 的设备
                mWifiP2pManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        showToast("Success");
                    }

                    @Override
                    public void onFailure(int reasonCode) {
                        showToast("Failure");
                        loadingDialog.cancel();
                    }
                });
                return true;
            }
            default:
                return true;
        }
    }

    @Override
    public void wifiP2pEnabled(boolean enabled) {
        mWifiP2pEnabled = enabled;
    }

    private WifiP2pInfo wifiP2pInfo;

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
        dismissLoadingDialog();
        wifiP2pDeviceList.clear();
        deviceAdapter.notifyDataSetChanged();
        btn_disconnect.setEnabled(true);
        btn_chooseFile.setEnabled(true);
        Log.e(TAG, "onConnectionInfoAvailable");
        Log.e(TAG, "onConnectionInfoAvailable groupFormed: " + wifiP2pInfo.groupFormed);
        Log.e(TAG, "onConnectionInfoAvailable isGroupOwner: " + wifiP2pInfo.isGroupOwner);
        Log.e(TAG, "onConnectionInfoAvailable getHostAddress: " + wifiP2pInfo.groupOwnerAddress.getHostAddress());
        StringBuilder stringBuilder = new StringBuilder();
        if (mWifiP2pDevice != null) {
            stringBuilder.append("连接的设备名：");
            stringBuilder.append(mWifiP2pDevice.deviceName);
            stringBuilder.append("\n");
            stringBuilder.append("连接的设备的地址：");
            stringBuilder.append(mWifiP2pDevice.deviceAddress);
        }
        stringBuilder.append("\n");
        stringBuilder.append("是否群主：");
        stringBuilder.append(wifiP2pInfo.isGroupOwner ? "是群主" : "非群主");
        stringBuilder.append("\n");
        stringBuilder.append("群主IP地址：");
        stringBuilder.append(wifiP2pInfo.groupOwnerAddress.getHostAddress());
        tv_status.setText(stringBuilder);
        if (wifiP2pInfo.groupFormed && !wifiP2pInfo.isGroupOwner) {
            this.wifiP2pInfo = wifiP2pInfo;
        }
    }

    @Override
    public void onDisconnection() {
        Log.e(TAG, "onDisconnection");
        btn_disconnect.setEnabled(false);
        btn_chooseFile.setEnabled(false);
        showToast("已断开连接");
        wifiP2pDeviceList.clear();
        deviceAdapter.notifyDataSetChanged();
        tv_status.setText(null);
        this.wifiP2pInfo = null;
    }

    @Override
    public void onSelfDeviceAvailable(WifiP2pDevice wifiP2pDevice) {
        Log.e(TAG, "onSelfDeviceAvailable");
        Log.e(TAG, "DeviceName: " + wifiP2pDevice.deviceName);
        Log.e(TAG, "DeviceAddress: " + wifiP2pDevice.deviceAddress);
        Log.e(TAG, "Status: " + wifiP2pDevice.status);
        tv_myDeviceName.setText(wifiP2pDevice.deviceName);
        tv_myDeviceAddress.setText(wifiP2pDevice.deviceAddress);
        tv_myDeviceStatus.setText(MainActivity.getDeviceStatus(wifiP2pDevice.status));
    }

    @Override
    public void onPeersAvailable(Collection<WifiP2pDevice> wifiP2pDeviceList) {
        Log.e(TAG, "onPeersAvailable :" + wifiP2pDeviceList.size());
        this.wifiP2pDeviceList.clear();
        this.wifiP2pDeviceList.addAll(wifiP2pDeviceList);
        deviceAdapter.notifyDataSetChanged();
        loadingDialog.cancel();
    }

    @Override
    public void onChannelDisconnected() {
        Log.e(TAG, "onChannelDisconnected");
    }

    private String getPath(Context context, Uri uri) {
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            Cursor cursor = context.getContentResolver().query(uri, new String[]{"_data"}, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    String data = cursor.getString(cursor.getColumnIndex("_data"));
                    cursor.close();
                    return data;
                }
            }
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

}