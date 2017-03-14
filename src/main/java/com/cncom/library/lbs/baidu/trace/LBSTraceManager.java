package com.cncom.library.lbs.baidu.trace;

import android.content.Context;
import android.util.Log;

import com.baidu.trace.LBSTraceClient;
import com.baidu.trace.LocationMode;
import com.baidu.trace.OnStartTraceListener;
import com.baidu.trace.OnStopTraceListener;
import com.baidu.trace.OnTrackListener;
import com.baidu.trace.Trace;

import java.util.HashMap;
import java.util.Map;

/**
 * 鹰眼管理
 * 注意: SDK目前只支持一个鹰眼轨迹
 * SDK DOC参见http://lbsyun.baidu.com/trace
 * SDK API DOC http://wiki.lbsyun.baidu.com/cms/android-yingyan/doc/1216v2.1.15/index.html
 * Created by bestjoy on 16/12/28.
 */

public class LBSTraceManager {
    private static final String TAG = "LBSTraceManager";
    private static LBSTraceManager INSTANCE;

    protected Context context;
    protected LBSTraceClient client;

    protected LBSTraceManager() {}

    public synchronized static void installDefaultInstance(LBSTraceManager instance) {
        INSTANCE = instance;
    }

    public synchronized static LBSTraceManager getInstance() {
        if(INSTANCE == null) {
            INSTANCE = new LBSTraceManager();
        }
        return INSTANCE;
    }


    public void setContext(Context context) {
        this.context = context;
        //实例化轨迹服务客户端
        client = new LBSTraceClient(context);
        initLBSTraceClient(10, 60, 1, LocationMode.High_Accuracy);
    }

    public LBSTraceClient getLBSTraceClient() {
        return client;
    }

    public void setOnTrackListener(OnTrackListener trackListener) {

        if (trackListener != null) {
            client.setOnTrackListener(trackListener);
        }
        if (true) {
            return;
        }
        //下面是注解
        client.setOnTrackListener(new OnTrackListener() {

            /**
             * 请求失败回调接口
             */
            @Override
            public void onRequestFailedCallback(String message) {

            }

            /**
             * 查询历史轨迹回调接口
             * @param message
             */
            public void onQueryHistoryTrackCallback(String message) {
            }

            /**
             * 查询里程回调接口
             * @param message
             */
            public void onQueryDistanceCallback(String message) {
            }

            /**
             * 轨迹属性回调接口
             * @return
             */
            public Map<String, String> onTrackAttrCallback() {
                HashMap<String, String> trackAttrs = new HashMap<String, String>();
                return trackAttrs;
            }

        });
    }

    /**
     *
     * 设置位置采集和打包周期
     * 概念：
     定位周期：多久定位一次，在定位周期大于15s时，SDK会将定位周期设置为5的倍数（如设置采集周期为18s，SDK会调整为15s；设置为33s，SDK会调整为30s）

     回传周期：鹰眼为节省电量和流量，并不是定位一次就回传一次数据，而是隔段时间将一批定位数据打包压缩回传。（回传周期最大不要超过定位周期的10倍，例如，定位周期为10s，则回传周期最好不要大于60s）

     鹰眼SDK支持开发者动态设置定位周期和回传周期（2s到5分钟），可以在开启服务前设置，也可以在服务运行过程中动态设置，随时生效。开发者可自行判断终端的运动速度和状态，动态调整定位周期。

     使用示例：
     //位置采集周期
     int gatherInterval = 10;
     //打包周期
     int packInterval = 60;
     // http协议类型
     int protocolType = 1;


     复杂网络状态下，SDK的连接与轨迹回传策略

     在网络状态持续良好的情况下，SDK将按照指定频率回传轨迹。如果在开启轨迹追踪时、追踪过程中、或结束轨迹追踪时遇到网络不稳定或断网的情况，SDK也有相应处理策略:

     ①开启追踪追踪，调用startTrace()时
     只要调用startTrace()，不论网络状态和startTrace()回调的状态码是什么，SDK都将立即开始采集并缓存轨迹，若此时网络连接正常，将实时回传轨迹；若连上不可上网的Wi-Fi或网络断开，则将缓存轨迹在手机数据库中，并自动监听网络，待联网时自动回传缓存数据（处于缓存状态时，因为轨迹在设备端，实时位置、历史轨迹和里程都不能获取到最新的轨迹信息）。

     ②轨迹追踪过程中
     若追踪过程中，出现网络中断、连上不可上网的Wi-Fi，或网络频繁切换时，SDK都将自动开启缓存模式，将采集的轨迹数据保存到数据库中，并自动监听网络，待联网时自动回传缓存数据。

     ③停止轨迹追踪，调用stopTrace()时
     只要调用stopTrace()，无论网络状态和返回值如何，SDK都将立即停止轨迹采集。
     若此时网络连接正常，SDK将加快上传已缓存的轨迹，上传成功后在回调用返回停止服务成功；
     若此时已断网或回传过程中断网，将立即停止回传，缓存数据存储至手机数据库中，返回停止服务成功，再次startTrace时才会继续回传缓存数据。

     * @param gatherInterval 采集周期
     * @param packInterval 打包周期
     * @param protocolType http协议类型
     * @param locationMode
     * Battery_Saving 低功耗定位模式，仅使用网络定位(WiFi和基站定位)
       Device_Sensors 仅使用设备(GPS)定位
       High_Accuracy 高精度定位模式，GPS与网络综合定位
     */
    public void initLBSTraceClient(int gatherInterval, int packInterval, int protocolType, LocationMode locationMode) {
        // 设置采集和打包周期
        client. setInterval(gatherInterval, packInterval);
        // 设置定位模式
        client. setLocationMode(locationMode);
        // 设置http协议类型
        client. setProtocolType(protocolType);
    }

    /**
     *
     * @param serviceId 轨迹服务ID
     * @param entityName 设备名称
     * @param traceType 轨迹服务类型（0 : 不上传位置数据，也不接收报警信息； 1 : 不上传位置数据，但接收报警信息；2 : 上传位置数据，且接收报警信息）
     * @return
     */
    public Trace newTrace(long serviceId, String entityName, int traceType) {
        Trace trace = new Trace(context, serviceId, entityName, traceType);
        return trace;
    }

    /**
     * 开启轨迹服务，该方法会持有电量锁，目的是避免手机锁屏后一段时间，cpu可能会进入休眠模式，此时无法严格按照采集周期获取定位依据，导致轨迹点缺失
     *
     * //实例化开启轨迹服务回调接口
         OnStartTraceListener  startTraceListener = new OnStartTraceListener() {
             //开启轨迹服务回调接口（errorNo : 错误编码，arg1 : 消息内容，详情查看类参考）
             @Override
             public void onTraceCallback(int errorNo, String arg1) {
                 0：success，
                 10000：开启服务请求发送失败，
                 10001：开启服务失败，
                 10002：参数错误，
                 10003：网络连接失败，
                 10004：网络未开启，
                 10005：服务正在开启，
                 10006：服务已开启，
                 10007：服务正在停止，
                 10008：开启缓存，
                 10009：已开启缓存
             }
             //轨迹服务推送接口（用于接收服务端推送消息，arg0 : 消息类型，arg1 : 消息内容，详情查看类参考）
             @Override
             public void onTracePushCallback(byte arg0, String arg1) {

                 0x01：配置下发，
                 0x02：语音消息，
                 0x03：服务端围栏报警消息，
                 0x04：本地围栏报警消息，
                 0x05~0x40：系统预留，
                 0x41~0xFF：开发者自定义

                 服务端报警消息(0x03)格式：
                 {
                 "fence_id":围栏编号(类型:整型),
                 "fence":"围栏名称(类型:String)",
                 "monitored_person":"监控对象名称(类型:String)",
                 "action":触发动作(离开还是进入围栏, 类型:int, 1:进入, 2:离开),
                 "time":触发报警时间戳(类型:long),
                 "longitude":报警点的经度(类型:double),
                 "latitude":报警点的纬度(类型:double,
                 "coord_type":报警点的坐标系(类型:int, 1：GPS经纬度, 2：国测局经纬度, 3：百度经纬度),
                 "radius":报警点定位精度(类型:int, 单位：米),
                 "pre_point": {
                 "longitude":上一个定位点的经度(类型:double),
                 "latitude":上一个定位点的纬度(类型:double),
                 "coord_type":上一个定位点的坐标系(类型:int, 1：GPS经纬度, 2：国测局经纬度, 3：百度经纬度),
                 "time":上一个定位点的定位时间(类型:long),
                 "radius":上一个定位点的定位精度(类型:int, 单位：米)
                 }
                 }
                 【示例】
                 {"fence_id":1,"fence":"围栏名称","monitored_person":"监控对象名称","action":2,"time":1481099583,
                 "longitude":116.3132045002151,"latitude":40.04783445671323,"coord_type":3,"radius":15,
                 "pre_point":{
                 "longitude":116.31320859444753,"latitude":40.04783438345468,"coord_type":3,"time":1481084260,"radius":15
                 }
                 }

                 本地围栏报警消息(0x04)格式：
                 {
                 "fence_id":围栏编号(类型:整型),
                 "fence":"围栏名称(类型:String)",
                 "monitored_person":"监控对象名称(类型:String)",
                 "action":触发动作(离开还是进入围栏, 类型:int, 1:进入, 2:离开),
                 "time":触发时间戳(类型:long)
                 }
                 【示例】
                 {"fence_id":1,"fence":"围栏名称","monitored_person":"监控对象名称","action":2,"time":1470028477}
             }
         };

     * @param trace
     * @param onStartTraceListener
     */
    public void startTrace(Trace trace, OnStartTraceListener onStartTraceListener) {
        Log.d(TAG, "startTrace serviceId:" + trace.getServiceId() + ",entityName:" + trace.getEntityName());
        synchronized (keepTraceMap) {
            client.startTrace(trace, onStartTraceListener);
            keepTrace(trace);
        }

    }

    public void startTrace(Trace trace) {
        Log.d(TAG, "startTrace serviceId:" + trace.getServiceId() + ",entityName:" + trace.getEntityName());
        synchronized (keepTraceMap) {
            client.startTrace(trace);
            keepTrace(trace);
        }

    }

    /**
     * 停止轨迹服务
     * //实例化停止轨迹服务回调接口
         OnStopTraceListener stopTraceListener = new OnStopTraceListener(){
             // 轨迹服务停止成功
             @Override
             public void onStopTraceSuccess() {
             }
             // 轨迹服务停止失败（arg0 : 错误编码，arg1 : 消息内容，详情查看类参考）
             @Override
             public void onStopTraceFailed(int arg0, String arg1) {
             }
         };
     * @param trace
     * @param stopTraceListener
     */
    public void stopTrace(Trace trace, OnStopTraceListener stopTraceListener) {
        Log.d(TAG, "stopTrace serviceId:" + trace.getServiceId() + ",entityName:" + trace.getEntityName());
        synchronized (keepTraceMap) {
            client.stopTrace(trace, stopTraceListener);
            removeKeepTrace(trace);
        }

    }

    private HashMap<Long, Trace> keepTraceMap = new HashMap<>();
    private HashMap<Long, Boolean> isTraceStartedMap = new HashMap<>();

    /**
     * 设置轨迹的开启状态
     * @param trace
     * @param started
     */
    public void setTraceStarted(Trace trace, boolean started) {
        synchronized (keepTraceMap) {
            if (keepTraceMap.containsKey(trace.getServiceId())) {
                isTraceStartedMap.put(trace.getServiceId(), started);
            }
        }
    }

    public void setTraceStarted(long serviceId, boolean started) {
        synchronized (keepTraceMap) {
            if (keepTraceMap.containsKey(serviceId)) {
                isTraceStartedMap.put(serviceId, started);
            }
        }
    }

    public boolean isTraceStarted(long serviceId) {
        synchronized (keepTraceMap) {
            if (keepTraceMap.containsKey(serviceId)) {
                return isTraceStartedMap.get(serviceId);
            }
            return false;
        }

    }
    /**
     * 保持某个Trace
     * @param keepTrace
     */
    public void keepTrace(Trace keepTrace) {
        synchronized (keepTraceMap) {
            if (!keepTraceMap.containsKey(keepTrace.getServiceId())) {
                keepTraceMap.put(keepTrace.getServiceId(), keepTrace);
            }
        }

    }
    public Trace getKeepTrace(long serviceId) {
        synchronized (keepTraceMap) {
            return keepTraceMap.get(serviceId);
        }
    }

    public HashMap<Long, Trace> getAllKeepTrace() {
        return keepTraceMap;
    }

    public Trace removeKeepTrace(Trace trace) {
        synchronized (keepTraceMap) {
            return keepTraceMap.remove(trace.getServiceId());
        }
    }

}
