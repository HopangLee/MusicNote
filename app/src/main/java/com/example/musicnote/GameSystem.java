package com.example.musicnote;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.Pose;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;

import java.sql.Time;
import java.util.Timer;
import java.util.TimerTask;

// 리듬 노드를 생성하는 GameSystem(Anchor node)
public class GameSystem extends AnchorNode {
    class NoteCreateTimer{
        float speed; // 노트의 움직이는 속도
        float scale; // 노트의 크기
        int score; // 노트 터치 성공당 점수
        int[] leftTimer; // 왼쪽에서 생성되는 노트의 생성 타이밍
        int[] rightTimer; // 오른쪽에서 생성되는 노트의 생성 타이밍

        NoteCreateTimer(int[][] timer, float speed, float scale, int score){
            this.leftTimer = timer[0];
            this.rightTimer = timer[1];
            this.speed = speed;
            this.scale = scale;
            this.score = score;
        }

        public void setScore(int score){
            this.score = score;
        }

        public void setScale(float scale){
            this.scale = scale;
        }

        public void setSpeed(float speed){
            this.speed = speed;
        }

        public void setTimer(int[][] timer){
            this.leftTimer = timer[0];
            this.rightTimer = timer[1];
        }

        public int getLeftTimer(int index){
            return leftTimer[index];
        }

        public int getRightTimer(int index){
            return rightTimer[index];
        }

        public int getLeftLength(){
            return leftTimer.length;
        }

        public int getRightLength(){
            return rightTimer.length;
        }
    }

    ArSceneView arSceneView;

    boolean isPlaying = false; // 게임이 진행 중인지

    MediaPlayer currentMediaPlayer; // 현재 흘러나오는 음악(터치한 오브젝트의 음악),
    // 노래 시간에 맞추기 위해서 필요? (아니면 그냥 일정 시간 마다 진행하는걸로 => 노래가 꺼져도 진행가능)
    int musicIndex = 0;
    MusicUi musicUi; // 현재 나오는 음악 정보(playing중인지) 및 index를 알기 위해

    int LeftTimerIndex = 0; // 왼쪽 노트 타이머 index
    int RightTimerIndex = 0; // 오른쪽 노트 타이머 index
    NoteCreateTimer musicCreater = null;

    int currentScore = 0; // 현재까지 얻은 점수

    final float DISTANCE = 15f; // 15m (얼마나 앞에서 생성되게 할 것인지)
    final int DELAY = 2000; // 생성되고 퍼펙트 존(터치시 점수를 얻는 구역)까지 오는 데 걸리는 시간 (ms)
    final float SPEED = (DISTANCE - 2) * 1000 / DELAY; // 노트의 이동 속도(m/s)

    final float SCALE = 1f;
    final int SCORE = 50;

    final float INTERVAL = 0.75f; // 0.75m

    final TextView textView;

    ModelRenderable blueRenderable;
    ModelRenderable redRenderable;
    ModelRenderable albumRenderable;
    ModelRenderable lineRenderable;
    Context context;

    // 곡 노트 타이밍 (왼쪽, 오른쪽) (ms) => [곡 인덱스][왼쪽, 오른쪽][노트 index] = 타이머
    final int[][][] NOTETIMER = {
            // 0번째 곡

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

            // 1번째 곡
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

            // 2번째 곡
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

    // 딜레이 고려
    private float time = 0;

    private Node line;

    GameSystem(Context context, ArSceneView arSceneView, MusicUi musicUi, TextView textView){
        // Setting
        this.context = context;
        this.arSceneView = arSceneView;
        this.musicUi = musicUi;
        this.textView = textView;

        // Create an ARCore Anchor at the position.

        this.setParent(arSceneView.getScene());

        setUpModel();
        SetPosition();

        // 아래 내용: 이래야만 onUpdate작동하는지 확인
        this.setRenderable(albumRenderable);
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

            // 왼쪽 노트 타이밍 계산
            if(LeftTimerIndex < musicCreater.getLeftLength() && musicCreater.getLeftTimer(LeftTimerIndex) < time){
                // GameNote 생성
                GameNote note = new GameNote(arSceneView, this, redRenderable, SPEED, DISTANCE, SCORE, false);

                LeftTimerIndex++;
            }

            // 오른쪽 노트 타이밍 계산
            if(RightTimerIndex < musicCreater.getRightLength() && musicCreater.getRightTimer(RightTimerIndex) < time){
                // GameNote 생성
                GameNote note = new GameNote(arSceneView, this, blueRenderable, SPEED, DISTANCE, SCORE, true);

                RightTimerIndex++;
            }

            /*
            // 왼쪽 노트 타이밍 계산
            if(LeftTimerIndex < musicCreater.getLeftLength() && musicCreater.getLeftTimer(LeftTimerIndex) <= currentMediaPlayer.getCurrentPosition() - DELAY){
                // GameNote 생성
                GameNote note = new GameNote(arSceneView, this, noteRenderable, SPEED, DISTANCE, SCORE, false);

                LeftTimerIndex++;
            }

            // 오른쪽 노트 타이밍 계산
            if(RightTimerIndex < musicCreater.getRightLength() && musicCreater.getRightTimer(RightTimerIndex) <= currentMediaPlayer.getCurrentPosition() - DELAY){
                // GameNote 생성
                GameNote note = new GameNote(arSceneView, this, noteRenderable, SPEED, DISTANCE, SCORE, true);

                RightTimerIndex++;
            }*/
        }
    }

    // 노드 생성 시작 (일정 시간 뒤에 생성되는 걸로)
    public void GameStart(){
        currentMediaPlayer = musicUi.getCurrentMediaPlayer();

        if(currentMediaPlayer == null){
            Log.e("ERROR: ", "currentMediaPlayer is null");
            return;
        }

        musicIndex = musicUi.getCurrentMediaPlayerIndex();
        LeftTimerIndex = 0;
        RightTimerIndex = 0;
        isPlaying = true;
        musicCreater = new NoteCreateTimer(NOTETIMER[musicIndex], SPEED, SCALE, SCORE);
        SetPosition();

        currentScore = 0;

        createLine();

        time = 0;
    }

    // 게임 정지
    public void GameStop(){
        isPlaying = false;
        LeftTimerIndex = 0;
        RightTimerIndex = 0;
        currentScore = 0;
        musicCreater = null;
        time = 0;

        removeLine();
    }

    // 게임 일시 정지
    public void GamePause(){
        isPlaying = !isPlaying;
        removeLine();
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
        /*
        position = Vector3.add(position, this.getLeft().scaled(0.25f));
        this.setWorldPosition(position);*/
    }

    // 왼쪽 노트와 오른쪽 노트의 생성 위치를 조정하여 반환 (0: 왼쪽, 1: 오른쪽)
    public Vector3 SetNotePosition(Vector3 up, Vector3 pos, boolean isRight){ // 안쓰는거
        Vector3 dirVec = new Vector3( pos.y * up.z - pos.z * up.y, pos.z * up.x - pos.x * up.z, pos.x * up.y - pos.y * up.x).normalized().scaled(INTERVAL);

        if(Vector3.equals(Vector3.cross(up, pos).normalized(), dirVec.normalized())){ // dirVec이 왼쪽을 가르키는 벡터라면
            Log.i("Debug Log: ", "Vector is Left");
        }
        else{ // 오른쪽을 가르키는 벡터라면
            dirVec = dirVec.negated();
            Log.i("Debug Log: ", "Vector is Right");
        }

        Vector3[] noteVector = new Vector3[2];

        // LocalPosition으로 return
        noteVector[0] = dirVec;
        noteVector[1] = dirVec.negated();

        if(isRight){
            return noteVector[1].scaled(0.4f);
        }
        else{
            return noteVector[0].scaled(1.5f);
        }
    }

    public Vector3 SetNotePosition(boolean isRight){
        if(isRight){
            return this.getRight().scaled(INTERVAL/2);
        }
        else{
            return this.getLeft().scaled(INTERVAL/2);
        }
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

        ModelRenderable.builder()
                .setSource(context, R.raw.boflogo)
                .build().thenAccept(renderable -> albumRenderable = renderable)
                .exceptionally(
                        throwable -> {
                            return null;
                        }
                );

        ModelRenderable.builder()
                .setSource(context, R.raw.line)
                .build().thenAccept(renderable -> lineRenderable = renderable)
                .exceptionally(
                        throwable -> {
                            return null;
                        }
                );
    }

    public void createLine(){
        line = new Node();
        line.setParent(this);
        line.setLocalScale(new Vector3(0.2f, 0.2f, 0.2f));
        line.setRenderable(lineRenderable);

        Camera camera = arSceneView.getScene().getCamera();

        Vector3 cameraPos = camera.getWorldPosition(); // 카메라 위치 받아옴
        Vector3 forward = camera.getForward(); // 핸드폰 앞 벡터 받아옴
        Vector3 up = line.getUp().normalized(); // 이걸로 해도 되는지 모르겠음 => 잘되네?

        // up vector를 법선벡터로 갖는 평면에 forward Vector 정사영구하기
        Vector3 upValue = new Vector3(up).scaled(Vector3.dot(up, forward));
        Vector3 systemPos = Vector3.subtract(forward, upValue).normalized().scaled(8f);
        Vector3 position = Vector3.add(cameraPos, systemPos);
        position = Vector3.add(position, line.getUp().scaled(-3.5f));

        line.setWorldPosition(position);

        Vector3 objPos = line.getWorldPosition();
        Vector3 objToCam = Vector3.subtract(cameraPos, objPos).negated();
        Quaternion direction = Quaternion.lookRotation(objToCam, up);
        line.setWorldRotation(direction);
    }

    public void removeLine(){
        this.removeChild(line);
        line.setParent(null);
    }
}
