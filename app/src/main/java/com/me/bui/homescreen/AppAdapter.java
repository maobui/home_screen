package com.me.bui.homescreen;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.me.bui.homescreen.model.AppInfo;

import java.util.List;

/**
 * Created by mao.bui on 7/11/2018.
 */
public class AppAdapter extends RecyclerView.Adapter<AppAdapter.AppViewHolder> {

    private Context mContext;
    private List<AppInfo> mAppInfoList;

    public AppAdapter(Context context) {
        mContext = context;
    }

    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View root = LayoutInflater.from(parent.getContext()).inflate(R.layout.app_item, parent, false);
        AppViewHolder viewHolder = new AppViewHolder(root);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        String appLabel = mAppInfoList.get(position).getLabel().toString();
        String appPackage = mAppInfoList.get(position).getPackageName().toString();
        Drawable appIcon = mAppInfoList.get(position).getIcon();

        TextView textView = holder.tv_name;
        textView.setText(appLabel);
        ImageView imageView = holder.img_app;
        imageView.setImageDrawable(appIcon);
    }

    @Override
    public int getItemCount() {
        return (mAppInfoList == null ? 0 : mAppInfoList.size());
    }

    public void setAppInfoList(List<AppInfo> appInfoList) {
        mAppInfoList = appInfoList;
    }

    public class AppViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public TextView tv_name;
        public ImageView img_app;

        public AppViewHolder(View itemView) {
            super(itemView);
            tv_name = itemView.findViewById(R.id.tv_name);
            img_app = itemView.findViewById(R.id.img_app);

            img_app.setOnClickListener(this);
        }


        @Override
        public void onClick(View view) {
            int pos = getAdapterPosition();
            Context context = view.getContext();

            Intent intent = context.getPackageManager().getLaunchIntentForPackage(mAppInfoList.get(pos).getPackageName().toString());
            context.startActivity(intent);
        }
    }
}
