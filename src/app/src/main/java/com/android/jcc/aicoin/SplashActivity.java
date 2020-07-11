package com.android.jcc.aicoin;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        boolean isConnected = CommonUtils.isNetworkConnected(this);
        if (isConnected) {
            new DownloadTask().execute(this);
        } else {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            intent.putExtra("adUrl", "");
            intent.putExtra("dialogUrl", "");
            intent.putExtra("eventUrl", "");
            intent.putExtra("title", "");
            intent.putExtra("content", "");
            intent.putExtra("isShowDialog", false);
            intent.putExtra("versionJson", "{}");
            finish();
            startActivity(intent);
        }

    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }

    /**
     * 检查更新AsyncTask
     */
    class DownloadTask extends AsyncTask<Activity, Integer, Integer> {
        private final int RES_NOR = 0;//获取更新信息成功
        private final int RES_ERR = 1;//获取更新信息失败
        private int RE_CONN = 0;
        private JSONObject versionJson = null;

        @Override
        protected void onPreExecute() {
            //todo
        }

        /**
         * 网络请求失败重试3次
         *
         * @param activity
         * @return
         */
        private InputStream connect(Activity activity) {
            final String VERSION_URL = BuildConfig.VERSION_URL;
            if (RE_CONN == 6) {
                return null;
            }
            HttpsURLConnection conn = null;
            String URL = VERSION_URL + "?" + System.currentTimeMillis();
            InputStream inputStream;
            try {
                RE_CONN++;
                java.net.URL url = new URL(URL);
                conn = (HttpsURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", activity.getString(R.string.user_agent));
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);
                inputStream = conn.getInputStream();
                int responseCode = conn.getResponseCode();
                if (responseCode != 200) {
                    conn.disconnect();
                    inputStream = connect(activity);
                }
            } catch (Exception e) {
                Log.v(TAG, e.getMessage());
                CommonUtils.upPhoneInfo(SplashActivity.this, e.getMessage() + "-" + e.getCause());
                inputStream = connect(activity);
            }
            return inputStream;
        }

        @Override
        protected Integer doInBackground(Activity... params) {
            Activity mActivity = params[0];
            InputStream inputStream = connect(mActivity);
            try {
                if (inputStream != null) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    int len;
                    byte[] buffer = new byte[1024];
                    while ((len = inputStream.read(buffer)) != -1) {
                        out.write(buffer, 0, len);
                    }
                    inputStream.close();
                    out.close();
                    String result = out.toString();
                    versionJson = new JSONObject(result);
                    return RES_NOR;
                } else {
                    return RES_ERR;
                }
            } catch (Exception e) {
                CommonUtils.upPhoneInfo(SplashActivity.this, e.getMessage() + "-" + e.getCause());
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
                    int firstStart = 0;
                    int firstEnd = 0;
                    int secondStart = 0;
                    int secondEnd = 0;
                    String adUrl = null;
                    String dialogUrl = null;
                    String eventUrl = null;
                    String title = null;
                    String content = null;
                    boolean isShowDialog = false;
                    try {
                        JSONObject adFirstJson = versionJson.getJSONObject("android").getJSONObject("AdFirst");
                        JSONObject adSecondJson = versionJson.getJSONObject("android").getJSONObject("AdSecond");
                        SharedPreferences sp = getSharedPreferences("zipInfo", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sp.edit();
                        editor.putString("zipMd5", versionJson.getJSONObject("android").getString("zipMD5"));
                        editor.putString("jsHash", versionJson.getJSONObject("android").getJSONObject("JSMD5").toString());
                        editor.apply();
                        boolean isShowAdFirst = adFirstJson.getBoolean("isShowAd");
                        boolean isShowAdSecond = adSecondJson.getBoolean("isShowAd");
                        if (isShowAdFirst && isShowAdSecond) {
                            firstStart = adFirstJson.getInt("start");
                            firstEnd = adFirstJson.getInt("end");
                            secondStart = adSecondJson.getInt("start");
                            secondEnd = adSecondJson.getInt("end");
                            int nowTime = CommonUtils.getSystemDate();
                            String sizeStr = getAdUrl();
                            if (nowTime >= firstStart && nowTime <= firstEnd) {
                                adUrl = adFirstJson.getString("adUrl") + "first_" + sizeStr + ".png" + "?" + firstStart;
                                dialogUrl = adFirstJson.getString("dialogUrl") + "first_" + sizeStr + ".png" + "?" + firstStart;
                                isShowDialog = adFirstJson.getBoolean("isShowDialog");
                                eventUrl = adFirstJson.getString("eventUrl");
                                title = adFirstJson.getString("shareTitle");
                                content = adFirstJson.getString("shareContent");
                            } else if (nowTime >= secondStart && nowTime <= secondEnd) {
                                adUrl = adFirstJson.getString("adUrl") + "second_" + sizeStr + ".png" + "?" + secondStart;
                                dialogUrl = adFirstJson.getString("dialogUrl") + "second_" + sizeStr + ".png" + "?" + secondStart;
                                isShowDialog = adSecondJson.getBoolean("isShowDialog");
                                eventUrl = adSecondJson.getString("eventUrl");
                                title = adSecondJson.getString("shareTitle");
                                content = adSecondJson.getString("shareContent");
                            } else {
                                versionJson.getJSONObject("android").put("isShowAd", false);
                            }
                        } else if (isShowAdFirst) {
                            firstStart = adFirstJson.getInt("start");
                            firstEnd = adFirstJson.getInt("end");
                            int nowTime = CommonUtils.getSystemDate();
                            String sizeStr = getAdUrl();
                            if (nowTime >= firstStart && nowTime <= firstEnd) {
                                adUrl = adFirstJson.getString("adUrl") + "first_" + sizeStr + ".png" + "?" + firstStart;
                                dialogUrl = adFirstJson.getString("dialogUrl") + "first_" + sizeStr + ".png" + "?" + firstStart;
                                isShowDialog = adFirstJson.getBoolean("isShowDialog");
                                eventUrl = adFirstJson.getString("eventUrl");
                                title = adFirstJson.getString("shareTitle");
                                content = adFirstJson.getString("shareContent");
                            } else {
                                versionJson.getJSONObject("android").put("isShowAd", false);
                            }
                        } else if (isShowAdSecond) {
                            secondStart = adSecondJson.getInt("start");
                            secondEnd = adSecondJson.getInt("end");
                            int nowTime = CommonUtils.getSystemDate();
                            String sizeStr = getAdUrl();
                            if (nowTime >= secondStart && nowTime <= secondEnd) {
                                adUrl = adFirstJson.getString("adUrl") + "second_" + sizeStr + ".png" + "?" + secondStart;
                                dialogUrl = adFirstJson.getString("dialogUrl") + "second_" + sizeStr + ".png" + "?" + secondStart;
                                isShowDialog = adSecondJson.getBoolean("isShowDialog");
                                eventUrl = adSecondJson.getString("eventUrl");
                                title = adSecondJson.getString("shareTitle");
                                content = adSecondJson.getString("shareContent");
                            } else {
                                versionJson.getJSONObject("android").put("isShowAd", false);
                            }
                        } else {
                            versionJson.getJSONObject("android").put("isShowAd", false);
                        }
                    } catch (JSONException e) {
                        Log.v(TAG, e.getMessage());
                        CommonUtils.upPhoneInfo(SplashActivity.this, e.getMessage() + "-" + e.getCause());
                    }
                    Log.v(TAG, "adUrl---->" + adUrl);
                    Log.v(TAG, "dialogUrl---->" + dialogUrl);
                    Log.v(TAG, "isShowDialog---->" + isShowDialog);
                    String jsonString = versionJson.toString();
                    Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                    intent.putExtra("adUrl", adUrl);
                    intent.putExtra("dialogUrl", dialogUrl);
                    intent.putExtra("eventUrl", eventUrl);
                    intent.putExtra("title", title);
                    intent.putExtra("content", content);
                    intent.putExtra("isShowDialog", isShowDialog);
                    intent.putExtra("versionJson", jsonString);
                    finish();
                    startActivity(intent);
                    break;
                case RES_ERR:
                    new MyDialog(SplashActivity.this, R.style.dialog, getString(R.string.msg_netError), (dialog, confirm) -> {
                        if (confirm) {
                            SplashActivity.this.finish();
                        }
                    }).setTitle(getString(R.string.msg_netErrTitle)).setPositiveButton(getString(R.string.button_ok)).show();
                    break;
            }
        }
    }

    /**
     * 根据屏幕尺寸获取广告
     *
     * @return
     */
    private String getAdUrl() {
        DisplayMetrics metrics = this.getResources().getDisplayMetrics();
        int dpi = metrics.densityDpi;
        Log.v(TAG, "屏幕密度---->" + dpi);
        if (120 <= dpi && dpi <= 160) {
            return "m";
        } else if (160 < dpi && dpi <= 240) {
            return "h";
        } else if (240 < dpi && dpi <= 320) {
            return "X";
        } else if (320 < dpi && dpi <= 480) {
            return "2X";
        } else {
            return "3X";
        }
    }
}