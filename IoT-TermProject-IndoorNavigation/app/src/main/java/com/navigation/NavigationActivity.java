package com.navigation;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.navigation.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NavigationActivity extends AppCompatActivity implements AccelerometerDistanceCalculator.OnDistanceChangedListener {
    private String dest;
    private TextView dest_tv;
    private TextView remainDistance;
    private String serverAddress = "";
    private String URL = "";
    private WifiManager wifiManager;
    private String scanLog;
    private boolean scanFlag = false;
    JSONObject one_wifi_json = new JSONObject();
    JSONObject result_json = new JSONObject();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);
        dest_tv = findViewById(R.id.destination_desc);
        dest = getIntent().getStringExtra("dest");
        dest_tv.setText(dest);
        String infoString = getIntent().getStringExtra("info");
        setStringToJson(infoString);

        remainDistance = findViewById(R.id.remain_distance);
        URL = serverAddress + "/findPosition";
        Log.d("test12", URL);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
       // scanWiFiInfo();

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
            List<Double> distanceList = new ArrayList<>();
            for (int i = 0; i < distanceArray.length(); i++) {
                distanceList.add(distanceArray.getDouble(i));
            }

            List<String> pathList = new ArrayList<>();
            for (int i = 0; i < pathArray.length(); i++) {
                pathList.add(pathArray.getString(i));
            }

            List<Integer> directionList = new ArrayList<>();
            for (int i = 0; i < directionArray.length(); i++) {
                directionList.add(directionArray.getInt(i));
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

    @Override
    public void onDistanceChanged(double distance) {
        Log.d("accel", String.valueOf(distance));
        remainDistance.setText(distance + "m");
    }

    private void scanWiFiInfo() {
        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        wifiManager.startScan();
    }

    BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ActivityCompat.checkSelfPermission(NavigationActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            List<ScanResult> scanResultList = wifiManager.getScanResults();
            unregisterReceiver(this);

            scanResultList.sort((s1, s2) -> s2.level - s1.level);


            scanLog = "";
            for (ScanResult scanResult : scanResultList) {
                scanLog += "BSSID: " + scanResult.BSSID + "  level: " + scanResult.level + "\n";

                //scanLog += scanResult.toString()+ "\n";
            }

            scanFlag = true;

            // 서버 통신을 별도의 쓰레드에서 처리
            new Thread(() -> {

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
                    result_json.put("wifi_data", json_array);
                    result_json.put("password", "asdfgh");
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                // 서버와 통신하는 부분
                try {
                    RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
                    String mRequestBody = result_json.toString(); // json 을 통신으로 보내기위해 문자열로 변환하는 부분

                    Log.d("제발", URL + mRequestBody);
                    StringRequest stringRequest = new StringRequest(Request.Method.POST, URL, response -> {
                        String position = response.split("\"")[3];

                    }, error -> {
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