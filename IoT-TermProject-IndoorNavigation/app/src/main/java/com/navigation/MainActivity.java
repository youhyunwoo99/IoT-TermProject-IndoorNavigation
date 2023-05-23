package com.navigation;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

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

public class MainActivity extends AppCompatActivity implements MyLocationDialogFragment.MyLocationDialogListener {
    private Spinner spinner;
    private Button btn;
    private Button btn2;

    private String spinnerInput;
    private static String serverAddress = "http://172.16.235.36:8006";
    private String URL;
    private WifiManager wifiManager;

    String scanLog;

    String start;



    private int mode = 0;

    private boolean scanFlag = false;

    JSONObject one_wifi_json = new JSONObject();
    JSONObject result_json = new JSONObject();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        spinner = findViewById(R.id.spinner);
        setSpinner();
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            Log.d("Permission", "권한 없음");
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {
                Log.d("Permission", "Permission is not granted, but can be requested");
            } else {
                Log.d("Permission", "권한 요청");
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_WIFI_STATE,
                                Manifest.permission.CHANGE_WIFI_STATE},
                        1000);
            }
        }
        btn = findViewById(R.id.button);
        btn.setOnClickListener(v -> {
            URL = serverAddress +"/findPosition";
            wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            Log.d("our wifi", "d");
            scanWiFiInfo();
            MyLocationDialogFragment dialog = new MyLocationDialogFragment(start, MainActivity.this);
            dialog.show(getSupportFragmentManager(), "MyLocationDialogFragment");

        });
    }
    private void setSpinner() {
        // Spinner Drop down elements
        List<String> locations = new ArrayList<String>();
        locations.add("413호");
        locations.add("431호");
        locations.add("428호");
        locations.add("4층 아르테크네");
        locations.add("435호");
        locations.add("414호");
        locations.add("420호");
        locations.add("504호");
        locations.add("531호");
        locations.add("520호");

        // Creating adapter for spinner
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, locations);

        // Drop down layout style - list view with radio button
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // attaching data adapter to spinner
        spinner.setAdapter(dataAdapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            }
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
    }
    @Override
    public void onDialogResult(boolean result) {
        if (result) {
            // YES
            spinnerInput = spinner.getSelectedItem().toString();
            URL = serverAddress +"/dijkstra";
            getDijkstraPath();
        } else {
            // NO
        }
    }
    private void scanWiFiInfo() {
        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        wifiManager.startScan();
        Log.d("our wifi", "dd");

    }

    private void getDijkstraPath() {
        // 서버 통신을 별도의 쓰레드에서 처리
        new Thread(() -> {

            // 서버에 보낼 JSON 설정 부분
//            JSONArray json_array = new JSONArray();
            try {
                result_json.put("start", start);
                result_json.put("end", spinner.getSelectedItem());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            Log.d("getDijkstraPath","Dijkstra url :" + URL);

            // 서버와 통신하는 부분
            try {
                RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
                String mRequestBody = result_json.toString(); // json 을 통신으로 보내기위해 문자열로 변환하는 부분

                Log.d("getDijkstraPath", "Dijkstra url, mRequestBody :"+URL + mRequestBody);
                StringRequest stringRequest = new StringRequest(Request.Method.POST, URL, response -> {
                    //String position = response.split("\"")[3];
                    Log.d("getDijkstraPath", "Dijkstra response : "+response);
                    //String position = response.split("\"")[3];
                    startActivity(new Intent(this, NavigationActivity.class)
                            .putExtra("dest", spinnerInput)
                            .putExtra("info", response)
                            .putExtra("start", start));
                    Toast.makeText(this, "Selected: " + spinnerInput, Toast.LENGTH_SHORT).show();

                }, error -> {
                    Log.d("getDijkstraPath", "Dijkstra url, mRequestBody :"+error.toString());

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

    BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                return;
            }
            Log.d("BroadcastReceiver", "Find url: "+URL);

            List<ScanResult> scanResultList = wifiManager.getScanResults();
            unregisterReceiver(this);

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

                        start = position;
                        if(position.length() == 3) {
                            start += "호";
                        }
                        btn2.setText(start);
                        Log.d("BroadCastReceiver", "Find start location : " + start);

                        MyLocationDialogFragment dialog = new MyLocationDialogFragment(start, MainActivity.this);
                        dialog.show(getSupportFragmentManager(), "MyLocationDialogFragment");

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