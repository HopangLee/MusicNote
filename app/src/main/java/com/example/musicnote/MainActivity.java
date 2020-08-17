package com.example.musicnote;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PointF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.MonthDisplayHelper;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewManager;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.google.ar.core.Anchor;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
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
import com.ssomai.android.scalablelayout.ScalableLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static android.hardware.SensorManager.AXIS_X;
import static android.hardware.SensorManager.AXIS_Z;

public class MainActivity extends AppCompatActivity
        implements OnMapReadyCallback,
        ActivityCompat.OnRequestPermissionsResultCallback,
        SensorEventListener{

    private static final String TAG = "MainActivity";

    private FrameLayout popupLayout;

    // 네이버 지도 관련
    private static final int PERMISSION_REQUEST_CODE = 1000;
    private static final String[] PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CAMERA
    };

    private FusedLocationSource mLocationSource;
    private NaverMap mNaverMap;

    // 위치 관련
    public Location mCurrentLocation;

    // 마커 관련
    private Location[] markers = new Location[3];
    private Location logoLocation;

    // ar 관련
    private View mLayout;  // Snackbar 사용하기 위해서는 View가 필요합니다.
    // (참고로 Toast에서는 Context가 필요했습니다.)

    // 아래는 ArCamera를 위한 변수 선언
    private ArFragment arFragment;
    private static ArSceneView arSceneView;
    private AnchorNode[] mAnchorNode = new AnchorNode[3];
    private AnchorNode logoAnchor;

    private ModelRenderable bofLogoRenderable;
    private ModelRenderable[] musicNotes = new ModelRenderable[2];

    private ModelRenderable[] albumRenderable = new ModelRenderable[3]; // album Object들

    // Device Orientation 관련
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mMagnetometer;
    private float[] mLastAccelerometer = new float[3];
    private float[] mLastMagnetometer = new float[3];
    private boolean mLastAccelerometerSet = false;
    private boolean mLastMagnetometerSet = false;
    public static float mCurrentAzim = 0f; // 방위각
    public static float mCurrentPitch = 0f; // 피치
    public static float mCurrentRoll = 0f; // 롤

    // 음악 노트
    private int[] timerArray =
            {1000, 2000, 3000, 4000, 5000, 6000, 7000, 8000, 9000, 10000,
                    11000, 12000, 13000, 14000, 15000, 16000, 17000, 18000, 19000, 20000,
                    21000, 22000, 23000, 24000, 25000, 26000, 27000, 28000, 29000, 30000,
                    31000, 32000, 33000, 34000, 35000, 36000, 37000, 38000, 39000, 40000,
                    41000, 42000, 43000, 44000, 45000, 46000, 47000, 48000, 49000, 50000,
                    51000, 52000, 53000, 54000, 55000, 56000, 57000, 58000, 59000, 60000};

    // UI
    private TextView musicTitle;
    private ImageView play;
    private ProgressBar musicBar;
    private MusicUi musicUiclass;
    private ScalableLayout musicUi;
    private ImageView album;

    // 게임 관련
    private GameSystem gameSystem;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        /*
        //팝업창 관련
        Intent intent = new Intent(getApplicationContext(), PopupActivity.class);
        startActivityForResult(intent, 1);
        */

        // Devicd Orientation 관련
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        // 레이아웃 받아오기
        mLayout = findViewById(R.id.layout_main);

        // 첫번째 마커//ddddd
        markers[0] = new Location("point A");
        markers[0].setLatitude(37.284944);
        markers[0].setLongitude(127.054483);
        // 두번째 마커
        markers[1] = new Location("point B");
        markers[1].setLatitude(37.285698);
        markers[1].setLongitude(127.055123);
        // 세번째 마커
        markers[2] = new Location("point C");
        markers[2].setLatitude(37.283768);
        markers[2].setLongitude(127.054366);

        // 로고 위치
        logoLocation = new Location("BOF LOGO");
        logoLocation.setLatitude(37.286259);
        logoLocation.setLongitude(127.053147);

        // 레이아웃을 위에 겹쳐서 올리는 부분
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        // 레이아웃 객체 생성
        LinearLayout ll = (LinearLayout) inflater.inflate(R.layout.navermap, null);
        // 레이아웃 배경 투명도 주기
        ll.setBackgroundColor(Color.parseColor("#00000000"));
        // 레이아웃 위에 겹치기
        LinearLayout.LayoutParams paramll = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        addContentView(ll, paramll);


        // 게임 ui 레이아웃 오버레이
        // 레이아웃 객체 생성
        FrameLayout gameLayout = (FrameLayout) inflater.inflate(R.layout.game_ui, null);
        // 레이아웃 배경 투명도 주기
        gameLayout.setBackgroundColor(Color.parseColor("#00000000"));
        // 레이아웃 위에 겹치기
        FrameLayout.LayoutParams paramll2 = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT
        );
        addContentView(gameLayout, paramll2);

        // '위치를 찾는 중' 팝업창 오버레이
        popupLayout = (FrameLayout)inflater.inflate(R.layout.activity_popup, null);
        popupLayout.setBackgroundColor(Color.parseColor("#CC000000"));
        FrameLayout.LayoutParams popParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
        );
        addContentView(popupLayout, popParams);

        // 음악 관련 세팅
        musicUi = (ScalableLayout) findViewById(R.id.musicUi);

        musicTitle = (TextView) findViewById(R.id.musicTitle);
        play = (ImageView) findViewById(R.id.play);
        play.setImageResource(R.drawable.ic_media_stop);
        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (musicUiclass.getCurrentMediaPlayer().isPlaying()) { // 음악이 재생되고 있을 때 => 음악을 멈춰야함
                    musicUiclass.musicPause();
                } else { // 음악이 멈춰있을 때 => 음악을 재생해야함
                    musicUiclass.musicPlay();
                }
            }
        });
        musicBar = (ProgressBar) findViewById(R.id.musicBar);
        //album = (ImageView) findViewById(R.id.album);
        musicUiclass = new MusicUi(this, this, musicBar, musicTitle, play);

        TextView score = findViewById(R.id.score);
        SpannableStringBuilder spannable = new SpannableStringBuilder(score.getText());
        spannable.setSpan(new AbsoluteSizeSpan(60),0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new AbsoluteSizeSpan(40), 2, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        score.setText(spannable, TextView.BufferType.EDITABLE);

        int colorWhite = getResources().getColor(R.color.colorWhite);
        TextView score2 = findViewById(R.id.scoreText);
        SpannableStringBuilder spannable2 = new SpannableStringBuilder(score2.getText());
        spannable2.setSpan(new AbsoluteSizeSpan(45),0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable2.setSpan(new AbsoluteSizeSpan(70),4, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable2.setSpan(new AbsoluteSizeSpan(45),6, 7, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable2.setSpan(new ForegroundColorSpan(colorWhite),4, 7, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        score2.setText(spannable2, TextView.BufferType.EDITABLE);

        // 지도 객체 생성
        FragmentManager fm = getSupportFragmentManager();
        MapFragment mapFragment = (MapFragment) fm.findFragmentById(R.id.map);
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
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.arCamera);
        arSceneView = arFragment.getArSceneView();
        setUpModel();

        arFragment.getArSceneView().getScene().setOnUpdateListener(this::onSceneUpdate);
    }

    @Override
    protected void onResume() {
        super.onResume();
        arFragment.getPlaneDiscoveryController().hide();
        arFragment.getPlaneDiscoveryController().setInstructionView(null);

        arFragment.onResume();

        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        arFragment.onPause();
        mSensorManager.unregisterListener(this, mAccelerometer);
        mSensorManager.unregisterListener(this, mMagnetometer);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        //arFragment.onDestroy();
    }

    private void setUpModel() {

        ModelRenderable.builder()
                .setSource(this, R.raw.boflogo)
                .build().thenAccept(renderable -> bofLogoRenderable = renderable)
                .exceptionally(
                        throwable -> {
                            Toast.makeText(this, "Unable to load bof logo model", Toast.LENGTH_SHORT).show();
                            return null;
                        }
                );

        ModelRenderable.builder()
                .setSource(this, R.raw.orange_note)
                .build().thenAccept(renderable -> musicNotes[0] = renderable)
                .exceptionally(
                        throwable -> {
                            Toast.makeText(this, "Unable to load orange note model", Toast.LENGTH_SHORT).show();
                            return null;
                        }
                );

        ModelRenderable.builder()
                .setSource(this, R.raw.red_note)
                .build().thenAccept(renderable -> musicNotes[1] = renderable)
                .exceptionally(
                        throwable -> {
                            Toast.makeText(this, "Unable to load red note model", Toast.LENGTH_SHORT).show();
                            return null;
                        }
                );

        ModelRenderable.builder()
                .setSource(this, R.raw.exo_album)
                .build().thenAccept(renderable -> albumRenderable[0] = renderable)
                .exceptionally(
                        throwable -> {
                            Toast.makeText(this, "Unable to load albumRenderable 1 model", Toast.LENGTH_SHORT).show();
                            return null;
                        }
                );

        ModelRenderable.builder()
                .setSource(this, R.raw.nct127_album)
                .build().thenAccept(renderable -> albumRenderable[1] = renderable)
                .exceptionally(
                        throwable -> {
                            Toast.makeText(this, "Unable to load albumRenderable 2 model", Toast.LENGTH_SHORT).show();
                            return null;
                        }
                );

        ModelRenderable.builder()
                .setSource(this, R.raw.redvelvet_album)
                .build().thenAccept(renderable -> albumRenderable[2] = renderable)
                .exceptionally(
                        throwable -> {
                            Toast.makeText(this, "Unable to load albumRenderable 3 model", Toast.LENGTH_SHORT).show();
                            return null;
                        }
                );
    }

    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        // NaverMap 객체 받아서 NaverMap 객체에 위치 소스 지정
        mNaverMap = naverMap;

        mNaverMap.setLocationSource(mLocationSource);

        Log.d(TAG, "onMapReady");

        // 마커 세팅
        Marker marker1 = new Marker();
        marker1.setPosition(new LatLng(markers[0].getLatitude(), markers[0].getLongitude()));
        marker1.setHeight(70);
        marker1.setWidth(60);
        marker1.setIcon(OverlayImage.fromResource(R.drawable.blacklogo));
        marker1.setAnchor(new PointF(0.5f, 1));
        marker1.setMap(naverMap);

        Marker marker2 = new Marker();
        marker2.setPosition(new LatLng(markers[1].getLatitude(), markers[1].getLongitude()));
        marker2.setHeight(70);
        marker2.setWidth(60);
        marker2.setIcon(OverlayImage.fromResource(R.drawable.btslogo));
        marker2.setAnchor(new PointF(0.5f, 1));
        marker2.setMap(naverMap);

        Marker marker3 = new Marker();
        marker3.setPosition(new LatLng(markers[2].getLatitude(), markers[2].getLongitude()));
        marker3.setHeight(70);
        marker3.setWidth(60);
        marker3.setIcon(OverlayImage.fromResource(R.drawable.redlogo));
        marker3.setAnchor(new PointF(0.5f, 1));
        marker3.setMap(naverMap);

        Marker logo = new Marker();
        logo.setPosition(new LatLng(logoLocation.getLatitude(), logoLocation.getLongitude()));
        logo.setHeight(60);
        logo.setWidth(70);
        logo.setIcon(OverlayImage.fromResource(R.drawable.boflogo));
        logo.setAnchor(new PointF(0.5f, 0.5f));
        logo.setMap(naverMap);

        // UI 컨트롤 재배치
        UiSettings uiSettings = mNaverMap.getUiSettings();
        uiSettings.setCompassEnabled(false); // 기본값 : true
        uiSettings.setScaleBarEnabled(false); // 기본값 : true
        uiSettings.setZoomControlEnabled(false); // 기본값 : true
        uiSettings.setLocationButtonEnabled(false); // 기본값 : false
        uiSettings.setLogoGravity(Gravity.LEFT | Gravity.BOTTOM);
        uiSettings.setLogoMargin(0, 0, 0, -5);

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

        mNaverMap.addOnLocationChangeListener(location ->{
                    mCurrentLocation = location;
                });
        mNaverMap.addOnCameraIdleListener(()->{
            if(mNaverMap.getLocationTrackingMode() == LocationTrackingMode.NoFollow ||
            mNaverMap.getLocationTrackingMode() == LocationTrackingMode.None){
                mNaverMap.setLocationTrackingMode(LocationTrackingMode.Follow);
            }
        });

        // 권한확인. 결과는 onRequestPermissionsResult 콜백 매서드 호출
        //ActivityCompat.requestPermissions(this, PERMISSIONS, LOCATION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (mLocationSource.onRequestPermissionsResult(
                requestCode, permissions, grantResults)) {
            if (!mLocationSource.isActivated()) { // 권한 거부됨
                mNaverMap.setLocationTrackingMode(LocationTrackingMode.None);
            }

            Log.i("디버그: ", " 퍼미션 요청");
            return;
        }

        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == mAccelerometer) {
            System.arraycopy(event.values, 0, mLastAccelerometer, 0, event.values.length);
            mLastAccelerometerSet = true;
        } else if (event.sensor == mMagnetometer) {
            System.arraycopy(event.values, 0, mLastMagnetometer, 0, event.values.length);
            mLastMagnetometerSet = true;
        }
        if (mLastAccelerometerSet && mLastMagnetometerSet) {
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
        // 로고 앵커가 사라졌을 시
        if(logoAnchor != null) {
            if (logoAnchor.getAnchor().getTrackingState() != TrackingState.TRACKING
                    && arSceneView.getArFrame().getCamera().getTrackingState() == TrackingState.TRACKING) {
                // Detach the old anchor
                List<Node> children = new ArrayList<>(logoAnchor.getChildren());
                for (Node n : children) {
                    Log.d(TAG, "find node list");
                    if (n instanceof BofLogo) {
                        Log.d(TAG, "removed");
                        logoAnchor.removeChild(n);
                        n.setParent(null);
                    }
                }
                arSceneView.getScene().removeChild(logoAnchor);
                logoAnchor.getAnchor().detach();
                logoAnchor.setParent(null);
                logoAnchor = null;
            }
        }

        for (int i = 0; i < 3; i++) {
            if (mAnchorNode[i] != null) {
                // 혹시라도 오브젝트가 사라졌다면 (트래킹 모드가 해제되어서)
                if (mAnchorNode[i].getAnchor().getTrackingState() != TrackingState.TRACKING
                        && arSceneView.getArFrame().getCamera().getTrackingState() == TrackingState.TRACKING) {
                    // Detach the old anchor
                    List<Node> children = new ArrayList<>(mAnchorNode[i].getChildren());
                    for (Node n : children) {
                        Log.d(TAG, "find node list");
                        if (n instanceof AlbumNode) {
                            Log.d(TAG, "removed");
                            mAnchorNode[i].removeChild(n);
                            n.setParent(null);
                        }
                    }
                    arSceneView.getScene().removeChild(mAnchorNode[i]);
                    mAnchorNode[i].getAnchor().detach();
                    mAnchorNode[i].setParent(null);
                    mAnchorNode[i] = null;
                }
            }
        }

        if(gameSystem != null && logoAnchor != null && mAnchorNode[0] != null && mAnchorNode[1] != null && mAnchorNode[2] != null){
            return;
        }

        if (mCurrentLocation == null) {
            Log.d(TAG, "Location is null");
            return;
        }
        else{
            if(popupLayout == null || popupLayout.getParent() != null)
                ((ViewManager)popupLayout.getParent()).removeView(popupLayout);
        }

        if (bofLogoRenderable == null) {
            Log.d(TAG, "onUpdate: bof logo Renderable is null");
            return;
        }

        for (ModelRenderable m : musicNotes) {
            if (m == null) {
                Log.d(TAG, "onUpdate: musicNotes Renderable is null");
                return;
            }
        }

        for (ModelRenderable m : albumRenderable) {
            if (m == null) {
                Log.d(TAG, "onUpdate: album Renderable is null");
                return;
            }
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
            if (mAnchorNode[i] == null) {
                //Log.d(TAG, "onUpdate: mAnchorNode["+ i +"] is null");
                // 여기에다가 내 gps의 위도 경도, 마커들의 위도 경도를 이용하여 마커들의 Pose값 구해야함!
                if (createNode(i) == false) continue;
            }
        }
        // BOF 로고 오브젝트 생성
        if(logoAnchor == null){
            createLogo();
        }

        if(gameSystem == null) {
            // 게임 시스템 생성
            gameSystem = new GameSystem(this, arSceneView, musicUiclass, findViewById(R.id.score), findViewById(R.id.scoreText));
            musicUiclass.setGameSystem(gameSystem);
            Log.i("GameSystem create: ", "true");
        }
    }

    public void createLogo(){
        float dLatitude = (float) (logoLocation.getLatitude() - mCurrentLocation.getLatitude()) * 110900f;
        float dLongitude = (float) (logoLocation.getLongitude() - mCurrentLocation.getLongitude()) * 88400f;

        // 테스트 용도


        dLatitude = 20f;
        dLongitude = 0f;

        float distance = (float) Math.sqrt((dLongitude * dLongitude) + (dLatitude * dLatitude));

        if (distance > 25) { // 25m보다 멀면 오브젝트 생성X
            return;
        }

        float height = 0.5f;
        Vector3 objVec = new Vector3(dLongitude, dLatitude, height);

        Vector3 xUnitVec;
        Vector3 yUnitVec;
        Vector3 zUnitVec;

        zUnitVec = new Vector3((float) (Math.cos(mCurrentPitch) * Math.sin(mCurrentAzim)), (float) (Math.cos(mCurrentPitch) * Math.cos(mCurrentAzim)), (float) (-Math.sin(mCurrentPitch)));
        zUnitVec = zUnitVec.normalized().negated();

        yUnitVec = new Vector3((float) (Math.sin(mCurrentPitch) * Math.sin(mCurrentAzim)), (float) (Math.sin(mCurrentPitch) * Math.cos(mCurrentAzim)), (float) (Math.cos(mCurrentPitch))).normalized();

        float wx = zUnitVec.x;
        float wy = zUnitVec.y;
        float wz = zUnitVec.z;

        float yx = yUnitVec.x;
        float yy = yUnitVec.y;
        float yz = yUnitVec.z;

        float t = 1 - (float) Math.cos(mCurrentRoll);
        float s = (float) Math.sin(mCurrentRoll);
        float c = (float) Math.cos(mCurrentRoll);

        float[][] rotMat = {{wx * wx * t + c, wx * wy * t + wz * s, wx * wz * t - wy * s},
                {wy * wx * t - wz * s, wy * wy * t + c, wy * wz * t + wx * s},
                {wz * wx * t + wy * s, wz * wy * t - wx * s, wz * wz * t + c}};

        yUnitVec = new Vector3(yx * rotMat[0][0] + yy * rotMat[0][1] + yz * rotMat[0][2],
                yx * rotMat[1][0] + yy * rotMat[1][1] + yz * rotMat[1][2],
                yx * rotMat[2][0] + yy * rotMat[2][1] + yz * rotMat[2][2]).normalized();


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

        logoAnchor = new AnchorNode(anchor);
        logoAnchor.setParent(arSceneView.getScene());

        // 윗벡터를 구해서 보내주기
        Vector3 v = new Vector3(0f, 0f, 1f);
        xPos = Vector3.dot(v, xUnitVec);
        yPos = Vector3.dot(v, yUnitVec);
        zPos = Vector3.dot(v, zUnitVec);

        xAxis = arSceneView.getScene().getCamera().getRight().normalized().scaled(xPos);
        yAxis = arSceneView.getScene().getCamera().getUp().normalized().scaled(yPos);
        zAxis = arSceneView.getScene().getCamera().getBack().normalized().scaled(zPos);

        Vector3 up = new Vector3(xAxis.x + yAxis.x + zAxis.x, xAxis.y + yAxis.y + zAxis.y, xAxis.z + yAxis.z + zAxis.z).normalized();

        BofLogo bofLogo = new BofLogo(logoAnchor, bofLogoRenderable, arSceneView);

        Snackbar.make(mLayout, "목적지 근처에 도착했습니다 (distance: " + distance + "m)", Snackbar.LENGTH_SHORT).show();
    }

    // 앨범 노드 생성~!
    public boolean createNode(int i) {
        float dLatitude = (float) (markers[i].getLatitude() - mCurrentLocation.getLatitude()) * 110900f;
        float dLongitude = (float) (markers[i].getLongitude() - mCurrentLocation.getLongitude()) * 88400f;

        // 테스트 용도


        if( i == 0 ) {
            dLatitude = 3f;
            dLongitude = 0f;
        }
        else if ( i == 1 ){
            dLatitude = -3f;
            dLongitude = 0f;
        }
        else{
            dLatitude = 0f;
            dLongitude = 3f;
        }

        float distance = (float) Math.sqrt((dLongitude * dLongitude) + (dLatitude * dLatitude));

        if (distance > 15) { // 15m보다 멀면 오브젝트 생성X
            return false;
        }
        float height = -0.5f;
        Vector3 objVec = new Vector3(dLongitude, dLatitude, height);

        Vector3 xUnitVec;
        Vector3 yUnitVec;
        Vector3 zUnitVec;

        zUnitVec = new Vector3((float) (Math.cos(mCurrentPitch) * Math.sin(mCurrentAzim)), (float) (Math.cos(mCurrentPitch) * Math.cos(mCurrentAzim)), (float) (-Math.sin(mCurrentPitch)));
        zUnitVec = zUnitVec.normalized().negated();

        yUnitVec = new Vector3((float) (Math.sin(mCurrentPitch) * Math.sin(mCurrentAzim)), (float) (Math.sin(mCurrentPitch) * Math.cos(mCurrentAzim)), (float) (Math.cos(mCurrentPitch))).normalized();

        float wx = zUnitVec.x;
        float wy = zUnitVec.y;
        float wz = zUnitVec.z;

        float yx = yUnitVec.x;
        float yy = yUnitVec.y;
        float yz = yUnitVec.z;

        float t = 1 - (float) Math.cos(mCurrentRoll);
        float s = (float) Math.sin(mCurrentRoll);
        float c = (float) Math.cos(mCurrentRoll);

        float[][] rotMat = {{wx * wx * t + c, wx * wy * t + wz * s, wx * wz * t - wy * s},
                {wy * wx * t - wz * s, wy * wy * t + c, wy * wz * t + wx * s},
                {wz * wx * t + wy * s, wz * wy * t - wx * s, wz * wz * t + c}};

        yUnitVec = new Vector3(yx * rotMat[0][0] + yy * rotMat[0][1] + yz * rotMat[0][2],
                yx * rotMat[1][0] + yy * rotMat[1][1] + yz * rotMat[1][2],
                yx * rotMat[2][0] + yy * rotMat[2][1] + yz * rotMat[2][2]).normalized();


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

        mAnchorNode[i] = new AnchorNode(anchor);
        mAnchorNode[i].setParent(arSceneView.getScene());

        // 윗벡터를 구해서 보내주기
        Vector3 v = new Vector3(0f, 0f, 1f);
        xPos = Vector3.dot(v, xUnitVec);
        yPos = Vector3.dot(v, yUnitVec);
        zPos = Vector3.dot(v, zUnitVec);

        xAxis = arSceneView.getScene().getCamera().getRight().normalized().scaled(xPos);
        yAxis = arSceneView.getScene().getCamera().getUp().normalized().scaled(yPos);
        zAxis = arSceneView.getScene().getCamera().getBack().normalized().scaled(zPos);

        Vector3 up = new Vector3(xAxis.x + yAxis.x + zAxis.x, xAxis.y + yAxis.y + zAxis.y, xAxis.z + yAxis.z + zAxis.z).normalized();

        AlbumNode albumNode = new AlbumNode(mAnchorNode[i], albumRenderable[i],
                timerArray, musicNotes, musicUiclass.getMediaPlayer(i), arSceneView);
        music(albumNode, i);

        int index = albumNode.getIndex();
        for(; albumNode.getTimer(index) < albumNode.getCurrentMediaPosition(); index++){
            ;
        }
        albumNode.setIndex(index);

        Snackbar.make(mLayout, "앨범이 근처에 있습니다 (distance: " + distance + "m)", Snackbar.LENGTH_SHORT).show();

        return true;
    }

    public void music(AlbumNode albumNode, int i) {
        Context c = this;

        albumNode.setOnTapListener((v, event) -> {
            // 디버깅용 터치하면 사라지게

            /* gps를 이용한 거리
            float dLatitude = (float) (markers[i].getLatitude() - mCurrentLocation.getLatitude()) * 110900f;
            float dLongitude = (float) (markers[i].getLongitude() - mCurrentLocation.getLongitude()) * 88400f;
            float distance = (float) Math.sqrt((dLongitude * dLongitude) + (dLatitude * dLatitude));
            */

            // AR자체의 world position을 이용한 거리
            Vector3 vec = Vector3.subtract(albumNode.getWorldPosition(), arSceneView.getScene().getCamera().getWorldPosition());
            float distance = (float) Math.sqrt(Vector3.dot(vec, vec));

            // 터치한 오브젝트와의 거리가 20m이내 일때만 터치 가능
            if (distance <= 20f) {
                if (musicUi.getVisibility() == View.INVISIBLE || musicUi.getVisibility() == View.GONE) {
                    musicUi.setVisibility(View.VISIBLE);
                }

                if (musicUiclass.isPlaying(i)) {
                    musicUiclass.musicStop();
                    albumNode.stopGame();
                    Snackbar.make(mLayout, "music stop (거리: " + distance + "m)", Snackbar.LENGTH_SHORT).show();
                }
                else {
                    musicUiclass.musicStop();
                    musicUiclass.setMediaPlayer(i);
                    musicUiclass.musicPlay();
                    albumNode.startGame();
                    Snackbar.make(mLayout, "music start (거리: " + distance + "m)", Snackbar.LENGTH_SHORT).show();
                }
            }
        });
    }

    public static float getMinScreenSize(){
        return Math.min(arSceneView.getWidth(), arSceneView.getHeight());
    }

    public static float getWidth(){
        return arSceneView.getWidth();
    }

    public static float getHeight(){
        return arSceneView.getHeight();
    }
}