package com.example.musicnote;

import android.preference.PreferenceActivity;
import android.util.Log;

import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.math.Vector3Evaluator;
import com.google.ar.sceneform.rendering.ModelRenderable;

public class GameNote extends Node {
    ArSceneView arSceneView; // 카메라 위치 알기 위함
    GameSystem gameSystem; // up Vector와 parnet의 위치 => SetNotePosition으로 위치 갱신
    int position; // 오른쪽 생성 노드인지 왼쪽 생성 노드인지 확인 (true => right, false => left)
    float speed; // 노트의 이동 속도
    int score;
    float distance = 0f;

    final float HEIGHT;

    GameNote(ArSceneView arSceneView, GameSystem gameSystem, ModelRenderable noteRenderable, float speed, float height, int score, int position){
        this.arSceneView = arSceneView;
        this.gameSystem = gameSystem;
        this.speed = speed;
        this.HEIGHT = height;
        this.score = score;
        this.position = position;

        this.setRenderable(noteRenderable);
        this.setLocalScale(new Vector3(0.65f, 0.45f, 0.65f));
        this.setParent(gameSystem);

        setPosition();

        // 오브젝트 카메라 바라보게 회전
        Vector3 cameraPos = arSceneView.getScene().getCamera().getWorldPosition();
        Vector3 objToCam = Vector3.subtract(cameraPos, gameSystem.getWorldPosition());
        Quaternion direction = Quaternion.lookRotation(objToCam, gameSystem.getUp());
        this.setWorldRotation(direction);

        this.setOnTapListener((v, event) ->{
            getScore();
        });
    }

    @Override
    public void onUpdate(FrameTime frameTime) {
        super.onUpdate(frameTime);

        float deltaTime = frameTime.getDeltaSeconds();

        setPosition(deltaTime);

        // 노래가 종료될 시에도 삭제 (어차피 게임중에는 pause 못하긴 할듯 => 그래서 그냥 노래 플레이중 아니면 삭제)
        if(!gameSystem.isPlaying) removeNote();


        if(distance > HEIGHT * 3f){
            removeNote();
        }

    }

    // 위치 조정
    public void setPosition(float deltaTime){
        Vector3 down = gameSystem.getDown();
        Vector3 parentPos = Vector3.add(gameSystem.getWorldPosition(), gameSystem.getUp().scaled(HEIGHT));
        Vector3 movePos = Vector3.add(parentPos, gameSystem.SetNotePosition(position));
        movePos = Vector3.add(movePos, down.scaled(distance));

        movePos = Vector3.add(movePos, gameSystem.getRight().scaled(0.25f));

        this.setWorldPosition(movePos);

        distance += deltaTime * speed;
    }

    public void setPosition(){
        Vector3 down = gameSystem.getDown();
        Vector3 parentPos = Vector3.add(gameSystem.getWorldPosition(), gameSystem.getUp().scaled(HEIGHT));
        Vector3 movePos = Vector3.add(parentPos, gameSystem.SetNotePosition(position));
        movePos = Vector3.add(movePos, down.scaled(distance));

        movePos = Vector3.add(movePos, gameSystem.getRight().scaled(0.25f));

        this.setWorldPosition(movePos);
    }

    public void getScore(){
        removeNote();
        gameSystem.getScore(score);
    }

    public void removeNote(){
        gameSystem.removeChild(this);
        this.setParent(null);
    }
}
