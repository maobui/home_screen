package com.me.bui.homescreen.service;

import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.me.bui.homescreen.Constants;
import com.me.bui.homescreen.R;
import com.me.bui.homescreen.widget.LocationWidget;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by mao.bui on 7/14/2018.
 */
public class FetchAddressIntentService extends IntentService{
    private static final String TAG = FetchAddressIntentService.class.getSimpleName();

    private static final String ACTION_FETCH_ADDRESS = "com.me.bui.homescreen.action.fetch_address";
    private static final String ACTION_UPDATE_LOCATION_WIDGETS = "com.me.bui.homescreen.action.update_location_widgets";

    protected ResultReceiver mReceiver;

    public FetchAddressIntentService() {
        super("FetchAddressIntentService");
    }

    public static void startActionFetchAddress(Context context, Intent intent) {
        intent.setAction(ACTION_FETCH_ADDRESS);
        context.startService(intent);
    }

//    public static void startActionFetchAddress(Context context) {
//        Intent intent = new Intent(context, FetchAddressIntentService.class);
//        intent.setAction(ACTION_FETCH_ADDRESS);
//        context.startService(intent);
//    }

    public static void startActionUpdateLocation(Context context, Intent intent) {
//        Intent intent = new Intent(context, FetchAddressIntentService.class);
        intent.setAction(ACTION_UPDATE_LOCATION_WIDGETS);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_FETCH_ADDRESS.equals(action)) {
                handleActionFetchAddress(intent);
            } else if (ACTION_UPDATE_LOCATION_WIDGETS.equals(action)) {
                handleActionUpdateLocation(intent);
            }
        }
    }

    private void deliverResultToReceiver(int resultCode, String message) {
        Bundle bundle = new Bundle();
        bundle.putString(Constants.RESULT_DATA_KEY, message);
        mReceiver.send(resultCode, bundle);
    }

    private void handleActionFetchAddress(Intent intent) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        String errorMessage = "";

        mReceiver = intent.getParcelableExtra(Constants.RECEIVER);
        // Get the location passed to this service through an extra.
        Location location = intent.getParcelableExtra(
                Constants.LOCATION_DATA_EXTRA);

        // ...

        List<Address> addresses = null;

        try {
            addresses = geocoder.getFromLocation(
                    location.getLatitude(),
                    location.getLongitude(),
                    // In this sample, get just a single address.
                    1);
        } catch (IOException ioException) {
            // Catch network or other I/O problems.
            errorMessage = getString(R.string.service_not_available);
            Log.e(TAG, errorMessage, ioException);
        } catch (IllegalArgumentException illegalArgumentException) {
            // Catch invalid latitude or longitude values.
            errorMessage = getString(R.string.invalid_lat_long_used);
            Log.e(TAG, errorMessage + ". " +
                    "Latitude = " + location.getLatitude() +
                    ", Longitude = " +
                    location.getLongitude(), illegalArgumentException);
        }

        // Handle case where no address was found.
        if (addresses == null || addresses.size()  == 0) {
            if (errorMessage.isEmpty()) {
                errorMessage = getString(R.string.no_address_found);
                Log.e(TAG, errorMessage);
            }
            deliverResultToReceiver(Constants.FAILURE_RESULT, errorMessage);
        } else {
            Address address = addresses.get(0);
            ArrayList<String> addressFragments = new ArrayList<String>();

            // Fetch the address lines using getAddressLine,
            // join them, and send them to the thread.
            for(int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                addressFragments.add(address.getAddressLine(i));
            }
            Log.i(TAG, getString(R.string.address_found));
            String country = getCoutryNameFromAddress(addressFragments);
            deliverResultToReceiver(Constants.SUCCESS_RESULT, country);

            Log.i(TAG, "addressFragments.toString() : " + addressFragments.toString());

            updateCountryName(country);
        }
    }

    private void updateCountryName(String country) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(this, LocationWidget.class));
        if (appWidgetIds == null) {
            Log.w(TAG, "appWidgetIds NULL");
        } else {
            Log.w(TAG, "appWidgetIds HERE " + appWidgetIds.length);
//                for (int i : appWidgetIds) {
//                    Log.w(TAG, "appWidgetIds i " + i);
//                }
        }
        //Now update all widgets
        LocationWidget.updateLocationWidgets(this, appWidgetManager, appWidgetIds, country);
    }

    private void handleActionUpdateLocation(Intent  intent) {

        String errorMessage = "";

        mReceiver = intent.getParcelableExtra(Constants.RECEIVER);
        // Get the location passed to this service through an extra.
        String  countryInfo = intent.getStringExtra(Constants.COUNTRY_DATA_INFO);


        // Handle case where no address was found.
        if (countryInfo == null || countryInfo  == "") {
            if (errorMessage.isEmpty()) {
                errorMessage = getString(R.string.no_country_info_found);
                Log.e(TAG, errorMessage);
            }
            deliverResultToReceiver(Constants.FAILURE_RESULT, errorMessage);
        } else {
            deliverResultToReceiver(Constants.SUCCESS_RESULT_QUERY_COUNTRY_INFO, countryInfo);
            Log.i(TAG, "countryInfo : " + countryInfo);
            updateCountryName(countryInfo);
        }
    }

    private String getCoutryNameFromAddress(ArrayList<String> addressArray) {
        String country = "";
        if (addressArray != null && addressArray.size() > 0) {
            String[] address = addressArray.get(0).split(",");
            country = address[address.length - 1];
        }
        Log.i(TAG, "country : " + country);
        return country;
    }

}
