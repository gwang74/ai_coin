package com.android.jcc.aicoin;

import android.Manifest;

public class Constant {

    public final static String ANDROID_MANIFEST_FILENAME = "AndroidManifest.xml";

    public final static String WEIDEX_URL = "file:///android_asset/index.html";

    public final static int FILE_CHOOSER_RESULT_CODE = 10000;

    public final static String NOTIFICATION_CHANNEL_ID = "my_channel_01";

    public final static int REQUEST_EXTERNAL_STORAGE = 1;

    public final static int REQUEST_EXTERNAL_STORAGE_UPDATE = 2;

    public final static int REQUEST_WEB_CAMERA = 3;

    public final static String OTC_VIP_URL = "https://devjccotc.jccdex.cn:8443/";

    public final static String OTC_VIP_HOME_URL = "https://devjccotc.jccdex.cn:8443/#/home";

    public final static String WEIDEX_VIP_URL = "http://weidex.vip/";

    public final static String LOAD_ERROR_URL = "file:///android_asset/index.html";

    public final static String LOAD_BLANK = "about:blank";

    public final static String MIANTENANCE_URL = "https://app.weidex.vip/static/error/error.html";


    public final static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE};
}
