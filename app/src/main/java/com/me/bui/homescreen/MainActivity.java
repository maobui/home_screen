package com.me.bui.homescreen;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.RemoteViews;

import com.me.bui.homescreen.model.AppInfo;
import com.me.bui.homescreen.widget.BatteryWidget;
import com.me.bui.homescreen.widget.ClockWidget;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private RecyclerView mRCAllApps;
    private RecyclerView.LayoutManager mLayoutManager;
    private AppAdapter mAdapter;
    private List<AppInfo> mAppInfoList;

    private AppWidgetManager mAppWidgetManager;
    private AppWidgetHost mAppWidgetHost;
    private ClockWidget mClockWidget;

    static final int APPWIDGET_HOST_ID = 1024;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRCAllApps = findViewById(R.id.rc_all_app);
        mLayoutManager = new GridLayoutManager(this, 3);
        mAdapter = new AppAdapter(this);
        mRCAllApps.setLayoutManager(mLayoutManager);
        mRCAllApps.setHasFixedSize(true);
        mRCAllApps.setAdapter(mAdapter);

        getAllApps();
        createWidget();
        createBatteryWidget();
    }

    private void getAllApps() {
        Single<List<AppInfo>> single = Single.fromCallable(callGetAppInfoList());
        single.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(getListAppObserver());

    }

    private Callable<List<AppInfo>> callGetAppInfoList() {
        return new Callable<List<AppInfo>>() {
            @Override
            public List<AppInfo> call() throws Exception {
                return getAppInfoList();
            }
        };
    }

    private List<AppInfo> getAppInfoList() {
        PackageManager packageManager = this.getPackageManager();
        List<AppInfo> appInfoList = new ArrayList<>();

        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> allApps = packageManager.queryIntentActivities(intent, 0);
        for (ResolveInfo ri : allApps) {
            AppInfo app = new AppInfo(ri.loadLabel(packageManager),
                    ri.activityInfo.packageName,
                    ri.activityInfo.loadIcon(packageManager));
            appInfoList.add(app);
        }
        return appInfoList;
    }

    private SingleObserver<List<AppInfo>> getListAppObserver() {
        return new SingleObserver<List<AppInfo>>() {

            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onSuccess(List<AppInfo> appInfos) {
                updateUI(appInfos);
            }

            @Override
            public void onError(Throwable e) {

            }
        };
    }

    private void updateUI(List<AppInfo> appInfos) {
        mAppInfoList = appInfos;
        mAdapter.setAppInfoList(mAppInfoList);
        mAdapter.notifyDataSetChanged();
    }

    public void createWidget() {
        // APPWIDGET_HOST_ID is any number you like
        mAppWidgetManager = AppWidgetManager.getInstance(this);
        mAppWidgetHost = new AppWidgetHost(this, APPWIDGET_HOST_ID);
        AppWidgetProviderInfo newAppWidgetProviderInfo = new AppWidgetProviderInfo();

        // Get an id
        int appWidgetId = mAppWidgetHost.allocateAppWidgetId();

        // Get the list of installed widgets
        List<AppWidgetProviderInfo> appWidgetInfos = new ArrayList<AppWidgetProviderInfo>();
        appWidgetInfos = mAppWidgetManager.getInstalledProviders();

        for(int j = 0; j < appWidgetInfos.size(); j++)
        {
            if (appWidgetInfos.get(j).provider.getPackageName().equals("com.me.bui.homescreen") && appWidgetInfos.get(j).provider.getClassName().equals("com.me.bui.homescreen.widget.ClockWidget"))
            {
                // Get the full info of the required widget
                newAppWidgetProviderInfo = appWidgetInfos.get(j);
                break;
            }
        }

        // Create Widget
        AppWidgetHostView hostView = mAppWidgetHost.createView(this, appWidgetId, newAppWidgetProviderInfo);
        hostView.setAppWidget(appWidgetId, newAppWidgetProviderInfo);

        // Add it to your layout
        LinearLayout ll_widget =  findViewById(R.id.ll_widget);
        ll_widget.addView(hostView);
    }

    public void createBatteryWidget() {
        // APPWIDGET_HOST_ID is any number you like
//        mAppWidgetManager = AppWidgetManager.getInstance(this);
//        mAppWidgetHost = new AppWidgetHost(this, APPWIDGET_HOST_ID);
        AppWidgetProviderInfo newAppWidgetProviderInfo = new AppWidgetProviderInfo();

        // Get an id
        int appWidgetId = mAppWidgetHost.allocateAppWidgetId();

        // Get the list of installed widgets
        List<AppWidgetProviderInfo> appWidgetInfos = new ArrayList<AppWidgetProviderInfo>();
        appWidgetInfos = mAppWidgetManager.getInstalledProviders();

        for(int j = 0; j < appWidgetInfos.size(); j++)
        {
            if (appWidgetInfos.get(j).provider.getPackageName().equals("com.me.bui.homescreen") && appWidgetInfos.get(j).provider.getClassName().equals("com.me.bui.homescreen.widget.BatteryWidget"))
            {
                // Get the full info of the required widget
                newAppWidgetProviderInfo = appWidgetInfos.get(j);
                break;
            }
        }

        // Create Widget
        AppWidgetHostView hostView = mAppWidgetHost.createView(this, appWidgetId, newAppWidgetProviderInfo);
        hostView.setAppWidget(appWidgetId, newAppWidgetProviderInfo);

        // Add it to your layout
        LinearLayout ll_widget =  findViewById(R.id.ll_widget);
        ll_widget.addView(hostView);
    }
}
