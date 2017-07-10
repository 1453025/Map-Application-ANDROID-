package com.example.trangngo.mapaplication;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.EditText;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.text.NumberFormat;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, View.OnClickListener {

    private static final int REQUES_CODE_LOCATION = 10;
    String TAG = "MapsActivity";
    Marker ortherClientMarker;
    ListenLatLngChange listenLatLngChange;
    private GoogleMap mMap;
    private FloatingActionButton fab;
    private Socket mSocket;
    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("BroadCastReceiver", "OnReceive");
            double latitude = Double.valueOf(intent.getStringExtra("latutide"));
            double longitude = Double.valueOf(intent.getStringExtra("longitude"));
            LatLng mineLatLng = new LatLng(latitude, longitude);
            Log.d("GPS_TEST", mineLatLng.toString());

            listenLatLngChange.onLatLngChange(mineLatLng);
            String lat[] = {String.valueOf(latitude), String.valueOf(longitude)};

            JSONObject obj = new JSONObject();
            try {
                obj.put("lat", latitude);
                obj.put("long", longitude);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            mSocket.emit("latLngFromClient", obj);
        }
    };
    private Emitter.Listener onConnected = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String result;
                    try {
                        result = data.getString("result");

                        if (result == "true") {
                            Toast.makeText(getApplicationContext(), "Dang ki thanh cong", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getApplicationContext(), "Dang ki that bai", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        return;
                    }
                }
            });
        }
    };
    private Emitter.Listener latLngFromServer = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];

                    String lati;
                    String longi;
                    try {
                        lati = data.getString("lat");
                        longi = data.getString("long");

                    } catch (JSONException e) {
                        return;
                    }

                    double latitude = Double.valueOf(lati);
                    double longitude = Double.valueOf(longi);
                    LatLng mineLatLng = new LatLng(latitude, longitude);

                    ortherClientMarker.setPosition(mineLatLng);

                    listenLatLngChange.onLatLngChange(mineLatLng);
                }
            });
        }
    };

    {
        try {
            mSocket = IO.socket("http://192.168.1.142:3000");
        } catch (URISyntaxException e) {}
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);


        fab = (FloatingActionButton) findViewById(R.id.fab);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        fab.setOnClickListener(this);

        mSocket.on("resultRegister", onConnected);
        mSocket.on("latLngFromServer", latLngFromServer);
        mSocket.connect();

        listenLatLngChange = new ListenLatLngChange() {
            @Override
            public void onLatLngChange(LatLng latLng) {
                Log.d(TAG, "onLatLngChange: " + latLng);
                mMap.addMarker(new MarkerOptions().position(latLng));
            }
        };
    }


    ;

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(broadcastReceiver, new IntentFilter(GPSService.str_gps_receiver));
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
        mMap.setPadding(0, 300, 0, 0);
        Intent gpsServiceIntent = new Intent(this, GPSService.class);
        startService(gpsServiceIntent);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.

            ActivityCompat.requestPermissions(this,
                    new String[]{
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION,
                            //android.Manifest.permission.INTERNET,
                    }, 10);

            Toast.makeText(this, "Permission denied", Toast.LENGTH_LONG).show();

            return;
        }
        mMap.setMyLocationEnabled(true);
        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.animateCamera(CameraUpdateFactory.newLatLng(sydney));
        ortherClientMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(10.77524647645796, 106.67981909018566)));

    }

    public String showLatLng(LatLng latLng) {
        NumberFormat formatter = new DecimalFormat("#0.0000000");
        String lat = "", lng = "";

        try {
            lat = formatter.format(Double.valueOf(latLng.latitude));
            lng = formatter.format(Double.valueOf(latLng.longitude));
        } catch (Exception e) {
            Log.e("Tag", e.getMessage().toString());
        }
        return "Lat " + lat + ", " + "Lng " + lng;
    }

    public void addMarker(LatLng latLng) {
        mMap.clear();
        mMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title(showLatLng(latLng))
        ).showInfoWindow();
    }

    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUES_CODE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                    }
                    mMap.setMyLocationEnabled(true);
                    Toast.makeText(this, "Permission granted", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_LONG).show();
                }
            }
        }

    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.fab){
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(MapsActivity.this);

            LayoutInflater inflater = this.getLayoutInflater();
            final View dialogView = inflater.inflate(R.layout.custom_dialog, null);
            dialogBuilder.setView(dialogView);

            final EditText userInput = (EditText) dialogView
                    .findViewById(R.id.editTextDialogUserInput);

            // set dialog message
            dialogBuilder
                    .setCancelable(false)
                    .setPositiveButton("OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,int id) {
                                    // get user input and set it to result
                                    // edit text
                                    mSocket.emit("username", userInput.getText());
                                }
                            })
                    .setNegativeButton("Cancel",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,int id) {
                                    dialog.cancel();
                                }
                            });

            // create alert dialog
            AlertDialog alertDialog = dialogBuilder.create();

            // show it
            alertDialog.show();
        }


    }

    public void animateMarker(final Marker marker, final LatLng toPosition,
                              final boolean hideMarker) {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        Projection proj = mMap.getProjection();
        Point startPoint = proj.toScreenLocation(marker.getPosition());
        final LatLng startLatLng = proj.fromScreenLocation(startPoint);
        final long duration = 500;
        final Interpolator interpolator = new LinearInterpolator();
        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed
                        / duration);
                double lng = t * toPosition.longitude + (1 - t)
                        * startLatLng.longitude;
                double lat = t * toPosition.latitude + (1 - t)
                        * startLatLng.latitude;
                marker.setPosition(new LatLng(lat, lng));
                if (t < 1.0) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16);
                } else {
                    if (hideMarker) {
                        marker.setVisible(false);
                    } else {
                        marker.setVisible(true);
                    }
                }
            }
        });
    }
}
