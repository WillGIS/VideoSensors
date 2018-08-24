package com.example.juju.e_labvideoapp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import android.text.InputType;
import android.view.inputmethod.EditorInfo;
import android.view.*; //?
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationListener;

import android.location.GpsStatus.Listener;
import android.os.Bundle;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity implements SensorEventListener {
    private Camera mCamera;
    private CameraPreview mPreview;
    private MediaRecorder mediaRecorder;
    private ImageButton capture, vid;
    private Context myContext;
    private FrameLayout cameraPreview;
    private Chronometer chrono;
    private TextView tv;
    private TextView txt;

    int quality = 0;
    int rate = 100;
    String timeStampFile;
    int clickFlag = 0;
    Timer timer;
    int VideoFrameRate = 24;

    LocationListener locationListener;
    LocationManager LM;

    private SensorManager sm;
    //需要两个Sensor
    private Sensor aSensor;
    private Sensor mSensor;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        myContext = this;
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        //accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        //head = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        //gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        rotv = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        cameraPreview = (FrameLayout) findViewById(R.id.camera_preview);

        mPreview = new CameraPreview(myContext, mCamera);
        cameraPreview.addView(mPreview);

        capture = (ImageButton) findViewById(R.id.button_capture);
        capture.setOnClickListener(captureListener);

        chrono = (Chronometer) findViewById(R.id.chronometer);
        txt = (TextView) findViewById(R.id.txt1);
        txt.setTextColor(Color.BLUE);

        vid = (ImageButton) findViewById(R.id.imageButton);
        vid.setVisibility(View.GONE);

        /*
        tv = (TextView) findViewById(R.id.textViewHeading);
        String setTextText = "Heading: " + heading + " Speed: " + speed;
        tv.setText(setTextText);
        */


        sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        aSensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensor = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        sm.registerListener(myListener, aSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sm.registerListener(myListener, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
        //更新显示数据的方法
        calculateOrientation();

    }

    @Override
    protected void onPause() {
        super.onPause();
        // when on Pause, release camera in order to be used from other
        // applications
        releaseCamera();
        sensorManager.unregisterListener(this);
        sensorManager.unregisterListener(myListener);
    }


    final SensorEventListener myListener = new SensorEventListener() {
        public void onSensorChanged(SensorEvent sensorEvent) {

            if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
                magneticFieldValues = sensorEvent.values;
            if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
                accelerometerValues = sensorEvent.values;
            calculateOrientation();
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };


    private int findBackFacingCamera() {
        int cameraId = -1;
        // Search for the back facing camera
        // get the number of cameras
        int numberOfCameras = Camera.getNumberOfCameras();
        // for every camera check
        for (int i = 0; i < numberOfCameras; i++) {
            CameraInfo info = new CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
                cameraId = i;
                break;
            }
        }
        return cameraId;
    }

    public void onResume() {
        super.onResume();
        if (!checkCameraHardware(myContext)) {
            Toast toast = Toast.makeText(myContext, "Phone doesn't have a camera!", Toast.LENGTH_LONG);
            toast.show();
            finish();
        }
        if (mCamera == null) {
            mCamera = Camera.open(findBackFacingCamera());
            mPreview.refreshCamera(mCamera);
        }
        //sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        //sensorManager.registerListener(this, head, SensorManager.SENSOR_DELAY_GAME);
        //sensorManager.registerListener(this, gyro, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, rotv, SensorManager.SENSOR_DELAY_NORMAL);


        locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider.

                latitude = location.getLatitude();
                longitude = location.getLongitude();

                if (location.hasSpeed()) {
                    speed = location.getSpeed();
                }
                location.distanceBetween(latitude_original, longitude_original, latitude, longitude, dist);
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
            }
        };

        // Acquire a reference to the system Location Manager
        LM = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Register the listener with the Location Manager to receive location updates
        LM.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
    }


    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }


    private static final String TAG = "sensor>>>>>>>>>>";
    float[] accelerometerValues = new float[3];
    float[] magneticFieldValues = new float[3];

    private void calculateOrientation() {
        float[] values = new float[3];
        float[] R = new float[9];
        SensorManager.getRotationMatrix(R, null, accelerometerValues, magneticFieldValues);
        SensorManager.getOrientation(R, values);

        // 要经过一次数据格式的转换，转换为度
        values[0] = (float) Math.toDegrees(values[0]);
        values[1] = (float) Math.toDegrees(values[1]);
        values[2] = (float) Math.toDegrees(values[2]);
        Log.i(TAG, values[0] + "");
        Log.i("TAG1>>>", values[1] + "");
        Log.i("TAG2>>>", values[2] + "");
        //values[1] = (float) Math.toDegrees(values[1]);
        //values[2] = (float) Math.toDegrees(values[2]);

        String dir = "";
        if (values[0] >= -5 && values[0] < 5) {
            Log.i(TAG, "正北");
            dir = "正北";
        } else if (values[0] >= 5 && values[0] < 85) {
            Log.i(TAG, "东北");
            dir = "东北";
        } else if (values[0] >= 85 && values[0] <= 95) {
            Log.i(TAG, "正东");
            dir = "正东";
        } else if (values[0] >= 95 && values[0] < 175) {
            Log.i(TAG, "东南");
            dir = "东南";
        } else if ((values[0] >= 175 && values[0] <= 180) || (values[0]) >= -180 && values[0] < -175) {
            Log.i(TAG, "正南");
            dir = "正南";
        } else if (values[0] >= -175 && values[0] < -95) {
            Log.i(TAG, "西南");
            dir = "西南";
        } else if (values[0] >= -95 && values[0] < -85) {
            Log.i(TAG, "正西");
            dir = "正西";
        } else if (values[0] >= -85 && values[0] < -5) {
            Log.i(TAG, "西北");
            dir = "西北";
        }

        ((TextView) findViewById(com.example.juju.e_labvideoapp.R.id.textView1)).setText("横滚值：" + String.valueOf(values[1]));
        ((TextView) findViewById(com.example.juju.e_labvideoapp.R.id.textView2)).setText("俯仰角:" + String.valueOf(values[2]) );

        ((TextView) findViewById(com.example.juju.e_labvideoapp.R.id.textView3)).setText("方向角:" + String.valueOf(values[0]));
        ((TextView) findViewById(com.example.juju.e_labvideoapp.R.id.textView4)).setText("朝向:" + dir);
    }

    boolean recording = false;
    OnClickListener captureListener = new OnClickListener() {
        @Override
        public void onClick(View v) {

            if (recording) {
                // stop recording and release camera
                mCamera.unlock();
                mediaRecorder.stop(); // stop the recording
                releaseMediaRecorder(); // release the MediaRecorder object
                Toast.makeText(MainActivity.this, "Video captured!", Toast.LENGTH_LONG).show();
                recording = false;
                //d.exportData();
                chrono.stop();
                chrono.setBase(SystemClock.elapsedRealtime());

                chrono.start();
                chrono.stop();
                txt.setTextColor(-16711936);
                //chrono.setBackgroundColor(0);
                enddata();
/*
                if(clickFlag == 1){
                    clickFlag = 0;
                    capture.performClick();
                }
*/
            } else {
                timeStampFile = String.valueOf((new Date()).getTime());
                File wallpaperDirectory = new File(Environment.getExternalStorageDirectory().getPath()+"/elab/");
                wallpaperDirectory.mkdirs();

                File wallpaperDirectory1 = new File(Environment.getExternalStorageDirectory().getPath()+"/elab/"+timeStampFile);
                wallpaperDirectory1.mkdirs();
                if (!prepareMediaRecorder()) {
                    Toast.makeText(MainActivity.this, "Fail in prepareMediaRecorder()!\n - Ended -", Toast.LENGTH_LONG).show();
                    finish();
                }

                // work on UiThread for better performance
                runOnUiThread(new Runnable() {
                    public void run() {
                        try {
                            mediaRecorder.start();
                        } catch (final Exception ex) {
                        }
                    }
                });
                Toast.makeText(MainActivity.this, "Recording...", Toast.LENGTH_LONG).show();

                if (mCamera == null) {
                    mCamera = Camera.open(findBackFacingCamera());
                }

                mCamera.lock();
                Camera.Parameters params = mCamera.getParameters();
                params.setPreviewFpsRange( 30000, 30000 ); // 30 fps
                if ( params.isAutoExposureLockSupported() )
                    params.setAutoExposureLock( true );

                params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                mCamera.setParameters(params);
                //d.beginData();
                storeData();
                chrono.setBase(SystemClock.elapsedRealtime());

                chrono.start();
                //chrono.setBackgroundColor(-65536);
                txt.setTextColor(-65536);
                recording = true;

            }
        }
    };

    private void releaseMediaRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder.reset(); // clear recorder configuration
            mediaRecorder.release(); // release the recorder object
            mediaRecorder = null;
            mCamera.lock(); // lock camera for later use
        }
    }

    private boolean prepareMediaRecorder() {

        mediaRecorder = new MediaRecorder();

        mCamera.unlock();
        mediaRecorder.setCamera(mCamera);

        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        if (quality == 0)
            mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_1080P));
        else if (quality == 1)
            mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_720P));
        else if (quality == 2)
            mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_480P));

        //String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        mediaRecorder.setOutputFile(Environment.getExternalStorageDirectory().getPath() + "/elab/" + timeStampFile + "/" + timeStampFile + ".mp4");
        mediaRecorder.setVideoFrameRate(VideoFrameRate);
        //mediaRecorder.setMaxDuration(5000);

        try {
            mediaRecorder.prepare();
        } catch (IllegalStateException e) {
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            releaseMediaRecorder();
            return false;
        }
        return true;

    }

    private void releaseCamera() {
        // stop and release camera
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    /* --------------------- Data Section ----------------------------*/

    Location location;
    LocationManager lm;
    double latitude = 0;
    double longitude = 0;

    double latitude_original = 0;
    double longitude_original = 0;
    //float distance = 0;
    float speed = 0;
    float dist[] = {0, 0, 0};
    PrintWriter writer = null;
    long timechecker = 5000;

    class SayHello extends TimerTask {
        public void run() {
            lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            //lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100, 0, locationListener );
            //longitude = location.getLongitude();
            //latitude = location.getLatitude();
            //if(location.hasSpeed()) {
            //  speed = location.getSpeed();
            //}
            //dist[0] = (float) 0.0;
            /*
            long elapsedMillis = SystemClock.elapsedRealtime() - chrono.getBase();
            if(elapsedMillis >= timechecker){
                clickFlag = 1;
                timechecker = timechecker + 5000;
                timer.cancel();
                timer.purge();
            }*/

            /*
            writer.println(longitude_original + "," + latitude_original + "," + speed + "," + dist[0] + "," + timeStamp + "," + linear_acc_x + "," + linear_acc_y + "," + linear_acc_z + "," +
                    heading + "," + gyro_x + "," + gyro_y + "," + gyro_z);
            */
            String timeStamp = String.valueOf((new Date()).getTime());
            writer.println(timeStamp + "," +
                    longitude_original + "," + latitude_original + "," +
                    rotv_x + "," + rotv_y + "," + rotv_z + "," + rotv_w + "," + rotv_accuracy);
        }
    }

    public void storeData() {

        String filePath = Environment.getExternalStorageDirectory().getPath() + "/elab/" + timeStampFile + "/" + timeStampFile + ".csv";
        try {
            writer = new PrintWriter(filePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        //writer.println("Longitude" + "," + "Latitude" + "," + "Speed" + "," + "Distance" + "," + "Time" + "," + "Acc X" + "," + "Acc Y" + "," + "Acc Z" + "," + "Heading"
        //        + "," + "gyro_x" + "," + "gyro_y" + "," + "gyro_z");
        writer.println("Timestamp" + "," +
                "Longitude" + "," + "Latitude" + "," +
                "RotationV X" + "," + "RotationV Y" + "," + "RotationV Z" + "," + "RotationV W" + "," + "RotationV Acc");
        LocationManager original = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location original_location = original.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (original.getLastKnownLocation(LocationManager.GPS_PROVIDER) != null) {
            latitude_original = original_location.getLatitude();
            longitude_original = original_location.getLongitude();
        }
        //String setTextText = "Heading: " + heading + " Speed: " + speed;
        //tv.setText(setTextText);
        timer = new Timer();
        timer.schedule(new SayHello(), 0, rate);
        /*if(clickFlag == 1) {
            capture.performClick();
        }
        */
    }

    public void enddata() {
        writer.close();
    }


    /* ---------------------- Sensor data ------------------- */

    private SensorManager sensorManager;

    //private Sensor accelerometer;
    //private Sensor head;
    //private Sensor gyro;
    private Sensor rotv;

    /*
    float linear_acc_x = 0;
    float linear_acc_y = 0;
    float linear_acc_z = 0;

    float heading = 0;

    float gyro_x = 0;
    float gyro_y = 0;
    float gyro_z = 0;
    */

    float rotv_x = 0;
    float rotv_y = 0;
    float rotv_z = 0;
    float rotv_w = 0;
    float rotv_accuracy = 0;

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            rotv_x = event.values[0];
            rotv_y = event.values[1];
            rotv_z = event.values[2];
            rotv_w = event.values[3];
            rotv_accuracy = event.values[4];
        }
        /*
        if(event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            linear_acc_x = event.values[0];
            linear_acc_y = event.values[1];
            linear_acc_z = event.values[2];
        }
        else if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) {
            heading = Math.round(event.values[0]);
            if(heading >= 270){
                heading = heading + 90;
                heading = heading - 360;
            }
            else{
                heading = heading + 90;
            }
        }
        else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE){
            gyro_x = event.values[0];
            gyro_y = event.values[1];
            gyro_z = event.values[2];
        }
        String setTextText = "Heading: " + heading + " Speed: " + speed;
        tv.setText(setTextText);
        */
    }

    String[] options = {"1080p", "720p", "480p"};
    String[] options1 = {"15 Hz", "10 Hz"};
    String[] options2 = {"10 fps", "20 fps", "30 fps"};


    public void addQuality(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String setting = new String();
        if (quality == 0) {
            setting = "1080p";
        } else if (quality == 1) {
            setting = "720p";
        } else if (quality == 2) {
            setting = "480p";
        }
        builder.setTitle("Pick Quality, Current setting: " + setting)
                .setItems(options, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // The 'which' argument contains the index position
                        // of the selected item
                        if (which == 0) {
                            quality = 0;
                        } else if (which == 1) {
                            quality = 1;
                        } else if (which == 2) {
                            quality = 2;
                        }
                    }
                });
        builder.show();
    }

    public void addRate(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String setting = new String();
        if (rate == 100) {
            setting = "10 Hz";
        } else if (rate == 67) {
            setting = "15 Hz";
        }
        builder.setTitle("Pick Data Save Rate, Current setting: " + setting)
                .setItems(options1, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // The 'which' argument contains the index position
                        // of the selected item
                        if (which == 0) {
                            rate = 67;
                        } else if (which == 1) {
                            rate = 100;
                        }
                    }
                });
        builder.show();
    }

    public void addFrameRate(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String setting = new String();
        if (VideoFrameRate == 10) {
            setting = "10 fps";
        } else if (VideoFrameRate == 20) {
            setting = "20 fps";
        } else if (VideoFrameRate == 30) {
            setting = "30 fps";
        }
        builder.setTitle("Pick Video fps, Current setting: " + setting)
                .setItems(options2, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // The 'which' argument contains the index position
                        // of the selected item
                        if (which == 0) {
                            VideoFrameRate = 10;
                        } else if (which == 1) {
                            VideoFrameRate = 20;
                        } else if (which == 2) {
                            VideoFrameRate = 30;
                        }
                    }
                });
        builder.show();
    }
}