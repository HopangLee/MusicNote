package com.example.musicnote;

import android.util.Log;

import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.math.Vector3Evaluator;
import com.google.ar.sceneform.rendering.ModelRenderable;

public class GameNote extends Node {
    ArSceneView arSceneView; // 카메라 위치 알기 위함
    GameSystem gameSystem; // up Vector와 parnet의 위치 => SetNotePosition으로 위치 갱신
    boolean isRight; // 오른쪽 생성 노드인지 왼쪽 생성 노드인지 확인 (true => right, false => left)
    float speed; // 노트의 이동 속도
    float distance = 0f; // 앵커 노드에서 떨어진 거리
    float limitDistance; // 생성될 때 카메라에서 떨어진 거리
    int score;

    GameNote(ArSceneView arSceneView, GameSystem gameSystem, ModelRenderable noteRenderable, float speed, float distance, int score, boolean isRight){
        this.arSceneView = arSceneView;
        this.gameSystem = gameSystem;
        this.speed = speed;
        this.limitDistance = distance;
        this.score = score;
        this.isRight = isRight;

        this.setRenderable(noteRenderable);
        this.setLocalScale(new Vector3(1.7f, 1.7f, 1.7f));
        this.setParent(gameSystem);

        Vector3 up = this.getUp().normalized();
        Vector3 cameraPos = arSceneView.getScene().getCamera().getWorldPosition();
        Vector3 parentPos = gameSystem.getWorldPosition();
        Vector3 pos = Vector3.subtract(parentPos, cameraPos);
        Vector3 localPos = gameSystem.SetNotePosition(up, pos, isRight);

        this.setLocalPosition(localPos);

        /*
        // up vector를 법선벡터로 갖는 평면에 forward Vector 정사영구하기
        Vector3 upValue = new Vector3(up).scaled(Vector3.dot(up, forward));
        Vector3 systemPos = Vector3.subtract(forward, upValue).normalized().scaled(distance);
        Vector3 position = Vector3.add(cameraPos, systemPos);

        Vector3 worldPos = Vector3.add(position, localPos);

        this.setWorldPosition(worldPos); // 처음 위치 세팅
        */

        this.setOnTapListener((v, event) ->{
            getScore();
        });

        Log.i("GameNote create: ", "생성!");
    }

    @Override
    public void onUpdate(FrameTime frameTime) {
        super.onUpdate(frameTime);

        float deltaTime = frameTime.getDeltaSeconds();

        setPosition(deltaTime);

        // 뒤로 지나가서 거리가 너무 멀어지면 삭제
        if(distance >= limitDistance * 1.5f){
            removeNote();
        }

        // 노래가 종료될 시에도 삭제 (어차피 게임중에는 pause 못하긴 할듯 => 그래서 그냥 노래 플레이중 아니면 삭제)
        if(!gameSystem.isPlaying) removeNote();

    }

    // 위치 조정
    public void setPosition(float deltaTime){

        Camera camera = arSceneView.getScene().getCamera();

        Vector3 up = this.getUp().normalized();
        Vector3 camerPos = camera.getWorldPosition();
        Vector3 parentPos = gameSystem.getWorldPosition();
        Vector3 pos = Vector3.subtract(parentPos, camerPos);
        Vector3 localPos = gameSystem.SetNotePosition(up, pos, isRight);

        Vector3 forward = camera.getForward();
        Vector3 upValue = new Vector3(up).scaled(Vector3.dot(up, forward));
        Vector3 direction = Vector3.subtract(forward, upValue).normalized().negated();

        distance += speed * deltaTime;

        Vector3 v = this.getLocalPosition();
        float d = (float)Math.sqrt(Vector3.dot(v, v));
        Log.i("GameNote Update: ", "업데이트! " + distance +"(m), "+"Local distance: " +d);

        direction = direction.scaled(distance);

        Vector3 movePos = Vector3.add(gameSystem.getWorldPosition(), Vector3.add(direction, localPos));

        movePos = Vector3.add(movePos, up.scaled(-1.25f)); // 아래로 좀 내리기

        this.setWorldPosition(movePos);

        //this.setWorldPosition(Vector3.add(camerPos, forward.scaled(3f)));
    }

    public void getScore(){
        // 일정 거리보다 가깝다면
        if(limitDistance - 4f <= distance && distance <= limitDistance) { // 일단 타격 인정 범위
            removeNote();
            if(limitDistance - 2.5f <= distance && distance <= limitDistance - 1.5f) {
                gameSystem.getScore(score * 2);
            }
            else{
                gameSystem.getScore(score);
            }
        }
    }

    public void removeNote(){
        gameSystem.removeChild(this);
        this.setParent(null);
    }
}
