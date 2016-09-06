package com.cncom.library.lbs.baidu.service;


import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;

import com.baidu.location.BDLocation;
import com.cncom.library.lbs.baidu.BaiduLocationManager;
import com.cncom.library.lbs.baidu.event.LocationChangeEvent;
import com.cncom.library.lbs.baidu.event.RequestLocationEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;


/**
 * 
 * @author chenkai
 *
 */
public class BaiduLocationService extends Service {
	private static String TAG = "BaiduLocationService";
	private static final boolean DEBUG = false;

    public static final String EXTRA_OVERTIME_CHECK = "overtime_check";
    public static final String EXTRA_REQUEST_CODE = "request_code";
    public static final int WHAT_OVERTIME_CHECK = 100;

    private boolean mNeedReportLocation = false;
    private MyLocationChangeCallback mLocationChangeCallback;
    private PowerManager.WakeLock mWakeLock;
    /**最近一次的定位*/
    public BDLocation mLastLocation;
    /**上次更新位置的时间*/
    public static final String KEY_LAST_LOCATION_UPDATE_TIME = "last_location_update_time";

    private Handler mHandler;

    public SharedPreferences mPreferManager;

    public String ACTION_REQUEST_LOCATION = "";
    public String ACTION_STOP_LOCATION = "";
    private static BaiduLocationService INSTANCE;

    public static BaiduLocationService getLocationService() {
        return INSTANCE;
    }

    private HashMap<Integer,RequestLocationEvent> requestLocationEventMap = new HashMap();

    @Override
    public void onCreate() {
        super.onCreate();
        INSTANCE = this;
        Log.v(TAG, "onCreate");
        mPreferManager = PreferenceManager.getDefaultSharedPreferences(this);
        PowerManager powerManager = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);

        mHandler = new Handler() {

            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                int what = msg.what;
                if(what == WHAT_OVERTIME_CHECK) {
                    LocationChangeEvent locationChangeEvent = new LocationChangeEvent();
                    locationChangeEvent.requestCode = msg.arg1;
                    locationChangeEvent.mLastBDLocation = mLastLocation;
                    locationChangeEvent.mBDLocation = null;
                    locationChangeEvent.mCode = LocationChangeEvent.CODE_TIME_CANCEL;
                    EventBus.getDefault().post(locationChangeEvent);
                    removeLocationRequest(msg.arg1);
                }
            }
        };
        mLocationChangeCallback = new MyLocationChangeCallback();

        EventBus.getDefault().register(this);
        ACTION_REQUEST_LOCATION = getRequestLocationAction(this);
        ACTION_STOP_LOCATION = getStopLocationAction(this);
    }

    /**返回最近一次定位的时间*/
    public long getLastLocationUpdateTime() {
        return mPreferManager.getLong(KEY_LAST_LOCATION_UPDATE_TIME, -1);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        if (intent == null) {
            return;
        }
        onServiceIntent(intent.getAction(), intent);
    }

    public void onDestroy() {
        super.onDestroy();
        if (!mWakeLock.isHeld()) {
            mWakeLock.acquire();
        }
        EventBus.getDefault().unregister(this);
    }


	protected boolean onServiceIntent(String action, Intent intent) {
        Log.d(TAG, "onServiceIntent " + intent);
        if (ACTION_REQUEST_LOCATION.equals(action)) {
            Log.d(TAG, "sendEmptyMessage(MSG_UPDATE_LOCATION)");
            int overtimeCheck = intent.getIntExtra(EXTRA_OVERTIME_CHECK, 0);
            int reuqestCode = intent.getIntExtra(EXTRA_REQUEST_CODE, 0);
            requestLocation(overtimeCheck, reuqestCode);
            return true;
        } else if (ACTION_STOP_LOCATION.equals(action)) {
            int reuqestCode = intent.getIntExtra(EXTRA_REQUEST_CODE, 0);
            stopLocation(reuqestCode);
        }
        return false;
	}

    /**
     * 请求定位
     * @param overtimeCheck
     */
    public void requestLocation(int overtimeCheck, int requestCode) {
        RequestLocationEvent requestLocationEvent = new RequestLocationEvent();
        requestLocationEvent.mEnable = true;
        requestLocationEvent.mOvertimeCheck = overtimeCheck;
        requestLocationEvent.requestCode = requestCode;
        EventBus.getDefault().post(requestLocationEvent);
    }
    /**
     * 停止定位服务
     */
    public void stopLocation() {
        RequestLocationEvent requestLocationEvent = new RequestLocationEvent();
        requestLocationEvent.mEnable = false;
        requestLocationEvent.mOvertimeCheck = 0;
        requestLocationEvent.requestCode = 0;
        EventBus.getDefault().post(requestLocationEvent);
    }

    /**
     * 停止某一次定位
     */
    public void stopLocation(int requestCode) {
        if (requestCode > 0) {
            LocationChangeEvent locationChangeEvent = new LocationChangeEvent();
            locationChangeEvent.requestCode = requestCode;
            locationChangeEvent.mLastBDLocation = mLastLocation;
            locationChangeEvent.mBDLocation = null;
            locationChangeEvent.mCode = LocationChangeEvent.CODE_TIME_CANCEL;
            EventBus.getDefault().post(locationChangeEvent);
            removeLocationRequest(requestCode);
        } else {
            RequestLocationEvent requestLocationEvent = new RequestLocationEvent();
            requestLocationEvent.mEnable = false;
            requestLocationEvent.mOvertimeCheck = 0;
            requestLocationEvent.requestCode = requestCode;
            EventBus.getDefault().post(requestLocationEvent);
        }

    }

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}


    private class MyLocationChangeCallback implements BaiduLocationManager.LocationChangeCallback {
        @Override
        public boolean isLocationChanged(BDLocation location) {
            Log.d(TAG, "isLocationChanged location " + location);

            boolean hasLatLng = location != null
                    && location.getLatitude() > 0
                    && location.getLatitude() > 0;
//            boolean hasCity = false;
//            if (hasLatLng) {
//                hasCity = !TextUtils.isEmpty(location.getCity());
//            }

//            return hasLatLng && hasCity;
            return hasLatLng;
        }

        @Override
        public boolean onLocationChanged(BDLocation location) {
            mHandler.removeMessages(WHAT_OVERTIME_CHECK);
//            BaiduLocationManager.getInstance().removeLocationChangeCallback(mLocationChangeCallback);
//            BaiduLocationManager.getInstance().mLocationClient.stop();
//            mNeedReportLocation = false;
            if (mWakeLock.isHeld()) {
                mWakeLock.release();
            }
            if (mLastLocation != location) {
                mLastLocation = location;
            }

            mPreferManager.edit().putLong(KEY_LAST_LOCATION_UPDATE_TIME, new Date().getTime()).commit();
            responseLocationRequest(location);
            return true;
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onEventBackgroundThread(RequestLocationEvent requestLocationEvent) {
        synchronized (mLocationChangeCallback) {
            boolean enabled = requestLocationEvent.mEnable;
            if (mNeedReportLocation != enabled) {
                mNeedReportLocation = enabled;
                if (mNeedReportLocation) {
                    if (!mWakeLock.isHeld()) {
                        mWakeLock.acquire();
                    }
                    BaiduLocationManager.getInstance().addLocationChangeCallback(mLocationChangeCallback);
                    if (!BaiduLocationManager.getInstance().mLocationClient.isStarted()) {
                        BaiduLocationManager.getInstance().mLocationClient.start();
                    }
                    addLocationRequest(requestLocationEvent);
                } else {
                    BaiduLocationManager.getInstance().removeLocationChangeCallback(mLocationChangeCallback);
                    BaiduLocationManager.getInstance().mLocationClient.stop();
                    if (mWakeLock.isHeld()) {
                        mWakeLock.release();
                    }
                }
            } else {
                if (mNeedReportLocation) {
                    addLocationRequest(requestLocationEvent);
                }
            }

        }
    }


    private void addLocationRequest(RequestLocationEvent requestLocationEvent) {
        synchronized(requestLocationEventMap) {
            if (!requestLocationEventMap.containsKey(requestLocationEvent.requestCode)) {
                requestLocationEventMap.put(requestLocationEvent.requestCode, requestLocationEvent);
            }
            BaiduLocationManager.getInstance().mLocationClient.requestLocation();
            postRequestLocationOvertimeCheck(requestLocationEvent.mOvertimeCheck, requestLocationEvent.requestCode);
        }
    }

    private void removeLocationRequest(int requestCode) {
        synchronized(requestLocationEventMap) {
            if (requestLocationEventMap.containsKey(requestCode)) {
                requestLocationEventMap.remove(requestCode);
            }
        }

    }

    /**
     * 定位后每个请求都要发一个定位结果，并且要移除请求，不在响应。
     * @param bdLocation
     */
    private void responseLocationRequest(BDLocation bdLocation) {
        synchronized(requestLocationEventMap) {
            Iterator<Integer> requestIterator = requestLocationEventMap.keySet().iterator();
            while(requestIterator.hasNext()) {
                int requestCode = requestIterator.next();
                LocationChangeEvent locationChangeEvent = new LocationChangeEvent();
                locationChangeEvent.mLastBDLocation = mLastLocation;
                locationChangeEvent.mBDLocation = bdLocation;
                locationChangeEvent.requestCode = requestCode;
                EventBus.getDefault().post(locationChangeEvent);
            }
            requestLocationEventMap.clear();
        }

    }

    private void postRequestLocationOvertimeCheck(int overtime, int requestCode) {
        if (overtime > 0) {
           mHandler.removeMessages(WHAT_OVERTIME_CHECK);
            Message message = Message.obtain();
            message.what = WHAT_OVERTIME_CHECK;
            message.arg1 = requestCode;
            mHandler.sendMessageDelayed(message, overtime * 1000);
        }
    }



    /**
     * 开始报告位置坐标
     * @param context
     */
    public static void startRequestLocation(Context context) {
        Intent service = new Intent();
        service.setAction(getRequestLocationAction(context));
        service.setPackage(context.getPackageName());
        context.startService(service);
    }

    public static void stopLocation(Context context) {
        Intent service = new Intent();
        service.setAction(getStopLocationAction(context));
        service.setPackage(context.getPackageName());
        context.startService(service);
    }

    public static void stopLocation(Context context, int requestCode) {
        Intent service = new Intent();
        service.setAction(getStopLocationAction(context));
        service.setPackage(context.getPackageName());
        service.putExtra(EXTRA_REQUEST_CODE, requestCode);
        context.startService(service);
    }

    public static void startRequestLocation(Context context, int overtimeCheck) {
        Intent service = new Intent();
        service.setAction(getRequestLocationAction(context));
        service.putExtra(EXTRA_OVERTIME_CHECK, overtimeCheck);
        service.setPackage(context.getPackageName());
        context.startService(service);
    }

    public static void startRequestLocation(Context context, int overtimeCheck, int requestCode) {
        Intent service = new Intent();
        service.setAction(getRequestLocationAction(context));
        service.putExtra(EXTRA_OVERTIME_CHECK, overtimeCheck);
        service.putExtra(EXTRA_REQUEST_CODE, requestCode);
        service.setPackage(context.getPackageName());
        context.startService(service);
    }

    public static String getRequestLocationAction(Context context) {
        return context.getPackageName() + ".intent.ACTION_REQUEST_LOCATION";
    }

    public static String getStopLocationAction(Context context) {
        return context.getPackageName() + ".intent.ACTION_STOP_LOCATION";
    }
}
