package com.example.musicnote;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PointF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.MonthDisplayHelper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.geometry.LatLngBounds;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.NaverMapOptions;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.UiSettings;
import com.naver.maps.map.overlay.LocationOverlay;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.util.FusedLocationSource;
import com.naver.maps.map.widget.CompassView;
import com.naver.maps.map.widget.LocationButtonView;
import com.naver.maps.map.widget.ScaleBarView;
import com.naver.maps.map.widget.ZoomControlView;

import java.util.ArrayList;
import java.util.Map;

import static android.hardware.SensorManager.AXIS_X;
import static android.hardware.SensorManager.AXIS_Z;
import static androidx.constraintlayout.widget.Constraints.TAG;

public class MainActivity extends AppCompatActivity
        implements OnMapReadyCallback,
        ActivityCompat.OnRequestPermissionsResultCallback,
        SensorEventListener {

    private static final String TAG = "MainActivity";

    // 네이버 지도 관련
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String[] PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    private FusedLocationSource mLocationSource;
    private NaverMap mNaverMap;

    // 위치 관련
    Location mCurrentLocation;

    // 마커 관련
    private Location[] markers = new Location[3];

    // ar 관련
    private View mLayout;  // Snackbar 사용하기 위해서는 View가 필요합니다.
    // (참고로 Toast에서는 Context가 필요했습니다.)

    // 아래는 ArCamera를 위한 변수 선언
    private ArFragment arFragment;
    private Session session;
    private ArSceneView arSceneView;
    private AnchorNode[] mAnchorNode = new AnchorNode[3];

    private ModelRenderable andyRenderable;
    private ModelRenderable foxRenderable;
    private ModelRenderable bofLogoRenderable;
    private ModelRenderable orangeNoteRenderable;
    private ModelRenderable redNoteRenderable;

    Handler mHandler = new Handler(); // 딜레이 시간

    // Device Orientation 관련
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mMagnetometer;
    private float[] mLastAccelerometer = new float[3];
    private float[] mLastMagnetometer = new float[3];
    private boolean mLastAccelerometerSet = false;
    private boolean mLastMagnetometerSet = false;
    private float mCurrentAzim = 0f; // 방위각
    private float mCurrentPitch = 0f; // 피치
    private float mCurrentRoll = 0f; // 롤

    // UI
    private TextView musicTitle;
    private ImageView play;
    private ProgressBar musicBar;
    private MusicUi musicUiclass;
    private RelativeLayout musicUi;
    private ImageView album;

    // 디버깅 ui 관련
    TextView pitchText, rollText;
    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Devicd Orientation 관련
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        // 레이아웃 받아오기
        mLayout = findViewById(R.id.layout_main);
        context = this;

        // 첫번째 마커
        markers[0] = new Location("point A");
        markers[0].setLatitude(37.284306);
        markers[0].setLongitude(127.053579);
        // 두번째 마커
        markers[1] = new Location("point B");
        markers[1].setLatitude(37.284097);
        markers[1].setLongitude(127.05389);
        // 세번째 마커
        markers[2] = new Location("point C");
        markers[2].setLatitude(37.283888);
        markers[2].setLongitude(127.054201);

        // 레이아웃을 위에 겹쳐서 올리는 부분
        LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        // 레이아웃 객체 생성
        LinearLayout ll = (LinearLayout)inflater.inflate(R.layout.navermap, null);
        // 레이아웃 배경 투명도 주기
        ll.setBackgroundColor(Color.parseColor("#00000000"));
        // 레이아웃 위에 겹치기
        LinearLayout.LayoutParams paramll = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        addContentView(ll, paramll);


        // 음악 관련 세팅
        musicUi = (RelativeLayout)findViewById(R.id.musicUi);
        musicTitle = (TextView)findViewById(R.id.musicTitle);
        play = (ImageView)findViewById(R.id.play);
        play.setImageResource(android.R.drawable.ic_media_pause);
        play.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if(musicUiclass.getMediaPlayer().isPlaying()){ // 음악이 재생되고 있을 때 => 음악을 멈춰야함
                    musicUiclass.musicPause();
                }
                else{ // 음악이 멈춰있을 때 => 음악을 재생해야함
                    musicUiclass.musicPlay();
                }
            }
        });
        musicBar = (ProgressBar)findViewById(R.id.musicBar);
        album = (ImageView)findViewById(R.id.album);
        musicUiclass = new MusicUi(this,this, musicBar, musicTitle, play, album);

        // 레이아웃 디버그용
        // 레이아웃 객체 생성
        RelativeLayout ll2 = (RelativeLayout)inflater.inflate(R.layout.mapbutton, null);
        // 레이아웃 배경 투명도 주기
        ll2.setBackgroundColor(Color.parseColor("#00000000"));
        // 레이아웃 위에 겹치기
        RelativeLayout.LayoutParams paramll2 = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        addContentView(ll2, paramll2);

        // 지도 객체 생성
        FragmentManager fm = getSupportFragmentManager();
        MapFragment mapFragment = (MapFragment)fm.findFragmentById(R.id.map);
        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            fm.beginTransaction().add(R.id.map, mapFragment).commit();
        }

        // getMapAsync를 호출하여 비동기로 onMapReady 콜백 메서드 호출
        // onMapReady에서 NaverMap 객체를 받음
        mapFragment.getMapAsync(this);

        // 위치를 반환하는 구현체인 FusedLocationSource 생성
        mLocationSource =
                new FusedLocationSource(this, PERMISSION_REQUEST_CODE);

        // ar 관련
        arFragment = (ArFragment)getSupportFragmentManager().findFragmentById(R.id.arCamera);
        session = arFragment.getArSceneView().getSession();
        arSceneView = arFragment.getArSceneView();
        setUpModel();
        arFragment.getArSceneView().getScene().setOnUpdateListener(this::onSceneUpdate);

        // 디버깅용 ui 설정
        pitchText = (TextView)findViewById(R.id.pitch);
        rollText = (TextView)findViewById(R.id.roll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        arFragment.getPlaneDiscoveryController().hide();
        arFragment.getPlaneDiscoveryController().setInstructionView(null);

        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this, mAccelerometer);
        mSensorManager.unregisterListener(this, mMagnetometer);
    }

    private void setUpModel(){

        ModelRenderable.builder()
                .setSource(this, R.raw.andy)
                .build().thenAccept(renderable -> andyRenderable = renderable)
                .exceptionally(
                        throwable -> {
                            Toast.makeText(this, "Unable to load andy model", Toast.LENGTH_SHORT).show();
                            return null;
                        }
                );

        ModelRenderable.builder()
                .setSource(this, R.raw.arcticfox_posed)
                .build().thenAccept(renderable -> foxRenderable = renderable)
                .exceptionally(
                        throwable -> {
                            Toast.makeText(this, "Unable to load fox model", Toast.LENGTH_SHORT).show();
                            return null;
                        }
                );

        ModelRenderable.builder()
                .setSource(this, R.raw.bof)
                .build().thenAccept(renderable -> bofLogoRenderable = renderable)
                .exceptionally(
                        throwable -> {
                            Toast.makeText(this, "Unable to load bof logo model", Toast.LENGTH_SHORT).show();
                            return null;
                        }
                );

        ModelRenderable.builder()
                .setSource(this, R.raw.orange_note)
                .build().thenAccept(renderable -> orangeNoteRenderable = renderable)
                .exceptionally(
                        throwable -> {
                            Toast.makeText(this, "Unable to load orange note model", Toast.LENGTH_SHORT).show();
                            return null;
                        }
                );

        ModelRenderable.builder()
                .setSource(this, R.raw.red_note)
                .build().thenAccept(renderable -> redNoteRenderable = renderable)
                .exceptionally(
                        throwable -> {
                            Toast.makeText(this, "Unable to load red note model", Toast.LENGTH_SHORT).show();
                            return null;
                        }
                );
    }

    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        Log.d( TAG, "onMapReady");

        // 마커 세팅
        Marker marker1 = new Marker();
        marker1.setPosition(new LatLng(markers[0].getLatitude(), markers[0].getLongitude()));
        marker1.setHeight(50);
        marker1.setWidth(40);
        marker1.setMap(naverMap);

        Marker marker2 = new Marker();
        marker2.setPosition(new LatLng(markers[1].getLatitude(), markers[1].getLongitude()));
        marker2.setHeight(50);
        marker2.setWidth(40);
        marker2.setMap(naverMap);

        Marker marker3 = new Marker();
        marker3.setPosition(new LatLng(markers[2].getLatitude(), markers[2].getLongitude()));
        marker3.setHeight(50);
        marker3.setWidth(40);
        marker3.setMap(naverMap);

        // NaverMap 객체 받아서 NaverMap 객체에 위치 소스 지정
        mNaverMap = naverMap;
        mNaverMap.setLocationSource(mLocationSource);

        // UI 컨트롤 재배치
        UiSettings uiSettings = mNaverMap.getUiSettings();
        uiSettings.setCompassEnabled(false); // 기본값 : true
        uiSettings.setScaleBarEnabled(false); // 기본값 : true
        uiSettings.setZoomControlEnabled(false); // 기본값 : true
        uiSettings.setLocationButtonEnabled(false); // 기본값 : false
        uiSettings.setLogoGravity(Gravity.LEFT|Gravity.BOTTOM);
        uiSettings.setLogoMargin(0,0,0, -5);

        CameraUpdate cameraUpdate = CameraUpdate.zoomTo(15);
        mNaverMap.moveCamera(cameraUpdate);
        mNaverMap.setLocationTrackingMode(LocationTrackingMode.Follow);
        mNaverMap.setLiteModeEnabled(true);

        LocationOverlay locationOverlay = mNaverMap.getLocationOverlay();
        locationOverlay.setIconWidth(40);
        locationOverlay.setIconHeight(40);

        locationOverlay.setSubIconWidth(40);
        locationOverlay.setSubIconHeight(40);
        locationOverlay.setSubAnchor(new PointF(0.5f, 0.9f));

        mNaverMap.addOnLocationChangeListener(location ->
                mCurrentLocation = location);
        // 권한확인. 결과는 onRequestPermissionsResult 콜백 매서드 호출
        ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // request code와 권한획득 여부 확인
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mNaverMap.setLocationTrackingMode(LocationTrackingMode.Follow);
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(event.sensor == mAccelerometer) {
            System.arraycopy(event.values, 0, mLastAccelerometer, 0, event.values.length);
            mLastAccelerometerSet = true;
        }else if (event.sensor == mMagnetometer){
            System.arraycopy(event.values, 0, mLastMagnetometer, 0, event.values.length);
            mLastMagnetometerSet = true;
        }
        if(mLastAccelerometerSet && mLastMagnetometerSet){
            float[] rotationMatrix = new float[9];
            SensorManager.getRotationMatrix(rotationMatrix, null, mLastAccelerometer, mLastMagnetometer);

            float[] adjustedRotationMatrix = new float[9];
            SensorManager.remapCoordinateSystem(rotationMatrix, AXIS_X, AXIS_Z, adjustedRotationMatrix);
            float[] orientation = new float[3];
            SensorManager.getOrientation(adjustedRotationMatrix, orientation);

            mCurrentAzim = orientation[0]; // 방위각 (라디안)
            mCurrentPitch = orientation[1]; // 피치
            mCurrentRoll = orientation[2]; // 롤
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void onSceneUpdate(FrameTime frameTime) {
        if (mAnchorNode[0] != null && mAnchorNode[1] != null && mAnchorNode[2] != null) {
            for (int i = 0; i < 3; i++) {
                // 혹시라도 오브젝트가 사라졌다면 (트래킹 모드가 해제되어서)
                if (mAnchorNode[i].getAnchor().getTrackingState() != TrackingState.TRACKING
                        && arSceneView.getArFrame().getCamera().getTrackingState() == TrackingState.TRACKING) {
                    // Detach the old anchor
                    mAnchorNode[i].getAnchor().detach();

                    // Create a new anchor and attach it to the anchorNode.
                    // Create an ARCore Anchor at the position.
                    float dLatitude = (float) (markers[i].getLatitude() - mCurrentLocation.getLatitude()) * 110900f;
                    float dLongitude = (float) (markers[i].getLongitude() - mCurrentLocation.getLongitude()) * 88400f;
                    float distance = (float) Math.sqrt((dLongitude * dLongitude) + (dLatitude * dLatitude));

                    if(distance > 15){ // 15m보다 멀면 오브젝트 생성X
                        continue;
                    }

                    float height = -0.5f;
                    Vector3 objVec = new Vector3(dLongitude, dLatitude, height);

                    Vector3 xUnitVec;
                    Vector3 yUnitVec;
                    Vector3 zUnitVec;

                    zUnitVec = new Vector3((float)(Math.cos(mCurrentPitch) * Math.sin(mCurrentAzim)), (float)(Math.cos(mCurrentPitch) * Math.cos(mCurrentAzim)), (float)(-Math.sin(mCurrentPitch)));
                    zUnitVec = zUnitVec.normalized().negated();

                    yUnitVec = new Vector3((float)(Math.sin(mCurrentPitch) * Math.sin(mCurrentAzim)), (float)(Math.sin(mCurrentPitch) * Math.cos(mCurrentAzim)), (float)(Math.cos(mCurrentPitch))).normalized();

                    float wx = zUnitVec.x;
                    float wy = zUnitVec.y;
                    float wz = zUnitVec.z;

                    float yx = yUnitVec.x;
                    float yy = yUnitVec.y;
                    float yz = yUnitVec.z;

                    float t = 1 - (float)Math.cos(mCurrentRoll);
                    float s = (float)Math.sin(mCurrentRoll);
                    float c = (float)Math.cos(mCurrentRoll);

                    float[][] rotMat = {{wx*wx*t+c, wx*wy*t+wz*s, wx*wz*t-wy*s},
                            {wy*wx*t-wz*s, wy*wy*t+c, wy*wz*t+wx*s},
                            {wz*wx*t+wy*s, wz*wy*t-wx*s, wz*wz*t+c}};

                    yUnitVec = new Vector3(yx*rotMat[0][0] + yy*rotMat[0][1] + yz*rotMat[0][2],
                            yx*rotMat[1][0] + yy*rotMat[1][1] + yz*rotMat[1][2],
                            yx*rotMat[2][0] + yy*rotMat[2][1] + yz*rotMat[2][2]).normalized();

                    xUnitVec = Vector3.cross(yUnitVec, zUnitVec).normalized();

                    float xPos = Vector3.dot(objVec, xUnitVec);
                    float yPos = Vector3.dot(objVec, yUnitVec);
                    float zPos = Vector3.dot(objVec, zUnitVec);

                    Vector3 xAxis = arSceneView.getScene().getCamera().getRight().normalized().scaled(xPos);
                    Vector3 yAxis = arSceneView.getScene().getCamera().getUp().normalized().scaled(yPos);
                    Vector3 zAxis = arSceneView.getScene().getCamera().getBack().normalized().scaled(zPos);
                    Vector3 objectPos = new Vector3(xAxis.x + yAxis.x + zAxis.x, xAxis.y + yAxis.y + zAxis.y, xAxis.z + yAxis.z + zAxis.z);
                    Vector3 cameraPos = arSceneView.getScene().getCamera().getWorldPosition();

                    Vector3 position = Vector3.add(cameraPos, objectPos);

                    // Create an ARCore Anchor at the position.
                    Pose pose = Pose.makeTranslation(position.x, position.y, position.z);
                    Anchor anchor = arSceneView.getSession().createAnchor(pose);

                    mAnchorNode[i].setAnchor(anchor);
                    mAnchorNode[i].setParent(arSceneView.getScene());

                    Node node = new Node();

                    node.setRenderable(foxRenderable);
                    //node.setLocalScale(new Vector3(0.75f, 0.75f, 0.75f));
                    node.setParent(mAnchorNode[i]);

                    music(node, i);

                    Toast.makeText(context, i + "번째 오브젝트 재생성", Toast.LENGTH_SHORT).show();
                }
            }
            return;
        }

        if (mCurrentLocation == null) {
            Log.d(TAG, "Location is null");
            return;
        }

        if (andyRenderable == null || foxRenderable == null || bofLogoRenderable == null || redNoteRenderable == null || orangeNoteRenderable == null) {
            Log.d(TAG, "onUpdate: objectRenderable is null");
            return;
        }

        if (arSceneView.getArFrame() == null) {
            Log.d(TAG, "onUpdate: No frame available");
            // No frame available
            return;
        }

        if (arSceneView.getArFrame().getCamera().getTrackingState() != TrackingState.TRACKING) {
            Log.d(TAG, "onUpdate: Tracking not started yet");
            // Tracking not started yet
            return;
        }

        // 오브젝트 생성!
        for (int i = 0; i < 3; i++) {
            if (mAnchorNode[i] == null && mCurrentLocation != null) {
                //Log.d(TAG, "onUpdate: mAnchorNode["+ i +"] is null");
                // 여기에다가 내 gps의 위도 경도, 마커들의 위도 경도를 이용하여 마커들의 Pose값 구해야함!

                float dLatitude = (float) (markers[i].getLatitude() - mCurrentLocation.getLatitude()) * 110900f;
                float dLongitude = (float) (markers[i].getLongitude() - mCurrentLocation.getLongitude()) * 88400f;
                float distance = (float) Math.sqrt((dLongitude * dLongitude) + (dLatitude * dLatitude));

                /*
                if( i == 0 ) {
                    dLatitude = 2f;
                    dLongitude = 0f;
                }
                else if ( i == 1 ){
                   dLatitude = -2f;
                   dLongitude = 0f;
                }
                else{
                   dLatitude = 0f;
                   dLongitude = 2f;
                }*/


                if(distance > 15){ // 15m보다 멀면 오브젝트 생성X
                    continue;
                }


                //mCurrentAzim = 0f;
                //mCurrentPitch = 0.785398f;
                //mCurrentRoll = 0f;

                float height = -0.5f;
                Vector3 objVec = new Vector3(dLongitude, dLatitude, height);

                Vector3 xUnitVec;
                Vector3 yUnitVec;
                Vector3 zUnitVec;

                zUnitVec = new Vector3((float)(Math.cos(mCurrentPitch) * Math.sin(mCurrentAzim)), (float)(Math.cos(mCurrentPitch) * Math.cos(mCurrentAzim)), (float)(-Math.sin(mCurrentPitch)));
                zUnitVec = zUnitVec.normalized().negated();

                yUnitVec = new Vector3((float)(Math.sin(mCurrentPitch) * Math.sin(mCurrentAzim)), (float)(Math.sin(mCurrentPitch) * Math.cos(mCurrentAzim)), (float)(Math.cos(mCurrentPitch))).normalized();

                float wx = zUnitVec.x;
                float wy = zUnitVec.y;
                float wz = zUnitVec.z;

                float yx = yUnitVec.x;
                float yy = yUnitVec.y;
                float yz = yUnitVec.z;

                float t = 1 - (float)Math.cos(mCurrentRoll);
                float s = (float)Math.sin(mCurrentRoll);
                float c = (float)Math.cos(mCurrentRoll);

                float[][] rotMat = {{wx*wx*t+c, wx*wy*t+wz*s, wx*wz*t-wy*s},
                                    {wy*wx*t-wz*s, wy*wy*t+c, wy*wz*t+wx*s},
                                    {wz*wx*t+wy*s, wz*wy*t-wx*s, wz*wz*t+c}};

                yUnitVec = new Vector3(yx*rotMat[0][0] + yy*rotMat[0][1] + yz*rotMat[0][2],
                                    yx*rotMat[1][0] + yy*rotMat[1][1] + yz*rotMat[1][2],
                                    yx*rotMat[2][0] + yy*rotMat[2][1] + yz*rotMat[2][2]).normalized();


                xUnitVec = Vector3.cross(yUnitVec, zUnitVec).normalized();

                /*
                Log.d(TAG, "디버그 xVector <"+xUnitVec.x +", "+xUnitVec.y+", "+ xUnitVec.z +">");
                Log.d(TAG, "디버그 yVector <"+yUnitVec.x +", "+yUnitVec.y+", "+ yUnitVec.z +">");
                Log.d(TAG, "디버그 zVector <"+zUnitVec.x +", "+zUnitVec.y+", "+ zUnitVec.z +">");*/

                float xPos = Vector3.dot(objVec, xUnitVec);
                float yPos = Vector3.dot(objVec, yUnitVec);
                float zPos = Vector3.dot(objVec, zUnitVec);

                Vector3 xAxis = arSceneView.getScene().getCamera().getRight().normalized().scaled(xPos);
                Vector3 yAxis = arSceneView.getScene().getCamera().getUp().normalized().scaled(yPos);
                Vector3 zAxis = arSceneView.getScene().getCamera().getBack().normalized().scaled(zPos);
                Vector3 objectPos = new Vector3(xAxis.x + yAxis.x + zAxis.x, xAxis.y + yAxis.y + zAxis.y, xAxis.z + yAxis.z + zAxis.z);
                Vector3 cameraPos = arSceneView.getScene().getCamera().getWorldPosition();

                Vector3 position = Vector3.add(cameraPos, objectPos);

                // Create an ARCore Anchor at the position.
                Pose pose = Pose.makeTranslation(position.x, position.y, position.z);
                Anchor anchor = arSceneView.getSession().createAnchor(pose);

                mAnchorNode[i] = new AnchorNode(anchor);
                mAnchorNode[i].setParent(arSceneView.getScene());

                Node node = new Node();

                node.setRenderable(foxRenderable);
                //node.setLocalScale(new Vector3(0.75f, 0.75f, 0.75f));
                node.setParent(mAnchorNode[i]);

                music(node, i);

                Toast.makeText(context, "오브젝트 생성[" + i + "] (distance: " + distance + "m)", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void music(Node node,int i){
        Context c = this;

        node.setOnTapListener((v, event) -> {
            /* gps를 이용한 거리
            float dLatitude = (float) (markers[i].getLatitude() - mCurrentLocation.getLatitude()) * 110900f;
            float dLongitude = (float) (markers[i].getLongitude() - mCurrentLocation.getLongitude()) * 88400f;
            float distance = (float) Math.sqrt((dLongitude * dLongitude) + (dLatitude * dLatitude));
            */

            // AR자체의 world position을 이용한 거리
            Vector3 vec = Vector3.subtract(node.getWorldPosition(), arSceneView.getScene().getCamera().getWorldPosition());
            float distance = (float)Math.sqrt(Vector3.dot(vec, vec));

            // 터치한 오브젝트와의 거리가 30m이내 일때만 터치 가능
            if(distance <= 30f) {
                if (musicUi.getVisibility() == View.INVISIBLE || musicUi.getVisibility() == View.GONE) {
                    musicUi.setVisibility(View.VISIBLE);
                }

                if (musicUiclass.isPlaying(i)) {
                    musicUiclass.musicStop();
                    Toast.makeText(c, "music stop (거리: " + distance + "m)", Toast.LENGTH_SHORT).show();
                } else {
                    musicUiclass.musicStop();
                    musicUiclass.setMediaPlayer(i);
                    musicUiclass.musicPlay();
                    Toast.makeText(c, "music start (거리: " + distance + "m)", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
