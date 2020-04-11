package com.example.zyf.bsclient.Util;

import android.util.Log;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class AsyncHttpUtil {
    public static void sendOkHttpRequest(String address, String station_id, okhttp3.Callback callback) {
        try {
            OkHttpClient client = new OkHttpClient();

            RequestBody requestBody = new FormBody.Builder().add("station_id",station_id).build();
            //装载请求
            Request request = new Request.Builder().url(address).post(requestBody).build();
            //发送请求
            client.newCall(request).enqueue(callback);
        }catch (Exception e){
            Log.d("TestOkHttp","-----------------");
            e.printStackTrace();
        }

    }
}
