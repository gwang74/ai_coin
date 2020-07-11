package com.android.jcc.aicoin;

import android.app.Activity;
import android.os.Process;
import android.widget.Toast;

import java.lang.Thread.UncaughtExceptionHandler;

public class CrashHandler implements UncaughtExceptionHandler {

    private static CrashHandler sInstance;
    private UncaughtExceptionHandler mDefaultCrashHandler;
    private Activity mActivity;

    private CrashHandler() {
    }

    public static CrashHandler getInstance() {
        if (sInstance == null) {
            sInstance = new CrashHandler();
        }
        return sInstance;
    }

    /**
     * 初始化
     *
     * @param activity
     */
    protected void init(Activity activity) {
        //得到系统的应用异常处理器
        mDefaultCrashHandler = Thread.getDefaultUncaughtExceptionHandler();
        //将当前应用异常处理器改为默认的
        Thread.setDefaultUncaughtExceptionHandler(this);
        mActivity = activity;

    }

    /**
     * @param thread 为出现未捕获异常的线程
     * @param ex     为未捕获的异常 ，可以通过e 拿到异常信息
     */
    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        CommonUtils.upPhoneInfo(mActivity, ex.getMessage() + "-" + ex.getCause());
        //如果系统提供了默认的异常处理器，则交给系统去结束程序，否则就由自己结束自己
        if (mDefaultCrashHandler != null) {
            mDefaultCrashHandler.uncaughtException(thread, ex);
        } else {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mActivity, "应用出现异常，即将关闭!", Toast.LENGTH_LONG);
                }
            });
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            Process.killProcess(Process.myPid());
        }
    }
}
