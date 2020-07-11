package com.android.jcc.aicoin;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Base64;

import cn.sharesdk.framework.Platform;
import cn.sharesdk.framework.ShareSDK;
import cn.sharesdk.system.text.ShortMessage;
import cn.sharesdk.tencent.qq.QQ;
import cn.sharesdk.wechat.friends.Wechat;
import cn.sharesdk.wechat.moments.WechatMoments;

public class Share {

    private static final String TAG = "Share";
    private static Share instance;

    private static Context mContext;
    private static String mTitle;
    private static String mContent;

    public static Share getInstance(Context context, String title, String content) {
        if (instance == null) {
            instance = new Share(context);
            mTitle = title;
            mContent = content;
        }
        return instance;
    }

    public Share(Context context) {
        this.mContext = context;
    }

    /**
     * 微信分享
     *
     * @param url
     * @param scene
     */
    @JavascriptInterface
    public void shareUrlToMM(String url, int scene) {
        Platform platform;
        if (scene == 1) {
            platform = ShareSDK.getPlatform(Wechat.NAME);
        } else {
            platform = ShareSDK.getPlatform(WechatMoments.NAME);
        }
        if (!platform.isClientValid()) {
            Toast.makeText(mContext, "微信未安装,请尝试其他分享方式", Toast.LENGTH_LONG).show();
        }
        if (TextUtils.isEmpty(url) || url.contains("undefined")) {
            return;
        }
        Platform.ShareParams shareParams = new Platform.ShareParams();
        shareParams.setTitle(mTitle);
        shareParams.setText(mContent);
        shareParams.setUrl(url);
        Bitmap thumbBmp = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.share);
        shareParams.setImageData(thumbBmp);
        shareParams.setShareType(Platform.SHARE_WEBPAGE);
        platform.share(shareParams);
    }

    /**
     * 微信图片分享
     *
     * @param imgName
     * @param scene
     */
    @JavascriptInterface
    public void shareImgToMM(String imgName, int scene) {
        Platform platform;
        if (scene == 1) {
            platform = ShareSDK.getPlatform(Wechat.NAME);
        } else {
            platform = ShareSDK.getPlatform(WechatMoments.NAME);
        }
        if (!platform.isClientValid()) {
            Toast.makeText(mContext, "微信未安装,请尝试其他分享方式", Toast.LENGTH_LONG).show();
        }
        if (TextUtils.isEmpty(imgName) || imgName.contains("undefined")) {
            return;
        }
        Platform.ShareParams shareParams = new Platform.ShareParams();
        String path = mContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getPath();
        File localFile = new File(path + "/assets/update/static/img/" + imgName);
        if (localFile.exists() && localFile.isFile()) {
            shareParams.setImagePath(path + "/assets/update/static/img/" + imgName);
        } else {
            Bitmap thumbBmp = null;
            try {
                thumbBmp = BitmapFactory.decodeStream(mContext.getResources().getAssets().open("static/img/" + imgName));
            } catch (IOException e) {
                e.printStackTrace();
            }
            shareParams.setImageData(thumbBmp);
        }
        shareParams.setShareType(Platform.SHARE_IMAGE);
        platform.share(shareParams);
    }

    /**
     * QQ分享
     *
     * @param url
     */
    @JavascriptInterface
    public void shareUrlToQQ(String url) {
        Platform platform = ShareSDK.getPlatform(QQ.NAME);
        if (!platform.isClientValid()) {
            Toast.makeText(mContext, "QQ未安装,请尝试其他分享方式", Toast.LENGTH_LONG).show();
            return;
        }
        if (TextUtils.isEmpty(url) || url.contains("undefined")) {
            return;
        }
        Platform.ShareParams shareParams = new Platform.ShareParams();
        shareParams.setTitle(mTitle);
        shareParams.setTitleUrl(url);
        shareParams.setText(mContent);
        File sharePath = mContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File file = new File(sharePath, "share.png");
        if (!file.isFile() || !file.exists()) {
            CommonUtils.copyShareImg(mContext, "share.png");
        }
        shareParams.setImagePath(sharePath + File.separator + "share.png");
        shareParams.setShareType(Platform.SHARE_WEBPAGE);
        platform.share(shareParams);
    }

    /**
     * QQ图片分享
     *
     * @param imgName
     */
    @JavascriptInterface
    public void shareImgToQQ(String imgName) {
        Platform platform = ShareSDK.getPlatform(QQ.NAME);
        if (!platform.isClientValid()) {
            Toast.makeText(mContext, "QQ未安装,请尝试其他分享方式", Toast.LENGTH_LONG).show();
            return;
        }
        if (TextUtils.isEmpty(imgName) || imgName.contains("undefined")) {
            return;
        }
        Platform.ShareParams shareParams = new Platform.ShareParams();
        String path = mContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getPath();
        File localFile = new File(path + "/assets/update/static/img/" + imgName);
        if (localFile.exists() && localFile.isFile()) {
            shareParams.setImagePath(path + "/assets/update/static/img/" + imgName);
        } else {
            File sharePath = mContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File file = new File(sharePath, imgName);
            if (!file.isFile() || !file.exists()) {
                CommonUtils.copyShareImg(mContext, imgName);
            }
            shareParams.setImagePath(sharePath + File.separator + imgName);
        }
        shareParams.setShareType(Platform.SHARE_IMAGE);
        platform.share(shareParams);
    }

    /**
     * 短信分享
     *
     * @param url
     */
    @JavascriptInterface
    public void shareUrlToSMS(String url) {
        if (TextUtils.isEmpty(url) || url.contains("undefined")) {
            return;
        }
        Platform platform = ShareSDK.getPlatform(ShortMessage.NAME);
        Platform.ShareParams shareParams = new Platform.ShareParams();
        shareParams.setTitle(mTitle);
        shareParams.setText(mContent + " " + url);
        Bitmap thumbBmp = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.share);
        shareParams.setImageData(thumbBmp);
        platform.share(shareParams);
    }

    /**
     * 截屏
     */
    @JavascriptInterface
    public void screenShot() {
        View dView = MainActivity.mView;
        dView.setDrawingCacheEnabled(true);
        dView.buildDrawingCache();
        Bitmap bitmap = Bitmap.createBitmap(dView.getDrawingCache());
        if (bitmap != null) {
            try {
                File sdCardPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                String fileName = "screenshot_" + System.currentTimeMillis() + ".png";
                File file = new File(sdCardPath, fileName);
                FileOutputStream os = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
                os.flush();
                os.close();
                Toast.makeText(mContext, sdCardPath + File.separator + fileName + "\n保存成功", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 保存weidex文件(二维码)
     *
     * @param url
     * @return
     */
    @JavascriptInterface
    public boolean saveWalletInfo(String url) {
        if (url != null && url.startsWith("data:")) {
            File path = mContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            String fileName = mContext.getString(R.string.file_name) + "_localStore.png";
            File file = new File(path, fileName);
            try {
                if (!path.exists())
                    path.mkdirs();
                if (file.exists()) {
                    file.delete();
                }
                file.createNewFile();
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
            } catch (IOException e) {
                CommonUtils.upPhoneInfo(mContext, e.getMessage() + "-" + e.getCause());
                Log.e(TAG, e.getMessage());
            }
            return true;
        }
        return false;
    }

    /**
     * 导入weidex文件(二维码)
     *
     * @return
     */
    @JavascriptInterface
    public String loadWalletInfo() {
        String path = mContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES).getPath();
        String filePath = path + File.separator + mContext.getString(R.string.file_name) + "_localStore.png";
        File file = new File(filePath);
        if (file.exists() && file.isFile()) {
            return CommonUtils.imageToBase64(filePath);
        }
        return null;
    }

    /**
     * 删除weidex文件(二维码)
     *
     * @return
     */
    @JavascriptInterface
    public boolean deleteWalletInfo() {
        File path = mContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        String fileName = mContext.getString(R.string.file_name) + "_localStore.png";
        File file = new File(path, fileName);
        if (file.exists() && file.isFile()) {
            return file.delete();
        }
        return true;
    }

    /**
     * 提币过程禁止返回键
     *
     * @param isWithdraw
     */
    @JavascriptInterface
    public void isWithdraw(boolean isWithdraw) {
        MyApplication.getInstance().setWithdraw(isWithdraw);
    }
}