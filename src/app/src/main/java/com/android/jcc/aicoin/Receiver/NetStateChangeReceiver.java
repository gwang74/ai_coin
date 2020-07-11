package com.android.jcc.aicoin.Receiver;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.android.jcc.aicoin.CommonUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.List;

public class NetStateChangeReceiver extends BroadcastReceiver {

    private static final String TAG = "NetStateChangeReceiver";

    private static BroadcastReceiver mBroadcastReceiver;
    private boolean isReConnect = false;
    private long lastTime;

    private static BroadcastReceiver getReceiver() {
        if (null == mBroadcastReceiver) {
            synchronized (NetStateChangeReceiver.class) {
                if (null == mBroadcastReceiver) {
                    mBroadcastReceiver = new NetStateChangeReceiver();
                }
            }
        }
        return mBroadcastReceiver;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == ConnectivityManager.CONNECTIVITY_ACTION) {
            boolean isConnected = CommonUtils.isNetworkConnected(context);
            if (isConnected) {
                if (isReConnect) {
                    long current = System.currentTimeMillis();
                    long timeDiff = current - lastTime;
                    isReConnect = false;
                    if (timeDiff > 5000) {
                        SharedPreferences sp = context.getSharedPreferences("zipInfo", Context.MODE_PRIVATE);
                        String jsHash = sp.getString("jsHash", "");
                        try {
                            String path = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getPath();
                            File indexFile = new File(path + "/assets/update/index.html");
                            boolean res = true;
                            if (indexFile.exists() && indexFile.isFile() && !TextUtils.isEmpty(jsHash)) {
                                res = CommonUtils.checkLocalFileMD5(context, new JSONObject(jsHash));
                            }
                            Log.v(TAG, "" + res);
                            if (!res) {
                                if (isBackground(context)) {
                                    android.os.Process.killProcess(android.os.Process.myPid());
                                }
                                Intent reIntent = context.getPackageManager()
                                        .getLaunchIntentForPackage(context.getPackageName());
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                context.startActivity(reIntent);
                                android.os.Process.killProcess(android.os.Process.myPid());
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            CommonUtils.upPhoneInfo(context, e.getMessage() + "-" + e.getCause());
                        }
                    }
                }
            } else {
                lastTime = System.currentTimeMillis();
                isReConnect = true;
            }
        }
    }

    /**
     * 注册
     *
     * @param mContext
     */
    public static void registerNetworkStateReceiver(Context mContext) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        mContext.getApplicationContext().registerReceiver(getReceiver(), filter);
    }

    /**
     * 反注册
     *
     * @param mContext
     */
    public static void unRegisterNetworkStateReceiver(Context mContext) {
        if (mBroadcastReceiver != null) {
            try {
                mContext.getApplicationContext().unregisterReceiver(mBroadcastReceiver);
            } catch (Exception e) {

            }
        }
    }

    // 判断程序是否在后台
    public static boolean isBackground(Context context) {
        ActivityManager activityManager = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager
                .getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.processName.equals(context.getPackageName())) {
                if (appProcess.importance != ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    return true;
                } else {
                    return false;
                }
            }
        }
        return false;
    }

}
