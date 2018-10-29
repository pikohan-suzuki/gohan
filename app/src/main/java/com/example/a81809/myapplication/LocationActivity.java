package com.example.a81809.myapplication;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.location.Location;

import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.util.Date;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

public class LocationActivity extends AppCompatActivity {

    // Fused Location Provider API.
    private FusedLocationProviderClient fusedLocationClient;

    // Location Settings APIs.
    private SettingsClient settingsClient;
    private LocationSettingsRequest locationSettingsRequest;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    private Location location;

    private String lastUpdateTime;
    private Boolean requestingLocationUpdates;
    private static final int REQUEST_CHECK_SETTINGS = 0x1;
    private int priority = 0;

    private Spinner type_spinner;
    private EditText building_EditText;
    private EditText des_EditText;
    private TextView log_TextView;
    private Button regButton;
    private Button logButton;
    private String spinnerItems[] = {
            "Conner",
            "Entrance",
            "Other"
    };

    private String saveData = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(this);
        settingsClient = LocationServices.getSettingsClient(this);

        priority = 0;

        createLocationCallback();
        createLocationRequest();
        buildLocationSettingsRequest();

        //スパイナーリストの内容を指定
        type_spinner = findViewById(R.id.type_spinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, spinnerItems
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        type_spinner.setAdapter(adapter);

        building_EditText = findViewById(R.id.building_EditText);
        des_EditText = findViewById(R.id.des_EditText);
        regButton = findViewById(R.id.reg_button);
        logButton = findViewById(R.id.Log_button);
        log_TextView = findViewById(R.id.log_Text);
//
//
//        textView = (TextView) findViewById(R.id.text_view);
//        textLog = "onCreate()\n";
//        textView.setText(textLog);

        // 測位開始
        regButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startLocationUpdates();
                stopLocationUpdates();
            }
        });

        logButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplication(), LogActivity.class);
                intent.putExtra("saveData",saveData);
                startActivity(intent);
            }
        });

//        // 測位終了
//        Button buttonStop = (Button) findViewById(R.id.button_stop);
//        buttonStop.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                stopLocationUpdates();
//            }
//        });

    }

    // locationのコールバックを受け取る
    private void createLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                location = locationResult.getLastLocation();

                lastUpdateTime = DateFormat.getTimeInstance().format(new Date());
                updateLocationUI();
            }
        };
    }

    private void updateLocationUI() {
        // getLastLocation()からの情報がある場合のみ
        if (location != null) {

            String dataInfo[] = {
                    type_spinner.getSelectedItem().toString(),
                    building_EditText.getText().toString(),
                    des_EditText.getText().toString()
            };

            double locationData[] = {
                    location.getLatitude(),
                    location.getLongitude(),
                    location.getAltitude()
            };

            for (int i = 0; i < 3; i++) {
                saveData += dataInfo[i];
                saveData += ",";
            }
            for (int i = 0; i < 3; i++) {
                saveData += String.valueOf(locationData[i]);
                if (i != 2) {
                    saveData += ",";
                } else {
                    saveData += "\n";
                }
            }
        }
    }

    private void createLocationRequest() {
        locationRequest = new LocationRequest();

        if (priority == 0) {
            locationRequest.setPriority(
                    LocationRequest.PRIORITY_HIGH_ACCURACY);

        } else if (priority == 1) {
            locationRequest.setPriority(
                    LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        } else if (priority == 2) {

            locationRequest.setPriority(
                    LocationRequest.PRIORITY_LOW_POWER);
        } else {

            locationRequest.setPriority(
                    LocationRequest.PRIORITY_NO_POWER);
        }

        //単位はms
        locationRequest.setInterval(60000);

        locationRequest.setFastestInterval(5000);

    }

    //GPSが利用できるか確認
    private void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder =
                new LocationSettingsRequest.Builder();

        builder.addLocationRequest(locationRequest);
        locationSettingsRequest = builder.build();
    }

    @Override
    protected void onActivityResult(int requestCode,
                                    int resultCode, Intent data) {
        switch (requestCode) {
            // Check for the integer request code originally supplied to startResolutionForResult().
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Log.i("debug", "User agreed to make required location settings changes.");
                        // Nothing to do. startLocationupdates() gets called in onResume again.
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.i("debug", "User chose not to make required location settings changes.");
                        requestingLocationUpdates = false;
                        break;
                }
                break;
        }
    }

    // FusedLocationApiによるlocation updatesをリクエスト
    private void startLocationUpdates() {
        // Begin by checking if the device has the necessary location settings.
        settingsClient.checkLocationSettings(locationSettingsRequest)
                .addOnSuccessListener(this,
                        new OnSuccessListener<LocationSettingsResponse>() {
                            @Override
                            public void onSuccess(
                                    LocationSettingsResponse locationSettingsResponse) {
                                Log.i("debug", "All location settings are satisfied.");

                                // パーミッションの確認
                                if (ActivityCompat.checkSelfPermission(
                                        LocationActivity.this,
                                        Manifest.permission.ACCESS_FINE_LOCATION) !=
                                        PackageManager.PERMISSION_GRANTED
                                        && ActivityCompat.checkSelfPermission(
                                        LocationActivity.this,
                                        Manifest.permission.ACCESS_COARSE_LOCATION) !=
                                        PackageManager.PERMISSION_GRANTED) {

                                    // TODO: Consider calling
                                    //    ActivityCompat#requestPermissions
                                    // here to request the missing permissions, and then overriding
                                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                    //                                          int[] grantResults)
                                    // to handle the case where the user grants the permission. See the documentation
                                    // for ActivityCompat#requestPermissions for more details.
                                    return;
                                }
                                fusedLocationClient.requestLocationUpdates(
                                        locationRequest, locationCallback, Looper.myLooper());

                            }
                        })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        int statusCode = ((ApiException) e).getStatusCode();
                        switch (statusCode) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                Log.i("debug", "Location settings are not satisfied. Attempting to upgrade " +
                                        "location settings ");
                                try {
                                    // Show the dialog by calling startResolutionForResult(), and check the
                                    // result in onActivityResult().
                                    ResolvableApiException rae = (ResolvableApiException) e;
                                    rae.startResolutionForResult(
                                            LocationActivity.this,
                                            REQUEST_CHECK_SETTINGS);

                                } catch (IntentSender.SendIntentException sie) {
                                    Log.i("debug", "PendingIntent unable to execute request.");
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                String errorMessage = "Location settings are inadequate, and cannot be " +
                                        "fixed here. Fix in Settings.";
                                Log.e("debug", errorMessage);
                                Toast.makeText(LocationActivity.this,
                                        errorMessage, Toast.LENGTH_LONG).show();

                                requestingLocationUpdates = false;
                        }

                    }
                });

        requestingLocationUpdates = true;
    }

    private void stopLocationUpdates() {
//        textLog += "onStop()\n";
//        textView.setText(textLog);

        if (!requestingLocationUpdates) {
            Log.d("debug", "stopLocationUpdates: " +
                    "updates never requested, no-op.");


            return;
        }

        fusedLocationClient.removeLocationUpdates(locationCallback)
                .addOnCompleteListener(this,
                        new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                requestingLocationUpdates = false;
                            }
                        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        // バッテリー消費を鑑みLocation requestを止める
        stopLocationUpdates();
    }

}
