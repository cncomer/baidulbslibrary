package com.cncom.library.lbs.baidu.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.baidu.location.BDLocation;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.Circle;
import com.baidu.mapapi.map.CircleOptions;
import com.baidu.mapapi.map.InfoWindow;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.Polyline;
import com.baidu.mapapi.map.Stroke;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.overlayutil.BikingRouteOverlay;
import com.baidu.mapapi.overlayutil.DrivingRouteOverlay;
import com.baidu.mapapi.overlayutil.PoiOverlay;
import com.baidu.mapapi.overlayutil.TransitRouteOverlay;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.geocode.GeoCodeOption;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiIndoorResult;
import com.baidu.mapapi.search.poi.PoiNearbySearchOption;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;
import com.baidu.mapapi.search.route.BikingRouteResult;
import com.baidu.mapapi.search.route.DrivingRouteResult;
import com.baidu.mapapi.search.route.OnGetRoutePlanResultListener;
import com.baidu.mapapi.search.route.PlanNode;
import com.baidu.mapapi.search.route.RoutePlanSearch;
import com.baidu.mapapi.search.route.TransitRoutePlanOption;
import com.baidu.mapapi.search.route.TransitRouteResult;
import com.baidu.mapapi.search.route.WalkingRouteResult;
import com.baidu.mapapi.utils.DistanceUtil;
import com.cncom.library.lbs.baidu.BaiduLocationManager;
import com.cncom.library.lbs.baidu.R;
import com.cncom.library.lbs.baidu.event.LocationChangeEvent;
import com.cncom.library.lbs.baidu.service.BaiduLocationService;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;


/**
 * Created by bestjoy on 16/4/11.
 */
public class BaseBaiduMapFragment extends Fragment implements View.OnClickListener{

    private static final String TAG = "BaiduMapBaseFragment";
    public static int DEFAULT_update_map_status_duration = 250;
    protected MapView mMapView = null;
    protected BaiduMap mBaiduMap =null;
    /**我的位置*/
    protected BDLocation mBDLocation;

    protected BitmapDescriptor mCurrentMarker;

    protected InfoWindow mInfoWindow;
    protected PoiInfo mCurrentPoiInfo;
    protected Marker mCurrentMarkerInfo;
    /**经度单位米*/
    protected String mFormatAccuracy;


    protected Bundle mBundle;

    protected PoiSearch mPoiSearch;
    protected RoutePlanSearch mRoutePlanSearch;

    protected GeoCoder mGeoCoderSearch;

    private BaiduMap.OnMarkerClickListener markerClickListener;
    private OnMarkerInfoWindowClickListener onMarkerInfoWindowClickListener;

    public static final String EXTRA_POI_NAME = "extra_poi_name";
    public static final String EXTRA_POI_PHONE = "extra_poi_phone";
    public static final String EXTRA_POI_BDLocation = "extra_poi_bdlocation";
    public static final String EXTRA_TITLE = "extra_title";

    /**默认的起始位置*/
    public BDLocation mStartLocation;
    public static final String EXTRA_START_LOCATION = "extra_start_location";

    private boolean mFirstOnResume = true;

    protected ProgressBar contentLoadingProgressBar;

    protected int getContentLayout() {
        return R.layout.fragment_baidu_mapview;
    }

    public static BaseBaiduMapFragment newInstance(Bundle bundle) {
        BaseBaiduMapFragment baiduMapViewBaseFragment = new BaseBaiduMapFragment();
        if (bundle != null) {
            baiduMapViewBaseFragment.setArguments(bundle);
        }
        return baiduMapViewBaseFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBundle = getArguments();
        mFormatAccuracy = BaiduLocationManager.getInstance().getString(R.string.format_accuracy);
        mFirstOnResume = true;
        mStartLocation = mBundle.getParcelable(EXTRA_START_LOCATION);

    }


    @Override
    public void onStart() {
        super.onStart();
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
        mMapView.onResume();

        if (mFirstOnResume) {
            mFirstOnResume = false;
            firstOnResume();
        }
    }

    protected void firstOnResume() {
        if (mStartLocation != null) {
            if (mStartLocation.getLatitude() > 0.0 && mStartLocation.getLongitude() > 0.0) {
                setMyLocationData(mStartLocation);
            } else if (!TextUtils.isEmpty(mStartLocation.getAddrStr())) {
                //只提供了地址，我们需要根据地址来正向地理编码获取经纬度
                mGeoCoderSearch.geocode(new GeoCodeOption()
                        .city(mStartLocation.getCity())
                        .address(mStartLocation.getAddrStr()));
            }

        }
    }
    @Override
    public void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        mMapView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        mMapView.onDestroy();
        if (mPoiSearch != null) {
            mPoiSearch.destroy();
        }

        if (mRoutePlanSearch != null) {
            mRoutePlanSearch.destroy();
        }

        if (mGeoCoderSearch != null) {
            mGeoCoderSearch.destroy();
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(getContentLayout(), container, false);

        mMapView = (MapView) view.findViewById(R.id.bmapView);
        mMapView.showScaleControl(true);
        mMapView.showZoomControls(false);

        mBaiduMap = mMapView.getMap();
        // 开启定位图层
        mBaiduMap.setMyLocationEnabled(true);
        MyLocationConfiguration config = new MyLocationConfiguration(MyLocationConfiguration.LocationMode.NORMAL, true, mCurrentMarker);
        mBaiduMap.setMyLocationConfigeration(config);

        mBaiduMap.getUiSettings().setCompassEnabled(true);

        mCurrentMarker = null;

        mBaiduMap.setOnMapClickListener(new BaiduMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                BaseBaiduMapFragment.this.onMapClick(latLng);
            }

            @Override
            public boolean onMapPoiClick(MapPoi mapPoi) {
                return false;
            }

        });

        mBaiduMap.setOnMapLongClickListener(new BaiduMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                BaseBaiduMapFragment.this.onMapLongClick(latLng);
            }

        });

        mBaiduMap.setOnMarkerClickListener(new BaiduMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                return BaseBaiduMapFragment.this.onMarkerClick(marker);
            }
        });

        mPoiSearch = PoiSearch.newInstance();
        mPoiSearch.setOnGetPoiSearchResultListener(mOnGetPoiSearchResultListener);

        mRoutePlanSearch = RoutePlanSearch.newInstance();
        mRoutePlanSearch.setOnGetRoutePlanResultListener(MyOnGetRoutePlanResultListener);

        mGeoCoderSearch = GeoCoder.newInstance();
        mGeoCoderSearch.setOnGetGeoCodeResultListener(new OnGetGeoCoderResultListener() {
            public void onGetGeoCodeResult(GeoCodeResult result) {
                BaseBaiduMapFragment.this.onGetGeoCodeResult(result);
            }

            @Override
            public void onGetReverseGeoCodeResult(ReverseGeoCodeResult result) {
                BaseBaiduMapFragment.this.onGetReverseGeoCodeResult(result);
            }
        });
        contentLoadingProgressBar = (ProgressBar) view.findViewById(R.id.progressbar);
        initMyLocationConfigeration();
        initView(view);
        return view;

    }


    public void showProgress(boolean show) {
        if (contentLoadingProgressBar != null) {
            contentLoadingProgressBar.setVisibility(show?View.VISIBLE:View.GONE);
        }
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mBundle != null && !TextUtils.isEmpty(mBundle.getString(EXTRA_TITLE))) {
            getActivity().setTitle(mBundle.getString(EXTRA_TITLE));
        }
    }

    protected void initMyLocationConfigeration() {
        BDLocation lastKnownLocation = BaiduLocationManager.getInstance().mLocationClient.getLastKnownLocation();
        if (lastKnownLocation != null) {
            Log.d(TAG, "lastKnownLocation!=null");
            mBDLocation = lastKnownLocation;
            setMyLocationData(mBDLocation);
        }
    }

    protected void initView(View content) {
        content.findViewById(R.id.ic_location_me).setOnClickListener(this);
    }

    protected void hideInfoWindow() {
        if (mInfoWindow != null) {
            mBaiduMap.hideInfoWindow();
            mInfoWindow = null;
        }
    }

    /**
     * 地图坐标点被单击了
     * @param latLng
     */
    protected void onMapClick(LatLng latLng) {
        hideInfoWindow();
    }
    /**
     * 地图坐标点被长按了
     * @param latLng
     */
    protected void onMapLongClick(LatLng latLng) {
    }


    public void searchNearby(LatLng latLng, String keyword, int radius, int pageSize, int pageIndex) {
        BaiduLocationManager.getInstance().showShortMessage(getString(R.string.format_msg_wait_request_search, keyword));
        mPoiSearch.searchNearby((new PoiNearbySearchOption())
                .location(latLng)
                .keyword(keyword)
                .radius(radius)
                .pageCapacity(pageSize)
                .pageNum(pageIndex));
    }

    /**
     * 正向编码地址获取经纬度
     * @param city
     * @param addrStr
     */
    public void geocode(String city, String addrStr) {
        mGeoCoderSearch.geocode(new GeoCodeOption()
                .city(city)
                .address(addrStr));
    }

    /**
     * 正向编码地址获取经纬度
     * @param addrStr
     */
    public void geocode(String addrStr) {
        geocode("", addrStr);
    }

    /**
     * 反向编码经纬度获取位置信息
     * @param location
     */
    public void reverseGeoCode(LatLng location) {
        mGeoCoderSearch.reverseGeoCode(new ReverseGeoCodeOption()
                .location(location));
    }

    /**
     * 发起公交线路规划检索
     * @param stNode
     * @param enNode
     * @param city
     */
    public void transitSearch(PlanNode stNode, PlanNode enNode, String city) {
        mRoutePlanSearch.transitSearch((new TransitRoutePlanOption())
                .from(stNode)
                .city(city)
                .to(enNode));
    }

    /**
     * 发起驾车线路规划检索
     * @param stNode
     * @param enNode
     */
    public void drivingSearch(PlanNode stNode, PlanNode enNode) {
        mRoutePlanSearch.transitSearch((new TransitRoutePlanOption())
                .from(stNode)
                .to(enNode));
    }

    protected OnGetPoiSearchResultListener mOnGetPoiSearchResultListener = new OnGetPoiSearchResultListener(){
        public void onGetPoiResult(PoiResult result){
            //获取POI检索结果
            if (result.error != SearchResult.ERRORNO.NO_ERROR) {
                //详情检索失败
                // result.error请参考SearchResult.ERRORNO
            } else {
                //检索成功
//                mPoiObjectList.clear();
//                for (PoiInfo poi : result.getAllPoi()) {
//                    if (poi.type == PoiInfo.POITYPE.POINT) {
//                        mPoiObjectList.add(new PoiObject(poi));
//                    }
//                }
//                for(PoiObject poiObject:mPoiObjectList) {
//                    //定义Maker坐标点
//                    LatLng point = poiObject.mPoiInfo.location;
//                    //构建Marker图标
//                    BitmapDescriptor bitmap = BitmapDescriptorFactory
//                            .fromResource(R.drawable.ic_poi);
//                    //构建MarkerOption，用于在地图上添加Marker
//                    OverlayOptions option = new MarkerOptions()
//                            .position(point)
//                            .icon(bitmap);
//                    //在地图上添加Marker，并显示
//                    mBaiduMap.addOverlay(option);
//
//
//                }
                mBaiduMap.clear();
                PoiOverlay overlay = new MyPoiOverlay(mBaiduMap);
                //设置overlay可以处理标注点击事件
                mBaiduMap.setOnMarkerClickListener(overlay);
                overlay.setData(result);
                //添加PoiOverlay到地图中
                overlay.addToMap();
                overlay.zoomToSpan();
            }

        }
        public void onGetPoiDetailResult(PoiDetailResult result){
            //获取Place详情页检索结果
            if (result.error != SearchResult.ERRORNO.NO_ERROR) {
                //详情检索失败
                // result.error请参考SearchResult.ERRORNO
            } else {
                //检索成功
            }
        }

        @Override
        public void onGetPoiIndoorResult(PoiIndoorResult poiIndoorResult) {

        }

    };


    //创建公交线路规划检索监听者
    protected OnGetRoutePlanResultListener MyOnGetRoutePlanResultListener = new OnGetRoutePlanResultListener() {
        public void onGetBikingRouteResult(BikingRouteResult result) {
            //骑行规划路线
            if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
                BaiduLocationManager.getInstance().showShortMessage("抱歉，未找到公交路线");
            }
            if (result.error == SearchResult.ERRORNO.AMBIGUOUS_ROURE_ADDR) {
                BaiduLocationManager.getInstance().showShortMessage("起终点或途经点地址有岐义");
                //起终点或途经点地址有岐义，通过以下接口获取建议查询信息
                //result.getSuggestAddrInfo()
                return;
            }
            if (result.error == SearchResult.ERRORNO.NO_ERROR) {
                MyBikingRouteOverlay overlay = new MyBikingRouteOverlay(mBaiduMap);
                mBaiduMap.setOnMarkerClickListener(overlay);
                overlay.setData(result.getRouteLines().get(0));
                overlay.addToMap();
                overlay.zoomToSpan();
            }
        }
        public void onGetWalkingRouteResult(WalkingRouteResult result) {
            //
        }
        public void onGetTransitRouteResult(TransitRouteResult result) {
            if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
                BaiduLocationManager.getInstance().showShortMessage("抱歉，未找到公交路线");
            }
            if (result.error == SearchResult.ERRORNO.AMBIGUOUS_ROURE_ADDR) {
                BaiduLocationManager.getInstance().showShortMessage("起终点或途经点地址有岐义");
                //起终点或途经点地址有岐义，通过以下接口获取建议查询信息
                //result.getSuggestAddrInfo()
                return;
            }
            if (result.error == SearchResult.ERRORNO.NO_ERROR) {
                TransitRouteOverlay overlay = new MyTransitRouteOverlay(mBaiduMap);
                mBaiduMap.setOnMarkerClickListener(overlay);
                overlay.setData(result.getRouteLines().get(0));
                overlay.addToMap();
                overlay.zoomToSpan();
            }
        }
        public void onGetDrivingRouteResult(DrivingRouteResult result) {
            if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
                BaiduLocationManager.getInstance().showShortMessage("抱歉，未找到驾车路线");
            }
            if (result.error == SearchResult.ERRORNO.AMBIGUOUS_ROURE_ADDR) {
                //起终点或途经点地址有岐义，通过以下接口获取建议查询信息
                BaiduLocationManager.getInstance().showShortMessage("起终点或途经点地址有岐义");
                //result.getSuggestAddrInfo()
                return;
            }
            if (result.error == SearchResult.ERRORNO.NO_ERROR) {
                MyDrivingRouteOverlay overlay = new MyDrivingRouteOverlay(mBaiduMap);
                mBaiduMap.setOnMarkerClickListener(overlay);
                overlay.setData(result.getRouteLines().get(0));
                overlay.addToMap();
                overlay.zoomToSpan();
            }
        }



    };
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 当不需要定位图层时关闭定位图层
        mBaiduMap.setMyLocationEnabled(false);
    }

    protected void requestLocation() {
        BaiduLocationManager.getInstance().showMessage(R.string.pull_to_refresh_locationing_label);
        BaiduLocationService.startRequestLocation(getActivity(), 60);
//        MyApplication.getInstance().postDelay(new Runnable() {
//            @Override
//            public void run() {
//                mSwipeLayout.setRefreshing(true);
//            }
//        }, 250);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(LocationChangeEvent locationChangeEvent) {
        Log.d(TAG, "onEventBackgroundThread locationChangeEvent");
        onLocationChanged(locationChangeEvent);
    }

    protected void onLocationChanged(LocationChangeEvent locationChangeEvent) {
        Log.d(TAG, "onLocationChanged locationChangeEvent");
        if (getActivity() != null) {
            if (locationChangeEvent.isReturnLocation()) {
                if (locationChangeEvent.mBDLocation != null) {
                    mBDLocation = locationChangeEvent.mBDLocation;
                    setMyLocationData(mBDLocation);
                }
            } else {
                Log.d(TAG, "locationChangeEvent.isReturnLocation() " + locationChangeEvent.isReturnLocation());
            }
        }
    }

//    protected void reCenter(BDLocation location, int duration) {
//        // 构造定位数据
//        MyLocationData locData = new MyLocationData.Builder()
//                .accuracy(location.getRadius())
//                // 此处设置开发者获取到的方向信息，顺时针0-360
//                .direction(location.getDirection()).latitude(location.getLatitude())
//                .longitude(location.getLongitude()).build();
//        // 设置定位数据
//        mBaiduMap.setMyLocationData(locData);
//        MyLocationConfiguration config = new MyLocationConfiguration(MyLocationConfiguration.LocationMode.NORMAL, true, mCurrentMarker);
//        mBaiduMap.setMyLocationConfigeration(config);
//
//        //设定中心点坐标
//        LatLng cenpt = new LatLng(location.getLatitude(),location.getLongitude());
//        //定义地图状态,地图缩放级别 3~19
//        MapStatus mMapStatus = new MapStatus.Builder().target(cenpt).zoom(16).build();
//        //定义MapStatusUpdate对象，以便描述地图状态将要发生的变化
//        MapStatusUpdate mMapStatusUpdate = MapStatusUpdateFactory.newMapStatus(mMapStatus);
//        //改变地图状态
//        mBaiduMap.animateMapStatus(mMapStatusUpdate, 1000);
//    }

    protected void onGetGeoCodeResult(GeoCodeResult result) {
        if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
            //没有检索到结果
            Log.e(TAG, "onGetGeoCodeResult 地址 " +result.getAddress() +" 没有检索到结果");
            return;
        }
        //获取地理编码结果
        LatLng latLng = result.getLocation();
        BDLocation bdLocation = new BDLocation();
        bdLocation.setLatitude(latLng.latitude);
        bdLocation.setLongitude(latLng.longitude);
        bdLocation.setAddrStr(result.getAddress());
        setMyLocationData(bdLocation);
    }

    protected void onGetReverseGeoCodeResult(ReverseGeoCodeResult result) {
        if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
            //没有找到检索结果
            return;
        }
        //获取反向地理编码结果
    }

    protected void centerToLocation(BDLocation location, int duration) {
        LatLng cenpt = new LatLng(location.getLatitude(),location.getLongitude());
        //定义地图状态,地图缩放级别 3~19
        MapStatus newMapStatus = new MapStatus.Builder().target(cenpt).zoom(16).build();
        //定义MapStatusUpdate对象，以便描述地图状态将要发生的变化
        MapStatusUpdate mMapStatusUpdate = MapStatusUpdateFactory.newMapStatus(newMapStatus);
        //改变地图状态
        mBaiduMap.animateMapStatus(mMapStatusUpdate, duration);
    }


    protected void setMyLocationData(BDLocation location) {
        MyLocationData locData = new MyLocationData.Builder()
//                    .accuracy(location.getRadius())
                // 此处设置开发者获取到的方向信息，顺时针0-360
                .direction(location.getDirection()).latitude(location.getLatitude())
                .longitude(location.getLongitude()).build();
        // 设置定位数据
        mBaiduMap.setMyLocationData(locData);
//        mCurrentMarker = BitmapDescriptorFactory.fromResource(R.drawable.icon_home_loaction);
        centerToLocation(location, DEFAULT_update_map_status_duration);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.ic_location_me) {
            //重新定位
            if (mBDLocation == null) {
                requestLocation();
            } else {
                setMyLocationData(mBDLocation);
            }
        }
    }

    /**
     * 画圆
     * @param center
     * @param radius
     * @param bundle
     */
    protected Circle createCircleOverlayout(LatLng center, int radius, Bundle bundle) {
        CircleOptions ooCircle = new CircleOptions().fillColor(0x38DFF3FE)
                .center(center).stroke(new Stroke(3, 0xff459CDA))
                .radius(radius)
                .extraInfo(bundle);
        return (Circle) mBaiduMap.addOverlay(ooCircle);
    }


    private class MyPoiOverlay extends PoiOverlay {
        public MyPoiOverlay(BaiduMap baiduMap) {
            super(baiduMap);
        }
        @Override
        public boolean onPoiClick(int index) {
            super.onPoiClick(index);
            mCurrentPoiInfo = getPoiResult().getAllPoi().get(index);
            showInfoWindow(mCurrentPoiInfo);
            return true;
        }

    }
    private class MyTransitRouteOverlay extends TransitRouteOverlay {
        public MyTransitRouteOverlay(BaiduMap baiduMap) {
            super(baiduMap);
        }

        @Override
        public boolean onPolylineClick(Polyline polyline) {
            return super.onPolylineClick(polyline);
        }
    }

    private class MyDrivingRouteOverlay extends DrivingRouteOverlay {
        public MyDrivingRouteOverlay(BaiduMap baiduMap) {
            super(baiduMap);
        }

        @Override
        public boolean onPolylineClick(Polyline polyline) {
            return super.onPolylineClick(polyline);
        }
    }

    private class MyBikingRouteOverlay extends BikingRouteOverlay {
        public MyBikingRouteOverlay(BaiduMap baiduMap) {
            super(baiduMap);
        }

        @Override
        public boolean onPolylineClick(Polyline polyline) {
            return super.onPolylineClick(polyline);
        }
    }


    protected InfoWindow.OnInfoWindowClickListener mInfoWindowClickListener = new InfoWindow.OnInfoWindowClickListener() {

        @Override
        public void onInfoWindowClick() {
            onInfoWindowClickImpl(mCurrentPoiInfo);
        }
    };

    protected void onInfoWindowClickImpl(PoiInfo poiInfo) {
        if (poiInfo != null) {
            if (!TextUtils.isEmpty(poiInfo.phoneNum)) {
                BaiduLocationManager.dialPhone(getActivity(), poiInfo.phoneNum.replaceAll("[\\(\\)]", ""));
            }
        }
    }

    /**PoiOverlay上的单击事件*/
    protected boolean showInfoWindow(PoiInfo poiInfo) {
        //创建InfoWindow展示的view
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.poidetailresult_pop_general, null);

        TextView textView = (TextView) view.findViewById(R.id.name);
        StringBuilder sb = new StringBuilder(poiInfo.name).append("\n");
        LatLng myLocation = new LatLng(mBDLocation.getLatitude(),mBDLocation.getLongitude());
        sb.append(String.format(mFormatAccuracy, (int)(0.5 + DistanceUtil.getDistance(poiInfo.location, myLocation)))).append(" | ").append(poiInfo.address);
        textView.setText(sb.toString());

        view.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT));
        //定义用于显示该InfoWindow的坐标点
        LatLng pt = poiInfo.location;
        //创建InfoWindow , 传入 view， 地理坐标， y 轴偏移量
        mInfoWindow = new InfoWindow(BitmapDescriptorFactory.fromView(view), pt, -47, mInfoWindowClickListener);
        //显示InfoWindow
        mBaiduMap.showInfoWindow(mInfoWindow);
        return true;
    }

    protected Marker createMarker(BDLocation bdLocation, Bundle extraInfo, int icon_marka, int period) {
//        MarkerOptions option = new MarkerOptions()
//                .position(new LatLng(bdLocation.getLatitude(), bdLocation.getLongitude()))
//                .icon(BitmapDescriptorFactory.fromResource(icon_marka)).extraInfo(extraInfo)
//                .period(period);
//
//        option.animateType(MarkerOptions.MarkerAnimateType.drop);
//        //在地图上添加Marker，并显示
//        Marker marker = (Marker) mBaiduMap.addOverlay(option);
        return createMarker(bdLocation, extraInfo, icon_marka, period, MarkerOptions.MarkerAnimateType.drop);
    }

    protected Marker createMarker(BDLocation bdLocation, Bundle extraInfo, int icon_marka, int period, MarkerOptions.MarkerAnimateType markerAnimateType) {
        MarkerOptions option = new MarkerOptions()
                .position(new LatLng(bdLocation.getLatitude(), bdLocation.getLongitude()))
                .icon(BitmapDescriptorFactory.fromResource(icon_marka)).extraInfo(extraInfo)
                .period(period);

        option.animateType(markerAnimateType);
        //在地图上添加Marker，并显示
        Marker marker = (Marker) mBaiduMap.addOverlay(option);

        return marker;
    }

    protected boolean onMarkerClick(Marker marker) {
        mCurrentMarkerInfo = marker;
        boolean handled = false;
        if (markerClickListener != null) {
            handled = markerClickListener.onMarkerClick(marker);
        }
        if (!handled) {
            handled = showMarkerInfoWindow(marker);
        }
        return handled;
    }


    /**
     * 设置Marker单击回调
     * @param markerClickListener
     */
    public void setOnMarkerClickListener(BaiduMap.OnMarkerClickListener markerClickListener) {
        this.markerClickListener = markerClickListener;
    }
    /**
     * 设置Marker弹出窗覆盖物单击回调
     * @param onMarkerInfoWindowClickListener
     */
    public void setOnMarkerInfoWindowClickListener(OnMarkerInfoWindowClickListener onMarkerInfoWindowClickListener) {
        this.onMarkerInfoWindowClickListener = onMarkerInfoWindowClickListener;
    }


    protected boolean showMarkerInfoWindow(Marker marker) {
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.poidetailresult_pop_general, null);
        TextView textView = (TextView) view.findViewById(R.id.name);
        Bundle bundle = marker.getExtraInfo();
        BDLocation bdLocation = bundle.getParcelable(EXTRA_POI_BDLocation);
        String markerName = bundle.getString(EXTRA_POI_NAME);
        if (markerName == null) {
            markerName = "";
        }
        StringBuilder sb = new StringBuilder(markerName);
        if (bdLocation !=null && !TextUtils.isEmpty(bdLocation.getAddrStr())) {
            sb.append("\n").append(bdLocation.getAddrStr());
        }

        textView.setText(sb.toString());

        ImageView phoneImage = (ImageView) view.findViewById(R.id.phone);
        String phoneStr = bundle.getString(EXTRA_POI_PHONE);
        if (TextUtils.isEmpty(phoneStr)) {
            phoneImage.setVisibility(View.GONE);
        }

        view.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT));
        //定义用于显示该InfoWindow的坐标点
        LatLng pt = marker.getPosition();
        //创建InfoWindow , 传入 view， 地理坐标， y 轴偏移量
        mInfoWindow = new InfoWindow(BitmapDescriptorFactory.fromView(view), pt, -47, mMarkerInfoWindowClickListener);
        //显示InfoWindow
        mBaiduMap.showInfoWindow(mInfoWindow);
        return true;
    }


    protected InfoWindow.OnInfoWindowClickListener mMarkerInfoWindowClickListener = new InfoWindow.OnInfoWindowClickListener() {

        @Override
        public void onInfoWindowClick() {
            onMarkerInfoWindowClickImpl(mCurrentMarkerInfo);
        }
    };

    /**
     * 当Marker弹出窗被点击时调用
     */
    protected void onMarkerInfoWindowClickImpl(Marker marker) {
        if (onMarkerInfoWindowClickListener != null) {
            onMarkerInfoWindowClickListener.onMarkerInfoWindowClick(marker);
        }
    }


    public static interface OnMarkerInfoWindowClickListener {
        public void onMarkerInfoWindowClick(Marker marker);
    }


    /**
     * 支持显示某个具体的Marker
     */
    public static class BaseBaiduMapMarkerFragmentImpl extends BaseBaiduMapFragment {

        private BDLocation markerBDLocation;

        public static Bundle buildMarkerExtraInfo(String poiName, String phone, BDLocation bdLocation) {
            Bundle bundle = new Bundle();
            bundle.putParcelable(EXTRA_POI_BDLocation, bdLocation);
            bundle.putString(EXTRA_POI_NAME, poiName);

            if (!TextUtils.isEmpty(phone)) {
                bundle.putString(EXTRA_POI_PHONE, phone);

            }
            return bundle;
        }

        @Override
        protected void firstOnResume() {
            super.firstOnResume();
            //移动地图中心点到bdLocation
            centerToMarker();
        }

        protected void centerToMarker() {
            if (mBundle != null) {
                if (mBundle.getParcelable(EXTRA_POI_BDLocation) != null) {
                    markerBDLocation = mBundle.getParcelable(EXTRA_POI_BDLocation);
                    centerToLocation(markerBDLocation, 300);
                    createMarker(markerBDLocation, mBundle, R.drawable.icon_marka, 1);
                }
            } else {
                BaiduLocationManager.getInstance().showMessage("centerToMarker faield");
            }

        }

        protected void initMyLocationConfigeration() {
            //不需要设置我的位置
        }

        protected void initView(View content) {
            //不需要我的位置
            View view = content.findViewById(R.id.ic_location_me);
            if (view != null) {
                view.setVisibility(View.GONE);
            }
        }

        /***
         * 在地图上创建标记点
         * @param poiInfo
         */
        protected void createMarker(PoiInfo poiInfo) {
            //构建MarkerOption，用于在地图上添加Marker
            BDLocation bdLocation = new BDLocation();
            bdLocation.setLatitude(poiInfo.location.latitude);
            bdLocation.setLongitude(poiInfo.location.longitude);
            bdLocation.setAddrStr(poiInfo.address);
            createMarker(markerBDLocation, buildMarkerExtraInfo(poiInfo.name, poiInfo.phoneNum, bdLocation), R.drawable.icon_marka, 1);
        }

//        protected boolean showMarkerInfoWindow(Marker marker) {
//            View view = LayoutInflater.from(getActivity()).inflate(R.layout.poidetailresult_pop_general, null);
//            TextView textView = (TextView) view.findViewById(R.id.name);
//            Bundle bundle = marker.getExtraInfo();
//            BDLocation bdLocation = bundle.getParcelable(EXTRA_POI_BDLocation);
//            String markerName = bundle.getString(EXTRA_POI_NAME);
//            if (markerName == null) {
//                markerName = "";
//            }
//            StringBuilder sb = new StringBuilder(markerName);
//            if (!TextUtils.isEmpty(bdLocation.getAddrStr())) {
//                sb.append("\n").append(bdLocation.getAddrStr());
//            }
//
//            textView.setText(sb.toString());
//
//            ImageView phoneImage = (ImageView) view.findViewById(R.id.phone);
//            String phoneStr = bundle.getString(EXTRA_POI_PHONE);
//            if (TextUtils.isEmpty(phoneStr)) {
//                phoneImage.setVisibility(View.GONE);
//            }
//
//            view.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT));
//            //定义用于显示该InfoWindow的坐标点
//            LatLng pt = marker.getPosition();
//            //创建InfoWindow , 传入 view， 地理坐标， y 轴偏移量
//            mInfoWindow = new InfoWindow(BitmapDescriptorFactory.fromView(view), pt, -47, mMarkerInfoWindowClickListener);
//            //显示InfoWindow
//            mBaiduMap.showInfoWindow(mInfoWindow);
//            return true;
//        }
    }

}
