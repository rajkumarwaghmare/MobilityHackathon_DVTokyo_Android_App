package com.bcgdv.dvhacks.mobilityhackathon_dvtokyo_android_app;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.test.mock.MockPackageManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.bcgdv.dvhacks.mobilityhackathon_dvtokyo_android_app.data.BaseLocation;
import com.bcgdv.dvhacks.mobilityhackathon_dvtokyo_android_app.network.VolleyNetwork;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

/**
 * Map Activity that displays chargeable cars/stations around current location.
 */
public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private final String TAG = getClass().getSimpleName();

    private GoogleMap mMap;
    private SupportMapFragment mMapFragment;
    private LocationManager mLocationManager;
    private FusedLocationProviderClient mFusedLocationClient;
    private BottomSheetBehavior mBottomSheetBehavior;
    private LinearLayout mBottomSheet;
    private BottomSheetBehavior mBottomSheetChargeBehavior;
    private LinearLayout mBottomSheetCharge;

    private static final String MSG_GPS_ERROR = "GPS Error!";
    private static final String MSG_LOCATION_ERROR = "Please get permissions!";
    private static final String MSG_PERM_GRANTED = "Permission granted";
    private static final int REQUEST_CODE_LOCATION_PERMISSION = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mMapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        // Get the location permissions if not yet
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_CODE_LOCATION_PERMISSION);
        }

        // Bottom sheet for charging start and stop view
        mBottomSheetCharge =  findViewById(R.id.bottom_sheet_charge);
        // init the bottom sheet behavior
        mBottomSheetChargeBehavior = BottomSheetBehavior.from(mBottomSheetCharge);
        // change the state of the bottom sheet
        mBottomSheetChargeBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        final ImageView button = mBottomSheetCharge.findViewById(R.id.button_charge);
        button.setTag("start");
        final TextView timeView = mBottomSheetCharge.findViewById(R.id.time);
        final TextView totalView = mBottomSheetCharge.findViewById(R.id.total);
        final CountDownTimer timer = prepareTimeCounterView(timeView, totalView, 80);//80 cents per min
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String status = (String) button.getTag();
                if("start".equals(status)) {
                    informServer(true);
                    timer.start();
                    button.setTag("stop");
                    button.setImageResource(R.drawable.ic_stop);
                } else {
                    informServer(false);
                    timer.cancel();
                    mBottomSheetChargeBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                    button.setTag("start");
                    button.setImageResource(R.drawable.ic_start);
                }

            }
        });

        // get the bottom sheet view
        mBottomSheet =  findViewById(R.id.bottom_sheet);
        // init the bottom sheet behavior
        mBottomSheetBehavior = BottomSheetBehavior.from(mBottomSheet);
        // change the state of the bottom sheet
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        mBottomSheet.findViewById(R.id.button_charge).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                timeView.setText("00:00");
                totalView.setText("$0.00");
                mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                mBottomSheetChargeBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });



    }

    /**
     * Inform server about charging start and stop events
     * @param isStart "START" event if true, "STOP" otherwise
     */
    private void informServer(boolean isStart) {
        String START_URL  = "http://192.168.1.63:3000/reserve";
        String STOP_URL  = "http://192.168.1.63:3000/finish";
        String url = isStart ? START_URL : STOP_URL;

        StringRequest request = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Successfully informed the server");
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Error in informing the server");
            }
        });

        VolleyNetwork.getInstance(this).addToRequestQueue(request);

    }

    /**
     *
     * @param timeView
     * @param totalView
     * @param rate
     * @return
     */
    private CountDownTimer prepareTimeCounterView(final TextView timeView, final TextView totalView, final int rate) {
        final int maxTimeInMSecs = 3600000;
        final CountDownTimer timer = new CountDownTimer(maxTimeInMSecs, 1000) {
            public void onTick(long millisUntilFinished) {
                millisUntilFinished = maxTimeInMSecs - millisUntilFinished;

                long secs = millisUntilFinished/1000;
                long mins = 00;

                int dollors = 0;
                int cents = (int) (((float)rate/60)*secs);
                if(cents >= 100) {
                    dollors = cents / 100;
                    cents = cents % 100;
                }

                if(secs >= 60) {
                    mins = secs / 60;
                    secs = secs % 60;
                }
                if(millisUntilFinished >= 60) {
                    mins = mins % 60;
                }



                timeView.setText(String.format("%02d", mins)+":"+String.format("%02d", secs));
                totalView.setText(String.format("$%01d", dollors)+"."+String.format("%02d", cents));
            }

            @Override
            public void onFinish() {

            }
        };

        return timer;
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Make sure GPS is enabled, else direct user to setting screen.
        if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(myIntent);
        } else {
            mMapFragment.getMapAsync(this);
        }

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_CODE_LOCATION_PERMISSION);
            return;
        }

        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setIndoorLevelPickerEnabled(true);
        mMap.setBuildingsEnabled(true);
        mMap.setIndoorEnabled(true);

        mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location currentLocation) {
                        // Got last known location. In some rare situations this can be null.
                        if (currentLocation != null) {
                            // Logic to handle location object
                            Log.d(TAG, "GPS is on");
                            final double currentLatitude = currentLocation.getLatitude();
                            final double currentLongitude = currentLocation.getLongitude();
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(currentLatitude, currentLongitude), 15));
                            /**
                             * The desired zoom level, in the range of 2.0 to 21.0. Values below this range are set to 2.0, and values above it are set to 21.0.
                             * Increase the value to zoom in. Not all areas have tiles at the largest zoom levels.
                             */
                            mMap.animateCamera(CameraUpdateFactory.zoomTo(16), 2000, null);
                            addMarkers();
                            mMap.setOnMarkerClickListener(MapsActivity.this);
                        } else {
                            Log.e(TAG, "GPS is off");
                            Toast.makeText(MapsActivity.this, MSG_GPS_ERROR, Toast.LENGTH_LONG);
                        }
                    }

                });
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "Req Code " + requestCode);
        if (requestCode == REQUEST_CODE_LOCATION_PERMISSION) {
            if (grantResults.length == 1 &&
                    grantResults[0] == MockPackageManager.PERMISSION_GRANTED) {
                // Success Stuff here
                Log.d(TAG, MSG_PERM_GRANTED);
            } else {
                Toast.makeText(this, MSG_LOCATION_ERROR, Toast.LENGTH_SHORT);
            }
        }
    }


    /**
     * Add markers for chargeable places around current location
     * TODO get them from server
     */
    private void addMarkers() {
        Log.d(TAG, "Adding markers");

        if(mMap != null) {
            BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.drawable.ic_marker);
            ArrayList<BaseLocation> locations = BaseLocation.generateDummyData();

            for(BaseLocation location : locations) {
                mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(location.getLat(), location.getLon()))
                        .icon(icon))
                        .setTag(locations.indexOf(location));
            }
        } else {
            Log.e(TAG, "Cant add markers as mMap object is null");
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        ArrayList<BaseLocation> locations = BaseLocation.generateDummyData();
        BaseLocation tappedLocation = locations.get((int)marker.getTag());

        // change the state of the bottom sheet
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        ((TextView)mBottomSheet.findViewById(R.id.title)).setText(tappedLocation.getName());
        ((TextView)mBottomSheet.findViewById(R.id.model)).setText(tappedLocation.getModel());
        ((TextView)mBottomSheet.findViewById(R.id.rate)).setText("$"+tappedLocation.getRate());

        return false;
    }
}
