package com.cncom.library.lbs.baidu.streetscape;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.baidu.lbsapi.panoramaview.PanoramaView;
import com.baidu.lbsapi.panoramaview.PanoramaViewListener;
import com.cncom.library.lbs.baidu.BaiduLocationManager;
import com.cncom.library.lbs.baidu.R;

/**
 * http://wiki.lbsyun.baidu.com/cms/androidpano/doc/v2_2_0/
 * Created by bestjoy on 16/3/31.
 */
public class BaseBaiduPanoramaFragment extends Fragment {

    /**利用地图POI ID展示全景图*/
    public static final int PanoramaType_POI_UID = 1;
    /**利用地理坐标展示全景图,EXTRA_PanoData值为"经度,纬度"*/
    public static final int PanoramaType_LAT_LONG = 2;
    /**利用全景图ID展示全景图*/
    public static final int PanoramaType_PID = 3;


    /**使用哪一种数据来显示全景*/
    public static final String EXTRA_PanoramaType = "extra_PanoramaType";
    public static final String EXTRA_PanoType = "extra_PanoType";
    //数据,主要的街景数据，如经纬度， uid, pid等
    public static final String EXTRA_PanoData = "extra_PanoData";

    //额外的数据,目前暂时没有用到
    public static final String EXTRA_PanoData2 = "extra_PanoData2";
    public static final String EXTRA_PanoData3 = "extra_PanoData3";
    public static final String EXTRA_PanoData4 = "extra_PanoData4";

    public static final String EXTRA_TITLE = "extra_title";
    /**全景图片显示级别(图片分辨率)*/
    public static final String EXTRA_PanoramaImageLevel = "extra_PanoramaImageLevel";
    /**ImageDefinitionLow:较低清晰度*/
    public static final int PanoramaImageLevel_ImageDefinitionLow = 1;
    /**ImageDefinitionMiddle中等清晰度*/
    public static final int PanoramaImageLevel_ImageDefinitionMiddle = 2;
    /**ImageDefinitionHigh:较高清晰度*/
    public static final int PanoramaImageLevel_ImageDefinitionHigh = 3;

    private int mPanoramaType = PanoramaType_PID;

    private PanoramaView mPanoView;

    protected Bundle mBundle;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBundle = getArguments();
        if (mBundle == null) {
            mBundle = new Bundle();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View content = inflater.inflate(R.layout.acticity_panorama_demo_main, container, false);
        mPanoView = (PanoramaView) content.findViewById(R.id.panorama);
        mPanoView.setPanoramaViewListener(new PanoramaViewListener() {
            @Override
            public void onDescriptionLoadEnd(String s) {

            }

            public void onLoadPanoramaBegin() {

            }

            public void onLoadPanoramaEnd(String json) {

            }

            public void onLoadPanoramaError(final String error) {
                BaiduLocationManager.getInstance().postAsync(new Runnable() {
                    @Override
                    public void run() {
                        new AlertDialog.Builder(getActivity())
                                .setMessage(error)
                                .setPositiveButton(android.R.string.ok, null)
                                .show();
                    }
                });

            }

            @Override
            public void onMessage(String s, int i) {

            }

            @Override
            public void onCustomMarkerClick(String s) {

            }
        });
        mPanoramaType = mBundle.getInt(EXTRA_PanoramaType, PanoramaType_PID);
        if (mPanoramaType == PanoramaType_POI_UID) {
            String uid = mBundle.getString(EXTRA_PanoData);
            mPanoView.setPanoramaByUid(uid, PanoramaView.PANOTYPE_STREET);
        } else if (mPanoramaType == PanoramaType_PID) {
            String pid = mBundle.getString(EXTRA_PanoData);
            if (TextUtils.isEmpty(pid)) {
                pid = "0100220000130817164838355J5";
            }
            mPanoView.setPanorama(pid);
        } else if (mPanoramaType == PanoramaType_LAT_LONG) {
            String lonAndLat = mBundle.getString(EXTRA_PanoData);
            String[] para = lonAndLat.split(",");
            mPanoView.setPanorama(Double.valueOf(para[1]), Double.valueOf(para[0]));
        }

//        if (ComConnectivityManager.getInstance().isWifiConnected()) {
//            mPanoView.setPanoramaImageLevel(PanoramaView.ImageDefinition.ImageDefinitionMiddle);
//        } else {
//            mPanoView.setPanoramaImageLevel(PanoramaView.ImageDefinition.ImageDefinitionLow);
//        }
        int panoramaImageLevel = mBundle.getInt(EXTRA_PanoramaImageLevel, PanoramaImageLevel_ImageDefinitionLow);
        if (panoramaImageLevel == PanoramaImageLevel_ImageDefinitionLow) {
            mPanoView.setPanoramaImageLevel(PanoramaView.ImageDefinition.ImageDefinitionLow);
        } else if (panoramaImageLevel == PanoramaImageLevel_ImageDefinitionMiddle) {
            mPanoView.setPanoramaImageLevel(PanoramaView.ImageDefinition.ImageDefinitionMiddle);
        } else if (panoramaImageLevel == PanoramaImageLevel_ImageDefinitionHigh) {
            mPanoView.setPanoramaImageLevel(PanoramaView.ImageDefinition.ImageDefinitionHigh);
        }

        return content;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        String title = mBundle.getString(EXTRA_TITLE);
        if (!TextUtils.isEmpty(title)) {
            getActivity().setTitle(title);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mPanoView.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mPanoView.onResume();
    }

    @Override
    public void onDestroy() {
        mPanoView.destroy();
        super.onDestroy();
    }
}
