package com.me.bui.homescreen;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.me.bui.homescreen.model.AppInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private RecyclerView mRCAllApps;
    private RecyclerView.LayoutManager mLayoutManager;
    private AppAdapter mAdapter;
    private List<AppInfo> mAppInfoList;

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
}
