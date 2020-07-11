package com.android.jcc.aicoin;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;


public class AdvertisementDialog extends Dialog implements View.OnClickListener {

    private static final String TAG = "AdvertisementDialog";

    private static ImageView closeImageView;
    private static ImageView adImageView;
    private SharedPreferences sp;
    private Context mContext;
    private String mAdurl;
    private ResultListener mResultListener;

    public interface ResultListener {
        void done();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
    }

    public AdvertisementDialog(Context context, String adUrl, ResultListener resultListener) {
        super(context);
        mContext = context;
        mAdurl = adUrl;
        mResultListener = resultListener;
        sp = context.getSharedPreferences("zipInfo", Context.MODE_PRIVATE);
        setContentView(R.layout.dialog_advertisement);
        adImageView = findViewById(R.id.img_advertisement);
        adImageView.setOnClickListener(this);
        closeImageView = findViewById(R.id.img_close);
        closeImageView.setOnClickListener(this);
        //设置点击布局外则Dialog消失
        setCanceledOnTouchOutside(false);
    }

    public void showDialog() {
        Window window = getWindow();
        //设置弹窗动画
        window.setWindowAnimations(R.style.style_dialog);
        WindowManager.LayoutParams wl = window.getAttributes();
        window.setBackgroundDrawableResource(R.color.transparent);
        window.setDimAmount(0f);
        //设置弹窗位置
        wl.gravity = Gravity.CENTER;
        window.setAttributes(wl);
        Glide.with(mContext).asDrawable().load(mAdurl).into(new CustomTarget<Drawable>() {
            @Override
            public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                adImageView.setVisibility(View.VISIBLE);
                adImageView.setImageDrawable(resource);
                closeImageView.setVisibility(View.VISIBLE);
                if (!isShowing()) {
                    show();
                }
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {
            }
        });
    }

    @Override
    public void onClick(View v) {
        if (v == adImageView) {
            adImageView.setClickable(false);
            mResultListener.done();
            SharedPreferences.Editor editor = sp.edit();
            editor.putLong("noShow", System.currentTimeMillis());
            editor.commit();
            dismiss();
        } else if (v == closeImageView) {
            SharedPreferences.Editor editor = sp.edit();
            editor.putLong("noShow", System.currentTimeMillis());
            editor.commit();
            dismiss();
        }
    }
}
