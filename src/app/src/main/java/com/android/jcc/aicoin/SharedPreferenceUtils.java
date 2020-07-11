package com.android.jcc.aicoin;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferenceUtils {

    private static SharedPreferenceUtils instance;
    private static final String VERSION = "version";
    private static SharedPreferences sharedPreferences;
    private static SharedPreferences.Editor editor;

    private SharedPreferenceUtils() {

    }

    public static SharedPreferenceUtils getInstance(Context context) {
        if (instance == null) {
            String fileName = context.getPackageName() + "_" + VERSION;
            sharedPreferences = context.getSharedPreferences(fileName, Context.MODE_PRIVATE);
            editor = sharedPreferences.edit();
            instance = new SharedPreferenceUtils();
        }
        return instance;
    }

    public void setVersionName(String userName) {
        editor.putString("versionName", userName);
        editor.apply();
    }

    public String getVersionName() {
        return sharedPreferences.getString("versionName", "");
    }

    public void setDownLoadUrl(String userName) {
        editor.putString("downLoadUrl", userName);
        editor.apply();
    }

    public String getDownLoadUrl() {
        return sharedPreferences.getString("downLoadUrl", "");
    }

    public void setMimetype(String mimetype) {
        editor.putString("mimetype", mimetype);
        editor.apply();
    }

    public String getMimetype() {
        return sharedPreferences.getString("mimetype", "");
    }

    public void setContentDisposition(String contentDisposition) {
        editor.putString("contentDisposition", contentDisposition);
        editor.apply();
    }

    public String getContentDisposition() {
        return sharedPreferences.getString("contentDisposition", "");
    }

    public void setMD5(String userName) {
        editor.putString("MD5", userName);
        editor.apply();
    }

    public String getMD5() {
        return sharedPreferences.getString("MD5", "");
    }

    public void setIsShowAd(Boolean isShowAd) {
        editor.putBoolean("isShowAd", isShowAd);
        editor.apply();
    }

    public Boolean getIsShowAd() {
        return sharedPreferences.getBoolean("isShowAd", false);
    }

    public void setIsMaintenance(Boolean isMaintenance) {
        editor.putBoolean("isMaintenance", false);
        editor.apply();
    }

    public Boolean getIsMaintenance() {
        return sharedPreferences.getBoolean("isMaintenance", false);
    }

    public void setIsForceUp(Boolean isForceUp) {
        editor.putBoolean("isForceUp", false);
        editor.apply();
    }

    public Boolean getIsForceUp() {
        return sharedPreferences.getBoolean("isForceUp", false);
    }

    public void setIsOptional(Boolean isOptional) {
        editor.putBoolean("isOptional", false);
        editor.apply();
    }

    public Boolean getIsOptional() {
        return sharedPreferences.getBoolean("isOptional", false);
    }

    public void setAdUrl(String adUrl) {
        editor.putString("adUrl", adUrl);
        editor.apply();
    }

    public String getAdUrl() {
        return sharedPreferences.getString("adUrl", "");
    }

    public void setDialogUrl(String dialogUrl) {
        editor.putString("dialogUrl", dialogUrl);
        editor.apply();
    }

    public String getDialogUrl() {
        return sharedPreferences.getString("dialogUrl", "");
    }

    public void setEventUrl(String eventUrl) {
        editor.putString("eventUrl", eventUrl);
        editor.apply();
    }

    public String getEventUrl() {
        return sharedPreferences.getString("eventUrl", "");
    }

    public void setTitle(String title) {
        editor.putString("title", title);
        editor.apply();
    }

    public String getTitle() {
        return sharedPreferences.getString("title", "");
    }

    public void setContent(String content) {
        editor.putString("content", content);
        editor.apply();
    }

    public String getContent() {
        return sharedPreferences.getString("content", "");
    }

    public void setIsShowDialog(Boolean isShowDialog) {
        editor.putBoolean("isShowDialog", false);
        editor.apply();
    }

    public Boolean getIsShowDialog() {
        return sharedPreferences.getBoolean("isShowDialog", false);
    }

    public void setJSMD5(String JSMD5) {
        editor.putString("JSMD5", JSMD5);
        editor.apply();
    }

    public String getJSMD5() {
        return sharedPreferences.getString("JSMD5", "");
    }

    public void setLocalZipVersion(int localZipVersion) {
        editor.putInt("localZipVersion", localZipVersion);
        editor.apply();
    }

    public int getLocalZipVersion() {
        return sharedPreferences.getInt("localZipVersion", 0);
    }

    public void setFileUri(String fileUri) {
        editor.putString("fileUri", fileUri);
        editor.apply();
    }

    public String getfileUri() {
        return sharedPreferences.getString("fileUri", "");
    }

    public void remove(String key) {
        editor.remove(key);
        editor.apply();
    }

}
