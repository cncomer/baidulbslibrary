package com.cncom.library.lbs.baidu.event;

import com.baidu.location.BDLocation;

/**
 * Created by bestjoy on 15/9/2.
 */
public class LocationChangeEvent {
    public int mId = -1;

    public int mCode = CODE_RETURN_LOCATION_OK;
    public BDLocation mBDLocation;
    public BDLocation mLastBDLocation;
    public int requestCode = 0;

    public boolean isReturnLocation() {
        return mCode == CODE_RETURN_LOCATION_OK;
    }

    /**定位失败，超时了*/
    public static final int CODE_TIME_CANCEL = 1;
    /**成功返回*/
    public static final int CODE_RETURN_LOCATION_OK = 200;
}
