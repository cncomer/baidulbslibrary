package com.cncom.library.lbs.baidu.streetscape;

import android.content.Context;
import android.util.Log;

import com.baidu.lbsapi.BMapManager;
import com.baidu.lbsapi.MKGeneralListener;

/**
 * Created by bestjoy on 16/3/31.
 * 百度街景
 */
public class StreetscapeManager {

    private static final String TAG = "StreetscapeManager";
    private static StreetscapeManager mInstance = new StreetscapeManager();
    public BMapManager mBMapManager = null;
    private Context mContext;

    public static StreetscapeManager getInstance() {
        return mInstance;
    }

    public void setContext(Context context) {
        mContext = context;
        initEngineManager(context);
    }

    public String getString(int resId) {
        return mContext.getString(resId);
    }

    public void initEngineManager(Context context) {
        if (mBMapManager == null) {
            mBMapManager = new BMapManager(context);
        }

        if (!mBMapManager.init(new MyGeneralListener())) {
            Log.e(TAG, "BMapManager  初始化错误!");
        }
        Log.d("ljx", "initEngineManager");
    }

    // 常用事件监听，用来处理通常的网络错误，授权验证错误等
    public static class MyGeneralListener implements MKGeneralListener {

        @Override
        public void onGetPermissionState(int iError) {
            // 非零值表示key验证未通过
            if (iError != 0) {
                // 授权Key错误：
                Log.e(TAG, "onGetPermissionState " + "请在AndoridManifest.xml中输入正确的授权Key,并检查您的网络连接是否正常！error: " + iError);
            } else {
                Log.d(TAG, "onGetPermissionState key认证成功");
            }
        }
    }
}
