package com.vincent.filepicker;

import android.content.Context;
import android.widget.Toast;

import java.lang.ref.WeakReference;

/**
 * Created by Vincent Woo
 * Date: 2016/1/22
 * Time: 17:27
 */
public class ToastUtil {
    private static WeakReference<Context> mContext;
    private static ToastUtil mInstance;
    private Toast mToast;

    public static ToastUtil getInstance(Context ctx) {
        if (mInstance == null || mContext.get() == null) {
            mInstance = new ToastUtil(ctx);
        }

        return mInstance;
    }

    private ToastUtil(Context ctx) {
        mContext = new WeakReference<>(ctx);
    }

    public void showToast(String text) {
        if(mToast == null) {
            mToast = Toast.makeText(mContext.get(), text, Toast.LENGTH_SHORT);
        } else {
            mToast.setText(text);
            mToast.setDuration(Toast.LENGTH_SHORT);
        }
        mToast.show();
    }

    public void showToast(int resID) {
        showToast(mContext.get().getResources().getString(resID));
    }

    public void cancelToast() {
        if (mToast != null) {
            mToast.cancel();
        }
    }
}
