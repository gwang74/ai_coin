package com.android.jcc.aicoin;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class OTCActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "OTCActivity";

    private static TextView loadText;
    private static Button reLoadBut;
    private static WebView OTCWebView;
    private static ImageView loadGif;
    private ValueCallback<Uri> uploadMessage;
    private ValueCallback<Uri[]> uploadMessageAboveL;
    private static boolean isErr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otc);
        loadGif = findViewById(R.id.loadgif);
        loadText = findViewById(R.id.loadtext);
        reLoadBut = findViewById(R.id.reload);
        reLoadBut.setOnClickListener(this);
        isErr = false;
        initView();
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        String url = bundle.getString("url");
        String title = bundle.getString("title");
        String content = bundle.getString("content");
        Log.v(TAG, url);
        OTCWebView.loadUrl(url);
        OTCWebView.addJavascriptInterface(OTCActivity.this, "OTCShare");
        Share share = Share.getInstance(this, title, content);
        OTCWebView.addJavascriptInterface(share, "Share");
        CrashHandler.getInstance().init(this);
    }

    /**
     * webView初期配置
     */
    private void initView() {
        OTCWebView = findViewById(R.id.OTCWebView);
        WebSettings setting = OTCWebView.getSettings();
        OTCWebView.setHorizontalScrollBarEnabled(false);
        OTCWebView.setVerticalScrollBarEnabled(false);
        //获取手势焦点
        OTCWebView.requestFocusFromTouch();
        setting.setNeedInitialFocus(true);
        //支持Js
        setting.setJavaScriptEnabled(true);
        //缓存模式
        setting.setCacheMode(WebSettings.LOAD_DEFAULT);
        setting.setAppCacheEnabled(true);
        setting.setAppCachePath(getExternalCacheDir().getPath());
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
//        int screenDensity = getResources().getDisplayMetrics().densityDpi;
//        WebSettings.ZoomDensity zoomDensity = WebSettings.ZoomDensity.MEDIUM;
//        switch (screenDensity) {
//
//            case DisplayMetrics.DENSITY_LOW:
//                zoomDensity = WebSettings.ZoomDensity.CLOSE;
//                break;
//
//            case DisplayMetrics.DENSITY_MEDIUM:
//                zoomDensity = WebSettings.ZoomDensity.MEDIUM;
//                break;
//
//            case DisplayMetrics.DENSITY_HIGH:
//                zoomDensity = WebSettings.ZoomDensity.FAR;
//                break;
//        }
//        setting.setDefaultZoom(zoomDensity);

        OTCWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (TextUtils.equals(url, Constant.OTC_VIP_HOME_URL)) {
                    finish();
                    return;
                }
                if (OTCWebView.getProgress() == 100 && !isErr) {
                    loadGif.setVisibility(View.GONE);
                    loadText.setVisibility(View.GONE);
                    if (!OTCWebView.isShown()) {
                        OTCWebView.setVisibility(View.VISIBLE);
                    }
                }
                Log.v(TAG, "onPageFinished---->" + url);
            }

//            @Override
//            public boolean shouldOverrideUrlLoading(WebView view, String url) {
//                Log.v(TAG, "shouldOverrideUrlLoading---->" + url);
//                if (TextUtils.equals(url, Constant.WEIDEX_VIP_URL)) {
//                    finish();
//                }
////                } else {
////                    Uri uri = Uri.parse(url);
////                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
////                    startActivity(intent);
////                }
//                return true;
//            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                Log.v(TAG, "shouldOverrideUrlLoading---->" + url);
                if (TextUtils.equals(url, Constant.WEIDEX_VIP_URL)) {
                    finish();
                }
//                } else {
//                    Uri uri = Uri.parse(url);
//                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
//                    startActivity(intent);
//                }
                return true;
            }
//
//            @Override
//            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
//                super.onReceivedError(view, errorCode, description, failingUrl);
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                    return;
//                }
//                isErr = true;
//                OTCWebView.setVisibility(View.INVISIBLE);
//                loadGif.setImageDrawable(getDrawable(R.drawable.loaderr));
//                loadText.setText(getString(R.string.loadErr));
//                reLoadBut.setVisibility(View.VISIBLE);
//            }

            @TargetApi(Build.VERSION_CODES.M)
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (request.isForMainFrame()) {
                    isErr = true;
                    OTCWebView.setVisibility(View.INVISIBLE);
                    loadGif.setImageDrawable(getDrawable(R.drawable.loaderr));
                    loadText.setText(getString(R.string.loadErr));
                    reLoadBut.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed(); // 接受所有网站的证书
            }
        });
        OTCWebView.setWebChromeClient(new WebChromeClient() {
            // For Android < 3.0
            public void openFileChooser(ValueCallback<Uri> valueCallback) {
                uploadMessage = valueCallback;
                openImageChooserActivity();
            }

//            // For Android  >= 3.0
//            public void openFileChooser(ValueCallback valueCallback, String acceptType) {
//                uploadMessage = valueCallback;
//                openImageChooserActivity();
//            }

            //For Android  >= 4.1
            public void openFileChooser(ValueCallback<Uri> valueCallback, String acceptType, String capture) {
                uploadMessage = valueCallback;
                openImageChooserActivity();
            }

            // For Android >= 5.0
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                uploadMessageAboveL = filePathCallback;
                openImageChooserActivity();
                return true;
            }
        });
    }

    /**
     * 文件选择
     */
    private void openImageChooserActivity() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "选择文件"), Constant.FILE_CHOOSER_RESULT_CODE);
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
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (OTCWebView.canGoBack()) {
                OTCWebView.goBack();
                return true;
            } else {
                finish();
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @JavascriptInterface
    public void closeOTC() {
        finish();
    }

    @Override
    public void onClick(View v) {
        if (v == reLoadBut) {
            OTCWebView.reload();
            OTCWebView.setVisibility(View.INVISIBLE);
            loadGif.setVisibility(View.VISIBLE);
            loadGif.setImageResource(R.drawable.loading);
            loadText.setVisibility(View.VISIBLE);
            loadText.setText(getString(R.string.loadText));
        }
    }
}
