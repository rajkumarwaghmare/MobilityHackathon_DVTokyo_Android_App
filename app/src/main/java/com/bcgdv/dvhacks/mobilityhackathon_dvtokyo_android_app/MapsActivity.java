package com.bcgdv.dvhacks.mobilityhackathon_dvtokyo_android_app;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.test.mock.MockPackageManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private SupportMapFragment mMapFragment;
    private LocationManager mLocationManager;
    private String mBestProvider;

    private static final String MSG_GPS_ERROR = "GPS Error!";
    private static final String MSG_NO_LOCATION_PERM_ERROR = "Please enable location permission";
    private static final String MSG_LOCATION_ERROR = "Please get permissions!";
    private static final int REQUEST_CODE_LOCATION_PERMISSION = 101;
    private static final int REQUEST_CODE_LOCATION_SETTINGS = 102;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mMapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mBestProvider = String.valueOf(mLocationManager.getBestProvider(new Criteria(), true));

        /**
         * Get the location premissions if not yet
         */
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_CODE_LOCATION_PERMISSION);
        }


        if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(myIntent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapFragment.getMapAsync(this);
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

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_CODE_LOCATION_PERMISSION);
            return;
        }

        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setIndoorLevelPickerEnabled(true);
        mMap.setBuildingsEnabled(true);
        mMap.setIndoorEnabled(true);

        /**
         * Make sure GPS is enabled, else direct user to setting screen.
         */
        if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, MSG_NO_LOCATION_PERM_ERROR, Toast.LENGTH_SHORT);
            Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivityForResult(myIntent, REQUEST_CODE_LOCATION_SETTINGS);
            return;
        }

        Location currentLocation = mLocationManager.getLastKnownLocation(mBestProvider);
        if (currentLocation != null) {
            Log.d("TAG", "GPS is on");
            final double currentLatitude = currentLocation.getLatitude();
            final double currentLongitude = currentLocation.getLongitude();
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(currentLatitude, currentLongitude), 15));
            /**
             *he desired zoom level, in the range of 2.0 to 21.0. Values below this range are set to 2.0, and values above it are set to 21.0.
             * Increase the value to zoom in. Not all areas have tiles at the largest zoom levels.
             */
            mMap.animateCamera(CameraUpdateFactory.zoomTo(16), 2000, null);
        } else {
            Log.e("TAG", "GPS is off");
            Toast.makeText(this, MSG_GPS_ERROR, Toast.LENGTH_SHORT);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d("Req Code", "" + requestCode);
        if (requestCode == REQUEST_CODE_LOCATION_PERMISSION) {
            if (grantResults.length == 1 &&
                    grantResults[0] == MockPackageManager.PERMISSION_GRANTED) {
                // Success Stuff here
                mMapFragment.getMapAsync(this);
            } else {
                Toast.makeText(this, MSG_LOCATION_ERROR, Toast.LENGTH_SHORT);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_LOCATION_SETTINGS) {
            // Make sure the request was successful
            if (resultCode != RESULT_OK) {
                finish();
            }
        }
    }


}
