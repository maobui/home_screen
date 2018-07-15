package com.me.bui.homescreen.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.Log;
import android.widget.RemoteViews;

import com.me.bui.homescreen.R;

/**
 * Created by mao.bui on 7/13/2018.
 */
public class BatteryWidget extends AppWidgetProvider {
    private static final String TAG = BatteryWidget.class.getSimpleName();

    private int batteryLevel = 0;

    @Override
    public void onEnabled(Context context) {
        Log.d(TAG, "onEnabled");
        super.onEnabled(context);
        updateViews(context);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(TAG, "onUpdate");
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    private void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        Log.d(TAG, "updateAppWidget");
        // Sometimes when the phone is booting, onUpdate method gets called before onEnabled()
        int currentLevel = calculateBatteryLevel(context);
        if (batteryChanged(currentLevel)) {
            batteryLevel = currentLevel;
            Log.d(TAG, "Battery changed");
        }
        updateViews(context);
    }

    private boolean batteryChanged(int currentLevelLeft) {
        return (batteryLevel != currentLevelLeft);
    }

    private int calculateBatteryLevel(Context context) {
        Log.d(TAG,"calculateBatteryLevel()");

        Intent batteryIntent = context.getApplicationContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        return level * 100 / scale;
    }

    private void updateViews(Context context) {
        Log.d(TAG, "updateViews()");

        if (batteryLevel == 0) {
            batteryLevel = calculateBatteryLevel(context);
        }
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.battery_widget);
        views.setTextViewText(R.id.batteryText, "Battery status : " + batteryLevel + "%");

        ComponentName componentName = new ComponentName(context, BatteryWidget.class);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        appWidgetManager.updateAppWidget(componentName, views);
    }
}
