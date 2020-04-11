package com.example.zyf.bsclient;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PagerSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.BDNotifyListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.CoordType;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.route.BikingRouteResult;
import com.baidu.mapapi.search.route.DrivingRoutePlanOption;
import com.baidu.mapapi.search.route.DrivingRouteResult;
import com.baidu.mapapi.search.route.IndoorRouteResult;
import com.baidu.mapapi.search.route.MassTransitRouteResult;
import com.baidu.mapapi.search.route.OnGetRoutePlanResultListener;
import com.baidu.mapapi.search.route.PlanNode;
import com.baidu.mapapi.search.route.RoutePlanSearch;
import com.baidu.mapapi.search.route.TransitRouteResult;
import com.baidu.mapapi.search.route.WalkingRouteResult;
import com.example.zyf.bsclient.Dao.Routes;
import com.example.zyf.bsclient.Dao.route;
import com.example.zyf.bsclient.Util.AdapterStop;
import com.example.zyf.bsclient.Util.AsyncHttpUtil;
import com.example.zyf.bsclient.Util.GetJson;
import com.example.zyf.bsclient.Util.myAdapter;
import com.example.zyf.bsclient.ovelayutil.DrivingRouteOverlay;
import com.yzq.zxinglibrary.android.CaptureActivity;
import com.yzq.zxinglibrary.bean.ZxingConfig;
import com.yzq.zxinglibrary.common.Constant;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.StompClient;

/**
 * 公交实时系统客户端
 * 功能1：扫描二维码获取经过当前站点的所有线路
 * 功能2：获取指定线路公交车的实时位置，并开启到站提醒*/
public class MainActivity extends AppCompatActivity {

    private final int  REQUEST_CODE_SCAN=22;
    private ImageButton imageButton;
    private Button startButton;
    private Button sendButton;
    private Button stopButton;
    private Button testButton;
    private StompClient stompClient;
    private CardView ScanCardView;
    private ImageButton getMyLocation;
    private TextView NearRoutes;


    public LocationClient mLocationClient;
    private MyLocationListener myListener=new MyLocationListener();
    public LocationClientOption option=new LocationClientOption();
    private MapView mapView=null;
    private BaiduMap baiduMap=null;
    private LatLng myLocation;

    //构建Marker坐标
    public BitmapDescriptor bitmapDescriptor;
    public OverlayOptions overlayOptions;
    public Marker marker;

    //底部抽屉
    private BottomSheetBehavior bottomSheetBehavior;
    public RecyclerView recyclerView;
    public TextView tip1;
    public TextView tip2;
    //适配器
    public myAdapter  adapter;
    private myAdapter test;
    private AdapterStop adapterStop;
    public List<Routes> routesList=new ArrayList<>();
    private List<route> list=new ArrayList<>();
    //路线规划
    private RoutePlanSearch mSearch;
    //位置提醒监听器
    private MyNotifyLister myNotifyLister=new MyNotifyLister();
    //震动提醒
    private Vibrator vibrator;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //在使用SDK各组件之前初始化context信息，传入ApplicationContext
        //所以放在 'setContentView(R.layout.activity_main);'之前
        SDKInitializer.initialize(getApplicationContext());
        SDKInitializer.setCoordType(CoordType.BD09LL);
        setContentView(R.layout.activity_main);
        imageButton=findViewById(R.id.imageButton);
        startButton=findViewById(R.id.start);
        sendButton=findViewById(R.id.send);
        stopButton=findViewById(R.id.stop);
        recyclerView=findViewById(R.id.recyclerView);
        tip1=findViewById(R.id.textView);
        tip2=findViewById(R.id.textView3);
        ScanCardView=findViewById(R.id.cardView3);
        getMyLocation=findViewById(R.id.location);
        NearRoutes=findViewById(R.id.textView2);
        //震动类
        vibrator=(Vibrator) getSystemService(VIBRATOR_SERVICE);

        //创建路线规划检索实例
        mSearch=RoutePlanSearch.newInstance();
        //创建路线规划检索结果监听器
        OnGetRoutePlanResultListener listener=new OnGetRoutePlanResultListener() {
            @Override
            public void onGetWalkingRouteResult(WalkingRouteResult walkingRouteResult) {

            }

            @Override
            public void onGetTransitRouteResult(TransitRouteResult transitRouteResult) {

            }

            @Override
            public void onGetMassTransitRouteResult(MassTransitRouteResult massTransitRouteResult) {

            }

            @Override
            public void onGetDrivingRouteResult(DrivingRouteResult drivingRouteResult) {
                //创建DrivingRouteOverlay实例
                DrivingRouteOverlay overlay=new DrivingRouteOverlay(baiduMap);
                if(drivingRouteResult.getRouteLines().size()>0){
                    //获取路径规划数据,(以返回的第一条路线为例）
                    //为DrivingRouteOverlay实例设置数据
                    overlay.setData(drivingRouteResult.getRouteLines().get(0));
                    //在地图上绘制DrivingRouteOverlay
                    overlay.addToMap();
                }

            }

            @Override
            public void onGetIndoorRouteResult(IndoorRouteResult indoorRouteResult) {

            }

            @Override
            public void onGetBikingRouteResult(BikingRouteResult bikingRouteResult) {

            }
        };
        //设置路线规划检索监听器
        mSearch.setOnGetRoutePlanResultListener(listener);

        //底部抽屉
        testButton=findViewById(R.id.test);
        bottomSheetBehavior=BottomSheetBehavior.from(findViewById(R.id.bottom_sheet));
        bottomSheetBehavior.setPeekHeight(400);
        bottomSheetBehavior.setSkipCollapsed(false);
        //底部抽屉数据
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(MainActivity.this);
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        adapter=new myAdapter(routesList,MainActivity.this);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(adapter);
        //实现横向轮播效果
        PagerSnapHelper snapHelper=new PagerSnapHelper();
        snapHelper.attachToRecyclerView(recyclerView);

        testButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("sheet",String.valueOf(bottomSheetBehavior.getState()));
                if(bottomSheetBehavior.getState()==BottomSheetBehavior.STATE_EXPANDED){
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

                }else if(bottomSheetBehavior.getState()==BottomSheetBehavior.STATE_COLLAPSED){
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                }
            }
        });
        mapView=findViewById(R.id.mapView);
        mapView.showZoomControls(false);
        baiduMap=mapView.getMap();
        baiduMap.setMyLocationEnabled(true);


        //声明LocationCLient类
        mLocationClient=new LocationClient(getApplicationContext());
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        //返回坐标类型
        option.setCoorType("BD09LL");
        //连续定位,>=1000ms
        option.setScanSpan(10000);
        //设置是否使用GPS
        option.setOpenGps(true);
        //设置是否当GPS有效时按照1S/1次频率输出GPS结果
        option.setLocationNotify(true);
        //可选，定位SDK内部是一个service，并放到了独立进程。
        //设置是否在stop的时候杀死这个进程，默认（建议）不杀死，即setIgnoreKillProcess(true)
        option.setIgnoreKillProcess(false);
        //设置是否手机crash信息
        option.SetIgnoreCacheException(false);

        //可选，V7.2版本新增能力
        //如果设置了该接口，首次启动定位时，会先判断当前Wi-Fi是否超出有效期，若超出有效期，会先重新扫描Wi-Fi，然后定位
        option.setWifiCacheTimeOut(5*60*1000);
        //设置option参数
        mLocationClient.setLocOption(option);
        //注册位置监听函数
        mLocationClient.registerLocationListener(myListener);

        /**
         * 运行时权限申请
         */
        List<String> permissionList=new ArrayList<>();
        if(ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if(ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if(ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.ACCESS_WIFI_STATE)!=PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.ACCESS_WIFI_STATE);
        }
        if(ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.ACCESS_NETWORK_STATE)!=PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.ACCESS_NETWORK_STATE);
        }
        if(ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.CHANGE_WIFI_STATE)!=PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.CHANGE_WIFI_STATE);
        }
        if(ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.READ_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if(ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if(ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.INTERNET)!=PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.INTERNET);
        }
        if(ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.CAMERA);
        }
        if(ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.VIBRATE)!=PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.VIBRATE);
        }
        //申请未添加的权限
        if(!permissionList.isEmpty()) {
            String[] permissions = permissionList.toArray(new String[permissionList.size()]);
            ActivityCompat.requestPermissions(MainActivity.this, permissions, 1);
        }else{
            //启动位置监听和位置提醒
            mLocationClient.start();
        }

        final Intent intent = new Intent(MainActivity.this, CaptureActivity.class);
        /*ZxingConfig是配置类
         *可以设置是否显示底部布局，闪光灯，相册，
         * 是否播放提示音  震动
         * 设置扫描框颜色等
         * 也可以不传这个参数
         * */
        ZxingConfig config = new ZxingConfig();
        config.setPlayBeep(true);//是否播放扫描声音 默认为true
        config.setShake(true);//是否震动  默认为true
        config.setDecodeBarCode(true);//是否扫描条形码 默认为true
        //config.setReactColor(R.color.scanLineColor);//设置扫描框四个角的颜色 默认为白色
        //config.setFrameLineColor(R.color.colorAccent);//设置扫描框边框颜色 默认无色
        config.setScanLineColor(R.color.green);//设置扫描线的颜色 默认白色
        config.setFullScreenScan(false);//是否全屏扫描  默认为true  设为false则只会在扫描框中扫描
        intent.putExtra(Constant.INTENT_ZXING_CONFIG, config);

        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(intent, REQUEST_CODE_SCAN);
            }
        });
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //createStompClient();
               // registerStompTopic();

            }
        });
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapter.stompClient.send("/app/welcome17", "My first STOMP message!").subscribe();
            }
        });
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapter.stompClient.disconnect();
                //stompClient.disconnect();
            }
        });


        //移动到我的位置
        getMyLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MapStatusUpdate mapStatusUpdate=MapStatusUpdateFactory.newLatLngZoom(myLocation,16);
                baiduMap.animateMapStatus(mapStatusUpdate);
            }
        });
    }
    //运行时权限申请回调
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case 1:
                if(grantResults.length>0){
                    for(int result:grantResults){
                        if(result!=PackageManager.PERMISSION_GRANTED){
                            Toast.makeText(MainActivity.this,"必须开启上述权限",Toast.LENGTH_LONG).show();
                            finish();
                        }
                    }
                }else{
                    Toast.makeText(MainActivity.this,"未知错误",Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
        }
    }
   //二维码扫描回调
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // 扫描二维码/条码回传
        if (requestCode == REQUEST_CODE_SCAN && resultCode == RESULT_OK) {
            if (data != null) {
                String content = data.getStringExtra(Constant.CODED_CONTENT);
                Toast.makeText(MainActivity.this,content,Toast.LENGTH_LONG).show();
                AsyncHttpUtil.sendOkHttpRequest("http://47.96.164.222:8080//getAllRoute", content, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        //response.body().string()只能调用一次
                        String responseJson=response.body().string();
                        //初始化线路
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                /**
                                 直接将从数据库获取的数据源集合直接赋值给当前routesList时，相当于当前数据源的对象发生了变化，
                                 当前对象已经不是adapter中的对象了，所以adaper调用notifyDataSetChanged()方法不会进行刷新数据和界面的操作
                                */
                                List<Routes> list=new ArrayList<>();
                                list=GetJson.resolve(responseJson);
                                routesList.clear();
                                routesList.addAll(list);
                                //数据更新
                                adapter.notifyDataSetChanged();
                                if(routesList.isEmpty()){
                                    //显示底部提示信息
                                    tip1.setVisibility(View.VISIBLE);
                                    tip2.setVisibility(View.VISIBLE);
                                    NearRoutes.setText("附近线路(0)");
                                    //弹出底框
                                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                                }else{
                                    //在地图上更新公交站点
                                    tip1.setVisibility(View.INVISIBLE);
                                    tip2.setVisibility(View.INVISIBLE);
                                    NearRoutes.setText("附近线路"+"("+String.valueOf(routesList.size())+")");
                                    //弹出底框
                                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                                }
                            }
                        });
                    }
                });
            }
        }
    }
    //创建StompClient，并监听生命周期
    public void createStompClient(){
        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, "ws://47.96.164.222:8080/hello/websocket");
        stompClient.withClientHeartbeat(1000).withServerHeartbeat(1000);
        stompClient.connect();
        stompClient.lifecycle().subscribe(lifecycleEvent -> {
            switch (lifecycleEvent.getType()) {

                case OPENED:
                    Log.d("MainActivity", "Stomp connection opened");
                    break;

                case ERROR:
                    Log.e("MainActivity", "Error", lifecycleEvent.getException());
                    break;

                case CLOSED:
                    Log.d("MainActivity", "Stomp connection closed");
                    break;
            }
        });

    }
    //订阅消息
    public void registerStompTopic(){
       stompClient.topic("/topic/getResponse16").subscribe(topicMessage -> {
           Log.d("MainActivity", topicMessage.getPayload());
        }, throwable -> {
            Log.e("MainActivity", "连接错误", throwable);
        });
    }
    public class MyLocationListener extends BDAbstractLocationListener {
        private boolean isFirst=true;
        @Override
        public void onReceiveLocation(BDLocation location) {
            //mapView 销毁后不在处理新接收的位置
            if (location == null || mapView == null){
                return;
            }
            double latitude=location.getLatitude();
            double longitude=location.getLongitude();
            //首次定位移动到我的位置
            LatLng point=new LatLng(latitude,longitude);
            //myLocation ：全局位置
            myLocation=point;
            MyLocationData locData = new MyLocationData.Builder()
                    .accuracy(location.getRadius())
                    // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(location.getDirection()).latitude(location.getLatitude())
                    .longitude(location.getLongitude()).build();
            baiduMap.setMyLocationData(locData);
            if(isFirst){
                //myNotifyLister.SetNotifyLocation(myLocation.latitude,myLocation.longitude,3000,mLocationClient.getLocOption().getCoorType());
                //注册位置提醒监听函数
               // mLocationClient.registerNotify(myNotifyLister);
                MapStatusUpdate mapStatusUpdate=MapStatusUpdateFactory.newLatLngZoom(point,18);
                baiduMap.animateMapStatus(mapStatusUpdate);
                isFirst=false;
            }else{
                //可重新设置位置提醒参数
                //myNotifyLister.SetNotifyLocation(myLocation.latitude,myLocation.longitude,3000,mLocationClient.getLocOption().getCoorType());
            }
        }
    }
    //定义MyNotifyLister类，继承BDNotifyListener，实现位置监听的回调。
    public class MyNotifyLister extends BDNotifyListener {
        public void onNotify(BDLocation mlocation, float distance){
            //已到达设置监听位置附近
            vibrator.vibrate(1000);//震动1s
            Toast.makeText(MainActivity.this,"车辆即将到站",Toast.LENGTH_LONG).show();
        }
    }

    public void addStopMarker(List<LatLng> list ,List<OverlayOptions> optionsList){
        BitmapDescriptor bitmap=BitmapDescriptorFactory.fromResource(R.drawable.stop);
        //批量创建OverlayOptions属性
        for(int i=1;i<list.size()-1;i++){
            OverlayOptions option=new MarkerOptions().position(list.get(i)).icon(bitmap);
            optionsList.add(option);
        }
    }

    @Override
    protected void onResume() {
        mapView.onResume();
        super.onResume();
    }

    @Override
    protected void onPause() {
        mapView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mLocationClient.stop();
        baiduMap.setMyLocationEnabled(false);
        mapView.onDestroy();
        mapView = null;
        //销毁路线检索实例
        mSearch.destroy();
        super.onDestroy();
        //断开长连接,并在回调函数中销毁mDisposable
        if(adapter.stompClient!=null){
            adapter.stompClient.disconnect();
        }
    }
}
