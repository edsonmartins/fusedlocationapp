package br.com.demo.fusedlocation;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.text.DateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
    private static final String TAG = "MainActivity";
    private TextView mLatitudeTextView;
    private TextView mLongitudeTextView;
    private TextView mActivity_textview;
    private GoogleApiClient mGoogleApiClient;
    private Location mLocation;
    private LocationRequest mLocationRequest;
    private String mLastUpdateTime;
    private Timer timer;
    private MyTimerTask myTimerTask;
    private static final int REQUEST_LOCATION = 9867;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mLatitudeTextView = (TextView) findViewById((R.id.latitude_textview));
        mLongitudeTextView = (TextView) findViewById((R.id.longitude_textview));
        mActivity_textview = (TextView) findViewById((R.id.activity_textview));


        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .addApi(ActivityRecognition.API)
                .build();

    }

    @Override
    public void onConnected(Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION);
        } else {
            getLocationAndActivities();
        }
    }

    private void getLocationAndActivities() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLocationRequest = LocationRequest.create();
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            mLocationRequest.setInterval(5000);
            mLocationRequest.setFastestInterval(3000);
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

            Intent intent = new Intent(this, ActivityRecognizedService.class);
            PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mGoogleApiClient, 3000, pendingIntent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_LOCATION) {
            if (grantResults.length == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocationAndActivities();
            } else {
                Toast.makeText(getApplicationContext(), "Não foi possível obter a localização", Toast.LENGTH_LONG).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Connection Suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        mLatitudeTextView.setText(String.valueOf(location.getLatitude()));
        mLongitudeTextView.setText(String.valueOf(location.getLongitude()));
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, "Connection failed. Error: " + connectionResult.getErrorCode());
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
        if (timer != null) {
            timer.cancel();
        }

        timer = new Timer();
        myTimerTask = new MyTimerTask();
        timer.schedule(myTimerTask, 1000, 500);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    class MyTimerTask extends TimerTask {

        @Override
        public void run() {

            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    mActivity_textview.setText("");
                    if (ActivityRecognizedService.probableActivities != null) {
                        for (DetectedActivity activity : ActivityRecognizedService.probableActivities) {
                            switch (activity.getType()) {
                                case DetectedActivity.IN_VEHICLE: {
                                    mActivity_textview.setText(mActivity_textview.getText()+"\nIn Vehicle: " + activity.getConfidence());
                                    break;
                                }
                                case DetectedActivity.ON_BICYCLE: {
                                    mActivity_textview.setText(mActivity_textview.getText()+"\nOn Bicycle: " + activity.getConfidence());
                                    break;
                                }
                                case DetectedActivity.ON_FOOT: {
                                    mActivity_textview.setText(mActivity_textview.getText()+"\nOn Foot: " + activity.getConfidence());
                                    break;
                                }
                                case DetectedActivity.RUNNING: {
                                    mActivity_textview.setText(mActivity_textview.getText()+"\nRunning: " + activity.getConfidence());
                                    break;
                                }
                                case DetectedActivity.STILL: {
                                    mActivity_textview.setText(mActivity_textview.getText()+"\nStill: " + activity.getConfidence());
                                    break;
                                }
                                case DetectedActivity.TILTING: {
                                    mActivity_textview.setText(mActivity_textview.getText()+"\nTilting: " + activity.getConfidence());
                                    break;
                                }
                                case DetectedActivity.WALKING: {
                                    mActivity_textview.setText(mActivity_textview.getText()+"\nWalking: " + activity.getConfidence());
//                                if( activity.getConfidence() >= 75 ) {
//                                    NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
//                                    builder.setContentText( "Are you walking?" );
//                                    builder.setSmallIcon( R.mipmap.ic_launcher );
//                                    builder.setContentTitle( getString( R.string.app_name ) );
//                                    NotificationManagerCompat.from(this).notify(0, builder.build());
//                                }
                                    break;
                                }
                                case DetectedActivity.UNKNOWN: {
                                    mActivity_textview.setText(mActivity_textview.getText()+"\nUnknown: " + activity.getConfidence());
                                    break;
                                }
                            }
                        }
                    }
                }
            });

        }

    }


}
