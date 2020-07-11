package com.android.jcc.aicoin;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;

public class UpdateDialog extends Dialog implements View.OnClickListener {
    private TextView contentTxt;
    private TextView contentTxt1;
    private TextView contentTxt2;
    private TextView contentTxt3;
    private TextView contentTxt4;
    private LinearLayout layout0;
    private LinearLayout layout1;
    private LinearLayout layout2;
    private LinearLayout layout3;
    private LinearLayout layout4;
    private TextView titleTxt;
    private TextView submitTxt;
    private TextView cancelTxt;

    private Context mContext;
    private JSONArray content;
    private OnCloseListener listener;
    private String positiveName;
    private String negativeName;
    private String title;

    public UpdateDialog(Context context) {
        super(context);
        this.mContext = context;
    }

    public UpdateDialog(Context context, int themeResId, JSONArray content) {
        super(context, themeResId);
        this.mContext = context;
        this.content = content;
    }

    public UpdateDialog(Context context, int themeResId, JSONArray content, OnCloseListener listener) {
        super(context, themeResId);
        this.mContext = context;
        this.content = content;
        this.listener = listener;
    }

    protected UpdateDialog(Context context, boolean cancelable, OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
        this.mContext = context;
    }

    public UpdateDialog setTitle(String title) {
        this.title = title;
        return this;
    }

    public UpdateDialog setPositiveButton(String name) {
        this.positiveName = name;
        return this;
    }

    public UpdateDialog setNegativeButton(String name) {
        this.negativeName = name;
        return this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.update_dialog);
        setCanceledOnTouchOutside(false);
        initView();
    }

    private void initView() {
        contentTxt = (TextView) findViewById(R.id.tv_content);
        contentTxt1 = (TextView) findViewById(R.id.tv_content1);
        contentTxt2 = (TextView) findViewById(R.id.tv_content2);
        contentTxt3 = (TextView) findViewById(R.id.tv_content3);
        contentTxt4 = (TextView) findViewById(R.id.tv_content4);
        TextView[] txtArr = {contentTxt, contentTxt1, contentTxt2, contentTxt3, contentTxt4};
        layout0 = (LinearLayout) findViewById(R.id.layout_tmp0);
        layout1 = (LinearLayout) findViewById(R.id.layout_tmp1);
        layout2 = (LinearLayout) findViewById(R.id.layout_tmp2);
        layout3 = (LinearLayout) findViewById(R.id.layout_tmp3);
        layout4 = (LinearLayout) findViewById(R.id.layout_tmp4);
        LinearLayout[] lyaoutArr = {layout0, layout1, layout2, layout3, layout4};
        titleTxt = (TextView) findViewById(R.id.tv_title);
        submitTxt = (TextView) findViewById(R.id.tv_yes);
        submitTxt.setOnClickListener(this);
        cancelTxt = (TextView) findViewById(R.id.tv_no);
        cancelTxt.setOnClickListener(this);
        try {
            for (int i = 0; i < content.length(); i++) {
                if (i > 4) {
                    break;
                }
                txtArr[i].setText(content.getString(i));
                lyaoutArr[i].setVisibility(View.VISIBLE);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (!TextUtils.isEmpty(positiveName)) {
            submitTxt.setText(positiveName);
        }

        if (!TextUtils.isEmpty(negativeName)) {
            cancelTxt.setText(negativeName);
        }

        if (!TextUtils.isEmpty(title)) {
            titleTxt.setText(title);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_no:
                if (listener != null) {
                    listener.onClick(this, false);
                }
                this.dismiss();
                break;
            case R.id.tv_yes:
                if (listener != null) {
                    listener.onClick(this, true);
                }
                this.dismiss();
                break;
        }
    }

    public interface OnCloseListener {
        void onClick(Dialog dialog, boolean confirm);
    }
}