package com.cncom.library.lbs.baidu.event;

/**
 * Created by bestjoy on 15/9/2.
 */
public class RequestLocationEvent {
    public int mId = -1;

    /**多少秒没有定位成功，我们取消本次定位，0表示不取消*/
    public int mOvertimeCheck = 0;

    public boolean mEnable = true;

    public int requestCode = 0;
}
