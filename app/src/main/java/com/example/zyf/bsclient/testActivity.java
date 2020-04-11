package com.example.zyf.bsclient;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.example.zyf.bsclient.Dao.Routes;
import com.example.zyf.bsclient.Dao.route;
import com.example.zyf.bsclient.Util.AdapterStop;
import com.example.zyf.bsclient.Util.myAdapter;

import java.util.ArrayList;
import java.util.List;

public class testActivity extends AppCompatActivity {


    private AdapterStop adapterStop;

    private List<route> list=new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        //模拟数据
        for(int i=0;i<20;i++){
            route myRoute=new route();
            myRoute.setStop_name("测试站点名字");
            list.add(myRoute);
        }

        RecyclerView recyclerView1=findViewById(R.id.recyclerView2);
        LinearLayoutManager linearLayoutManager1=new LinearLayoutManager(testActivity.this);

        adapterStop=new AdapterStop(list,testActivity.this);
        recyclerView1.setLayoutManager(linearLayoutManager1);
        recyclerView1.setAdapter(adapterStop);
    }
}
