package com.example.takepack;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.api.Response;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, ActivityCompat.OnRequestPermissionsResultCallback {

    private final long FINISH_INTERVAL_TIME = 2000;
    private long backPressedTime = 0;


    LoginActivity lg = new LoginActivity();
    String m_ip = lg.mip;
    private GpsTracker gpsTracker;
    //map 부분
    private FragmentManager fragmentManager;
    private MapFragment mapFragment;
    private String title = "";
    private GoogleMap mMap;


    private MarkerOptions mop = new MarkerOptions();

    String add_name;
    int insert_count;
    String add_item_list;
    double add_lat;
    double add_lng;


    String user_id;

    public int getitem_count;
    public String[] location_name;
    public String[] item_name;
    public double[] marker_lat;
    public double[] marker_lng;
    public String[] place_state;
    String item_temp = "";
    LatLng c_location;

    String[] result_items;

    LinkedHashMap hash;
    LinkedHashMap hash2;
    ArrayList<Pair<Double, Double>> pairs;

    //list 부분
    public List<String> ListItems;
    CharSequence[] items;

    SharedPreferences pref;
    SharedPreferences.Editor editor;

    private LocationManager locationManager;
    private static final int REQUEST_CODE_LOCATION = 2;
    double current_lng, current_lat;
    // 쓰레드
    Thread t;
    Handler mHandler = null;

    //타이머

    static TimerTask tt2;
    static Timer timer2;
    private boolean stopped = false;

    public void stop() {
        stopped = true;
    }

    public void handler_start() {
        stopped = false;
    }

    private int second = 0;


    //
    int timerTime = 30; // 30 초를 디폴트로.
    // Timer 를 처리해주는 핸들러
    TimerHandler timer;
    boolean isRunning = true;
    int status = 0; // 0:정지, 1:시작, 2:일시정지

    //진동
    boolean vib = false;
    Vibrator v;
    MarkerOptions markerOptions = new MarkerOptions();
    String dummy;

    boolean location_in = false;
    String dlg_msg = "아래 소지품을 확인하세요";

    Marker marker;

    Intent foreground_intent;

    public void mapload() {
        new list_Get().execute(m_ip + "/list?id=" + user_id);
    }

    public void mainload() {
        new init_Marker_Get().execute(m_ip + "/marker?id=" + user_id);
    }

    public void update_marker_state(String place_name, String state) {
        new Update_Marker_Get().execute(m_ip + "/update_marker?id=" + user_id + "&place_name=" + place_name + "&state=" + state);
    }

    class TimerHandler extends Handler {

        @Override
        public void handleMessage(@NonNull Message msg) {
            //  Looper.prepare();
            super.handleMessage(msg);

            switch (msg.what) {
                case 0: // 시작 하기
                    if (timerTime == 0) {
                        removeMessages(0);
                        break;
                    }
                    mainload();

                    sendEmptyMessageDelayed(0, 10000); //10초 마다 반복

                    //Log.d("test", "msg.what:0 time = " + timerTime);
                    second += 10;
                    Log.i("Thread", "작동중 " + second + "초"); //배포시 삭제
                    current_lat = gpsTracker.getLatitude();
                    current_lng = gpsTracker.getLongitude();
                    dummy = dis(current_lat, current_lng);

                    //Log.i("dis전체 결과", dummy);
                 //   System.out.println(dummy);

                    if (dummy.startsWith("in")) { // 들어왔을때
                        dlg_msg = " 이라는 장소에 들어왔습니다. 아래 소지품을 확인하세요";
                        startVibrate(); //두번 울리는거 수정 필요

                    } else if (dummy.startsWith("out")) { // 나갔을때
                        dlg_msg = " 이라는 장소에서 나왔습니다. 아래 소지품을 확인하세요";
                        update_marker_state("", "");
                        startVibrate();
                    } else if (dummy.startsWith("null")) { // 아무것도 아닐때

                    }
                    break;

                case 1: //일시 정지
                    removeMessages(0); // 타이머 메시지 삭제
                    Log.d("test", "msg.what:1 time = " + timerTime);
                    break;

                case 2: // 정지 후 타이머 초기화
                    removeMessages(0); // 타이머 메시지 삭제
                    timerTime = 30; // 타이머 초기화
                    Log.d("test", "msg.what:1 time = " + timerTime);
                    break;


            }
            //  Looper.loop();
        }
    }

    public void startVibrate() {

        v.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE));
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        if (status == 1) { // 타이머 동작 중이라면, 일시 정지 시키기
            status = 0;
            // 1번 메시지를 던진다.
            timer.sendEmptyMessage(1);
        }
        mainload();
        builder.setTitle(dummy.substring(dummy.indexOf("$") + 1, dummy.indexOf("#")) + dlg_msg)
                .setMessage(dummy.substring(dummy.indexOf("#") + 1, dummy.length() - 1))
                .setPositiveButton("확인", new AlertDialog.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Log.i("알람", "종료");
                        v.cancel();
                        if (status == 0) // 정지 상태 라면, 재 시작.
                        {
                            status = 1;
                            timer.sendEmptyMessage(0);
                        }
                    }
                })
                .setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        pref = getSharedPreferences("pref", Activity.MODE_PRIVATE);
        String sid = pref.getString("id_save", "");
        user_id = sid;

        foreground_intent = new Intent(this, ForegroundService.class);

        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(foreground_intent);
        } else {
            startService(foreground_intent);
        }
        hash2 = new LinkedHashMap<String, String>();
        mainload();
        super.onCreate(savedInstanceState);

        //사용자의 현재 위치
        setContentView(R.layout.activity_main);
        mapload();
        gpsTracker = new GpsTracker(MainActivity.this);
        timer = new TimerHandler();
        if (status == 0) // 정지 상태 라면, 재 시작.
        {
            status = 1;
            timer.sendEmptyMessageDelayed(0, 10000);//10초 후 타이머 실행
        }
        fragmentManager = getFragmentManager();
        mapFragment = (MapFragment) fragmentManager.findFragmentById(R.id.googleMap);
        mapFragment.getMapAsync(this);
        pairs = new ArrayList<>();
        hash = new LinkedHashMap<String, String>();
        Button listmode = (Button) findViewById(R.id.listMode);
        ListItems = new ArrayList<>();
        listmode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ItemListActivity.class);
                intent.putExtra("user_id", user_id);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    public void onBackPressed() {
        long tempTime = System.currentTimeMillis();
        long intervalTime = tempTime - backPressedTime;

        if (0 <= intervalTime && FINISH_INTERVAL_TIME >= intervalTime) {
            //연속 두번 backbtn 눌렀을 때 (INTERVAL 2초)
            super.onBackPressed();
            mHandler.removeCallbacksAndMessages(null);
            this.stopService(foreground_intent);
            android.os.Process.killProcess(android.os.Process.myPid());
        } else {
            backPressedTime = tempTime;
        }
    }

    //마커와 내 위치들 간 거리 비교
    public String dis(double my_lat, double my_lng) {

        String result = "null";
        String in_location_name = "";
        String in_item_name = "";
        String[] location;
        location = new HashSet<String>(Arrays.asList(location_name)).toArray(new String[0]);
        if (getitem_count > 0) {
            String[] result_t = new String[getitem_count];
            for (int i = 0; i < location.length; i++) {
                if ((distance(my_lat, my_lng, marker_lat[i], marker_lng[i]) < 50.0) && (place_state[i].equals("null"))) {//마커 반경 10미터 내에 들어 왔을 때
                    Set<Map.Entry<String, String>> entries = hash.entrySet();
                    for (Map.Entry<String, String> entry : entries) {
                        if (location[i].equals(entry.getKey())) {

                            in_location_name = entry.getKey();
                            in_item_name = entry.getValue();
                            result = "in";
                            update_marker_state(in_location_name, result);
                        }
                    }
                   // System.out.println("장소  : " + in_location_name + " , 아이템 : " + in_item_name);
                    break;

                } else if ((distance(my_lat, my_lng, marker_lat[i], marker_lng[i]) > 50.0) && (place_state[i].equals("in")))//마커 내부에 있다가 반경 10미터 외로 떨어졌을 때
                {
                    result_t[i] = "out";
                    place_state[i] = "out";
                    Set<Map.Entry<String, String>> entries = hash.entrySet();
                    for (Map.Entry<String, String> entry : entries) {
                        if (location[i].equals(entry.getKey())) {
                            in_location_name = entry.getKey();
                            in_item_name = entry.getValue();
                            result = "out";
                            update_marker_state(in_location_name, result);

                        }
                    }
                    break;
                } else if (place_state[i].equals("null")) {
                    result_t[i] = "";
                    result = "null";
                }
            }

            return (result + "$" + in_location_name + "#" + in_item_name);
        } else
            return (result + "$" + in_location_name + "#" + in_item_name);
    }

    private double distance(double lat1, double lon1, double lat2, double lon2) {
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) +
                Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) *
                        Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        dist = dist * 1609.344; // m 단위로 변경
        return (dist);
    }

    private double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    private double rad2deg(double rad) {
        return (rad * 180.0 / Math.PI);
    }


    @Override
    public void onMapReady(final GoogleMap googleMap) {
        // mainload();
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mMap.setMyLocationEnabled(true);

        googleMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(final LatLng point) {
                //mapload();
//                timer = new Timer();
//                tt=timerTaskMaker();
//                timer.schedule(tt,3000,3000);
                c_location = new LatLng(point.latitude, point.longitude); //커스텀 위치
                add_lat = point.latitude;
                add_lng = point.longitude;
                final MarkerOptions mop = new MarkerOptions();
                items = ListItems.toArray(new String[ListItems.size()]);

                final List SelectedItems = new ArrayList();
                final EditText edittext = new EditText(MainActivity.this);

                edittext.setHint("장소이름 추가");
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                if (ListItems.size() == 0) {
                    builder.setTitle("장소추가(아이템을 추가하면 함께 등록할 수 있습니다.)");
                } else {
                    builder.setTitle("장소추가");
                }
                builder.setView(edittext);

                builder.setMultiChoiceItems(items, null,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which,
                                                boolean isChecked) {
                                if (isChecked) {
                                    //사용자가 체크한 경우 리스트에 추가
                                    SelectedItems.add(which);
                                } else if (SelectedItems.contains(which)) {
                                    //이미 리스트에 들어있던 아이템이면 제거
                                    SelectedItems.remove(Integer.valueOf(which));
                                }
                            }
                        });
                builder.setPositiveButton("Ok",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                String msg = "";
                                String temp = "";
                                for (int i = 0; i < SelectedItems.size(); i++) {
                                    int index = (int) SelectedItems.get(i);
                                    msg = msg + "\n" + (i + 1) + " : " + ListItems.get(index);
                                    temp += ListItems.get(index) + ",";
                                }
                                mop.title(edittext.getText().toString());
                                if (temp.length() > 0)
                                    temp = temp.substring(0, temp.length() - 1);
                                else {
                                    Toast.makeText(getApplicationContext(), "아이템을 선택해 주세요", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                mop.snippet(temp);
                                mop.position(c_location);
                                googleMap.addMarker(mop);
                                add_name = edittext.getText().toString();
                                insert_count = SelectedItems.size();
                                add_item_list = temp;//nodejs에서 split후 string[] 에 담아 insert사용
                                Toast.makeText(getApplicationContext(),
                                        "Total " + SelectedItems.size() + " Items Selected.\n" + msg, Toast.LENGTH_LONG)
                                        .show();
                                new add_Marker_Post().execute(m_ip + "/add_marker");
                                mainload();
                            }
                        });
                builder.setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                builder.show();
            }

        });
        for (int i = 0; i < getitem_count; i++) {
            if (hash.containsKey(location_name[i]))//장소이름이 중복될 경우
            {
                item_temp += item_name[i] + ",";
                hash.put(location_name[i], item_temp);
            } else//새로운 장소를 추가하는 경우
            {
                item_temp = "";
                item_temp += item_name[i] + ",";
                hash.put(location_name[i], item_temp);
                pairs.add(new Pair<>(marker_lat[i], marker_lng[i]));

            }
        }

        MarkerOptions m = new MarkerOptions();
        Set<Map.Entry<String, String>> entries = hash.entrySet();
        int x = 0;
        for (Map.Entry<String, String> entry : entries) {
         //   System.out.print("key: " + entry.getKey());
         //   System.out.println(", Value: " + entry.getValue());

            m.title(entry.getKey())
                    .snippet(entry.getValue().substring(0, entry.getValue().length() - 1))
                    .position(new LatLng(pairs.get(x).first, pairs.get(x).second));

            marker = googleMap.addMarker(m);
            x++;
        }
        double latitude = gpsTracker.getLatitude();
        double longitude = gpsTracker.getLongitude();
        current_lat = latitude;
        current_lng = longitude;


        LatLng location = new LatLng(current_lat, current_lng); //현재 내 위치
        markerOptions.title("현재 내 위치");
//        markerOptions.snippet("스니펫");
        markerOptions.position(location);
//        BitmapDrawable bitmapdraw=(BitmapDrawable)getResources().getDrawable(R.drawable.myposition);
//        Bitmap b=bitmapdraw.getBitmap();
//        Bitmap smallMarker = Bitmap.createScaledBitmap(b, 75, 75, false);
//        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(smallMarker));
//       // markerOptions.alpha(0.8f);
//        googleMap.addMarker(markerOptions);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 16));

    }

    public class list_Get extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... urls) {

            HttpURLConnection con = null;
            BufferedReader reader = null;
            try {
                URL url = new URL(urls[0]);
                con = (HttpURLConnection) url.openConnection();
                con.connect();
                InputStream stream = con.getInputStream();
                reader = new BufferedReader(new InputStreamReader(stream));
                StringBuffer buffer = new StringBuffer();
                String line = "";
                while ((line = reader.readLine()) != null) buffer.append(line);
                return buffer.toString();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                con.disconnect();
                try {
                    if (reader != null)
                        reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            try {
                JSONObject jsonObject = new JSONObject(s);
                //String code = jsonObject.getString("code");
                String r_item = jsonObject.getString("item");
                result_items = r_item.split("#");
                ListItems.clear();
                for (int a = 1; a < result_items.length; a++) {
                    if (!ListItems.contains(result_items[a]))
                        ListItems.add(result_items[a]);
                }
//                Adapter.notifyDataSetChanged();

//                if (code.equals("200")) {
//                    Toast.makeText(getApplicationContext(), r_item, Toast.LENGTH_SHORT).show();
//                } else {
//                    Toast.makeText(getApplicationContext(), "실패", Toast.LENGTH_SHORT).show();
//                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
    }

    //서버 통신 부분
    public class init_Marker_Get extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... urls) {
            try {
                HttpURLConnection con = null;
                BufferedReader reader = null;
                try {
                    URL url = new URL(urls[0]);//url을 가져온다.
                    con = (HttpURLConnection) url.openConnection();
                    con.connect();//연결 수행
                    //입력 스트림 생성
                    InputStream stream = con.getInputStream();

                    //속도를 향상시키고 부하를 줄이기 위한 버퍼를 선언한다.
                    reader = new BufferedReader(new InputStreamReader(stream));

                    //실제 데이터를 받는곳
                    StringBuffer buffer = new StringBuffer();

                    //line별 스트링을 받기 위한 temp 변수
                    String line = "";

                    //아래라인은 실제 reader에서 데이터를 가져오는 부분이다. 즉 node.js서버로부터 데이터를 가져온다.
                    while ((line = reader.readLine()) != null) {
                        buffer.append(line);
                    }

                    //다 가져오면 String 형변환을 수행한다. 이유는 protected String doInBackground(String... urls) 니까
                    return buffer.toString();

                    //아래는 예외처리 부분이다.
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    //종료가 되면 disconnect메소드를 호출한다.
                    if (con != null) {
                        con.disconnect();
                    }
                    try {
                        //버퍼를 닫아준다.
                        if (reader != null) {
                            reader.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }//finally 부분
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        protected void onPostExecute(String result) {
            try {
                JSONObject jsonObject = new JSONObject(result);
                String code = jsonObject.getString("code");
                String msg = jsonObject.getString("message");
                String itemlist = jsonObject.getString("item");
                JSONArray getitem = new JSONArray(itemlist);

                getitem_count = getitem.length();

                location_name = new String[getitem_count];
                item_name = new String[getitem_count];
                marker_lat = new double[getitem_count];
                marker_lng = new double[getitem_count];
                place_state = new String[getitem_count];
                for (int i = 0; i < getitem_count; i++) {
                    JSONObject itemobject = getitem.getJSONObject(i);
                    location_name[i] = itemobject.getString("name");
                    item_name[i] = itemobject.getString("item_name");
                    marker_lat[i] = itemobject.getDouble("lat");
                    marker_lng[i] = itemobject.getDouble("lng");
                    place_state[i] = itemobject.getString("state");
                }

                if (code.equals("200")) {
                   // Toast.makeText(getApplicationContext(), "마커정보받아옴", Toast.LENGTH_SHORT).show();

                } else {
                    Toast.makeText(getApplicationContext(), "실패", Toast.LENGTH_SHORT).show();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public class add_Marker_Post extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... urls) {
            try {
                //JSONObject를 만들고 key value 형식으로 값을 저장해준다.
                JSONObject jsonObject = new JSONObject();
                //  System.out.println(user_id+","+add_name+","+add_item_list+","+add_lat+","+add_lng+","+insert_count);
                jsonObject.put("id", user_id);
                jsonObject.put("name", add_name);
                jsonObject.put("item_list", add_item_list);
                jsonObject.put("lat", add_lat);
                jsonObject.put("lng", add_lng);
                jsonObject.put("count", insert_count);
                HttpURLConnection con = null;
                BufferedReader reader = null;

                try {
                    URL url = new URL(urls[0]);
                    //연결을 함
                    con = (HttpURLConnection) url.openConnection();
                    con.setRequestMethod("POST");//POST방식으로 보냄
                    con.setRequestProperty("Cache-Control", "no-cache");//캐시 설정
                    con.setRequestProperty("Content-Type", "application/json");//application JSON 형식으로 전송
                    con.setRequestProperty("Accept", "text/html");//서버에 response 데이터를 html로 받음
                    con.setDoOutput(true);//Outstream으로 post 데이터를 넘겨주겠다는 의미
                    con.setDoInput(true);//Inputstream으로 서버로부터 응답을 받겠다는 의미
                    con.connect();
                    //서버로 보내기위해서 스트림 만듬
                    OutputStream outStream = con.getOutputStream();
                    //버퍼를 생성하고 넣음
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outStream));
                    writer.write(jsonObject.toString());
                    writer.flush();
                    writer.close();//버퍼를 받아줌

                    //서버로 부터 데이터를 받음
                    InputStream stream = con.getInputStream();
                    reader = new BufferedReader(new InputStreamReader(stream));
                    StringBuffer buffer = new StringBuffer();
                    String line = "";
                    while ((line = reader.readLine()) != null) {
                        buffer.append(line);
                    }
                    return buffer.toString();//서버로 부터 받은 값을 리턴해줌 아마 OK!!가 들어올것임
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (con != null) {
                        con.disconnect();
                    }
                    try {
                        if (reader != null) {
                            reader.close();//버퍼를 닫아줌
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        protected void onPostExecute(String result) {
            try {
                JSONObject jsonObject = new JSONObject(result);
                String code = jsonObject.getString("code");
                String msg = jsonObject.getString("message");
                if (code.equals("200")) {
                    Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                } else {
                    // Toast.makeText(getApplicationContext(), "실패", Toast.LENGTH_SHORT).show();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public class Update_Marker_Get extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... urls) {
            try {


                HttpURLConnection con = null;
                BufferedReader reader = null;
                try {
                    URL url = new URL(urls[0]);//url을 가져온다.
                    con = (HttpURLConnection) url.openConnection();
                    con.connect();//연결 수행
                    //입력 스트림 생성
                    InputStream stream = con.getInputStream();

                    //속도를 향상시키고 부하를 줄이기 위한 버퍼를 선언한다.
                    reader = new BufferedReader(new InputStreamReader(stream));

                    //실제 데이터를 받는곳
                    StringBuffer buffer = new StringBuffer();

                    //line별 스트링을 받기 위한 temp 변수
                    String line = "";

                    //아래라인은 실제 reader에서 데이터를 가져오는 부분이다. 즉 node.js서버로부터 데이터를 가져온다.
                    while ((line = reader.readLine()) != null) {
                        buffer.append(line);
                    }

                    //다 가져오면 String 형변환을 수행한다. 이유는 protected String doInBackground(String... urls) 니까
                    return buffer.toString();

                    //아래는 예외처리 부분이다.
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    //종료가 되면 disconnect메소드를 호출한다.
                    if (con != null) {
                        con.disconnect();
                    }
                    try {
                        //버퍼를 닫아준다.
                        if (reader != null) {
                            reader.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }//finally 부분
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        protected void onPostExecute(String result) {
            try {
                JSONObject jsonObject = new JSONObject(result);
                String code = jsonObject.getString("code");

                if (code.equals("200")) {
                    Toast.makeText(getApplicationContext(), "마커정보갱신", Toast.LENGTH_SHORT).show();

                } else {
                    Toast.makeText(getApplicationContext(), "실패", Toast.LENGTH_SHORT).show();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

}
