package com.cncom.library.lbs.baidu;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * 使用说明http://developer.baidu.com/map/index.php?title=android-locsdk/guide/v4-2
 *
 * API控制台 http://lbsyun.baidu.com/apiconsole/key
 * @author bestjoy
 *
 */
public class BaiduLocationManager {
	public static final String TAG = "BaiduLocationManager";
	public LocationClient mLocationClient;
	private List<LocationChangeCallback> mLocationChangeCallbackList = new LinkedList<LocationChangeCallback>();
	private Context mContext;
	private static final BaiduLocationManager INSTANCE = new BaiduLocationManager();
	private MyLocationListener mMyLocationListener;

	/**对于不同的保修卡，我们只要确保该变量为正确的应用包名即可*/
	public Handler mHandler;
	private Toast mLongToast;
	private Toast mShortToast;

	private BaiduLocationManager(){}


	
	public static BaiduLocationManager getInstance() {
		return INSTANCE;
	}
	
	public void setContext(Context context) {
		mContext = context;
		mHandler = new Handler();
		mLongToast = Toast.makeText(mContext, "Test", Toast.LENGTH_LONG);
		mShortToast = Toast.makeText(mContext, "Test", Toast.LENGTH_SHORT);
        SDKInitializer.initialize(mContext);
        mLocationClient = new LocationClient(context);
		mMyLocationListener = new MyLocationListener();
		setScanSpan(0);
	}

	public String getString(int resId) {
		return mContext.getString(resId);
	}


	public void setScanSpan(int timeInMiliSecond) {
		mLocationClient.unRegisterLocationListener(mMyLocationListener);
		LocationClientOption option = new LocationClientOption();
		/**
		 * //Hight_Accuracy高精度、Battery_Saving低功耗、Device_Sensors仅设备(GPS)
		 * 1、高精度模式定位策略：这种定位模式下，会同时使用网络定位和GPS定位，优先返回最高精度的定位结果；
		 * 2、低功耗模式定位策略：该定位模式下，不会使用GPS，只会使用网络定位（Wi-Fi和基站定位）；
		 * 3、仅用设备模式定位策略：这种定位模式下，不需要连接网络，只使用GPS进行定位，这种模式下不支持室内环境的定位。
		 */
		option.setLocationMode(LocationClientOption.LocationMode.Battery_Saving);//设置定位模式
		option.setCoorType("bd09ll");//返回的定位结果是百度经纬度，默认值gcj02, 定位SDK可以返回bd09、bd09ll、gcj02三种类型坐标，若需要将定位点的位置通过百度Android地图 SDK进行地图展示，请返回bd09ll，将无偏差的叠加在百度地图上
		/**
		 * 说明：
			当所设的整数值大于等于1000（ms）时，定位SDK内部使用定时定位模式。调用requestLocation( )后，每隔设定的时间，定位SDK就会进行一次定位。
			如果定位SDK根据定位依据发现位置没有发生变化，就不会发起网络请求，返回上一次定位的结果；如果发现位置改变，就进行网络请求进行定位，得到新的定位结果。
			定时定位时，调用一次requestLocation，会定时监听到定位结果。

			当不设此项，或者所设的整数值小于1000（ms）时，采用一次定位模式。每调用一次requestLocation( )，定位SDK会发起一次定位。请求定位与监听结果一一对应。
			设定了定时定位后，可以热切换成一次定位，需要重新设置时间间隔小于1000（ms）即可。locationClient对象stop后，将不再进行定位。如果设定了定时定位模式后，
			多次调用requestLocation（），则是每隔一段时间进行一次定位，同时额外的定位请求也会进行定位，但频率不会超过1秒一次。
		 */
		option.setScanSpan(timeInMiliSecond);//设置发起定位请求的间隔时间为5000ms
		option.setIsNeedAddress(true);
		option.setOpenGps(false);//设置是否打开gps，使用gps前提是用户硬件打开gps。默认是不打开gps的。
		option.setProdName(mContext.getString(R.string.app_name));//设置产品线名称。强烈建议您使用自定义的产品线名称，方便我们以后为您提供更高效准确的定位服务
		mLocationClient.setLocOption(option);
		mLocationClient.registerLocationListener(mMyLocationListener);
	}
	
	public synchronized void addLocationChangeCallback(LocationChangeCallback callback) {
		if (!mLocationChangeCallbackList.contains(callback)) {
			mLocationChangeCallbackList.add(callback);
		}
	}
	
	public synchronized void removeLocationChangeCallback(LocationChangeCallback callback) {
		if (mLocationChangeCallbackList.contains(callback)) {
			mLocationChangeCallbackList.remove(callback);
		}
	}
	
	/**
	 * 实现实位回调监听
	 */
	public class MyLocationListener implements BDLocationListener {

		@Override
		public void onReceiveLocation(BDLocation location) {
			//Receive Location 
//			receiveLocationAsync(location);
            if (location == null) {
                Log.e(TAG, "onReceiveLocation location=" + location);
                return;
            }
            StringBuffer sb = new StringBuffer(256);
			Log.d(TAG, "ReceiveLocationTask time " + new Date().getTime());
            sb.append("time : ");
            sb.append(location.getTime());
            sb.append("\nerror code : ");
            sb.append(location.getLocType());
            sb.append("\nlatitude : ");
            sb.append(location.getLatitude());
            sb.append("\nlontitude : ");
            sb.append(location.getLongitude());
            sb.append("\nradius : ");
            sb.append(location.getRadius());
            if (location.getLocType() == BDLocation.TypeGpsLocation){
                sb.append("\nspeed : ");
                sb.append(location.getSpeed());
                sb.append("\nsatellite : ");
                sb.append(location.getSatelliteNumber());
                sb.append("\ndirection : ");
                sb.append("\naddr : ");
                sb.append(location.getAddrStr());
                sb.append(location.getDirection());
            } else if (location.getLocType() == BDLocation.TypeNetWorkLocation){
                sb.append("\naddr : ");
                sb.append(location.getAddrStr());
                //运营商信息
                sb.append("\noperationers : ");
                sb.append(location.getOperators());
            }
			Log.d(TAG, sb.toString());
            if (location.getLocType() == BDLocation.TypeGpsLocation
                    || location.getLocType() == BDLocation.TypeNetWorkLocation){
                if (location != null) {
					List<LocationChangeCallback> temp = null;
					synchronized(BaiduLocationManager.getInstance()) {
						temp = new ArrayList<LocationChangeCallback>(mLocationChangeCallbackList);
					}
					Iterator<LocationChangeCallback> iterator = temp.iterator();
					LocationChangeCallback callback;
					while(iterator.hasNext()){
						callback = iterator.next();
						if (callback != null &&  callback.isLocationChanged(location)) {
							callback.onLocationChanged(location);
						}
					}

//                    for(LocationChangeCallback callback : mLocationChangeCallbackList) {
//                        if (callback != null &&  callback.isLocationChanged(location)) {
//                            callback.onLocationChanged(location);
//                        }
//                    }
                }
            }
		}

	}
	
//	private void receiveLocationAsync(BDLocation location) {
//		new ReceiveLocationTask(location).execute();
//	}
	
//	private class ReceiveLocationTask extends AsyncTask<Void, LocationChangeCallback, Void> {
//
//		private BDLocation _location;
//		public ReceiveLocationTask(BDLocation location) {
//			_location = location;
//		}
//		@Override
//		protected Void doInBackground(Void... params) {
//			StringBuffer sb = new StringBuffer(256);
//			DebugUtils.logD(TAG, "ReceiveLocationTask time " + new Date().getTime());
//			sb.append("time : ");
//			sb.append(_location.getTime());
//			sb.append("\nerror code : ");
//			sb.append(_location.getLocType());
//			sb.append("\nlatitude : ");
//			sb.append(_location.getLatitude());
//			sb.append("\nlontitude : ");
//			sb.append(_location.getLongitude());
//			sb.append("\nradius : ");
//			sb.append(_location.getRadius());
//			if (_location.getLocType() == BDLocation.TypeGpsLocation){
//				sb.append("\nspeed : ");
//				sb.append(_location.getSpeed());
//				sb.append("\nsatellite : ");
//				sb.append(_location.getSatelliteNumber());
//				sb.append("\ndirection : ");
//				sb.append("\naddr : ");
//				sb.append(_location.getAddrStr());
//				sb.append(_location.getDirection());
//			} else if (_location.getLocType() == BDLocation.TypeNetWorkLocation){
//				sb.append("\naddr : ");
//				sb.append(_location.getAddrStr());
//				//运营商信息
//				sb.append("\noperationers : ");
//				sb.append(_location.getOperators());
//			}
//			DebugUtils.logD(TAG, sb.toString());
//			if (_location.getLocType() == BDLocation.TypeGpsLocation
//				 || _location.getLocType() == BDLocation.TypeNetWorkLocation){
//				if (_location != null) {
//					for(LocationChangeCallback callback : mLocationChangeCallbackList) {
//						if (callback != null &&  callback.isLocationChanged(_location)) {
//							publishProgress(callback);
//						}
//					}
//				}
//			}
//
//			return null;
//		}
//
//		@Override
//		protected void onProgressUpdate(LocationChangeCallback... values) {
//			super.onProgressUpdate(values);
//			values[0].onLocationChanged(_location);
//		}
//		@Override
//		protected void onPostExecute(Void result) {
//			super.onPostExecute(result);
//		}
//
//		@Override
//		protected void onCancelled() {
//			super.onCancelled();
//		}
//
//	}
	
	public static interface LocationChangeCallback {
		public boolean isLocationChanged(BDLocation location);
		public boolean onLocationChanged(BDLocation location);
	}



	public static final void dialPhone(Context context, String phoneNumber) {
		launchIntent(context, new Intent(Intent.ACTION_DIAL, Uri.parse("tel:"+ phoneNumber)));
	}
	public static final void callPhone(Context context, String phoneNumber) {
		launchIntent(context, new Intent(Intent.ACTION_CALL, Uri.parse("tel:"+ phoneNumber)));
	}

	public static final void openURL(Context context, String url) {
		launchIntent(context, new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
	}

	public static final void launchIntent(Context context, Intent intent) {
		if (intent != null) {
			try {
				if (!(context instanceof Activity)) {
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				}
				context.startActivity(intent);
			} catch (ActivityNotFoundException e) {
				AlertDialog.Builder builder = new AlertDialog.Builder(context);
				builder.setMessage(R.string.s_msg_intent_failed);
				builder.setPositiveButton(android.R.string.ok, null);
				builder.show();
			}
		}
	}




	/**
	 * @deprecated  instead use {@link #showMessage(int)}
	 * @param resId
	 */
	public void showMessageAsync(final int resId) {
		showMessage(resId, Toast.LENGTH_LONG);
	}
	/**
	 * @deprecated  instead use {@link #showMessage(String)}
	 * @param msg
	 */
	public void showMessageAsync(final String msg) {
		showMessage(msg, Toast.LENGTH_LONG);
	}
	/**
	 * @deprecated  instead use {@link #showMessage(int, int)}
	 * @param resId
	 * @param length
	 */
	public void showMessageAsync(final int resId, final int length) {
		showMessage(resId, length);
	}
	/**
	 * @deprecated  instead use {@link #showMessage(String, int)}
	 * @param msg
	 * @param length
	 */
	public void showMessageAsync(final String msg, final int length) {
		showMessage(msg, length);
	}
	/**
	 * @deprecated  instead use {@link #showMessage(int, int)}
	 * @param msgId
	 * @param length
	 */
	public void showShortMessageAsync(final int msgId, final int length) {
		showMessage(msgId, length);
	}

	public void showMessage(int resId) {
		showMessage(resId, Toast.LENGTH_LONG);

	}

	public void showMessage(String msg) {
		showMessage(msg, Toast.LENGTH_LONG);

	}
	public void showMessage(final String msg, final int length) {
		if (Looper.myLooper() != Looper.getMainLooper()) {
			mHandler.post(new Runnable() {

				@Override
				public void run() {
					showMessageInternal(msg, length, false);
				}
			});
		} else {
			showMessageInternal(msg, length, true);
		}
	}

	public void showMessage(final int resId, final int length) {
		if (Looper.myLooper() != Looper.getMainLooper()) {
			mHandler.post(new Runnable() {

				@Override
				public void run() {
					showMessageInternal(resId, length, false);
				}
			});
		} else {
			showMessageInternal(resId, length, true);
		}
	}
	public void showShortMessage(int resId) {
		showMessage(resId, Toast.LENGTH_SHORT);
	}
	public void showShortMessage(String message) {
		showMessage(message, Toast.LENGTH_SHORT);
	}

	private void showMessageInternal(String msg, int length, boolean uiThread) {
		Log.d(TAG, "showMessageInternal from uiThread? " + uiThread);
		if (length == Toast.LENGTH_SHORT) {
			mShortToast.setText(msg);
			mShortToast.show();
		} else {
			mLongToast.setText(msg);
			mLongToast.show();
		}
	}
	private void showMessageInternal(int resId, int length, boolean uiThread) {
		Log.d(TAG, "showMessageInternal from uiThread? " + uiThread);
		if (length == Toast.LENGTH_SHORT) {
			mShortToast.setText(resId);
			mShortToast.show();
		} else {
			mLongToast.setText(resId);
			mLongToast.show();
		}
	}

	public void postAsync(Runnable runnable){
		mHandler.post(runnable);
	}
	public void postDelay(Runnable runnable, long delayMillis){
		mHandler.postDelayed(runnable, delayMillis);
	}

	public void removeRunnable(Runnable runnable) {
		mHandler.removeCallbacks(runnable);
	}

}
