package com.example.musicnote;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.HitTestResult;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.math.Vector3Evaluator;
import com.google.ar.sceneform.rendering.ModelRenderable;

public class GameNote extends Node{
    ArSceneView arSceneView; // 카메라 위치 알기 위함
    GameSystem gameSystem; // up Vector와 parnet의 위치 => SetNotePosition으로 위치 갱신
    boolean isRight; // 오른쪽 생성 노드인지 왼쪽 생성 노드인지 확인 (true => right, false => left)
    float speed; // 노트의 이동 속도
    float distance = 0f; // 앵커 노드에서 떨어진 거리
    float limitDistance; // 생성될 때 카메라에서 떨어진 거리
    int score;

    float dragStartX = -1;
    float dragStartY = -1;
    float dragEndX;
    float dragEndY;

    MediaPlayer effectSound;

    GameNote(ArSceneView arSceneView, GameSystem gameSystem, ModelRenderable noteRenderable, float speed, float distance, int score, boolean isRight){
        this.arSceneView = arSceneView;
        this.gameSystem = gameSystem;
        this.speed = speed;
        this.limitDistance = distance;
        this.score = score;
        this.isRight = isRight;

        effectSound = MediaPlayer.create(gameSystem.context, R.raw.ui_menu_button_click_19);

        this.setLocalRotation(Quaternion.axisAngle(this.getUp(), -90));
        this.setRenderable(noteRenderable);
        this.setLocalScale(Vector3.one().scaled(gameSystem.getSCALE()));
        this.setParent(gameSystem);

        Vector3 localPos = gameSystem.SetNotePosition(isRight);

        this.setLocalPosition(localPos);

        this.setOnTapListener((v, event) ->{
            // getScore();
        });

        this.setOnTouchListener((v, event)->{
            if(event.getAction() == MotionEvent.ACTION_MOVE){
                getScore();
            }
            return super.onTouchEvent(v, event);
        });

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
        Vector3 cameraPos = camera.getWorldPosition();
        Vector3 parentPos = gameSystem.getWorldPosition();
        Vector3 localPos = gameSystem.SetNotePosition(isRight);

        Vector3 forward = camera.getForward();
        Vector3 upValue = new Vector3(up).scaled(Vector3.dot(up, forward));
        Vector3 direction = Vector3.subtract(forward, upValue).normalized().negated();

        distance += speed * deltaTime;

        Vector3 v = this.getLocalPosition();
        float d = (float)Math.sqrt(Vector3.dot(v, v));

        direction = direction.scaled(distance);

        Vector3 movePos = Vector3.add(gameSystem.getWorldPosition(), Vector3.add(direction, localPos));

        movePos = Vector3.add(movePos, up.scaled(-0.5f)); // 아래로 좀 내리기

        this.setWorldPosition(movePos);
    }

    public void getScore(){
        // 일정 거리보다 가깝다면
        float perfectLine = gameSystem.getZONEDISTANCE();

        if(limitDistance - (perfectLine + 1f) <= distance && distance <= limitDistance - (perfectLine - 1f)) { // 일단 타격 인정 범위
            effectSound.start();
            removeNote();
            if(limitDistance - (perfectLine + 0.5f) <= distance &&
                    distance <= limitDistance - (perfectLine - 0.5f)) {
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
