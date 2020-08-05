package com.example.musicnote;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.HitTestResult;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.collision.Ray;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 리듬 노드를 생성하는 GameSystem(Anchor node)
public class GameSystem extends AnchorNode{

    class NoteCreateTimer{
        float speed; // 노트의 움직이는 속도
        float scale; // 노트의 크기
        Note[] notes;

        NoteCreateTimer(Note[] notes, float speed, float scale){
            this.notes = notes;
            this.speed = speed;
            this.scale = scale;
        }

        public void setScale(float scale){
            this.scale = scale;
        }

        public void setSpeed(float speed){
            this.speed = speed;
        }

        public int getNoteTimer(int index){
            return notes[index].timer;
        }

        public int getDirection(int index){
            return notes[index].direction;
        }

        boolean isRight(int index){
            return notes[index].coordinate.x >= 0 ? true : false;
        }

        public int getNoteSize(){
            return notes.length;
        }

        public Coordinate getCoordinate(int index){
            return notes[index].coordinate;
        }
    }

    class Note{ // 시간 + 위치 + 방향을 담고 있는 클래스
        int timer;
        Coordinate coordinate;
        int direction; // 오른쪽(0), 오른쪽 아래(1), 아래(2), 왼쪽 아래(3), 왼쪽(4), 왼쪽 위(5), 위(6), 오른쪽 위(7)
        Vector3 pos; // parent(GameSystem)를 기준으로 한 로컬 벡터

        Note(){ }
        Note(int timer, Coordinate coordinate, int direction){
            this.timer = timer;
            this.coordinate = coordinate;
            this.direction = direction;
        }
    }

    final GameSystem gameSystem = this;

    ArSceneView arSceneView;

    boolean isPlaying = false; // 게임이 진행 중인지

    MediaPlayer currentMediaPlayer; // 현재 흘러나오는 음악(터치한 오브젝트의 음악),
    // 노래 시간에 맞추기 위해서 필요? (아니면 그냥 일정 시간 마다 진행하는걸로 => 노래가 꺼져도 진행가능)
    int musicIndex = 0;
    MusicUi musicUi; // 현재 나오는 음악 정보(playing중인지) 및 index를 알기 위해

    int currentIndex = 0;
    NoteCreateTimer musicCreater = null;

    int currentScore = 0; // 현재까지 얻은 점수

    final float DISTANCE = 15f; // 15m (얼마나 앞에서 생성되게 할 것인지)
    final int DELAY = 2000; // 생성되고 퍼펙트 존(터치시 점수를 얻는 구역)까지 오는 데 걸리는 시간 (ms)
    final float ZONEDISTANCE = 1.75f; // 퍼펙트 존 거리
    final float SPEED = (DISTANCE - ZONEDISTANCE) * 1000 / DELAY; // 노트의 이동 속도(m/s)

    final float SCALE = 0.5f;
    final int SCORE = 50;

    final float INTERVAL = 0.6f; // 0.6m

    // 터치 관련
    class Coordinate{
        Coordinate(float x, float y){
            this.x = x;
            this.y = y;
        }
        float x;
        float y;
    }
    class Touch{
        List<Coordinate> points;

        // 오른쪽(0), 오른쪽 아래(1), 아래(2), 왼쪽 아래(3), 왼쪽(4), 왼쪽 위(5), 위(6), 오른쪽 위(7)
        int direction;

        Touch(){
            points = new ArrayList<Coordinate>();
            direction = -1;
        }
    }
    Map<Integer, Touch> touchs = new HashMap<>();
    final float minDistance; // 드래그의 최소 거리

    // 점수 관련
    final TextView textView;

    ModelRenderable blueRenderable;
    ModelRenderable redRenderable;

    Context context;

    // 쓰기 쉽게 아래에 자주 쓰이는 좌표 나열
    final Coordinate RIGHT = new Coordinate(INTERVAL/2, 0);
    final Coordinate LEFT = new Coordinate(-INTERVAL/2, 0);
    final Coordinate RIGHTUP = new Coordinate(INTERVAL/2, INTERVAL/3);
    final Coordinate RIGHTDOWN = new Coordinate(INTERVAL/2, -INTERVAL/3);
    final Coordinate LEFTUP = new Coordinate(-INTERVAL/2, INTERVAL/3);
    final Coordinate LEFTDOWN = new Coordinate(-INTERVAL/2, -INTERVAL/3);

    final int mR = 0, mRD = 1, mD = 2, mLD = 3, mL = 4, mLU = 5, mU = 6, mRU = 7;

    // 새롭게 바뀐 곡 노트 타이밍
    Note[][] NOTES = {
        // 0번째 곡
        {       new Note(1000, RIGHT, mU), new Note(2000, LEFT, mU),
                new Note(3000, RIGHT, mD), new Note(4000, LEFT, mD),
                new Note(5000, RIGHT, mR), new Note(6000, LEFT, mL),
                new Note(7000, RIGHTUP, mRU), new Note(8000, LEFTUP, mLU),
                new Note(9000, RIGHTDOWN, mRD), new Note(10000, LEFTDOWN, mLD),
                new Note(11000, RIGHTUP, mR), new Note(12000, LEFTUP, mL),
                new Note(13000, RIGHT, mR), new Note(14000, LEFT, mL),
                new Note(15000, RIGHTDOWN, mR), new Note(16000, LEFTDOWN, mL),
                new Note(17000, RIGHT, mU), new Note(18000, LEFT, mU),
                new Note(19000, RIGHT, mD), new Note(20000, LEFT, mD),
                new Note(21000, RIGHT, mR), new Note(22000, LEFT, mL),
                new Note(23000, RIGHTUP, mRU), new Note(24000, LEFTUP, mLU),
                new Note(25000, RIGHTDOWN, mRD), new Note(26000, LEFTDOWN, mLD),
                new Note(27000, RIGHTUP, mR), new Note(28000, LEFTUP, mL),
                new Note(29000, RIGHT, mR), new Note(30000, LEFT, mL),
                new Note(31000, RIGHTDOWN, mR), new Note(32000, LEFTDOWN, mL),
                new Note(33000, RIGHT, mU), new Note(34000, LEFT, mU),
                new Note(35000, RIGHT, mD), new Note(36000, LEFT, mD),
                new Note(37000, RIGHT, mR), new Note(38000, LEFT, mL),
                new Note(39000, RIGHTUP, mRU), new Note(40000, LEFTUP, mLU),
                new Note(41000, RIGHTDOWN, mRD), new Note(42000, LEFTDOWN, mLD),
                new Note(43000, RIGHTUP, mR), new Note(44000, LEFTUP, mL),
                new Note(45000, RIGHT, mR), new Note(46000, LEFT, mL),
                new Note(47000, RIGHTDOWN, mR), new Note(48000, LEFTDOWN, mL),
                new Note(50000, RIGHTUP, mR), new Note(50000, LEFTDOWN, mL),
                new Note(52000, RIGHT, mU), new Note(52000, LEFT, mU),
                new Note(54000, RIGHT, mD), new Note(54000, LEFT, mD),
                new Note(55000, RIGHT, mU), new Note(55000, LEFT, mU),
                new Note(56000, RIGHT, mD), new Note(56000, LEFT, mD),
                new Note(57000, RIGHT, mU), new Note(57000, LEFT, mU),
                new Note(58000, RIGHT, mD), new Note(58000, LEFT, mD),
                new Note(59000, RIGHT, mU), new Note(59000, LEFT, mU),
                new Note(60000, RIGHT, mD), new Note(60000, LEFT, mD),
                new Note(62000, RIGHTUP, mU), new Note(62000, LEFTUP, mU),
                new Note(62500, RIGHT, mU), new Note(62500, LEFT, mU),
                new Note(63000, RIGHTDOWN, mU), new Note(63000, LEFTDOWN, mU),
                new Note(65000, RIGHTDOWN, mD), new Note(65000, LEFTDOWN, mD),
                new Note(65500, RIGHT, mD), new Note(65500, LEFT, mD),
                new Note(66000, RIGHTUP, mD), new Note(66000, LEFTUP, mD),
                new Note(67000, RIGHTUP, mRU), new Note(67000, LEFTUP, mLU),
                new Note(68000, RIGHTDOWN, mRD), new Note(68000, LEFTDOWN, mLD)
                },
        // 1번째 곡
        {       new Note(1000, RIGHT, mU), new Note(2000, LEFT, mU),
                new Note(3000, RIGHT, mD), new Note(4000, LEFT, mD),
                new Note(5000, RIGHT, mR), new Note(6000, LEFT, mL),
                new Note(7000, RIGHTUP, mRU), new Note(8000, LEFTUP, mLU),
                new Note(9000, RIGHTDOWN, mRD), new Note(10000, LEFTDOWN, mLD),
                new Note(11000, RIGHTUP, mR), new Note(12000, LEFTUP, mL),
                new Note(13000, RIGHT, mR), new Note(14000, LEFT, mL),
                new Note(15000, RIGHTDOWN, mR), new Note(16000, LEFTDOWN, mL),
                new Note(17000, RIGHT, mU), new Note(18000, LEFT, mU),
                new Note(19000, RIGHT, mD), new Note(20000, LEFT, mD),
                new Note(21000, RIGHT, mR), new Note(22000, LEFT, mL),
                new Note(23000, RIGHTUP, mRU), new Note(24000, LEFTUP, mLU),
                new Note(25000, RIGHTDOWN, mRD), new Note(26000, LEFTDOWN, mLD),
                new Note(27000, RIGHTUP, mR), new Note(28000, LEFTUP, mL),
                new Note(29000, RIGHT, mR), new Note(30000, LEFT, mL),
                new Note(31000, RIGHTDOWN, mR), new Note(32000, LEFTDOWN, mL),
                new Note(33000, RIGHT, mU), new Note(34000, LEFT, mU),
                new Note(35000, RIGHT, mD), new Note(36000, LEFT, mD),
                new Note(37000, RIGHT, mR), new Note(38000, LEFT, mL),
                new Note(39000, RIGHTUP, mRU), new Note(40000, LEFTUP, mLU),
                new Note(41000, RIGHTDOWN, mRD), new Note(42000, LEFTDOWN, mLD),
                new Note(43000, RIGHTUP, mR), new Note(44000, LEFTUP, mL),
                new Note(45000, RIGHT, mR), new Note(46000, LEFT, mL),
                new Note(47000, RIGHTDOWN, mR), new Note(48000, LEFTDOWN, mL),
                new Note(50000, RIGHTUP, mR), new Note(50000, LEFTDOWN, mL),
                new Note(52000, RIGHT, mU), new Note(52000, LEFT, mU),
                new Note(54000, RIGHT, mD), new Note(54000, LEFT, mD),
                new Note(55000, RIGHT, mU), new Note(55000, LEFT, mU),
                new Note(56000, RIGHT, mD), new Note(56000, LEFT, mD),
                new Note(57000, RIGHT, mU), new Note(57000, LEFT, mU),
                new Note(58000, RIGHT, mD), new Note(58000, LEFT, mD),
                new Note(59000, RIGHT, mU), new Note(59000, LEFT, mU),
                new Note(60000, RIGHT, mD), new Note(60000, LEFT, mD),
                new Note(62000, RIGHTUP, mU), new Note(62000, LEFTUP, mU),
                new Note(62500, RIGHT, mU), new Note(62500, LEFT, mU),
                new Note(63000, RIGHTDOWN, mU), new Note(63000, LEFTDOWN, mU),
                new Note(65000, RIGHTDOWN, mD), new Note(65000, LEFTDOWN, mD),
                new Note(65500, RIGHT, mD), new Note(65500, LEFT, mD),
                new Note(66000, RIGHTUP, mD), new Note(66000, LEFTUP, mD),
                new Note(67000, RIGHTUP, mRU), new Note(67000, LEFTUP, mLU),
                new Note(68000, RIGHTDOWN, mRD), new Note(68000, LEFTDOWN, mLD)},
        // 2번째 곡
        {       new Note(1000, RIGHT, mU), new Note(2000, LEFT, mU),
                new Note(3000, RIGHT, mD), new Note(4000, LEFT, mD),
                new Note(5000, RIGHT, mR), new Note(6000, LEFT, mL),
                new Note(7000, RIGHTUP, mRU), new Note(8000, LEFTUP, mLU),
                new Note(9000, RIGHTDOWN, mRD), new Note(10000, LEFTDOWN, mLD),
                new Note(11000, RIGHTUP, mR), new Note(12000, LEFTUP, mL),
                new Note(13000, RIGHT, mR), new Note(14000, LEFT, mL),
                new Note(15000, RIGHTDOWN, mR), new Note(16000, LEFTDOWN, mL),
                new Note(17000, RIGHT, mU), new Note(18000, LEFT, mU),
                new Note(19000, RIGHT, mD), new Note(20000, LEFT, mD),
                new Note(21000, RIGHT, mR), new Note(22000, LEFT, mL),
                new Note(23000, RIGHTUP, mRU), new Note(24000, LEFTUP, mLU),
                new Note(25000, RIGHTDOWN, mRD), new Note(26000, LEFTDOWN, mLD),
                new Note(27000, RIGHTUP, mR), new Note(28000, LEFTUP, mL),
                new Note(29000, RIGHT, mR), new Note(30000, LEFT, mL),
                new Note(31000, RIGHTDOWN, mR), new Note(32000, LEFTDOWN, mL),
                new Note(33000, RIGHT, mU), new Note(34000, LEFT, mU),
                new Note(35000, RIGHT, mD), new Note(36000, LEFT, mD),
                new Note(37000, RIGHT, mR), new Note(38000, LEFT, mL),
                new Note(39000, RIGHTUP, mRU), new Note(40000, LEFTUP, mLU),
                new Note(41000, RIGHTDOWN, mRD), new Note(42000, LEFTDOWN, mLD),
                new Note(43000, RIGHTUP, mR), new Note(44000, LEFTUP, mL),
                new Note(45000, RIGHT, mR), new Note(46000, LEFT, mL),
                new Note(47000, RIGHTDOWN, mR), new Note(48000, LEFTDOWN, mL),
                new Note(50000, RIGHTUP, mR), new Note(50000, LEFTDOWN, mL),
                new Note(52000, RIGHT, mU), new Note(52000, LEFT, mU),
                new Note(54000, RIGHT, mD), new Note(54000, LEFT, mD),
                new Note(55000, RIGHT, mU), new Note(55000, LEFT, mU),
                new Note(56000, RIGHT, mD), new Note(56000, LEFT, mD),
                new Note(57000, RIGHT, mU), new Note(57000, LEFT, mU),
                new Note(58000, RIGHT, mD), new Note(58000, LEFT, mD),
                new Note(59000, RIGHT, mU), new Note(59000, LEFT, mU),
                new Note(60000, RIGHT, mD), new Note(60000, LEFT, mD),
                new Note(62000, RIGHTUP, mU), new Note(62000, LEFTUP, mU),
                new Note(62500, RIGHT, mU), new Note(62500, LEFT, mU),
                new Note(63000, RIGHTDOWN, mU), new Note(63000, LEFTDOWN, mU),
                new Note(65000, RIGHTDOWN, mD), new Note(65000, LEFTDOWN, mD),
                new Note(65500, RIGHT, mD), new Note(65500, LEFT, mD),
                new Note(66000, RIGHTUP, mD), new Note(66000, LEFTUP, mD),
                new Note(67000, RIGHTUP, mRU), new Note(67000, LEFTUP, mLU),
                new Note(68000, RIGHTDOWN, mRD), new Note(68000, LEFTDOWN, mLD)}
    };

/*
    // 곡 노트 타이밍 (왼쪽, 오른쪽) (ms) => [곡 인덱스][왼쪽, 오른쪽][노트 index] = 타이머
    final int[][][] NOTETIMER = {
            // 0번째 곡
            // 수고가 많습니다.. 총총 ㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋ뭐야 언제썼어
            {{1000, 3000, 5000, 7000, 9000, 10000, 11000, 13000, 15000, 17000, 19000,
              20000, 21000, 23000, 25000, 27000, 29000, 30000, 31000, 33000, 35000, 37000, 39000,
              40000, 41000, 43000, 45000, 47000, 49000, 50000, 51000, 53000, 55000, 57000, 59000,
              60000, 61000, 63000, 65000, 67000, 69000, 70000, 71000, 73000, 75000, 77000, 79000,
              80000, 81000, 83000, 85000, 87000, 89000, 90000, 91000, 93000, 95000, 97000, 99000,
              100000, 101000, 103000, 105000, 107000, 109000, 110000, 111000, 113000, 115000, 117000, 119000,
              120000}, // 왼쪽 노트
             {2000, 4000, 6000, 8000, 10000, 12000, 14000, 16000, 18000, 20000,
              22000, 24000, 26000, 28000, 30000, 32000, 34000, 36000, 38000, 40000,
              42000, 44000, 46000, 48000, 50000, 52000, 54000, 56000, 58000, 60000,
              62000, 64000, 66000, 68000, 70000, 72000, 74000, 76000, 78000, 80000,
              82000, 84000, 86000, 88000, 90000, 92000, 94000, 96000, 98000, 100000,
              102000, 104000, 106000, 108000, 110000, 112000, 114000, 116000, 118000, 120000}}, // 오른쪽 노트
    };
*/
    // 딜레이 고려
    private float time = 0;

    GameSystem(Context context, ArSceneView arSceneView, MusicUi musicUi, TextView textView){
        // Setting
        this.context = context;
        this.arSceneView = arSceneView;
        this.musicUi = musicUi;
        this.textView = textView;

        minDistance = Math.min(arSceneView.getWidth(), arSceneView.getHeight()) / 6f;

        arSceneView.setOnTouchListener(this::onTouch); // 실험 -> 오 잘된다 레전드

        // Create an ARCore Anchor at the position.
        this.setParent(arSceneView.getScene());

        setUpModel();
        SetPosition();

        // 아래 내용: 이래야만 onUpdate작동하는지 확인
        this.setLocalScale(new Vector3(0.5f, 0.5f , 0.5f));

        // 오브젝트 카메라 바라보게 회전
        Vector3 cameraPos = arSceneView.getScene().getCamera().getWorldPosition();
        Vector3 objPos = this.getWorldPosition();
        Vector3 objToCam = Vector3.subtract(cameraPos, objPos).negated();
        Vector3 up = this.getUp();
        Quaternion direction = Quaternion.lookRotation(objToCam, up);
        this.setWorldRotation(direction);
    }

    @Override
    public void onUpdate(FrameTime frameTime) {
        super.onUpdate(frameTime);

        time += frameTime.getDeltaSeconds() * 1000;

        SetPosition(); // Game System의 위치를 핸드폰 앞으로 잡기
        if (isPlaying){

            if(currentMediaPlayer == null){
                Log.e("ERROR: ", "currentMediaPlayer is null");
                return;
            }

            for(; currentIndex < musicCreater.getNoteSize() && musicCreater.getNoteTimer(currentIndex) < time; currentIndex++) {
                int direction = musicCreater.getDirection(currentIndex);
                Coordinate coordinate = musicCreater.getCoordinate(currentIndex);

                if (musicCreater.isRight(currentIndex)) {
                    GameNote note = new GameNote(arSceneView, this, blueRenderable, SPEED, DISTANCE, SCORE, coordinate, direction);
                }
                else {
                    GameNote note = new GameNote(arSceneView, this, redRenderable, SPEED, DISTANCE, SCORE, coordinate, direction);
                }
            }
        }
    }

    // 노드 생성 시작 (일정 시간 뒤에 생성되는 걸로)
    public void GameStart(){
        currentMediaPlayer = musicUi.getCurrentMediaPlayer();

        if(currentMediaPlayer == null){
            Log.e("ERROR: ", "currentMediaPlayer is null");
            return;
        }

        textView.setVisibility(View.VISIBLE);

        musicIndex = musicUi.getCurrentMediaPlayerIndex();
        currentIndex = 0;
        isPlaying = true;
        musicCreater = new NoteCreateTimer(NOTES[musicIndex], SPEED, SCALE);
        SetPosition();

        currentScore = 0;
        time = 0;
    }

    // 게임 정지
    public void GameStop(){
        textView.setVisibility(View.GONE);

        isPlaying = false;
        currentIndex = 0;
        currentScore = 0;
        musicCreater = null;
        time = 0;
    }

    // 게임 일시 정지
    public void GamePause(){
        isPlaying = !isPlaying;
    }

    // Game System(this)의 위치 조정
    public void SetPosition(){
        Camera camera = arSceneView.getScene().getCamera();

        Vector3 cameraPos = camera.getWorldPosition(); // 카메라 위치 받아옴
        Vector3 forward = camera.getForward(); // 핸드폰 앞 벡터 받아옴
        Vector3 up = this.getUp().normalized(); // 이걸로 해도 되는지 모르겠음 => 잘되네?

        // up vector를 법선벡터로 갖는 평면에 forward Vector 정사영구하기
        Vector3 upValue = new Vector3(up).scaled(Vector3.dot(up, forward));
        Vector3 systemPos = Vector3.subtract(forward, upValue).normalized().scaled(DISTANCE);
        Vector3 position = Vector3.add(cameraPos, systemPos);

        this.setWorldPosition(position); // 위치 설정

        // 오브젝트 카메라 바라보게 회전
        Vector3 objPos = this.getWorldPosition();
        Vector3 objToCam = Vector3.subtract(cameraPos, objPos).negated();
        Quaternion direction = Quaternion.lookRotation(objToCam, up);
        this.setWorldRotation(direction);
    }

    public boolean onTouch(View view, MotionEvent motionEvent) {
        boolean ret = false;
        int touch_count = motionEvent.getPointerCount();
        if(touch_count > 2) touch_count = 2; // 2개 이상의 포인트를 터치했어도 2개만 본다.

        final int action = motionEvent.getAction();
        int key;

        switch(action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: //한 개 포인트에 대한 DOWN을 얻을 때.
                //Log.i("디버그: ", " 터치 다운");
                key = motionEvent.getPointerId(0);
                touchs.put(key, new Touch());
                touchs.get(key).points.add(new Coordinate(motionEvent.getX(), motionEvent.getY()));
                ret = true;
                break;

            case MotionEvent.ACTION_POINTER_DOWN: //두 개 이상의 포인트에 대한 DOWN을 얻을 때.
                //Log.i("디버그: ", " 터치 다운 (2개)");
                for(int i = 0; i < touch_count; i++){
                    key = motionEvent.getPointerId(i);
                    touchs.put(key, new Touch());
                    touchs.get(key).points.add(new Coordinate(motionEvent.getX(i), motionEvent.getY(i)));
                }
                ret = true;
                break;

            case MotionEvent.ACTION_MOVE:
                //Log.i("디버그: ", " 터치 무브");
                for(int i = 0; i < touch_count; i++){
                    key = motionEvent.getPointerId(i);
                    touchs.get(key).points.add(new Coordinate(motionEvent.getX(i), motionEvent.getY(i)));
                    checkDirection(key);
                }
                ret = true;
                break;

            case MotionEvent.ACTION_UP:
                //Log.i("디버그: ", " 터치 업");
                for(int i = 0; i < touch_count; i++){
                    key = motionEvent.getPointerId(i);
                    touchs.remove(key);
                }
                break;
        }
        arSceneView.onTouchEvent(motionEvent);
        return ret;
    }

    // 해당 터치의 연속된 기록이 어떤 방향을 나타내고 있는지 => 이상하면 가장 오래된 기록 삭제
    public void checkDirection(int key){
        List<Coordinate> coordinates = touchs.get(key).points;
        int size = coordinates.size();
        Coordinate startPoint = coordinates.get(0);
        Coordinate endPoint = coordinates.get(size - 1);

        float x = endPoint.x - startPoint.x;
        float y = endPoint.y - startPoint.y;

        float distance = (float)Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));

        if(distance >= minDistance){
            // touch의 방향 확인
            float theta = (float)Math.atan2(y, x);
            if(theta < 0) theta += (float)Math.PI * 2;
            if(0 <= theta && theta < Math.PI/8 || Math.PI/8 + Math.PI/4 * 7 <= theta && theta < Math.PI*2){
                // 오른쪽
                touchs.get(key).direction = 0;
            }
            else if(Math.PI/8 <= theta && theta < Math.PI/8 + Math.PI/4){
                // 오른쪽 아래
                touchs.get(key).direction = 1;
            }
            else if(Math.PI/8 + Math.PI/4 <= theta && theta < Math.PI/8 + Math.PI/4*2){
                // 아래
                touchs.get(key).direction = 2;
            }
            else if(Math.PI/8 + Math.PI/4*2 <= theta && theta < Math.PI/8 + Math.PI/4*3){
                // 왼쪽 아래
                touchs.get(key).direction = 3;
            }
            else if(Math.PI/8 + Math.PI/4*3 <= theta && theta < Math.PI/8 + Math.PI/4*4){
                // 왼쪽
                touchs.get(key).direction = 4;
            }
            else if(Math.PI/8 + Math.PI/4*4 <= theta && theta < Math.PI/8 + Math.PI/4*5){
                // 왼쪽 위
                touchs.get(key).direction = 5;
            }
            else if(Math.PI/8 + Math.PI/4*5 <= theta && theta < Math.PI/8 + Math.PI/4*6){
                // 위
                touchs.get(key).direction = 6;
            }
            else if(Math.PI/8 + Math.PI/4*6 <= theta && theta < Math.PI/8 + Math.PI/4*7){
                // 오른쪽 위
                touchs.get(key).direction = 7;
            }

            checkCollision(key, startPoint, endPoint);
            // 디버그

            String str = "";
            switch(touchs.get(key).direction){
                case 0:
                    str = "오른쪽";
                   break;
                case 1:
                    str = "오른쪽 아래";
                    break;
                case 2:
                    str = "아래";
                    break;
                case 3:
                    str = "왼쪽 아래";
                    break;
                case 4:
                    str = "왼쪽";
                    break;
                case 5:
                    str = "왼쪽 위";
                    break;
                case 6:
                    str = "위";
                    break;
                case 7:
                    str = "오른쪽 위";
                    break;
                default:
                    str = "기본";
            }
            Log.i("디버그: ", " 드래그 -> "+ str);

            while(distance >= minDistance){
                coordinates.remove(0);
                int tsize = coordinates.size();

                if(tsize == 0) break;

                Coordinate tempStart = coordinates.get(0);
                Coordinate tempEnd = coordinates.get(tsize - 1);

                float tx = tempEnd.x - tempStart.x;
                float ty = tempEnd.y - tempStart.y;

                distance = (float)Math.sqrt(Math.pow(tx, 2) + Math.pow(ty, 2));
            }
        }
        else{
            return;
        }
    }

    // 충돌 감지
    public void checkCollision(int key, Coordinate start, Coordinate end){
        Camera camera = arSceneView.getScene().getCamera();
        Vector3 forward = camera.getForward();

        // 1번째 생각 스크린 포인트를 Ray로

        Ray startRay = camera.screenPointToRay(start.x, start.y);
        Ray endRay = camera.screenPointToRay(end.x, end.y);

        for(int i = 0; i < 13; i++){
            Vector3 startPoint = startRay.getPoint(ZONEDISTANCE - 1.25f + 0.3f * i);
            Vector3 endPoint = endRay.getPoint(ZONEDISTANCE - 1.25f + 0.3f * i);
            Vector3 direction = Vector3.subtract(endPoint, startPoint).normalized();
            startPoint = Vector3.add(startPoint, direction.negated().scaled(0.1f));

            Ray ray = new Ray(startPoint, direction);
            List<HitTestResult> hits = arSceneView.getScene().hitTestAll(ray);
            for(int j = 0; j < hits.size(); j++){
                if(hits.get(j).getDistance() <= minDistance * 2f){
                    Node n = hits.get(j).getNode();
                    if(n instanceof GameNote){
                        ((GameNote) n).getScore(touchs.get(key).direction);
                    }
                }
            }
        }

    }

    Vector3 getPosVector(Coordinate coordinate){
        Vector3 up = gameSystem.getUp();
        Vector3 right = gameSystem.getRight();

        Vector3 pos = Vector3.add(right.scaled(coordinate.x), up.scaled(coordinate.y));

        return pos;
    }

    public void getScore(int score){
        currentScore += score;
        textView.setText(Integer.toString(currentScore));
        Log.i("Time: ", currentMediaPlayer.getCurrentPosition()+"ms, "+"Time: "+time);
    }

    public int getDELAY(){
        return DELAY;
    }

    public void setUpModel(){
        ModelRenderable.builder()
                .setSource(context, R.raw.blueblock)
                .build().thenAccept(renderable -> blueRenderable = renderable)
                .exceptionally(
                        throwable -> {
                            return null;
                        }
                );
        ModelRenderable.builder()
                .setSource(context, R.raw.redblock)
                .build().thenAccept(renderable -> redRenderable = renderable)
                .exceptionally(
                        throwable -> {
                            return null;
                        }
                );
    }


    public float getSCALE(){
        return SCALE;
    }

    public float getZONEDISTANCE(){
        return ZONEDISTANCE;
    }
}
