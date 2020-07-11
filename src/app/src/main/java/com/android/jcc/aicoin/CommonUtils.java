package com.android.jcc.aicoin;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.URLUtil;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.URL;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.net.ssl.HttpsURLConnection;

import static android.content.Context.DOWNLOAD_SERVICE;

/**
 * 共通类
 */
public class CommonUtils {

    private final static String TAG = "CommonUtils";
    private final static long day = 24 * 60;

    private static DownloadManager downloadManager;
    public static DownloadChangeObserver downloadChangeObserver;

    /**
     * 文件下载
     *
     * @param activity
     */
    public static void downloadBySystem(Activity activity) {
        String url = MyApplication.getInstance().getUrl();
        String contentDisposition = MyApplication.getInstance().getContentDisposition();
        String mimeType = MyApplication.getInstance().getMimetype();

        if (!MyApplication.getInstance().isZip() && !MyApplication.getInstance().isNotification()) {
            MyApplication.getInstance().setNotification(true);
            if (!getNotificationPromissions(activity)) {
                return;
            }
        }

        if (url != null && url.startsWith("data:")) {
            if (verifyStoragePermissions(activity, false)) {
                //when url is base64 encoded data
                Log.v(TAG, "downloadBySystem------->" + url);
                createAndSaveFileFromBase64Url(activity, url);
            }
            return;
        }

        long systemTime = System.currentTimeMillis();
        SharedPreferences sp = activity.getSharedPreferences("zipInfo", Context.MODE_PRIVATE);
        long downTime = sp.getLong("downTime", -1);
        int downCount = sp.getInt("downCount", 0);
        long time = (systemTime - downTime) / 60000;
        // 控制下载次数，一天内最多下载3次
        if (time < day && downTime != -1 && downCount >= 3) {
            return;
        }
        // 一天后重置
        else if (time >= day || downTime == -1) {
            SharedPreferences.Editor editor = sp.edit();
            editor.putLong("downTime", systemTime);
            editor.putInt("downCount", 1);
            editor.apply();
        } else {
            downCount++;
            SharedPreferences.Editor editor = sp.edit();
            editor.putInt("downCount", downCount);
            editor.apply();
        }


        // 指定下载地址
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        // 允许媒体扫描，根据下载的文件类型被加入相册、音乐等媒体库
        request.allowScanningByMediaScanner();
        // 允许在计费流量下下载
        request.setAllowedOverMetered(true);
        // 允许该记录在下载管理界面可见
        request.setVisibleInDownloadsUi(true);
        // 允许漫游时下载
        request.setAllowedOverRoaming(true);
        request.setMimeType(mimeType);
        request.addRequestHeader("User-Agent", MyApplication.getInstance().getUserAgent());

        // 设置下载文件保存的路径和文件名
        String fileName = URLUtil.guessFileName(url, contentDisposition, mimeType);
        MyApplication.getInstance().setFileName(fileName);
        request.setDestinationInExternalFilesDir(activity, Environment.DIRECTORY_DOWNLOADS, fileName);
        String filePath = activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getPath() + File.separator + fileName;
        File file = new File(filePath);
        if (file.exists() && file.isFile()) {
            file.delete();
        }
        if (MyApplication.getInstance().isZip()) {
            // 设置通知的显示类型，下载进行时和完成后显示通知
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);
        } else {
            // 设置通知的显示类型，下载进行时和完成后显示通知
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        }

        downloadManager = (DownloadManager) activity.getSystemService(DOWNLOAD_SERVICE);
        // 添加一个下载任务
        long downloadId = downloadManager.enqueue(request);
        MyApplication.getInstance().setDownloadId(downloadId);
        if (MyApplication.getInstance().isZip() && MyApplication.getInstance().isForceUp()) {
            try {
                Uri uri = Uri.parse("content://downloads/my_downloads");
                if (downloadChangeObserver == null) {
                    downloadChangeObserver = new DownloadChangeObserver(MainActivity.myHandler);
                }
                activity.getContentResolver().registerContentObserver(uri, true, downloadChangeObserver);
            } catch (Exception e) {
                upPhoneInfo(activity, e.getMessage());
                Log.v(TAG, e.getMessage());
            }
        }
    }

    static class DownloadChangeObserver extends ContentObserver {

        private Handler mHandler;

        /**
         * Creates a content observer.
         *
         * @param handler The handler to run {@link #onChange} on, or null if none.
         */
        public DownloadChangeObserver(Handler handler) {
            super(handler);
            this.mHandler = handler;
        }

        @Override
        public void onChange(boolean selfChange) {
            queryDownloadStatus(mHandler);
        }
    }

    /**
     * 获取下载状况
     */
    private static void queryDownloadStatus(Handler handler) {
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(MyApplication.getInstance().getDownloadId());
        Cursor c = downloadManager.query(query);
        if (c != null && c.moveToFirst()) {
            int fileSizeIdx =
                    c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
            int bytesDLIdx =
                    c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
            int fileSize = c.getInt(fileSizeIdx);
            int bytesDL = c.getInt(bytesDLIdx);
            float percent = (float) bytesDL / (float) fileSize;
            float progress = (float) Math.floor(percent * 100);
            handler.obtainMessage(1, (int) progress, 0).sendToTarget();
        }
    }

    /**
     * Base64编译下载
     *
     * @param activity
     * @param url
     * @return
     */
    private static void createAndSaveFileFromBase64Url(Activity activity, String url) {
        File path = new File(Environment.getExternalStorageDirectory()
                + File.separator + Environment.DIRECTORY_DCIM
                + File.separator + "Camera" + File.separator);
        String filetype = url.substring(url.indexOf("/") + 1, url.indexOf(";"));
        String filename = activity.getString(R.string.file_name) + "_" + System.currentTimeMillis() + "." + filetype;
        File file = new File(path, filename);
        try {
            if (!path.exists())
                path.mkdirs();
            if (!file.exists()) {
                file.createNewFile();
            }

            String base64EncodedString = url.substring(url.indexOf(",") + 1);
            byte[] decodedBytes;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                decodedBytes = Base64.getDecoder().decode(base64EncodedString);
            } else {
                decodedBytes = android.util.Base64.decode(base64EncodedString, android.util.Base64.DEFAULT);
            }
            OutputStream os = new FileOutputStream(file);
            os.write(decodedBytes);
            os.close();

            //Tell the media scanner about the new file so that it is immediately available to the user.
            MediaScannerConnection.scanFile(activity,
                    new String[]{file.toString()}, null,
                    (path1, uri) -> {
                        Log.i("ExternalStorage", "Scanned " + path1 + ":");
                        Log.i("ExternalStorage", "-> uri=" + uri);
                    });

            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);

            Uri fileUri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                fileUri = FileProvider.getUriForFile(activity, "com.android.weidex.fileprovider", file);
            } else {
                fileUri = Uri.fromFile(file);
            }

            String mimetype = url.substring(url.indexOf(":") + 1, url.indexOf("/"));
            intent.setDataAndType(fileUri, (mimetype + "/*"));

            List<ResolveInfo> resInfoList = activity.getPackageManager()
                    .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo resolveInfo : resInfoList) {
                String packageName = resolveInfo.activityInfo.packageName;
                activity.grantUriPermission(packageName, fileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            PendingIntent pIntent = PendingIntent.getActivity(activity, 0, intent, 0);
            NotificationManager notificationManager = (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // 用户可以看到的通知渠道的名字
                CharSequence name = activity.getString(R.string.channel_name);
                // 用户可以看到的通知渠道的描述
                String description = activity.getString(R.string.channel_description);
                int importance = NotificationManager.IMPORTANCE_HIGH;
                NotificationChannel mChannel = new NotificationChannel(Constant.NOTIFICATION_CHANNEL_ID, name, importance);
                // 配置通知渠道的属性
                mChannel.setDescription(description);
                // 设置通知出现时的闪灯（如果 android 设备支持的话）
                mChannel.enableLights(true);
                mChannel.setLightColor(Color.RED);
                // 设置通知出现时的震动（如果 android 设备支持的话）
                mChannel.enableVibration(true);
                mChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
                notificationManager.createNotificationChannel(mChannel);
            }

            Notification notification = new NotificationCompat.Builder(activity, Constant.NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_download)
                    .setContentText(activity.getString(R.string.msg_file_downloaded))
                    .setContentTitle(file.getAbsolutePath() + "/" + file.getName())
                    .setContentIntent(pIntent)
                    .build();

            notification.flags |= Notification.FLAG_AUTO_CANCEL;
            int notificationId = 85851;
            notificationManager.notify(notificationId, notification);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            upPhoneInfo(activity, e.getMessage() + "-" + e.getCause());
            Toast.makeText(activity.getApplicationContext(), activity.getString(R.string.error_downloading), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 权限验证
     *
     * @param activity
     * @return
     */
    public static boolean verifyStoragePermissions(Activity activity, boolean updateflg) {
        int reuestCode;
        boolean verifyFlg = false;
        try {
            if (updateflg) {
                reuestCode = Constant.REQUEST_EXTERNAL_STORAGE_UPDATE;
            } else {
                reuestCode = Constant.REQUEST_EXTERNAL_STORAGE;
            }
            int permission = ContextCompat.checkSelfPermission(activity,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (permission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, Constant.PERMISSIONS_STORAGE, reuestCode);
            } else {
                verifyFlg = true;
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        return verifyFlg;
    }

    /**
     * 获取文件信息
     *
     * @param context
     * @param uri
     * @return
     */
    public static String getRealFilePath(final Context context, final Uri uri) {
        if (null == uri) return null;
        final String scheme = uri.getScheme();
        String data = null;
        if (scheme == null)
            data = uri.getPath();
        else if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            data = uri.getPath();
        } else if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            Cursor cursor = context.getContentResolver().query(uri, new String[]{MediaStore.Files.FileColumns.DATA}, null, null, null);
            if (null != cursor) {
                if (cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA);
                    if (index > -1) {
                        data = cursor.getString(index);
                    }
                }
                cursor.close();
            }
        }
        return data;
    }

    /**
     * 验证MD5
     *
     * @param file
     * @param md5
     * @return
     */
    public static boolean checkFileMD5(Context context, File file, String md5) {
        if (!file.isFile()) {
            return false;
        }
        boolean md5Flg = false;
        MessageDigest digest;
        FileInputStream in;
        String localMD5;
        byte buffer[] = new byte[1024];
        int len;
        try {
            digest = MessageDigest.getInstance("MD5");
            in = new FileInputStream(file);
            while ((len = in.read(buffer, 0, 1024)) != -1) {
                digest.update(buffer, 0, len);
            }
            in.close();
            BigInteger bigInt = new BigInteger(1, digest.digest());
            localMD5 = bigInt.toString(16);
            while (localMD5.length() < 32) {
                localMD5 = "0" + localMD5;
            }
            md5Flg = localMD5.equals(md5);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            CommonUtils.upPhoneInfo(context, e.getMessage() + "-" + e.getCause());
        }
        return md5Flg;
    }

    /**
     * 获取APK的公钥
     *
     * @param context
     * @return
     */
    @SuppressLint("PackageManagerGetSignatures")
    public static String getLocalPubKey(Context context) {
        String pubKey = null;
        PackageInfo packageInfo;
        try {
            Signature[] signs;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo = context.getPackageManager().getPackageInfo(
                        context.getPackageName(), PackageManager.GET_SIGNING_CERTIFICATES);
                signs = packageInfo.signingInfo.getApkContentsSigners();
            } else {
                packageInfo = context.getPackageManager().getPackageInfo(
                        context.getPackageName(), PackageManager.GET_SIGNATURES);
                signs = packageInfo.signatures;
            }
            Signature sign = signs[0];
            CertificateFactory certFactory = CertificateFactory
                    .getInstance("X.509");
            X509Certificate cert = (X509Certificate) certFactory
                    .generateCertificate(new ByteArrayInputStream(sign.toByteArray()));
            pubKey = cert.getPublicKey().toString();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        return pubKey;
    }

    /**
     * 获取下载文件的公钥
     *
     * @param jarFile
     * @param je
     * @return
     */
    public static String getNewPubKye(JarFile jarFile, JarEntry je) {
        byte[] readBuffer = new byte[1024];
        InputStream is;
        String publicKey = null;
        try {
            is = new BufferedInputStream(jarFile.getInputStream(je));
            while (is.read(readBuffer, 0, readBuffer.length) != -1) {
                // not using
            }
            is.close();
            Certificate[] certificates = je != null ? je.getCertificates() : null;
            publicKey = certificates != null ? certificates[0].getPublicKey().toString() : null;
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
        return publicKey;
    }

    /**
     * 跳到通知栏设置界面
     *
     * @param activity
     */
    public static boolean getNotificationPromissions(Activity activity) {
        if (!isNotificationEnabled(activity)) {
            if (!TextUtils.isEmpty(isMiui())) {
                new MyDialog(activity, R.style.dialog, activity.getString(R.string.msg_miui_notification), (dialog, confirm) -> {
                    if (confirm) {
                        CommonUtils.downloadBySystem(activity);
                    }
                }).setPositiveButton(activity.getString(R.string.button_continue)).setNegativeButton(null).show();

            } else {
                new MyDialog(activity, R.style.dialog, activity.getString(R.string.msg_miui_notification), (dialog, confirm) -> {
                    if (confirm) {
                        setNotification(activity);
                    } else {
                        CommonUtils.downloadBySystem(activity);
                    }
                }).setPositiveButton(activity.getString(R.string.button_continue)).setNegativeButton(null).show();
            }
            return false;
        }
        return true;
    }

    /**
     * 跳到通知栏设置界面
     *
     * @param activity
     */
    public static void setNotification(Activity activity) {
        Intent localIntent = new Intent();
        //直接跳转到应用通知设置的代码：
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            localIntent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            localIntent.putExtra(Settings.EXTRA_APP_PACKAGE, activity.getPackageName());
        } else if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            localIntent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
            localIntent.putExtra("app_package", activity.getPackageName());
            localIntent.putExtra("app_uid", activity.getApplicationInfo().uid);
        } else if (android.os.Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            localIntent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            localIntent.addCategory(Intent.CATEGORY_DEFAULT);
            localIntent.setData(Uri.parse("package:" + activity.getPackageName()));
        } else {
            //4.4以下没有从app跳转到应用通知设置页面的Action，可考虑跳转到应用详情页面,
            localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (Build.VERSION.SDK_INT >= 9) {
                localIntent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
                localIntent.setData(Uri.fromParts("package", activity.getPackageName(), null));
            } else if (Build.VERSION.SDK_INT <= 8) {
                localIntent.setAction(Intent.ACTION_VIEW);
                localIntent.setClassName("com.android.settings", "com.android.setting.InstalledAppDetails");
                localIntent.putExtra("com.android.settings.ApplicationPkgName", activity.getPackageName());
            }
        }
        activity.startActivity(localIntent);
    }


    public static String isMiui() {
        final String KEY_MIUI_VERSION_CODE = "ro.miui.ui.version.code";
        String line = null;
        BufferedReader input = null;
        try {
            Process p = Runtime.getRuntime().exec("getprop " + KEY_MIUI_VERSION_CODE);
            input = new BufferedReader(new InputStreamReader(p.getInputStream()), 1024);
            line = input.readLine();
            input.close();
        } catch (IOException ex) {
            Log.e(TAG, "Unable to read prop " + KEY_MIUI_VERSION_CODE, ex);
            return null;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    Log.v(TAG, e.getMessage());
                }
            }
        }
        return line;
    }


    /**
     * 获取通知权限
     *
     * @param activity
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static boolean isNotificationEnabled(Activity activity) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            //8.0手机以上
//            if (((NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE)).getImportance() == NotificationManager.IMPORTANCE_NONE) {
//                return false;
//            }
//        }
        return NotificationManagerCompat.from(activity).areNotificationsEnabled();
    }


    /**
     * 校验本地assets的JS
     *
     * @param activity
     * @param md5Array
     * @return
     */
    public static boolean checkAssestsFileMD5(Activity activity, JSONObject md5Array) {
        activity.getCacheDir().mkdir();
//        activity.getExternalCacheDir().mkdir();
        FileOutputStream fos;
        File file;
        String md5;
        boolean res = true;
        try {
            String[] assets = activity.getAssets().list("static/js");
            for (int i = (assets.length - 1); i >= 0; i--) {
                String assetPath = assets[i];
                md5 = "";
                InputStream assestsFileImputStream = activity.getAssets().open("static/js/" + assetPath);
                file = new File(activity.getCacheDir() + "/" + assetPath);
//                file = new File(context.getExternalCacheDir() + "/" + assetPath);
                if (!file.exists()) {
                    file.createNewFile();
                }
                fos = new FileOutputStream(file);
                byte[] buffer = new byte[1024];
                int byteCount;
                //循环从输入流读取 buffer字节
                while ((byteCount = assestsFileImputStream.read(buffer)) != -1) {
                    //将读取的输入流写入到输出流
                    fos.write(buffer, 0, byteCount);
                }
                //刷新缓冲区
                fos.flush();
                assestsFileImputStream.close();
                fos.close();
                if (md5Array.has(assetPath)) {
                    md5 = md5Array.getString(file.getName());
                }
                if (!checkFileMD5(activity, file, md5)) {
                    res = false;
                    file.delete();
                    break;
                }
                file.delete();
            }

        } catch (Exception e) {
            res = false;
            Log.e(TAG, e.getMessage());
            upPhoneInfo(activity, e.getMessage() + "-" + e.getCause());
        }
        return res;
//        zip(context,context.getExternalCacheDir().getAbsolutePath(),context.getExternalFilesDir("").getAbsolutePath()+"/assets.zip");
    }

    /**
     * 校验本地更新资源的JS
     *
     * @param context
     * @param md5Array
     * @return
     */
    public static boolean checkLocalFileMD5(Context context, JSONObject md5Array) {
        String path = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getPath();
        File assetFile = new File(path + "/assets/update/static/js");
        if (!assetFile.exists()) {
            return false;
        }

        File[] files = assetFile.listFiles();

        if (files == null) {
            return false;
        }
        boolean res = true;
        for (File file : files) {
            String md5 = "";
            if (md5Array.has(file.getName())) {
                try {
                    md5 = md5Array.getString(file.getName());
                    if (!checkFileMD5(context, file, md5)) {
                        res = false;
                        break;
                    }
                } catch (JSONException e) {
                    res = false;
                    Log.e(TAG, e.getMessage());
                    upPhoneInfo(context, e.getMessage() + "-" + e.getCause());
                }
            }
        }
        return res;
    }

    /**
     * 解压zip到指定的路径
     *
     * @param zipFile
     * @param outPath
     */
    public static void UnZipFolder(Activity activity, String zipFile, String outPath) {
        ZipInputStream inZip = null;
        try {
            inZip = new ZipInputStream(new FileInputStream(zipFile));
            ZipEntry zipEntry;
            String szName = "";
            while ((zipEntry = inZip.getNextEntry()) != null) {
                szName = zipEntry.getName();
                if (szName.contains("../")) {
                    continue;
                }
                if (zipEntry.isDirectory()) {
                    //获取部件的文件夹名
                    szName = szName.substring(0, szName.length() - 1);
                    File folder = new File(outPath + File.separator + szName);
                    folder.mkdirs();
                } else {
                    Log.e(TAG, outPath + File.separator + szName);
                    File file = new File(outPath + File.separator + szName);
                    if (!file.exists()) {
                        Log.e(TAG, "Create the file:" + outPath + File.separator + szName);
                        file.getParentFile().mkdirs();
                        file.createNewFile();
                    }
                    // 获取文件的输出流
                    FileOutputStream out = new FileOutputStream(file);
                    int len;
                    byte[] buffer = new byte[1024];
                    // 读取（字节）字节到缓冲区
                    while ((len = inZip.read(buffer)) != -1) {
                        // 从缓冲区（0）位置写入（字节）字节
                        out.write(buffer, 0, len);
                        out.flush();
                    }
                    out.close();
                }
            }
            inZip.close();
            File file = new File(zipFile);
            if (file.exists()) {
                file.delete();
            }
        } catch (Exception e) {
            Log.v(TAG, e.getMessage());
            upPhoneInfo(activity, e.getMessage() + "-" + e.getCause());
        }
    }

    /**
     * 收集错误机型信息
     */
    public static void upPhoneInfo(Context context, String msg) {
        final String info = "phoneName=" + Build.BRAND + "&phoneOS=" + Build.MODEL + "&androidAPI=" +
                +Build.VERSION.SDK_INT + "&androidOS="
                + Build.VERSION.RELEASE + "&msg=" + msg;
        final String VERSION_URL = "https://app.weidex.vip/infos/android/error";
        String URL = VERSION_URL + "?" + info;
        Log.v(TAG, URL);
        new Thread() {
            public void run() {
                try {
                    HttpsURLConnection conn = null;
                    java.net.URL url = new URL(URL);
                    conn = (HttpsURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("User-Agent", context.getString(R.string.user_agent));
                    conn.setConnectTimeout(3000);
                    conn.setReadTimeout(3000);
                    conn.getInputStream();
                } catch (Exception e) {
                    Log.v(TAG, e.getMessage());
                }
            }
        }.start();
    }

    /**
     * 获取系统当前日期
     *
     * @return
     */
    public static int getSystemDate() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
        Date date = new Date(System.currentTimeMillis());
        return Integer.parseInt(simpleDateFormat.format(date));
    }

    /**
     * 将图片转换成Base64编码的字符串
     */
    public static String imageToBase64(String path) {
        if (TextUtils.isEmpty(path)) {
            return null;
        }
        final String header = "data:image/png;base64,";
        InputStream is = null;
        byte[] data = null;
        String result = null;
        try {
            is = new FileInputStream(path);
            //创建一个字符流大小的数组。
            data = new byte[is.available()];
            //写入数组
            is.read(data);
            //用默认的编码格式进行编码
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                result = Base64.getEncoder().encodeToString(data);
            } else {
                result = android.util.Base64.encodeToString(data, android.util.Base64.DEFAULT);
            }
        } catch (Exception e) {
            Log.v(TAG, e.getMessage());
        } finally {
            if (null != is) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
        Log.v("imageToBase64", header + result);
        return header + result;
    }

    public static boolean isNetworkConnected(Context context) {
        if (context != null) {
            ConnectivityManager mConnectivityManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);

            NetworkInfo mWiFiNetworkInfo = mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if (mWiFiNetworkInfo != null && mWiFiNetworkInfo.isConnected()) {
                Log.v("NetStateChangeReceiver", "wifi:" + mWiFiNetworkInfo.isConnected());
                return true;
            }

            NetworkInfo mMobileNetworkInfo = mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            if (mMobileNetworkInfo != null && mMobileNetworkInfo.isConnected()) {
                Log.v("NetStateChangeReceiver", "mobile:" + mMobileNetworkInfo.isConnected());
                return true;
            }
        }
        return false;
    }

    /**
     * 复制分享图片到本地
     *
     * @param context
     * @param imgName
     * @return
     */
    public static boolean copyShareImg(Context context, String imgName) {
        File sharePath = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File file = new File(sharePath, imgName);
        try {
            if (!sharePath.exists())
                sharePath.mkdirs();
            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();

            InputStream is;
            if (TextUtils.equals("share.png", imgName)) {
                is = context.getResources().getAssets().open(imgName);
            } else {
                is = context.getResources().getAssets().open("static/img/" + imgName);
            }
            OutputStream os = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int byteCount = 0;
            while ((byteCount = is.read(buffer)) != -1) {// 循环从输入流读取
                // buffer字节
                os.write(buffer, 0, byteCount);// 将读取的输入流写入到输出流
            }
            os.flush();// 刷新缓冲区
            is.close();
            os.close();
        } catch (IOException e) {
            CommonUtils.upPhoneInfo(context, e.getMessage() + "-" + e.getCause());
            Log.e(TAG, e.getMessage());
        }
        return true;
    }

//    public static void zip(Context context, String src, String dest) {
//        //提供了一个数据项压缩成一个ZIP归档输出流
//        ZipOutputStream out = null;
//        try {
//
//            File outFile = new File(dest);//源文件或者目录
//            File fileOrDirectory = new File(src);//压缩文件路径
//            out = new ZipOutputStream(new FileOutputStream(outFile));
//            //如果此文件是一个文件，否则为false
//            if (fileOrDirectory.isFile()) {
//                zipFileOrDirectory(out, fileOrDirectory, "");
//            } else {
//                //返回一个文件或空阵列。
//                File[] entries = fileOrDirectory.listFiles();
//                for (int i = 0; i < entries.length; i++) {
//                    // 递归压缩，更新curPaths
//                    zipFileOrDirectory(out, entries[i], "");
//                    entries[i].delete();
//                }
//            }
//        } catch (IOException ex) {
//            ex.printStackTrace();
//        } finally {
//            //关闭输出流
//            if (out != null) {
//                try {
//                    out.close();
//                } catch (IOException ex) {
//                    ex.printStackTrace();
//                }
//            }
//        }
//    }
//
//    private static void zipFileOrDirectory(ZipOutputStream out,
//                                           File fileOrDirectory, String curPath) {
//        //从文件中读取字节的输入流
//        FileInputStream in = null;
//        try {
//            //如果此文件是一个目录，否则返回false。
//            if (!fileOrDirectory.isDirectory()) {
//                // 压缩文件
//                byte[] buffer = new byte[4096];
//                int bytes_read;
//                in = new FileInputStream(fileOrDirectory);
//                //实例代表一个条目内的ZIP归档
//                ZipEntry entry = new ZipEntry(curPath
//                        + fileOrDirectory.getName());
//                entry.setTime(fileOrDirectory.lastModified());
//                //条目的信息写入底层流
//                out.putNextEntry(entry);
//                while ((bytes_read = in.read(buffer)) != -1) {
//                    out.write(buffer, 0, bytes_read);
//                }
//                out.closeEntry();
//            } else {
//                // 压缩目录
//                File[] entries = fileOrDirectory.listFiles();
//                for (int i = 0; i < entries.length; i++) {
//                    // 递归压缩，更新curPaths
//                    zipFileOrDirectory(out, entries[i], curPath
//                            + fileOrDirectory.getName() + "/");
//                }
//            }
//        } catch (IOException ex) {
//            ex.printStackTrace();
//        } finally {
//            if (in != null) {
//                try {
//                    in.close();
//                } catch (IOException ex) {
//                    ex.printStackTrace();
//                }
//            }
//        }
//    }
}
