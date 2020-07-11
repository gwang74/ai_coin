package com.android.jcc.aicoin;

import android.app.Application;
import android.content.Context;
//import android.support.multidex.MultiDex;

import com.android.jcc.aicoin.Receiver.NetStateChangeReceiver;

public class MyApplication extends Application {

    private static MyApplication mApp;

    public static MyApplication getInstance() {
        return mApp;
    }

    private boolean isZip;
    private boolean isNotification;
    private boolean isForceUp;
    private boolean isWithdraw;
    private String userAgent;
    private String fileName;
    private long downloadId;
    private String url;
    private String contentDisposition;
    private String mimetype;

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return this.url;
    }


    public void setContentDisposition(String url) {
        this.contentDisposition = url;
    }

    public String getContentDisposition() {
        return this.contentDisposition;
    }


    public void setMimetype(String url) {
        this.mimetype = url;
    }

    public String getMimetype() {
        return this.mimetype;
    }


    public boolean isZip() {
        return isZip;
    }

    public void setZip(boolean zip) {
        isZip = zip;
    }

    public boolean isNotification() {
        return isNotification;
    }

    public void setNotification(boolean notification) {
        isNotification = notification;
    }

    public boolean isForceUp() {
        return isForceUp;
    }

    public void setForceUp(boolean forceUp) {
        isForceUp = forceUp;
    }

    public boolean isWithdraw() {
        return isWithdraw;
    }

    public void setWithdraw(boolean withdraw) {
        isWithdraw = withdraw;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getDownloadId() {
        return downloadId;
    }

    public void setDownloadId(long downloadId) {
        this.downloadId = downloadId;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mApp = this;
        isForceUp = false;
        isWithdraw = false;
        isZip = false;
        isNotification = false;
        NetStateChangeReceiver.registerNetworkStateReceiver(this);//初始化网络监听
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
//        MultiDex.install(this);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        NetStateChangeReceiver.unRegisterNetworkStateReceiver(this);
        android.os.Process.killProcess(android.os.Process.myPid());
    }
}
