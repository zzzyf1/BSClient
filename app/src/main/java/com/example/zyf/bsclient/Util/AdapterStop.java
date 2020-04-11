package com.example.zyf.bsclient.Util;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.zyf.bsclient.Dao.route;
import com.example.zyf.bsclient.R;

import java.util.List;

public class AdapterStop extends RecyclerView.Adapter<AdapterStop.ViewHolder> {
    private List<route> list;
    private Activity activity;
    public AdapterStop(List<route> list,Activity activity){
        this.activity=activity;
        this.list=list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view=LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_stop,viewGroup,false);
        final AdapterStop.ViewHolder holder=new AdapterStop.ViewHolder(view);
        //此处添加点击事件

        return holder;
    }
    //绑定列表项内容
    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        route mRoute = list.get(i);
        //绘制车站轴
       /* if(i==0){
            viewHolder.upLine.setVisibility(View.INVISIBLE);
        }else if(i==list.size()-1){
            viewHolder.bottomLine.setVisibility(View.INVISIBLE);
        }*/
        //其余的只需设置一下站点名字即可
        viewHolder.information.setText(mRoute.getStop_name());
    }
    //返回列表项个数
    @Override
    public int getItemCount() {
        return list.size();
    }

    //内部类实现缓存器,缓存布局以及该布局里包含的控件
    static class ViewHolder extends RecyclerView.ViewHolder{
        TextView upLine;
        TextView bottomLine;
        CardView cardView;
        TextView information;
        public ViewHolder(View view){
            super(view);
            upLine=view.findViewById(R.id.upLine);
            bottomLine=view.findViewById(R.id.bottomLine);
            cardView=view.findViewById(R.id.cycle);
            information=view.findViewById(R.id.information);
        }
    }
}
