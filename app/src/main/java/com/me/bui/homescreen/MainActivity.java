package com.me.bui.homescreen;

import android.Manifest;
import android.app.Activity;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.me.bui.homescreen.model.AppInfo;
import com.me.bui.homescreen.network.ApiClient;
import com.me.bui.homescreen.network.ApiService;
import com.me.bui.homescreen.network.model.Country;
import com.me.bui.homescreen.service.FetchAddressIntentService;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        LocationListener ,
        ActivityCompat.OnRequestPermissionsResultCallback
{

    private static final String TAG = MainActivity.class.getSimpleName();

    private CompositeDisposable disposable = new CompositeDisposable();
    private ApiService mApiService;
    private Country mCountry;

    private static final int UPDATE_INTERVAL_IN_MILLISECONDS = 10 * 1000;
    private static final int FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = 1 * 1000;
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1001;
    private static final int REQUEST_CHECK_SETTINGS = 9999;

    private RecyclerView mRCAllApps;
    private RecyclerView.LayoutManager mLayoutManager;
    private AppAdapter mAdapter;
    private List<AppInfo> mAppInfoList;

    private AppWidgetManager mAppWidgetManager;
    private AppWidgetHost mAppWidgetHost;


    static final int APPWIDGET_HOST_ID = 1024;

    protected Location mLastLocation;
    private AddressResultReceiver mResultReceiver;
    private String mAddressOutput;
    private FusedLocationProviderClient mFusedLocationProviderClient;

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private LocationSettingsRequest mLocationSettingsRequest;
    private SettingsClient mSettingsClient;
    private LocationCallback mLocationCallback;
    private boolean isNeedShowSettings = false;

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

        mApiService = ApiClient.getClient().create(ApiService.class);

        initLocationService();

        getAllApps();
        createWidget();
        createBatteryWidget();
        createLocationWidget();

        getCountry();
    }

    private void initLocationService() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        mResultReceiver = new AddressResultReceiver(new Handler());
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        mSettingsClient = LocationServices.getSettingsClient(this);

        // Create the LocationRequest object
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_INTERVAL_IN_MILLISECONDS)        // 10 seconds, in milliseconds
                .setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS); // 1 second, in milliseconds

        // Settings request location.
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();

        // Callback request update location.
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Log.d(TAG, "locationResult.getLastLocation : -- " + locationResult.getLastLocation());
                if (locationResult == null) {
                    Log.d(TAG, getString(R.string.call_back_location_result_null));
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    // Update UI with location data
                    // ...
                    Log.d(TAG, getString(R.string.call_back_location_result) + location.toString());

                    mLastLocation = location;
                }
                handleNewLocation(mLastLocation);
            }
        };

    }

    private void getCountry() {
        Single<List<Country>> countryObservable = getCountry("VietNam");
        disposable.add(countryObservable
                .subscribeWith(new DisposableSingleObserver<List<Country>>() {
                    @Override
                    public void onSuccess(List<Country> listCountry) {
                        mCountry = listCountry.get(0);
                        Log.i(TAG, "Country info : " + mCountry.toString());
                        String lang = Locale.getDefault().getLanguage();
                        String nameCountry;
                        switch (lang) {
                            case "en":
                                nameCountry = mCountry.getName();
                                break;
                            case "de":
                                nameCountry = mCountry.getTranslations().getDe();
                                break;
                            case "es":
                                nameCountry = mCountry.getTranslations().getEs();
                                break;
                            case "fr":
                                nameCountry = mCountry.getTranslations().getFr();
                                break;
                            case "it":
                                nameCountry = mCountry.getTranslations().getIt();
                                break;
                            case "br":
                                nameCountry = mCountry.getTranslations().getBr();
                                break;
                            case "pt":
                                nameCountry = mCountry.getTranslations().getPt();
                                break;
                            case "nl":
                                nameCountry = mCountry.getTranslations().getNl();
                                break;
                            case "hr":
                                nameCountry = mCountry.getTranslations().getHr();
                                break;
                            case "fa":
                                nameCountry = mCountry.getTranslations().getFa();
                                break;
                                default: nameCountry = mCountry.getName();
                        }

                        TextView tvShowCountry = findViewById(R.id.tv_show_country);
                        tvShowCountry.setText(nameCountry + "\n"
                                + mCountry.getCapital() + "\n"
                                + mCountry.getCurrencies().get(0).getCode() + " "
                                + mCountry.getCurrencies().get(0).getName()+ " "
                                + mCountry.getCurrencies().get(0).getSymbol()+ " "
                        );
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "onError : " + e.toString());
                    }
                })
        );
    }

    private Single<List<Country>> getCountry(String name) {
        return mApiService.getCountryByName(name)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    private void getAllApps() {
        Single<List<AppInfo>> single = Single.fromCallable(callGetAppInfoList());
//        single.subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(getListAppObserver());
        disposable.add(single
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableSingleObserver<List<AppInfo>>() {
                    @Override
                    public void onSuccess(List<AppInfo> appInfos) {
                        updateUI(appInfos);
                    }

                    @Override
                    public void onError(Throwable e) {

                    }
                }));

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

        for (int j = 0; j < appWidgetInfos.size(); j++) {
            if (appWidgetInfos.get(j).provider.getPackageName().equals("com.me.bui.homescreen") && appWidgetInfos.get(j).provider.getClassName().equals("com.me.bui.homescreen.widget.ClockWidget")) {
                // Get the full info of the required widget
                newAppWidgetProviderInfo = appWidgetInfos.get(j);
                break;
            }
        }

        // Create Widget
        AppWidgetHostView hostView = mAppWidgetHost.createView(this, appWidgetId, newAppWidgetProviderInfo);
        hostView.setAppWidget(appWidgetId, newAppWidgetProviderInfo);

        // Add it to your layout
        LinearLayout ll_widget = findViewById(R.id.ll_widget);
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

        for (int j = 0; j < appWidgetInfos.size(); j++) {
            if (appWidgetInfos.get(j).provider.getPackageName().equals("com.me.bui.homescreen") && appWidgetInfos.get(j).provider.getClassName().equals("com.me.bui.homescreen.widget.BatteryWidget")) {
                // Get the full info of the required widget
                newAppWidgetProviderInfo = appWidgetInfos.get(j);
                break;
            }
        }

        // Create Widget
        AppWidgetHostView hostView = mAppWidgetHost.createView(this, appWidgetId, newAppWidgetProviderInfo);
        hostView.setAppWidget(appWidgetId, newAppWidgetProviderInfo);

        // Add it to your layout
        LinearLayout ll_widget = findViewById(R.id.ll_widget);
        ll_widget.addView(hostView);
    }

    public void createLocationWidget() {
        AppWidgetProviderInfo newAppWidgetProviderInfo = new AppWidgetProviderInfo();

        // Get an id
        int appWidgetId = mAppWidgetHost.allocateAppWidgetId();

        // Get the list of installed widgets
        List<AppWidgetProviderInfo> appWidgetInfos = new ArrayList<AppWidgetProviderInfo>();
        appWidgetInfos = mAppWidgetManager.getInstalledProviders();

        for (int j = 0; j < appWidgetInfos.size(); j++) {
            if (appWidgetInfos.get(j).provider.getPackageName().equals("com.me.bui.homescreen") && appWidgetInfos.get(j).provider.getClassName().equals("com.me.bui.homescreen.widget.LocationWidget")) {
                // Get the full info of the required widget
                newAppWidgetProviderInfo = appWidgetInfos.get(j);
                break;
            }
        }

        // Create Widget
        AppWidgetHostView hostView = mAppWidgetHost.createView(this, appWidgetId, newAppWidgetProviderInfo);
        hostView.setAppWidget(appWidgetId, newAppWidgetProviderInfo);

        // Add it to your layout
        LinearLayout ll_widget = findViewById(R.id.ll_widget);
        ll_widget.addView(hostView);
    }

    protected void startIntentService() {
        Intent intent = new Intent(this, FetchAddressIntentService.class);
        intent.putExtra(Constants.RECEIVER, mResultReceiver);
        intent.putExtra(Constants.LOCATION_DATA_EXTRA, mLastLocation);
        startService(intent);
    }

    private void updateUILoc() {
        TextView tvShowLoc = findViewById(R.id.tv_show_loc);
        tvShowLoc.setText("");
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "Location services connected.");
        if (checkPermissions()) {
            Log.d(TAG, getString(R.string.please_allow_permission_warning));

            if (isNeedShowSettings) {
                showSettings();
            } else {
                requestLocationPermission();
            }
            return;
        }
        isNeedShowSettings = false;

        getLocation();
    }

    private void requestLocationPermission() {
        Log.d(TAG,"requestLocationPermission");
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            Log.d(TAG,"required permission.");
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
//                openSetting();
            Snackbar.make(findViewById(android.R.id.content), R.string.location_access_required,
                    Snackbar.LENGTH_INDEFINITE).setAction(R.string.ok, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Request the permission
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
                }
            }).show();
        } else {
            Log.d(TAG,"requestPermissions");
            // No explanation needed; request the permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    private void getLocation() {
        getLastLocation();
        if (mLastLocation == null) {
            startLocationUpdates();
        }
    }

    private void getLastLocation() {
        mFusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                Log.d(TAG, "onSuccess !!!!!!!!!");

                // In some rare cases the location returned can be null
                if (location == null) {
                    Log.w(TAG, "Last Location !!!!!!!!! NULL");
                    return;
                }

                if (!Geocoder.isPresent()) {
                    Toast.makeText(MainActivity.this,
                            R.string.no_geocoder_available,
                            Toast.LENGTH_LONG).show();
                    return;
                }

                handleNewLocation(location);
            }
        });
    }

    private void handleNewLocation(Location location) {
        Log.d(TAG, "handleNewLocation " + location.toString());
        stopLocationUpdates();
        mLastLocation = location;
        // Start service and update UI to reflect new location
        startIntentService();
        updateUILoc();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Location services connection suspended.");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(TAG, "Location services connection failed.");
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        } else {
            Log.i(TAG, "Location services connection failed with code " + connectionResult.getErrorCode());
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i(TAG, "Location services on changed.");
        handleNewLocation(location);
    }

    class AddressResultReceiver extends ResultReceiver {

        public AddressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

            if (resultData == null) {
                return;
            }

            // Display the address string
            // or an error message sent from the intent service.
            mAddressOutput = resultData.getString(Constants.RESULT_DATA_KEY);
            if (mAddressOutput == null) {
                mAddressOutput = "";
            }
            displayAddressOutput();

            // Show a toast message if an address was found.
            if (resultCode == Constants.SUCCESS_RESULT) {
                showToast(getString(R.string.address_found));
            }

        }

    }

    private void displayAddressOutput() {
        TextView tvShowLoc = findViewById(R.id.tv_show_loc);
        tvShowLoc.setText(mAddressOutput);
    }

    private void showToast(String string) {
        Toast.makeText(this, string, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mGoogleApiClient.isConnected()) {
            stopLocationUpdates();
            mGoogleApiClient.disconnect();
        }
    }

    private void startLocationUpdates() {
        mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
                .addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        mFusedLocationProviderClient.requestLocationUpdates(mLocationRequest,
                                mLocationCallback,
                                null /* Looper */);
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        int statusCode = ((ApiException) e).getStatusCode();
                        switch (statusCode) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                Log.i(TAG, "Location settings are not satisfied. Attempting to upgrade " +
                                        "location settings ");
                                try {
                                    // Show the dialog by calling startResolutionForResult(), and check the
                                    // result in onActivityResult().
                                    ResolvableApiException rae = (ResolvableApiException) e;
                                    rae.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
                                } catch (IntentSender.SendIntentException sie) {
                                    Log.i(TAG, "PendingIntent unable to execute request.");
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                String errorMessage = "Location settings are inadequate, and cannot be " +
                                        "fixed here. Fix in Settings.";
                                Log.e(TAG, errorMessage);

                                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                })
        ;

    }

    private void stopLocationUpdates() {
        mFusedLocationProviderClient.removeLocationUpdates(mLocationCallback);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if ( requestCode == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION ) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission was granted, yay! Do the
                // contacts-related task you need to do.
                Log.d(TAG, "PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION ok");
                getLocation();
            } else {
                Log.d(TAG, "PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION denied");
                // permission denied, boo! Disable the
                // functionality that depends on this permission.
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    showLocationPermissionRationale();
                } else {
                    isNeedShowSettings = true;
                }
            }
        } else {

            // other 'case' lines to check for other
            // permissions this app might request.
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }

    }

    private boolean checkPermissions() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED /*&& ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED*/;
    }

    private void openSetting() {
        Intent intent = new Intent();
        intent.setAction(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package",
                BuildConfig.APPLICATION_ID, null);
        intent.setData(uri);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void showLocationPermissionRationale() {
        Log.w(TAG, "showLocationPermissionRationale");
    }

    private void showSettings () {
        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), getResources().getString(R.string.location_access_required), Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction(getResources().getString(R.string.settings), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            }
        });
        snackbar.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            // Check for the integer request code originally supplied to startResolutionForResult().
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Log.e(TAG, "User agreed to make required location settings changes.");
                        // Nothing to do. startLocationupdates() gets called in onResume again.
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.e(TAG, "User chose not to make required location settings changes.");
                        break;
                }
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposable.dispose();
    }
}


