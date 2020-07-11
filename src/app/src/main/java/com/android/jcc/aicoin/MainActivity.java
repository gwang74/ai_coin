package com.android.jcc.aicoin;


import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.NinePatch;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.net.Uri;
import android.net.http.SslError;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.PermissionRequest;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.daimajia.numberprogressbar.NumberProgressBar;
import com.mob.MobSDK;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Timer;
import java.util.TimerTask;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static com.android.jcc.aicoin.CommonUtils.downloadChangeObserver;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "testMainActivity";

    private static WebView mWebView;
    private static WebView mReloadWebView;
    private static ImageView ErrImg;
    private static TextView loadText;
    public static View mView;
    private Boolean isUpdate;
    private JSONObject jsHashJson = null;
    private JSONObject updateMsg = null;
    private String latestVersionName = null;
    private int latestVersionCode;
    private int latestZipVersionCode;
    private String MD5 = null;
    private String downLoadUrl = null;
    private String dialogUrl;
    private boolean isShowAd;
    private boolean isShowDialog;
    private boolean isMaintenance;
    private boolean isOver;
    private String eventUrl;
    private String title = null;
    private String content = null;
    private JSONObject versionJson = null;
    private ValueCallback<Uri> uploadMessage;
    private ValueCallback<Uri[]> uploadMessageAboveL;
    private DownloadCompleteReceiver downloadCompleteReceiver;
    private AdvertisementDialog mAdvertisementDialog;
    private Share share;
    private Timer timer;
    private PermissionRequest request;
    private boolean isErr;
    public boolean isOptional;
    public static MyHandler myHandler;
    private NumberProgressBar numberProgressBar = null;
    public String userAgent = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
        setContentView(R.layout.activity_main);

        //初期化
        this.initView();
        myHandler = new MyHandler(this);
//        mWebView.loadUrl(Constant.WEIDEX_URL);
//        mWebView.loadUrl("http://192.168.66.253");
        if (isMaintenance) {
            isOver = true;
            try {
                mWebView.loadUrl(versionJson.getJSONObject("android").getString("maintenanceUrl"));
            } catch (JSONException e) {
                CommonUtils.upPhoneInfo(MainActivity.this, e.getMessage() + "-" + e.getCause());
                Log.v(TAG, e.getMessage());
            }
        } else {
            if (!MyApplication.getInstance().isForceUp()) {
                load();
            } else {
                isOver = true;
//                new DownloadTask().execute(MainActivity.this);
            }
        }
        share = Share.getInstance(this, title, content);
        mWebView.addJavascriptInterface(share, "Share");

        // 1. 实例化BroadcastReceiver子类 &  IntentFilter
        if (downloadCompleteReceiver == null) {
            downloadCompleteReceiver = new DownloadCompleteReceiver();
            IntentFilter intentFilter = new IntentFilter();
            // 2. 设置接收广播的类型
            intentFilter.addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
            // 3. 动态注册：调用Context的registerReceiver（）方法
            registerReceiver(downloadCompleteReceiver, intentFilter);
        }
    }

    @Override
    protected void onDestroy() {
        //销毁在onResume()方法中的广播
        unregisterReceiver(downloadCompleteReceiver);
        downloadCompleteReceiver = null;
        if (downloadChangeObserver != null) {
            getContentResolver().unregisterContentObserver(downloadChangeObserver);
        }
        //销毁WebView
        WebView webView;
        if (mWebView != null) {
            webView = mWebView;
        } else {
            webView = mReloadWebView;
        }
        if (webView != null) {
            webView.loadDataWithBaseURL(null, "", "text/html", "utf-8", null);
            webView.clearHistory();
            ((ViewGroup) webView.getParent()).removeView(webView);
            webView.destroy();
        }
        myHandler.removeCallbacksAndMessages(null);
        share = null;
        timer = null;
        super.onDestroy();
    }

    /**
     * webView初期配置
     */
    private void initView() {
        ErrImg = findViewById(R.id.errImg);
        loadText = findViewById(R.id.loadtext);
        mWebView = findViewById(R.id.mWebView);
        initWebView(mWebView);
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Log.v(TAG, "shouldOverrideUrlLoading1---->" + url);
                if (TextUtils.equals(url, Constant.MIANTENANCE_URL)) {
                    return false;
                }
                Intent intent;
                if (url.contains(Constant.OTC_VIP_URL) || url.contains("myReward")) {
                    intent = new Intent(MainActivity.this, OTCActivity.class);
                    intent.putExtra("url", url);
                } else {
                    Uri uri = Uri.parse(url);
                    intent = new Intent(Intent.ACTION_VIEW, uri);
                }
                startActivity(intent);
                return true;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                Log.v(TAG, "shouldOverrideUrlLoading2---->" + url);
                if (TextUtils.equals(url, Constant.MIANTENANCE_URL)) {
                    return false;
                }
                Intent intent;
                if (url.contains(Constant.OTC_VIP_URL) || url.contains("myReward")) {
                    intent = new Intent(MainActivity.this, OTCActivity.class);
                    intent.putExtra("url", url);
                } else {
                    Uri uri = Uri.parse(url);
                    intent = new Intent(Intent.ACTION_VIEW, uri);
                }
                startActivity(intent);
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                Log.v(TAG, "onPageStarted---->" + url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.v(TAG, "onPageFinished---->" + url);
//                if (TextUtils.equals(url, Constant.LOAD_ERROR_URL)) {
//                    ErrImg.setVisibility(View.VISIBLE);
//                    loadText.setVisibility(View.VISIBLE);
//                    mWebView.loadUrl("about:blank");// 清空当前加载
//                    isErr = true;
//                    CommonUtils.upPhoneInfo(MainActivity.this, null);
//                    return;
//                }
                if (!isOver && !TextUtils.equals(url, Constant.LOAD_BLANK)) {
                    isOver = true;
                    Log.v(TAG, "isOver----->");
//                    new DownloadTask().execute(MainActivity.this);
                }
                if (mWebView != null && mWebView.getProgress() == 100) {
                    mWebView.setVisibility(View.VISIBLE);
                    getWindow().setNavigationBarColor(Color.BLACK);
                    getWindow().setStatusBarColor(Color.BLACK);
                    if (timer == null && isShowAd && isShowDialog && !isMaintenance) {
                        timer = new Timer();
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                Message message = new Message();
                                message.what = 0;
                                myHandler.sendMessage(message);
                            }
                        }, 1000, 60000);
                    }
                }
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError
                    error) {
                handler.proceed(); // 接受所有网站的证书
            }
        });
    }

//    private ObjectAnimator alpha() {
//        float alpha = (float) loadImage.getAlpha();
//        Log.v("drop", String.valueOf(alpha));
//        ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(loadImage, "alpha", alpha, 0.3f);
//        objectAnimator.setRepeatMode(ValueAnimator.REVERSE);
//        objectAnimator.setRepeatCount(ValueAnimator.INFINITE);
//        objectAnimator.setDuration(2000);
//        return objectAnimator;
//    }
//
//    private ObjectAnimator drop() {
//        loadImage.setVisibility(View.VISIBLE);
//        loadImage.setImageDrawable(getDrawable(R.drawable.weichain_pre));
//        WindowManager manager = this.getWindowManager();
//        DisplayMetrics outMetrics = new DisplayMetrics();
//        manager.getDefaultDisplay().getMetrics(outMetrics);
//        int height = outMetrics.heightPixels;
//        float dropHeight = (float) (height * 0.23);
//        Log.v("drop", String.valueOf(dropHeight));
//        ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(loadImage, "translationY", 0f, dropHeight);
//        objectAnimator.setInterpolator(new BounceInterpolator());
//        objectAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
//            @Override
//            public void onAnimationUpdate(ValueAnimator animation) {
//                float num = (float) animation.getAnimatedValue();
//                if (num > 180f) {
//                    loadImage.setImageDrawable(getDrawable(R.drawable.weichain));
//                }
//            }
//        });
//        objectAnimator.setDuration(2500);
//        return objectAnimator;
//    }

    /**
     * 文件选择
     */
    private void openImageChooserActivity() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        String[] mimeTypes = {"image/jpeg", "image/png", "text/plain"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        startActivityForResult(Intent.createChooser(intent, "选择文件"), 10000);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constant.FILE_CHOOSER_RESULT_CODE) {
            if (null == uploadMessage && null == uploadMessageAboveL) return;
            Uri result = data == null || resultCode != RESULT_OK ? null : data.getData();
            if (uploadMessageAboveL != null) {
                onActivityResultAboveL(requestCode, resultCode, data);
            } else if (uploadMessage != null) {
                uploadMessage.onReceiveValue(result);
                uploadMessage = null;
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void onActivityResultAboveL(int requestCode, int resultCode, Intent intent) {
        if (requestCode != Constant.FILE_CHOOSER_RESULT_CODE || uploadMessageAboveL == null)
            return;
        Uri[] results = null;
        if (resultCode == Activity.RESULT_OK) {
            if (intent != null) {
                String dataString = intent.getDataString();
                ClipData clipData = intent.getClipData();
                if (clipData != null) {
                    results = new Uri[clipData.getItemCount()];
                    for (int i = 0; i < clipData.getItemCount(); i++) {
                        ClipData.Item item = clipData.getItemAt(i);
                        results[i] = item.getUri();
                    }
                }
                if (dataString != null)
                    results = new Uri[]{Uri.parse(dataString)};
            }
        }
        uploadMessageAboveL.onReceiveValue(results);
        uploadMessageAboveL = null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, getString(R.string.success_permission), Toast.LENGTH_LONG).show();
                    CommonUtils.downloadBySystem(this);
                } else {
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        new MyDialog(this, R.style.dialog, getString(R.string.msg_permissionRequest), (dialog, confirm) -> {
                            if (confirm) {
                                Intent intent = new Intent();
                                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", MainActivity.this.getPackageName(), null);
                                intent.setData(uri);
                                MainActivity.this.startActivity(intent);
                            }
                        }).setTitle(getString(R.string.msg_permissionTitle)).setPositiveButton(getString(R.string.button_setting)).setNegativeButton(getString(R.string.button_cancel)).show();
                        break;
                    }
                    Toast.makeText(this, getString(R.string.error_permission), Toast.LENGTH_LONG).show();
                }
                break;
            }
            case 2: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, getString(R.string.success_permission), Toast.LENGTH_LONG).show();
                    CommonUtils.downloadBySystem(this);
                } else {
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        new MyDialog(this, R.style.dialog, getString(R.string.msg_permissionRequest), (dialog, confirm) -> {
                            if (confirm) {
                                Intent intent = new Intent();
                                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", MainActivity.this.getPackageName(), null);
                                intent.setData(uri);
                                MainActivity.this.startActivity(intent);
                                MainActivity.this.finish();
                            } else {
                                MainActivity.this.finish();
                            }
                        }).setTitle(getString(R.string.msg_permissionTitle)).setPositiveButton(getString(R.string.button_setting)).setNegativeButton(getString(R.string.button_cancel)).show();
                        break;
                    }
                    Toast.makeText(this, getString(R.string.error_permission), Toast.LENGTH_LONG).show();
                    MainActivity.this.finish();
                }
            }
            break;
            case 3: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    request.grant(request.getResources());
                } else {
                    request.deny();
                }
                request = null;
            }
            break;
            default:
        }
    }

    /**
     * 下载结果处理
     */
    private class DownloadCompleteReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                if (isErr) {
                    return;
                }
                long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (downloadId == -1 && downloadId != MyApplication.getInstance().getDownloadId()) {
                    return;
                }
                DownloadManager downloadManager = (DownloadManager) context.getSystemService(DOWNLOAD_SERVICE);
                if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {
                    File file = null;
                    boolean pubKeyFlg = false;
                    try {
                        String filePath = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getPath() + File.separator + MyApplication.getInstance().getFileName();
                        Log.v(TAG, filePath);
                        file = new File(filePath);
                        if ("application/zip".equals(MyApplication.getInstance().getMimetype())) {
                            if (MyApplication.getInstance().isForceUp()) {
                                showDownLoadProgress(100);
                            }
                            if (!CommonUtils.checkFileMD5(MainActivity.this, file, MD5)) {
                                downloadManager.remove(downloadId);
                                CommonUtils.downloadBySystem(MainActivity.this);
                                CommonUtils.upPhoneInfo(MainActivity.this, "资源包MD5不匹配！");
                                return;
                            }
                            SharedPreferences sp = MainActivity.this.getSharedPreferences("zipInfo", Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = sp.edit();
                            editor.putInt("localZipVersion", latestZipVersionCode);
                            editor.commit();
                            String finalFilePath = filePath;
                            if (MyApplication.getInstance().isForceUp()) {
                                CommonUtils.UnZipFolder(MainActivity.this, finalFilePath, context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getPath() + "/assets/");
                                boolean res = CommonUtils.checkLocalFileMD5(MainActivity.this, jsHashJson);
                                if (!res) {
                                    downloadManager.remove(downloadId);
                                    CommonUtils.downloadBySystem(MainActivity.this);
                                    CommonUtils.upPhoneInfo(MainActivity.this, "JS文件MD5不匹配！");
                                    return;
                                }
                                String path = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getPath();
                                String SD_ASSETS_URL = "file://" + path + "/assets/update/index.html";
                                mWebView.loadUrl(SD_ASSETS_URL);
                                MyApplication.getInstance().setForceUp(false);
                                return;

                            }

                            if (!isOptional) {
                                CommonUtils.UnZipFolder(MainActivity.this, finalFilePath, context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getPath() + "/assets/");
                                boolean res = CommonUtils.checkLocalFileMD5(MainActivity.this, jsHashJson);
                                if (!res) {
                                    downloadManager.remove(downloadId);
                                    CommonUtils.downloadBySystem(MainActivity.this);
                                    CommonUtils.upPhoneInfo(MainActivity.this, "JS文件MD5不匹配！");
                                    return;
                                }
                                isUpdate = false;
                                initReloadWebView();
//                                Intent reIntent = getBaseContext().getPackageManager()
//                                        .getLaunchIntentForPackage(getBaseContext().getPackageName());
//                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                                startActivity(reIntent);
//                                android.os.Process.killProcess(android.os.Process.myPid());
                                return;
                            }

                            new MyDialog(MainActivity.this, R.style.dialog, getString(R.string.assetsUp_content), (dialog, confirm) -> {
                                if (confirm) {
                                    CommonUtils.UnZipFolder(MainActivity.this, finalFilePath, context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getPath() + "/assets/");
                                    boolean res = CommonUtils.checkLocalFileMD5(MainActivity.this, jsHashJson);
                                    if (!res) {
                                        downloadManager.remove(downloadId);
                                        CommonUtils.downloadBySystem(MainActivity.this);
                                        CommonUtils.upPhoneInfo(MainActivity.this, "JS文件MD5不匹配！");
                                        return;
                                    }
                                    Intent reIntent = getBaseContext().getPackageManager()
                                            .getLaunchIntentForPackage(getBaseContext().getPackageName());
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    startActivity(reIntent);
                                    android.os.Process.killProcess(android.os.Process.myPid());
                                    return;
                                } else {
                                    isUpdate = false;
                                    setShow();
                                }
                            }).setTitle(getString(R.string.assetsUpdate_title)).show();
                            return;
                        }
                        JarFile jarFile = new JarFile(filePath);
                        JarEntry jarEntry = jarFile.getJarEntry(Constant.ANDROID_MANIFEST_FILENAME);
                        String newPubKey = CommonUtils.getNewPubKye(jarFile, jarEntry);
                        String localPubKey = CommonUtils.getLocalPubKey(context);
                        if (newPubKey != null && localPubKey != null && localPubKey.equals(newPubKey)) {
                            pubKeyFlg = true;
                        }

                        // 验证APK MD5 & 签名
                        if (!(CommonUtils.checkFileMD5(MainActivity.this, file, MD5) && pubKeyFlg)) {
                            downloadManager.remove(downloadId);
                            new MyDialog(MainActivity.this, R.style.dialog, getString(R.string.msg_fileCorrect), (dialog, confirm) -> {
                                if (confirm) {
                                    CommonUtils.downloadBySystem(MainActivity.this);
                                } else {
                                    MainActivity.this.finish();
                                }
                            }).setTitle(getString(R.string.msg_downloadTitle)).show();
                            return;
                        }

                        String type = downloadManager.getMimeTypeForDownloadedFile(downloadId);
                        if (TextUtils.isEmpty(type)) {
                            type = "*/*";
                        }
                        Uri uri;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            uri = FileProvider.getUriForFile(MainActivity.this, "com.android.weidex.fileprovider", file);
                        } else {
                            uri = Uri.fromFile(file);
                        }
                        if (uri != null) {
                            Intent installIntent = new Intent(Intent.ACTION_VIEW);
                            installIntent.setDataAndType(uri, type);
                            if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)) {
                                installIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            }
                            context.startActivity(installIntent);
                        }
                        MainActivity.this.finish();
                    } catch (Exception e) {
                        CommonUtils.upPhoneInfo(MainActivity.this, e.getMessage() + "-" + e.getCause());
                    }
                }
            }
        }
    }

    /**
     * 检查更新AsyncTask
     */
    class DownloadTask extends AsyncTask<Activity, Integer, Integer> {
        private final int RES_NOR = 0;//无需更新
        private final int RES_ERR = 1;//获取更新失败
        private final int RES_APP = 2;//APK需更新
        private final int RES_ZIP = 3;//assets需更新
        private final int RES_HAS = 4;//本地有最新APK
        private final int RES_NO_NET = 5;//未联网
        private Uri uri;

        @Override
        protected void onPreExecute() {
            //todo
        }

        @Override
        protected Integer doInBackground(Activity... params) {
            Activity mActivity = params[0];
            boolean isConnected = CommonUtils.isNetworkConnected(mActivity);
            if (!isConnected) {
                return RES_NO_NET;
            }
            try {
                latestVersionCode = versionJson.getJSONObject("android").getInt("versionCode");
                latestVersionName = versionJson.getJSONObject("android").getString("versionName");
                downLoadUrl = versionJson.getJSONObject("android").getString("zipDownLoadUrl");
                MD5 = versionJson.getJSONObject("android").getString("zipMD5");
                jsHashJson = versionJson.getJSONObject("android").getJSONObject("JSMD5");
                latestZipVersionCode = versionJson.getJSONObject("android").getInt("zipVersionCode");
                SharedPreferences sp = mActivity.getSharedPreferences("zipInfo", MODE_PRIVATE);
                int localZipVersion = sp.getInt("localZipVersion", -1);
                if (latestVersionCode > (getVersionCode(mActivity))) {
                    //检查本地是否有最新APK
                    downLoadUrl = versionJson.getJSONObject("android").getString("downLoadUrl");
                    MD5 = versionJson.getJSONObject("android").getString("APKMD5");
                    String fileName = URLUtil.guessFileName(downLoadUrl, "", "application/vnd.android.package-archive");

                    File file = new File(mActivity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        uri = FileProvider.getUriForFile(mActivity, "com.android.weidex.fileprovider", file);
                    } else {
                        uri = Uri.fromFile(file);
                    }

                    if (file.exists() && file.isFile()) {
                        if (CommonUtils.checkFileMD5(mActivity, file, MD5)) {
                            isUpdate = true;
                            return RES_HAS;
                        }
                        file.delete();
                    }
                    MyApplication.getInstance().setUrl(downLoadUrl);
                    MyApplication.getInstance().setContentDisposition("");
                    MyApplication.getInstance().setMimetype("application/vnd.android.package-archive");
                    isUpdate = true;
                    SharedPreferences.Editor editor = sp.edit();
                    editor.remove("localZipVersion");
                    editor.commit();
                    MyApplication.getInstance().setZip(false);
                    return RES_APP;
                } else if ((localZipVersion != -1 && latestZipVersionCode > localZipVersion) ||
                        (localZipVersion == -1 && latestZipVersionCode > BuildConfig.ZIP_VERSION)) {
                    MyApplication.getInstance().setUrl(downLoadUrl);
                    MyApplication.getInstance().setMimetype("application/zip");
                    isUpdate = true;
                    MyApplication.getInstance().setZip(true);
                    CommonUtils.downloadBySystem(mActivity);
                    CommonUtils.upPhoneInfo(MainActivity.this, "正常升级！" + localZipVersion + "-->" + latestZipVersionCode);
                    return RES_ZIP;
                } else {
                    String path = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getPath();
                    File indexFile = new File(path + "/assets/update/index.html");
                    String jsHash = sp.getString("jsHash", null);
                    JSONObject mJsHash;
                    if (jsHash == null) {
                        mJsHash = jsHashJson;
                    } else {
                        mJsHash = new JSONObject(jsHash);
                    }

                    if (indexFile.exists() || indexFile.isFile()) {
                        boolean res = CommonUtils.checkLocalFileMD5(mActivity, mJsHash);
                        if (!res) {
                            MyApplication.getInstance().setUrl(downLoadUrl);
                            MyApplication.getInstance().setMimetype("application/zip");
                            isUpdate = true;
                            MyApplication.getInstance().setZip(true);
                            CommonUtils.downloadBySystem(mActivity);
                            CommonUtils.upPhoneInfo(MainActivity.this, "正常升级！" + localZipVersion + "-->" + latestZipVersionCode);
                            return RES_ZIP;
                        }
                    } else {
                        boolean res = CommonUtils.checkAssestsFileMD5(mActivity, mJsHash);
                        if (!res) {
                            MyApplication.getInstance().setUrl(downLoadUrl);
                            MyApplication.getInstance().setMimetype("application/zip");
                            isUpdate = true;
                            MyApplication.getInstance().setZip(true);
                            CommonUtils.downloadBySystem(mActivity);
                            CommonUtils.upPhoneInfo(MainActivity.this, "正常升级！" + localZipVersion + "-->" + latestZipVersionCode);
                            return RES_ZIP;
                        }
                    }
                }
                return RES_NOR;
            } catch (Exception e) {
                CommonUtils.upPhoneInfo(mActivity, e.getMessage() + "-" + e.getCause());
                Log.v(TAG, "---->" + e.getMessage());
                return RES_ERR;
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            //todo
        }

        @Override
        protected void onPostExecute(Integer result) {
            switch (result) {
                case RES_NOR:
                    if (MyApplication.getInstance().isForceUp()) {
                        load();
                    }
                    break;
                case RES_APP:
                    showUpdateDialog();
                    break;
                case RES_ZIP:
                    if (MyApplication.getInstance().isForceUp()) {
                        numberProgressBar = findViewById(R.id.mProgress);
                        numberProgressBar.setVisibility(View.VISIBLE);
                    }
                    break;
                case RES_ERR:
                    new MyDialog(MainActivity.this, R.style.dialog, getString(R.string.msg_netError), (dialog, confirm) -> {
                        if (confirm) {
                            finish();
                        }
                    }).setTitle(getString(R.string.msg_netErrTitle)).setPositiveButton(getString(R.string.button_ok)).setNegativeButton(null).show();
                    break;
                case RES_HAS:
                    new MyDialog(MainActivity.this, R.style.dialog, getString(R.string.msg_haveNewApk), (dialog, confirm) -> {
                        if (confirm) {
                            Intent installIntent = new Intent(Intent.ACTION_VIEW);
                            installIntent.setDataAndType(uri, "application/vnd.android.package-archive");
                            installIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            MainActivity.this.startActivity(installIntent);
                            finish();
                        } else {
                            finish();
                        }
                    }).setTitle(getString(R.string.msg_versionUpdateTitle)).show();
                    break;
            }
        }
    }

    /**
     * 为避免handler造成的内存泄漏
     * 1、使用静态的handler，对外部类不保持对象的引用
     * 2、但Handler需要与Activity通信，所以需要增加一个对Activity的弱引用
     */
    public class MyHandler extends Handler {
        private final WeakReference<Activity> mActivityReference;

        MyHandler(Activity activity) {
            this.mActivityReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            //获取弱引用队列中的activity
            MainActivity activity = (MainActivity) mActivityReference.get();
            //获取消息,更新UI
            switch (msg.what) {
                case 0:
                    setShow();
                    break;
                case 1:
                    if (MyApplication.getInstance().isForceUp()) {
                        showDownLoadProgress(msg.arg1);
                    }
                    break;
            }
        }
    }

    /**
     * showDownLoadProgress
     *
     * @param progress
     */
    public void showDownLoadProgress(int progress) {
        try {
            if (numberProgressBar != null) {
                numberProgressBar.setProgress(progress);
                if (progress == 100 && numberProgressBar.isShown()) {
                    numberProgressBar.setVisibility(View.INVISIBLE);
                }
            }
        } catch (Exception e) {
            CommonUtils.upPhoneInfo(this, e.getMessage());
            Log.v("showDownLoadProgress", e.getMessage());
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (MyApplication.getInstance().isWithdraw()) {
                return true;
            }
            if (isErr) {
                finish();
            }
            WebView webView;
            if (mWebView != null) {
                webView = mWebView;
            } else {
                webView = mReloadWebView;
            }
            String url = webView.getUrl();
            Log.v(TAG, "onKeyDown---->" + url);
            if (url != null && (url.matches(".*/home/my$") || url.matches(".*/home/discover$") || url.contains("market") || url.contains("deal"))) {
                moveTaskToBack(false);
                return true;
            }
            if (webView.canGoBack()) {
                webView.goBack();
                return true;
            } else {
                finish();
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * 获取版本号
     *
     * @param context
     * @return
     */
    private static int getVersionCode(Context context) {
        PackageManager packageManager = context.getPackageManager();
        PackageInfo packInfo = null;
        try {
            packInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, e.getMessage());
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return (int) packInfo.getLongVersionCode();
        } else {
            return packInfo.versionCode;
        }
    }

    /**
     * 版本更新Dialog
     * 检测WIFI
     */
    private void showUpdateDialog() {
        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
//        StringBuilder updateMsg = new StringBuilder().append(latestVersionName);
//        if (!wm.isWifiEnabled()) {
//            updateMsg.append(getString(R.string.msg_wifi));
//        }
        try {
            new UpdateDialog(this, R.style.dialog, updateMsg.getJSONArray("update_content"), (dialog, confirm) -> {
                if (confirm) {
                    CommonUtils.downloadBySystem(this);
                    dialog.dismiss();
                } else {
                    finish();
                }
            }).setTitle(updateMsg.getString("update_title")).show();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 每24h弹一次广告
     */
    private void setShow() {
        if (isUpdate || isErr || !isShowDialog) return;
        final long day = 24 * 60;
        long systemTime = System.currentTimeMillis();
        SharedPreferences sp = getSharedPreferences("zipInfo", Context.MODE_PRIVATE);
        long noShow = sp.getLong("noShow", -1);
        long time = (systemTime - noShow) / 60000;
        if (noShow == -1 || time >= day) {
            if (mAdvertisementDialog == null) {
                mAdvertisementDialog = new AdvertisementDialog(MainActivity.this, dialogUrl, new AdvertisementDialog.ResultListener() {
                    @Override
                    public void done() {
                        loadAdUrl(eventUrl);
                    }
                });
            }
            if (!mAdvertisementDialog.isShowing()) {
                mAdvertisementDialog.showDialog();
            }
        }
    }

    /**
     * 加载活动页
     */
    public void loadAdUrl(String eventUrl) {
        WebView webView;
        if (mWebView != null) {
            webView = mWebView;
        } else {
            webView = mReloadWebView;
        }
        if (eventUrl.startsWith("http")) {
            webView.loadUrl(eventUrl);
            return;
        }
        String mFilePath = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getPath();
        File indexFile = new File(mFilePath + "/assets/update/index.html");
        if (indexFile.exists() && indexFile.isFile()) {
            String SD_ASSETS_URL = "file://" + mFilePath + "/assets/update/index.html#" + eventUrl;
            webView.loadUrl(SD_ASSETS_URL);
        } else {
            webView.loadUrl("file:///android_asset/index.html#" + eventUrl);
        }
    }

    private void init() {
        isMaintenance = false;
        isOptional = false;
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        String jsonString = bundle.getString("versionJson");
        String adUrl = bundle.getString("adUrl");
        dialogUrl = bundle.getString("dialogUrl");
        eventUrl = bundle.getString("eventUrl");
        title = bundle.getString("title");
        content = bundle.getString("content");
        isShowDialog = bundle.getBoolean("isShowDialog");
        Log.v(TAG, "adUrl---->" + adUrl);
        Log.v(TAG, "dialogUrl---->" + dialogUrl);
        try {
            versionJson = new JSONObject(jsonString);
            updateMsg = versionJson.getJSONObject("android").getJSONObject("updateMsg");
            isShowAd = versionJson.getJSONObject("android").getBoolean("isShowAd");
            isMaintenance = versionJson.getJSONObject("android").getBoolean("isMaintenance");
            boolean isForceUp = versionJson.getJSONObject("android").getBoolean("isForceUp");
            MyApplication.getInstance().setForceUp(isForceUp);
            if (!isForceUp) {
                isOptional = versionJson.getJSONObject("android").getBoolean("isOptional");
            }
        } catch (JSONException e) {
            isShowAd = false;
            isShowDialog = false;
            CommonUtils.upPhoneInfo(MainActivity.this, e.getMessage() + "-" + e.getCause());
            Log.v(TAG, e.getMessage());
        }
        if (isShowAd) {
            Glide.with(this).asFile().load(adUrl).error(R.drawable.splash).into(new CustomTarget<File>() {
                @Override
                public void onResourceReady(@NonNull File resource, @Nullable Transition<? super File> transition) {
                    Bitmap bitmap = BitmapFactory.decodeFile(resource.getAbsolutePath());
                    byte[] chunk = bitmap.getNinePatchChunk();
                    NinePatchDrawable ninePatchDrawable = null;
                    if (NinePatch.isNinePatchChunk(chunk)) {
                        ninePatchDrawable = new NinePatchDrawable(getResources(), bitmap, chunk,
                                new Rect(), null);
                        getWindow().getDecorView().setBackground(ninePatchDrawable);
                    }
                }

                @Override
                public void onLoadCleared(@Nullable Drawable placeholder) {
                }
            });
        } else {
            isShowDialog = false;
        }
        isUpdate = false;
//        MobSDK.init(this);
        mView = getWindow().getDecorView();
        isErr = false;
        isOver = false;
        CrashHandler.getInstance().init(this);
    }

    /**
     * 加载index.html
     */
    private void load() {
        String path = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getPath();
        String SD_ASSETS_URL = "file://" + path + "/assets/update/index.html";
        File indexFile = new File(path + "/assets/update/index.html");
        if (indexFile.exists() || indexFile.isFile()) {
            mWebView.loadUrl(SD_ASSETS_URL);
            return;
        }
        mWebView.loadUrl(Constant.WEIDEX_URL);
    }

    private void initWebView(WebView webView) {
        userAgent = webView.getSettings().getUserAgentString();
        webView.setOnLongClickListener(view -> {
            if (isMaintenance) {
                return true;
            }
            return false;
        });
        WebSettings setting = webView.getSettings();
        webView.setHorizontalScrollBarEnabled(false);
        webView.setVerticalScrollBarEnabled(false);
        //获取手势焦点
        webView.requestFocusFromTouch();
        setting.setNeedInitialFocus(true);
        //支持Js
        setting.setJavaScriptEnabled(true);
        //缓存模式
        setting.setCacheMode(WebSettings.LOAD_DEFAULT);
        //是否支持画面缩放，默认不支持
        setting.setBuiltInZoomControls(true);
        setting.setSupportZoom(true);
        //是否显示缩放图标，默认显示
        setting.setDisplayZoomControls(false);
        //设置网页内容自适应屏幕大小
        setting.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        setting.setUseWideViewPort(true);
        setting.setLoadWithOverviewMode(true);
        setting.setDomStorageEnabled(true);
        //http图片显示
        setting.setBlockNetworkImage(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setting.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        setting.setMediaPlaybackRequiresUserGesture(false);

        //跨域访问
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            setting.setAllowUniversalAccessFromFileURLs(true);
        }

        //主要用于平板，针对特定屏幕代码调整分辨率
        int screenDensity = getResources().getDisplayMetrics().densityDpi;
        WebSettings.ZoomDensity zoomDensity = WebSettings.ZoomDensity.MEDIUM;
        switch (screenDensity) {

            case DisplayMetrics.DENSITY_LOW:
                zoomDensity = WebSettings.ZoomDensity.CLOSE;
                break;

            case DisplayMetrics.DENSITY_MEDIUM:
                zoomDensity = WebSettings.ZoomDensity.MEDIUM;
                break;

            case DisplayMetrics.DENSITY_HIGH:
                zoomDensity = WebSettings.ZoomDensity.FAR;
                break;
        }
        setting.setDefaultZoom(zoomDensity);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Log.v(TAG, "shouldOverrideUrlLoading1---->" + url);
                if (TextUtils.equals(url, Constant.MIANTENANCE_URL)) {
                    return false;
                }
                Intent intent;
                if (url.contains(Constant.OTC_VIP_URL) || url.contains("myReward")) {
                    intent = new Intent(MainActivity.this, OTCActivity.class);
                    intent.putExtra("url", url);
                    intent.putExtra("title", title);
                    intent.putExtra("content", content);
                } else {
                    Uri uri = Uri.parse(url);
                    intent = new Intent(Intent.ACTION_VIEW, uri);
                }
                startActivity(intent);
                return true;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                Log.v(TAG, "shouldOverrideUrlLoading2---->" + url);
                if (TextUtils.equals(url, Constant.MIANTENANCE_URL)) {
                    return false;
                }
                Intent intent;
                if (url.contains(Constant.OTC_VIP_URL) || url.contains("myReward")) {
                    intent = new Intent(MainActivity.this, OTCActivity.class);
                    intent.putExtra("url", url);
                } else {
                    Uri uri = Uri.parse(url);
                    intent = new Intent(Intent.ACTION_VIEW, uri);
                }
                startActivity(intent);
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                Log.v(TAG, "onPageStarted---->" + url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.v(TAG, "onPageFinished---->" + url);
                if (TextUtils.equals(url, Constant.LOAD_ERROR_URL)) {
                    ErrImg.setVisibility(View.VISIBLE);
                    loadText.setVisibility(View.VISIBLE);
                    view.loadUrl("about:blank");// 清空当前加载
                    isErr = true;
                    CommonUtils.upPhoneInfo(MainActivity.this, null);
                    return;
                }
                if (!isOver && !TextUtils.equals(url, Constant.LOAD_BLANK)) {
                    isOver = true;
                    Log.v(TAG, "isOver----->");
                    new DownloadTask().execute(MainActivity.this);
                }
                if (view != null && view.getProgress() == 100) {
                    view.setVisibility(View.VISIBLE);
                    if (mWebView != null) {
                        mWebView.setVisibility(View.GONE);
                        mWebView.clearHistory();
                        ((ViewGroup) mWebView.getParent()).removeView(mWebView);
                        mWebView.destroy();
                        mWebView = null;
                    }
                    getWindow().setNavigationBarColor(Color.BLACK);
                    getWindow().setStatusBarColor(Color.BLACK);
                    if (timer == null && isShowAd && isShowDialog && !isMaintenance) {
                        timer = new Timer();
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                Message message = new Message();
                                message.what = 0;
                                myHandler.sendMessage(message);
                            }
                        }, 1000, 60000);
                    }
                }
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError
                    error) {
                handler.proceed(); // 接受所有网站的证书
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            // For Android < 3.0
            public void openFileChooser(ValueCallback<Uri> valueCallback) {
                uploadMessage = valueCallback;
                openImageChooserActivity();
            }

            // For Android  >= 3.0
            public void openFileChooser(ValueCallback valueCallback, String acceptType) {
                uploadMessage = valueCallback;
                openImageChooserActivity();
            }

            //For Android  >= 4.1
            public void openFileChooser(ValueCallback<Uri> valueCallback, String
                    acceptType, String capture) {
                uploadMessage = valueCallback;
                openImageChooserActivity();
            }

            // For Android >= 5.0
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]>
                    filePathCallback, FileChooserParams fileChooserParams) {
                uploadMessageAboveL = filePathCallback;
                openImageChooserActivity();
                return true;
            }

            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                int permission = ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.CAMERA);
                if (permission != PackageManager.PERMISSION_GRANTED) {
                    MainActivity.this.request = request;
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, Constant.REQUEST_WEB_CAMERA);
                } else {
                    request.grant(request.getResources());
                }
            }
        });

        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            MyApplication.getInstance().setUrl(url);
            MyApplication.getInstance().setContentDisposition(contentDisposition);
            MyApplication.getInstance().setMimetype(mimetype);
            CommonUtils.downloadBySystem(this);
        });
    }

    private void initReloadWebView() {
        mReloadWebView = findViewById(R.id.mWebView_reload);
        initWebView(mReloadWebView);
        String url = mWebView.getUrl();
        if (url != null && url.contains("android_asset")) {
            String[] urlArr = url.split("android_asset");
            url = "file:///storage/emulated/0/Android/data/com.android.aicoin/files/Download/assets/update" + urlArr[1];
        }
        Log.v(TAG, "url----->" + url);
        mReloadWebView.loadUrl(url);
        mReloadWebView.addJavascriptInterface(share, "Share");
    }

//    public static void testJs(String url) {
////        String js = "window.Share.saveWalletInfo(\"" + url + "\")";
//        String js = "window.localStorage.clear()";
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//            mWebView.evaluateJavascript(js, null);
//            Log.v("javascript", js);
//        } else {
//            mWebView.loadUrl("javascript:(function(){" + js + "})()");
//        }
//    }
}

