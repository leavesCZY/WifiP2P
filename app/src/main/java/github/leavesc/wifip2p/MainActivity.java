package github.leavesc.wifip2p;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

/**
 * @Author: leavesC
 * @Date: 2019/2/27 23:52
 * @Desc:
 * @Github：https://github.com/leavesC
 */
public class MainActivity extends BaseActivity {

    private static final int CODE_REQ_PERMISSIONS = 665;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btnCheckPermission).setOnClickListener(v ->
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.CHANGE_NETWORK_STATE,
                                Manifest.permission.ACCESS_NETWORK_STATE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.ACCESS_WIFI_STATE,
                                Manifest.permission.CHANGE_WIFI_STATE,
                                Manifest.permission.ACCESS_FINE_LOCATION}, CODE_REQ_PERMISSIONS));
        findViewById(R.id.btnSender).setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, SendFileActivity.class)));
        findViewById(R.id.btnReceiver).setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ReceiveFileActivity.class)));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CODE_REQ_PERMISSIONS) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    showToast("缺少权限，请先授予权限: " + permissions[i]);
                    return;
                }
            }
            showToast("已获得权限");
        }
    }

}