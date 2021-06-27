package com.setting;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import androidx.appcompat.app.AlertDialog;

public class network {
    private static network networkUtil;
    public static network getInstance(){
        if (networkUtil==null){
            networkUtil = new network();
        }
        return networkUtil;
    }
    public boolean isNetworkAvailable(Context context){
        ConnectivityManager manager = (ConnectivityManager)context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager == null){
            return false;
        }
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isAvailable()) {
            return false;
        }
        return true;
    }
    public void showSetNetworkUI(final Context context) { // 提示对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("网络设置提示").setMessage("网络连接不可用,是否进行设置?").
                setPositiveButton("设置", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) { // TODO Auto-generated method stub
                        Intent intent = null;                      // 判断手机系统的版本 即API大于10 就是3.0或以上版本
                        if (android.os.Build.VERSION.SDK_INT > 10) {
                            intent = new Intent( android.provider.Settings.ACTION_WIFI_SETTINGS);
                        } else {
                            intent = new Intent();
                            ComponentName component = new ComponentName( "com.android.settings","com.android.settings.WirelessSettings");
                            intent.setComponent(component);intent.setAction("android.intent.action.VIEW");}
                        context.startActivity(intent);}
                }).setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                //MainActivity.this.finish();
                //System.exit(0);
            }
        }).show();
    }
}
