package com.example.zyf.bsclient.Util;

import com.example.zyf.bsclient.Dao.Location;
import com.example.zyf.bsclient.Dao.Routes;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.List;

public class GetJson {
    public static List<Routes> resolve(String json){
        return new Gson().fromJson(json,new TypeToken<List<Routes>>(){}.getType());
    }
    public static Location resovle(String json){
        return new Gson().fromJson(json,new TypeToken<Location>(){}.getType());
    }


}
