package com.me.bui.homescreen.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.RemoteViews;

import com.me.bui.homescreen.R;

/**
 * Created by mao.bui on 7/12/2018.
 */
public class ClockWidget extends AppWidgetProvider {
    private static final String TAG = ClockWidget.class.getSimpleName();

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    private void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        Log.d(TAG, "updateAppWidget");
        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.clock_widget);
        Intent intent = new Intent(android.provider.Settings.ACTION_DATE_SETTINGS);
        // In widget we are not allowing to use intents as usually. We have to use PendingIntent instead of 'startActivity'
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        // Here the basic operations the remote view can do.
        views.setOnClickPendingIntent(R.id.clock, pendingIntent);
        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}
