package com.example.musicnote;

import android.media.MediaPlayer;
import android.util.Log;

import com.google.ar.core.Anchor;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;

import java.util.ArrayList;
import java.util.List;

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
    }

    ArSceneView arSceneView;

    float[] orientation;

    boolean isPlaying = false; // 게임이 진행 중인지

    MediaPlayer cuurentMediaPlayer; // 현재 흘러나오는 음악(터치한 오브젝트의 음악),
    // 노래 시간에 맞추기 위해서 필요? (아니면 그냥 일정 시간 마다 진행하는걸로 => 노래가 꺼져도 진행가능)
    int musicIndex = 0;
    MusicUi musicUi; // 현재 나오는 음악 정보(playing중인지) 및 index를 알기 위해

    int LeftTimerIndex = 0; // 왼쪽 노트 타이머 index
    int RightTimerIndex = 0; // 오른쪽 노트 타이머 index
    NoteCreateTimer musicCreater = null;

    final float DISTANCE = 10f; // 10m (얼마나 앞에서 생성되게 할 것인지)
    final float SPEED = 3f; // 3(m/s)
    final float DELAY = DISTANCE / SPEED * 100; // 생성되고 퍼펙트 존(터치시 점수를 얻는 구역)까지 오는 데 걸리는 시간 (ms)

    final float SCALE = 1f;
    final int SCORE = 50;

    final float INTERVAL = 3f; // 3m

    // 곡 노트 타이밍 (왼쪽, 오른쪽) (ms) => [곡 인덱스][왼쪽, 오른쪽][노트 index] = 타이머
    final int[][][] NOTETIMER = {
            // 0번째 곡
            {{1000, 1100, 1300, 1500, 1700, 1900, 2000, 2100, 2300, 2500, 2700, 2900,
              3000, 3100, 3300, 3500, 3700, 3900, 4000, 4100, 4300, 4500, 4700, 4900,
              5000, 5100, 5300, 5500, 5700, 5900, 6000, 6100, 6300, 6500, 6700, 6900,
              7000, 7100, 7300, 7500, 7700, 7900, 8000, 8100, 8300, 8500, 8700, 8900,
              9000, 9100, 9300, 9500, 9700, 9900, 10000, 10100, 10300, 10500, 10700, 10900,
              11000, 11100, 11300, 11500, 11700, 11900, 12000}, // 왼쪽 노트
             {1000, 1200, 1400, 1600, 1800, 2000, 2200, 2400, 2600, 2800,
              3000, 3200, 3400, 3600, 3800, 4000, 4200, 4400, 4600, 4800,
              5000, 5200, 5400, 5600, 5800, 6000, 6200, 6400, 6600, 6800,
              7000, 7200, 7400, 7600, 7800, 8000, 8200, 8400, 8600, 8800,
              9000, 9200, 9400, 9600, 9800, 10000, 10200, 10400, 10600, 10800,
              11000, 11200, 11400, 11600, 11800, 12000}}, // 오른쪽 노트

            // 1번째 곡
            {{1000, 1100, 1300, 1500, 1700, 1900, 2000, 2100, 2300, 2500, 2700, 2900,
              3000, 3100, 3300, 3500, 3700, 3900, 4000, 4100, 4300, 4500, 4700, 4900,
              5000, 5100, 5300, 5500, 5700, 5900, 6000, 6100, 6300, 6500, 6700, 6900,
              7000, 7100, 7300, 7500, 7700, 7900, 8000, 8100, 8300, 8500, 8700, 8900,
              9000, 9100, 9300, 9500, 9700, 9900, 10000, 10100, 10300, 10500, 10700, 10900,
              11000, 11100, 11300, 11500, 11700, 11900, 12000}, // 왼쪽 노트
             {1000, 1200, 1400, 1600, 1800, 2000, 2200, 2400, 2600, 2800,
              3000, 3200, 3400, 3600, 3800, 4000, 4200, 4400, 4600, 4800,
              5000, 5200, 5400, 5600, 5800, 6000, 6200, 6400, 6600, 6800,
              7000, 7200, 7400, 7600, 7800, 8000, 8200, 8400, 8600, 8800,
              9000, 9200, 9400, 9600, 9800, 10000, 10200, 10400, 10600, 10800,
              11000, 11200, 11400, 11600, 11800, 12000}}, // 오른쪽 노트

            // 2번째 곡
            {{1000, 1100, 1300, 1500, 1700, 1900, 2000, 2100, 2300, 2500, 2700, 2900,
              3000, 3100, 3300, 3500, 3700, 3900, 4000, 4100, 4300, 4500, 4700, 4900,
              5000, 5100, 5300, 5500, 5700, 5900, 6000, 6100, 6300, 6500, 6700, 6900,
              7000, 7100, 7300, 7500, 7700, 7900, 8000, 8100, 8300, 8500, 8700, 8900,
              9000, 9100, 9300, 9500, 9700, 9900, 10000, 10100, 10300, 10500, 10700, 10900,
              11000, 11100, 11300, 11500, 11700, 11900, 12000}, // 왼쪽 노트
             {1000, 1200, 1400, 1600, 1800, 2000, 2200, 2400, 2600, 2800,
              3000, 3200, 3400, 3600, 3800, 4000, 4200, 4400, 4600, 4800,
              5000, 5200, 5400, 5600, 5800, 6000, 6200, 6400, 6600, 6800,
              7000, 7200, 7400, 7600, 7800, 8000, 8200, 8400, 8600, 8800,
              9000, 9200, 9400, 9600, 9800, 10000, 10200, 10400, 10600, 10800,
              11000, 11200, 11400, 11600, 11800, 12000}}, // 오른쪽 노트
    };

    GameSystem(ArSceneView arSceneView, MusicUi musicUi, float[] orientation){
        // Setting
        this.arSceneView = arSceneView;
        this.musicUi = musicUi;
        this.orientation = orientation; // [0]방위각, [1]피치, [2]롤
    }

    @Override
    public void onUpdate(FrameTime frameTime) {
        if (isPlaying){
            Vector3[] notePos = SetPosition(); // Game System의 위치를 핸드폰 앞으로 잡기 + 음악 노트 생성 위치 벡터 받아오기
            /*
             * 지정 시간이 되면 GameNote를 notePos위치에 생성(Local Position)
             * 아닐 때도 GameNote는 계속 움직여야함
             */

            if(cuurentMediaPlayer == null){
                Log.e("ERROR: ", "currentMediaPlayer is null");
                return;
            }

            // 왼쪽 노트 타이밍 계산
            if(musicCreater.getLeftTimer(LeftTimerIndex) >= cuurentMediaPlayer.getCurrentPosition()){
                // GameNote 생성
                GameNote note = new GameNote(arSceneView, this, false);
                LeftTimerIndex++;
            }

            // 오른쪽 노트 타이밍 계산
            if(musicCreater.getRightTimer(RightTimerIndex) >= cuurentMediaPlayer.getCurrentPosition()){
                // GameNote 생성
                GameNote note = new GameNote(arSceneView, this, true);
                RightTimerIndex++;
            }
        }
    }

    // 노드 생성 시작 (일정 시간 뒤에 생성되는 걸로)
    public void GameStart(){
        cuurentMediaPlayer = musicUi.getCurrentMediaPlayer();

        if(cuurentMediaPlayer == null){
            Log.e("ERROR: ", "currentMediaPlayer is null");
            return;
        }

        musicIndex = musicUi.getCurrentMediaPlayerIndex();
        LeftTimerIndex = 0;
        RightTimerIndex = 0;
        isPlaying = true;
        musicCreater = new NoteCreateTimer(NOTETIMER[musicIndex], SPEED, SCALE, SCORE);
        SetPosition();
    }

    // 게임 정지
    public void GameStop(){
        isPlaying = false;
        LeftTimerIndex = 0;
        RightTimerIndex = 0;
        musicCreater = null;
    }

    // 게임 일시 정지
    public void GamePause(){
        isPlaying = !isPlaying;
    }

    // Game System(this)의 위치 조정
    public Vector3[] SetPosition(){


        Camera camera = arSceneView.getScene().getCamera();

        Vector3 cameraPos = camera.getWorldPosition(); // 카메라 위치 받아옴
        Vector3 forward = camera.getForward(); // 핸드폰 앞 벡터 받아옴
        Vector3 up = getUpVector(); // up Vector를 받아옴

        // up vector를 법선벡터로 갖는 평면에 forward Vector 정사영구하기
        Vector3 upValue = new Vector3(up).scaled(Vector3.dot(up, forward));
        Vector3 systemPos = Vector3.subtract(forward, upValue).normalized();
        Vector3 position = Vector3.add(cameraPos, systemPos);

        this.setWorldPosition(position); // 위치 설정

        return SetNotePosition(up, systemPos);
    }

    // 왼쪽 노트와 오른쪽 노트의 생성 위치를 조정하여 반환 (0: 왼쪽, 1: 오른쪽)
    public Vector3[] SetNotePosition(Vector3 up, Vector3 pos){
        Vector3 dirVec = new Vector3( pos.y * up.z - pos.z * up.y, pos.z * up.x - pos.x * up.z, pos.x * up.y - pos.y * up.x).normalized().scaled(INTERVAL/2);

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

        return noteVector;
        /*
        noteVector[0] = Vector3.add(pos, dirVec); // 왼쪽 노트 생성 위치 벡터
        noteVector[1] = Vector3.add(pos, dirVec.negated()); // 오른쪽 노트 생성 위치 벡터

        return noteVector;
         * WorldPosition으로 return할 때
         */
    }

    public Vector3 getUpVector(){
        // up Vector 구하기
        float azim = orientation[0];
        float pitch = orientation[1];
        float roll = orientation[2];

        Log.i("orientation Debug: ", "azim: "+azim+", pitch: "+pitch+", roll: "+roll);

        Vector3 xUnitVec;
        Vector3 yUnitVec;
        Vector3 zUnitVec;

        zUnitVec = new Vector3((float) (Math.cos(pitch) * Math.sin(azim)), (float) (Math.cos(pitch) * Math.cos(azim)), (float) (-Math.sin(pitch)));
        zUnitVec = zUnitVec.normalized().negated();

        yUnitVec = new Vector3((float) (Math.sin(pitch) * Math.sin(azim)), (float) (Math.sin(pitch) * Math.cos(azim)), (float) (Math.cos(pitch))).normalized();

        float wx = zUnitVec.x;
        float wy = zUnitVec.y;
        float wz = zUnitVec.z;

        float yx = yUnitVec.x;
        float yy = yUnitVec.y;
        float yz = yUnitVec.z;

        float t = 1 - (float) Math.cos(roll);
        float s = (float) Math.sin(roll);
        float c = (float) Math.cos(roll);

        float[][] rotMat = {{wx * wx * t + c, wx * wy * t + wz * s, wx * wz * t - wy * s},
                {wy * wx * t - wz * s, wy * wy * t + c, wy * wz * t + wx * s},
                {wz * wx * t + wy * s, wz * wy * t - wx * s, wz * wz * t + c}};

        yUnitVec = new Vector3(yx * rotMat[0][0] + yy * rotMat[0][1] + yz * rotMat[0][2],
                yx * rotMat[1][0] + yy * rotMat[1][1] + yz * rotMat[1][2],
                yx * rotMat[2][0] + yy * rotMat[2][1] + yz * rotMat[2][2]).normalized();


        xUnitVec = Vector3.cross(yUnitVec, zUnitVec).normalized();

        Vector3 v = new Vector3(0f, 0f, 1f);
        float xPos = Vector3.dot(v, xUnitVec);
        float yPos = Vector3.dot(v, yUnitVec);
        float zPos = Vector3.dot(v, zUnitVec);

        Vector3 xAxis = arSceneView.getScene().getCamera().getRight().normalized().scaled(xPos);
        Vector3 yAxis = arSceneView.getScene().getCamera().getUp().normalized().scaled(yPos);
        Vector3 zAxis = arSceneView.getScene().getCamera().getBack().normalized().scaled(zPos);

        Vector3 up = new Vector3(xAxis.x + yAxis.x + zAxis.x, xAxis.y + yAxis.y + zAxis.y, xAxis.z + yAxis.z + zAxis.z).normalized();

        return up;
    }
}
