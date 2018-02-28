package com.ly.yscoco.autoupdate;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.TextAppearanceSpan;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by pan_g on 2018/2/28.
 */

public class AutoUpdate {

    /* 下载包安装路径 */
    private static final int DOWN_UPDATE = 1;
    private static final int DOWN_OVER = 2;
    private static final int DOWN_CANCEL = 4;
    // private final String mServerDir;
    private static String mAppFile = "tuanzi.apk";

    private Activity mContext;
    private boolean isForceUpdate;
    private String mDownloadUrl;
    private String verName;
    private String updateContent;
    private boolean mInterceptFlag;
    private int mProgress;

    private Dialog mNoticeDialog;
    private Dialog mDownloadDialog;
    private ProgressBar mProgressBar;

    public static AutoUpdate create(Activity mContext, boolean isForceUpdate, String mDownloadUrl, String verName, String updateContent) {
        return new AutoUpdate(mContext, isForceUpdate, mDownloadUrl, verName, updateContent);
    }

    public AutoUpdate(Activity mContext, boolean isForceUpdate, String mDownloadUrl, String verName, String updateContent) {
        this.mContext = mContext;
        this.isForceUpdate = isForceUpdate;
        this.mDownloadUrl = mDownloadUrl;
        this.verName = verName;
        this.updateContent = updateContent;
        showNoticeDialog();
    }

    private void showNoticeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setInverseBackgroundForced(true);
        builder.setCancelable(!isForceUpdate);
        FrameLayout dialogView = (FrameLayout) LayoutInflater.from(mContext).inflate(R.layout.dialog_content_layout, null);
        // 设置标题字体
        String title = "总裁大大，发现新版本啦！";
        String content = "\n" + verName + "\n版本新增内容如下\n" + updateContent;
        SpannableString styledText = new SpannableString(title + content);
        styledText.setSpan(new TextAppearanceSpan(mContext, R.style.dialogTitleStyle), 0, title.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ((TextView) dialogView.findViewById(R.id.content)).setText(styledText, TextView.BufferType.SPANNABLE);
        if (isForceUpdate){
            TextView cancleTv = (TextView) dialogView.findViewById(R.id.no);
            cancleTv.setVisibility(View.GONE);
        } else {
            ((TextView) dialogView.findViewById(R.id.no)).setText("残忍拒绝");
            ((TextView) dialogView.findViewById(R.id.no)).setTextColor(mContext.getResources().getColor(R.color._0479f6));
            ((TextView) dialogView.findViewById(R.id.no)).setTextSize(mContext.getResources().getDimension(R.dimen.text_size12));
            dialogView.findViewById(R.id.no).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mNoticeDialog.dismiss();
                }
            });
        }
        ((TextView) dialogView.findViewById(R.id.ok)).setText("立即更新");
        ((TextView) dialogView.findViewById(R.id.ok)).setTextColor(mContext.getResources().getColor(R.color._0479f6));
        ((TextView) dialogView.findViewById(R.id.ok)).setTextSize(mContext.getResources().getDimension(R.dimen.text_size12));
        dialogView.findViewById(R.id.ok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mNoticeDialog.dismiss();
                showDownloadDialog();
            }
        });
        builder.setView(dialogView);
        mNoticeDialog = builder.create();
        WindowManager m = mContext.getWindowManager();
        Display d = m.getDefaultDisplay();  //为获取屏幕宽、高
        mNoticeDialog = builder.create();
        mNoticeDialog.show();
        mNoticeDialog.getWindow().setLayout((int) (d.getWidth() * 0.8), FrameLayout.LayoutParams.WRAP_CONTENT);
        mNoticeDialog.getWindow().setBackgroundDrawableResource(R.drawable.comm_dialog_bg_white);
        mNoticeDialog.setCancelable(!isForceUpdate);
    }

    private void showDownloadDialog() {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View v = inflater.inflate(R.layout.activity_update, null);
        mProgressBar = (ProgressBar) v.findViewById(R.id.progress_bar);
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setCancelable(false);
        builder.setTitle("版本更新");
        builder.setView(v);
        // 强制更新 不可取消
        if (!isForceUpdate) {
            builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mInterceptFlag = false;
                }
            });
        }
        mDownloadDialog = builder.create();
        mDownloadDialog.show();
        mInterceptFlag = true;
        new DownloadThread().start();
    }

    /**
     * 下载apk
     */
    private class DownloadThread extends Thread {

        @SuppressWarnings("deprecation")
        @Override
        public void run() {
            InputStream is = null;
            FileOutputStream fos = null;
            try {
                URL url = new URL(mDownloadUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.connect();
                int length = conn.getContentLength();
                is = conn.getInputStream();
                // 必须为MODE_WORLD_READABLE模式，否则不能成功解析包
                fos = mContext.openFileOutput(mAppFile, Context.MODE_WORLD_READABLE);

                int count = 0;
                byte[] buf = new byte[1024];
                while (mInterceptFlag) {
                    int numread = is.read(buf);
                    count += numread;
                    if (numread <= 0) {
                        fos.flush();
                        mHandler.sendEmptyMessage(DOWN_OVER);
                        break;
                    }
                    mProgress = (int) (((float) count / length) * 100);
                    mHandler.sendEmptyMessage(DOWN_UPDATE);
                    fos.write(buf, 0, numread);
                }
            } catch (MalformedURLException e) {
                mHandler.sendEmptyMessage(DOWN_UPDATE);
                e.printStackTrace();
            } catch (IOException e) {
                mHandler.sendEmptyMessage(DOWN_UPDATE);
            } finally {
                try {
                    if (fos != null) {
                        fos.close();
                    }

                    if (is != null) {
                        is.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case DOWN_UPDATE:
                    mProgressBar.setProgress(mProgress);
                    // mProgressTextView.setText(mProgress + "%");
                    break;
                case DOWN_OVER:
                    if (mDownloadDialog != null) {

                        mDownloadDialog.dismiss();
                    }
                    installApk();
                    break;
                case DOWN_CANCEL:
                    if (mDownloadDialog != null) {

                        mDownloadDialog.dismiss();
                    }
                    break;
                default:
                    break;
            }
        }

        ;
    };

    /**
     * 安装apk
     *
     * @param
     */
    private void installApk() {
        File tempFile = new File("/mnt/internal_sd/install.sys");// 安装权限
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(Intent.ACTION_VIEW);
        File file = mContext.getFileStreamPath(mAppFile);
        intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
        mContext.startActivity(intent);

        mContext.getFileStreamPath(mAppFile).deleteOnExit();
        tempFile.deleteOnExit();
    }
}
