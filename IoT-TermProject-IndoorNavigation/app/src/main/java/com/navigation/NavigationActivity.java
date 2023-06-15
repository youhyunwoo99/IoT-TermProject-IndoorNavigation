package com.navigation;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class NavigationActivity extends AppCompatActivity implements SensorEventListener {
    private Boolean arriveFlag = false;
    private String dest;
    private TextView dest_tv;
    private TextView nextPath_tv;
    private TextView remainDistance_tv;
    private TextView myLocation_tv;
    private TextView totalRemainDistance_tv;
    private TextView title;
    private String serverAddress = "http://172.16.234.164:8006";
    private String URL = "";
    private String myLocation = "";
    private int currentIndex = -1;
    private WifiManager wifiManager;
    private String scanLog;
    private boolean scanFlag = false;
    private static String remainTitle = "다음 경로까지 : ";
    private static String totalRemainTitle = "총 남은 거리 : ";
    private static String nextPathTitle = "다음 경로 :";
    private List<Double> distanceList = new ArrayList<>();
    private List<String> pathList = new ArrayList<>();
    private List<String> directionList = new ArrayList<>();
    JSONObject one_wifi_json = new JSONObject();
    JSONObject result_json = new JSONObject();
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private ImageView direction;
    private ImageView myDirection;
    private String currentDirectionAngle = "0";
    private float[] gravity = new float[3];
    private float[] geomagnetic = new float[3];
    private float azimuth = 0f;
    private final float alpha = 0.3f;
    private float[] accelData = new float[3];
    private float[] magnetData = new float[3];
    private String cameraId;
    private TextureView textureView;
    private CameraDevice cameraDevice;
    private Size imageDimension;
    private CaptureRequest.Builder captureRequestBuilder;
    private CameraCaptureSession cameraCaptureSessions;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);
        dest_tv = findViewById(R.id.destination_desc);
        nextPath_tv = findViewById(R.id.next_path);
        remainDistance_tv = findViewById(R.id.remain_distance);
        totalRemainDistance_tv = findViewById(R.id.total_remain_distance);
        myLocation_tv = findViewById(R.id.current_location_desc);
        title = findViewById(R.id.project_title);

        direction = findViewById(R.id.direction);
        myDirection = findViewById(R.id.my_direction);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);


        dest = getIntent().getStringExtra("dest");
        dest_tv.setText(dest);
        myLocation = getIntent().getStringExtra("start");
        myLocation_tv.setText(myLocation);


        String infoString = getIntent().getStringExtra("info");
        setStringToJson(infoString);


        URL = serverAddress + "/findPosition";
        Log.d("test12", URL);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        scanWiFiInfo();

        textureView = findViewById(R.id.textureView);
    }

    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            // TextureView가 사용 가능하면 카메라를 엽니다.
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {

        }
    };

    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
                    return;
                }
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            // 카메라가 열렸을 때 호출
            cameraDevice = camera;
            createCameraPreview();
        }
        @Override
        public void onDisconnected(CameraDevice camera) {
            cameraDevice.close();
        }
        @Override
        public void onError(CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    private void createCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);

            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    if (cameraDevice == null) {
                        return;
                    }
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

                }
                // ...
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void updatePreview() {
        if(cameraDevice == null) {
            Toast.makeText(this, "Camera error", Toast.LENGTH_SHORT).show();
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private void setStringToJson(String str) {
        try {
            // Convert the string to a JSONObject
            JSONObject jsonObject = new JSONObject(str);

            // Get the arrays
            JSONArray distanceArray = jsonObject.getJSONArray("distance");
            JSONArray pathArray = jsonObject.getJSONArray("path");
            JSONArray directionArray = jsonObject.getJSONArray("direction");

            // Convert them to Java arrays or arraylists
            for (int i = 0; i < distanceArray.length(); i++) {
                distanceList.add(distanceArray.getDouble(i));
            }
            for (int i = 0; i < pathArray.length(); i++) {
                pathList.add(pathArray.getString(i));
            }
            for (int i = 0; i < directionArray.length(); i++) {
                directionList.add(directionArray.getString(i));
            }
            Log.d("stringToJson", distanceList.toString());
            Log.d("stringToJson", pathList.toString());
            Log.d("stringToJson", directionList.toString());
            // Now distanceList, pathList, and directionList hold the parsed data


        } catch (JSONException e) {
            // This is the line where if your jsonString is not a valid JSON format, an error will be thrown
            e.printStackTrace();
        }
    }

    private void scanWiFiInfo() {
        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        wifiManager.startScan();
    }

    @Override
    protected void onResume() {
        super.onResume();
        textureView.setSurfaceTextureListener(textureListener);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this, accelerometer);
        sensorManager.unregisterListener(this, magnetometer);
        unregisterReceiver(wifiReceiver);
    }
    private float[] lowPassFilter( float input[], float output[] ) {
        if ( output == null ) return input;

        for ( int i=0; i<input.length; i++ ) {
            output[i] = output[i] + alpha * (input[i] - output[i]);
        }
        return output;
    }
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            accelData = lowPassFilter(event.values.clone(), accelData);
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            magnetData = lowPassFilter(event.values.clone(), magnetData);
        }

        final float[] rotationMatrix = new float[9];
        if (SensorManager.getRotationMatrix(rotationMatrix, null, accelData, magnetData)) {
            final float[] orientation = new float[3];
            SensorManager.getOrientation(rotationMatrix, orientation);
            azimuth = (float) Math.toDegrees(orientation[0]); // 방위각
            azimuth = (azimuth + 360) % 360;
        }

        Log.d("currentDirectionAngle",currentDirectionAngle);
        if(currentDirectionAngle.contains("방위")) {
            Integer currentAngle = Integer.valueOf(currentDirectionAngle.split("방위")[1]);

            Float cal = (azimuth - currentAngle + 360) % 360;
            Log.d("asd", "azi"+azimuth);
            Log.d("asd", "cal"+cal);
            myDirection.setRotation(-cal);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // you can choose to handle accuracy changes here
    }
    private String getTotalRemainDistance() {
        Double total = Double.valueOf(0);
        for(int i = currentIndex ; i < distanceList.toArray().length; i++){
            total += distanceList.get(i);
        }
        return Double.toString(Math.round(total));
    }

    BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ActivityCompat.checkSelfPermission(NavigationActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                return;
            }
            Log.d("BroadcastReceiver", "Find url: "+URL);

            List<ScanResult> scanResultList = wifiManager.getScanResults();

            scanResultList.sort((s1, s2) -> s2.level - s1.level);

            scanLog = "";
            for (ScanResult scanResult : scanResultList) {
                scanLog += "BSSID: " + scanResult.BSSID + "  level: " + scanResult.level + "\n";
            }
            scanFlag = true;

            // 서버 통신을 별도의 쓰레드에서 처리
            new Thread(() -> {
                Log.d("BroadcastReceiverThread", "Find url: "+URL);

                // 서버에 보낼 JSON 설정 부분
                JSONArray json_array = new JSONArray();
                for (ScanResult scanResult : scanResultList) {
                    one_wifi_json = new JSONObject();
                    String bssid = scanResult.BSSID;
                    int rssi = scanResult.level;

                    try {
                        one_wifi_json.put("bssid", bssid);
                        one_wifi_json.put("rssi", rssi);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    json_array.put(one_wifi_json);
                }
                try {
                    result_json.put("position", "");
                    result_json.put("wifi_data", json_array);
                    Log.d("BroadCastReceiver", "Find json array :"+json_array);
                    result_json.put("password", "asdfgh");
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                // 서버와 통신하는 부분
                try {
                    RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
                    String mRequestBody = result_json.toString(); // json 을 통신으로 보내기위해 문자열로 변환하는 부분

                    Log.d("BroadCastReceiver", "Find url, mRequestBody : " +URL + mRequestBody);
                    StringRequest stringRequest = new StringRequest(Request.Method.POST, URL, response -> {
                        String position = response.split("\"")[3];
                        if(position.length() == 3) {
                            position += "호";
                        }
                        myLocation_tv.setText(position);
                        Log.d("NavigationActivityDijkstra", String.valueOf(pathList));
                        Log.d("BroadCastReceiver", "Find start location : " + position);
                        Log.d("positionAndDest", position + dest);
                        Log.d("positionAndDest", String.valueOf(position.equals(dest)));
                        if(position.equals(dest) && !arriveFlag) {
                            arriveFlag = true;
                            Log.d("arrive", position);
                            totalRemainDistance_tv.setText("목적지에 도착했습니다!");
                            remainDistance_tv.setText("목적지에 도착했습니다!");
                            myLocation_tv.setText(position);
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    finish();
                                    Toast.makeText(context, "목적지에 도착했습니다!", Toast.LENGTH_LONG).show();
                                }
                            }, 3000); // 1000 milliseconds (1 second) delay before execution
                        } else {
                            Log.d("myLocation", position);
                            if (pathList.contains(position)) {
                                // 현재 위치 인덱스
                                currentIndex = pathList.indexOf(position);
                                nextPath_tv.setText(nextPathTitle + " "+ pathList.get(currentIndex+1));
                                totalRemainDistance_tv.setText(totalRemainTitle + " "+ getTotalRemainDistance() + "m");
                                remainDistance_tv.setText(remainTitle + distanceList.get(currentIndex) + "m");
                                myLocation_tv.setText(position);


                                Log.d("directionList.get(currentIndex)",directionList+"dfdfdfd");

                                switch (directionList.get(currentIndex)) {
                                    case "0" : direction.setImageResource(R.drawable.arrow_up_bold);break;
                                    case "1" : direction.setImageResource(R.drawable.arrow_right_top_bold);break;
                                    case "-1" : direction.setImageResource(R.drawable.arrow_left_top_bold);break;
                                    default: {
                                        direction.setImageResource(R.drawable.arrow_up_bold);
                                        currentDirectionAngle = directionList.get(currentIndex);
                                    }
                                }
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        scanWiFiInfo();
                                    }
                                }, 300); // 1000 milliseconds (1 second) delay before execution

                            } else {
                                // 다시 find
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        scanWiFiInfo();
                                    }
                                }, 300); // 1000 milliseconds (1 second) delay before execution
                            }
                        }
                    }, error -> {
                        Log.d("BroadCastReceiver", "Find error : "+error.toString());

                    }) {
                        @Override
                        public String getBodyContentType() {
                            return "application/json; charset=utf-8";
                        }

                        @Override
                        public byte[] getBody() { // 요청 보낼 데이터를 처리하는 부분
                            return mRequestBody.getBytes(StandardCharsets.UTF_8);
                        }

                        @Override
                        protected Response<String> parseNetworkResponse(NetworkResponse response) { // onResponse 에 넘겨줄 응답을 처리하는 부분
                            String responseString = "";
                            if (response != null) {
                                responseString = new String(response.data, StandardCharsets.UTF_8); // 응답 데이터를 변환해주는 부분
                            }
                            return Response.success(responseString, HttpHeaderParser.parseCacheHeaders(response));
                        }
                    };
                    requestQueue.add(stringRequest);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    };



}