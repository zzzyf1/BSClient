package com.example.zyf.bsclient.Util;

import android.app.Activity;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.InfoWindow;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.Overlay;
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
import com.example.zyf.bsclient.Dao.Location;
import com.example.zyf.bsclient.Dao.Routes;
import com.example.zyf.bsclient.Dao.route;
import com.example.zyf.bsclient.R;
import com.example.zyf.bsclient.ovelayutil.DrivingRouteOverlay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.disposables.Disposable;
import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.StompClient;

public class myAdapter extends RecyclerView.Adapter<myAdapter.ViewHolder> {
    private List<Routes> routesList;
    private Activity activity;
    //路线规划
    private RoutePlanSearch mSearch;
    private BaiduMap baiduMap;
    private MapView mapView;
    private List<InfoWindow> infoWindowList=new ArrayList<>();
    private List<Overlay> overlayList=new ArrayList<>();
    //不同marker对应不同的信息窗
    private Map<Marker,InfoWindow> map=new HashMap<>();
    //实时获取公交位置
    //Stomp连接客户端
    public StompClient stompClient;
    //公交实时位置Marker
    public Marker busLocationMarker;
    //当前订阅对象 因为只允许订阅一个对象，所以不再使用CompositeDisposable统一管理
    //CompositeDisposable具体用法见：（github）https://github.com/NaikSoftware/StompProtocolAndroid/blob/master/example-client/src/main/java/ua/naiksoftware/stompclientexample/MainActivity.java
    //简书介绍：https://www.jianshu.com/p/2a882604bbe8
    public Disposable mDisposable;

    //构造函数
    public myAdapter(List<Routes> routes, Activity activity){
        this.activity=activity;
        this.routesList=routes;
        this.mapView=this.activity.findViewById(R.id.mapView);
        baiduMap=this.mapView.getMap();
        //与服务器建立长链接
        createStompClient();
    }
    //为列表项中的控件添加对应事件
    @NonNull
    @Override
    public myAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view=LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_route,viewGroup,false);
        final myAdapter.ViewHolder holder=new myAdapter.ViewHolder(view);
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
        //Marker点击回调事件，显示信息窗
        baiduMap.setOnMarkerClickListener(new BaiduMap.OnMarkerClickListener() {
            //若响应点击事件，返回true。
            //默认返回false
            @Override
            public boolean onMarkerClick(Marker marker) {
                baiduMap.showInfoWindow(map.get(marker));
                return true;
            }
        });

        /**
         *此处添加列表项点击事件 */
        holder.showMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        final int position=holder.getAdapterPosition();
                        //获取线路
                        //线路节点
                        List<LatLng> StopLatLng=new ArrayList<>();
                        //当前线路
                        List<route> currentRoute=routesList.get(position).getRouteList();
                        //准备站点信息
                        for(int i=0;i<currentRoute.size();i++){
                            StopLatLng.add(new LatLng(currentRoute.get(i).getLatitude(),currentRoute.get(i).getLongitude()));
                        }
                        //准备起点终点信息
                        PlanNode stNode=PlanNode.withLocation(StopLatLng.get(0));
                        PlanNode edNode=PlanNode.withLocation(StopLatLng.get(StopLatLng.size()-1));
                        //准备途径点信息
                        List<PlanNode> passByList=new ArrayList<>();
                        if(StopLatLng.size()>2){
                            for(int i=1;i<StopLatLng.size()-1;i++){
                                passByList.add(PlanNode.withLocation(StopLatLng.get(i)));
                            }
                        }
                        //添加公交站点图标，不包括起点站，终点站
                        baiduMap.clear();
                        List<OverlayOptions> options = new ArrayList<>();
                        addStopMarker(StopLatLng,options);
                        //List<Marker> testList=new ArrayList<>();
                        //先清空站点标记集合
                        if(overlayList.isEmpty()){
                            overlayList.clear();
                        }
                        overlayList=baiduMap.addOverlays(options);
                        //添加信息窗
                        addInfoWindow(currentRoute);
                        //刚开始默认显示第二个站点标记的信息窗
                        if(!map.isEmpty()&&!overlayList.isEmpty()){
                            baiduMap.showInfoWindow(map.get((Marker)overlayList.get(0)));
                        }
                        //发起检索
                        mSearch.drivingSearch(new DrivingRoutePlanOption().from(stNode).to(edNode).passBy(passByList));
                        //暂时将地图中心移动到始发站
                        MapStatusUpdate mapStatusUpdate=MapStatusUpdateFactory.newLatLngZoom(StopLatLng.get(0),16);
                        baiduMap.animateMapStatus(mapStatusUpdate);
                        //订阅消息(返回公交实时位置)
                        //先取消之前的订阅
                        if(mDisposable!=null){
                            mDisposable.dispose();
                        }
                        //订阅当前线路
                        //获取当前线路公交的实时位置信息
                        registerStompTopic(String.valueOf(currentRoute.get(0).getRoute_id()));
                    }
                });

            }
        });

        return holder;
    }
    //绑定每个列表项内容
    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        Routes routes=routesList.get(i);
        viewHolder.name.setText(routes.getRouteList().get(0).getDescription());
        viewHolder.number.setText(String.valueOf(routes.getRouteList().size())+"站");
        viewHolder.FirstStop.setText("始发 "+routes.getRouteList().get(0).getStop_name());
        viewHolder.LastStop.setText("终到 "+routes.getRouteList().get(routes.getRouteList().size() -1 ).getStop_name());
    }
    //返回列表项的个数
    @Override
    public int getItemCount() {
        return routesList.size();
    }

    //内部类实现缓存器,缓存布局以及该布局里包含的控件
    static class ViewHolder extends RecyclerView.ViewHolder{
        TextView name;
        TextView number;
        TextView FirstStop;
        TextView LastStop;
        TextView showMap;
        public ViewHolder(View view){
            super(view);
            name=view.findViewById(R.id.routeName);
            number=view.findViewById(R.id.StopsNumber);
            FirstStop=view.findViewById(R.id.firstStop);
            LastStop=view.findViewById(R.id.LastStop);
            showMap=view.findViewById(R.id.showmap);
        }

    }
    public void addStopMarker(List<LatLng> list ,List<OverlayOptions> optionsList){
        BitmapDescriptor bitmap=BitmapDescriptorFactory.fromResource(R.drawable.stop2);
        //批量创建OverlayOptions属性
        for(int i=1;i<list.size()-1;i++){
            OverlayOptions option=new MarkerOptions().position(list.get(i)).icon(bitmap);
            optionsList.add(option);
        }
    }
    public void addInfoWindow(List<route> currentRouteList){
        //先清空map
        if(map.isEmpty()){
            map.clear();
        }
        if(currentRouteList!=null&&!currentRouteList.isEmpty()){
            for(int i=1;i<currentRouteList.size()-1;i++){
                TextView textView=new TextView(activity);
                textView.setBackgroundResource(R.drawable.pop);
                textView.setText(currentRouteList.get(i).getStop_name());
                textView.setTextColor(Color.WHITE);
                textView.setTextSize(10);
                textView.setGravity(Gravity.CENTER);
                textView.setPadding(8,0,8,10);
                InfoWindow infoWindow=new InfoWindow(textView,new LatLng(currentRouteList.get(i).getLatitude(),currentRouteList.get(i).getLongitude()),-100);
                infoWindowList.add(infoWindow);
                //添加到Map
                map.put((Marker) overlayList.get(i-1),infoWindow);
            }
        }
    }
    //创建StompClient，连接服务器并监听生命周期
    public void createStompClient(){
        //192.168.68.217
        //47.96.164.222
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
                    if(mDisposable!=null){
                        mDisposable.dispose();
                    }
                    break;

                case CLOSED:
                    Log.d("MainActivity", "Stomp connection closed");
                    if(mDisposable!=null){
                        mDisposable.dispose();
                    }
                    break;
            }
        });
    }
    //断开stomp连接
    public void disconnectStomp(){
        stompClient.disconnect();
    }
    //订阅消息
    public void registerStompTopic(String routeId){
        mDisposable=stompClient.topic("/topic/getResponse"+routeId).subscribe(topicMessage -> {
            String json=topicMessage.getPayload();
            Location location=GetJson.resovle(json);
            //在地图上实时显示公交位置
            if(busLocationMarker!=null){
                //每10秒更新位置
                busLocationMarker.setPosition(new LatLng(location.getLatitude(),location.getLongitude()));
            }else{
                //初始化公交实时位置标记
                BitmapDescriptor bitmap=BitmapDescriptorFactory.fromResource(R.drawable.bus_location);
                OverlayOptions option=new MarkerOptions().position(new LatLng(location.getLatitude(),location.getLongitude())).icon(bitmap);
                busLocationMarker=(Marker)baiduMap.addOverlay(option);
            }
            Log.d("MainActivity", json);
        }, throwable -> {
            Log.e("MainActivity", "订阅错误", throwable);
        });
    }
}
