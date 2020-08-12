package com.example.musicnote;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

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
    float speed; // 노트의 이동 속도
    float distance = 0f; // 앵커 노드에서 떨어진 거리
    float limitDistance; // 생성될 때 카메라에서 떨어진 거리
    int score;

    // 오른쪽(0), 오른쪽 아래(1), 아래(2), 왼쪽 아래(3), 왼쪽(4), 왼쪽 위(5), 위(6), 오른쪽 위(7)
    int DIRECTION = -1; // 기본(-1)

    MediaPlayer effectSound;
    GameSystem.Coordinate coordinate;

    GameNote(ArSceneView arSceneView, GameSystem gameSystem, ModelRenderable noteRenderable, float speed, float distance, int score, GameSystem.Coordinate coordinate, int DIRECTION){
        this.arSceneView = arSceneView;
        this.gameSystem = gameSystem;
        this.speed = speed;
        this.limitDistance = distance;
        this.score = score;
        this.coordinate = coordinate;
        this.DIRECTION = DIRECTION;

        effectSound = MediaPlayer.create(gameSystem.context, R.raw.ui_menu_button_click_19);

        Quaternion rotation = Quaternion.axisAngle(this.getUp(), -90);
        //Quaternion rotation = Quaternion.axisAngle(this.getUp(), 0);

        switch (DIRECTION){
            case 0: // 오른쪽
                rotation = Quaternion.multiply(Quaternion.axisAngle(this.getForward(), +90), rotation);
                break;
            case 1: // 오른쪽 아래
                rotation = Quaternion.multiply(Quaternion.axisAngle(this.getForward(), 90 + 45), rotation);
                break;
            case 2: // 아래
                rotation = Quaternion.multiply(Quaternion.axisAngle(this.getForward(), 180), rotation);
                break;
            case 3: // 왼쪽 아래
                rotation = Quaternion.multiply(Quaternion.axisAngle(this.getForward(), -90 - 45), rotation);
                break;
            case 4: // 왼쪽
                rotation = Quaternion.multiply(Quaternion.axisAngle(this.getForward(), -90), rotation);
                break;
            case 5: // 왼쪽 위
                rotation = Quaternion.multiply(Quaternion.axisAngle(this.getForward(), -45), rotation);
                break;
            case 6: // 위
                break;
            case 7: // 오른쪽 위
                rotation = Quaternion.multiply(Quaternion.axisAngle(this.getForward(), +45), rotation);
                break;
            default:

        }

        this.setLocalRotation(rotation);

        this.setRenderable(noteRenderable);
        this.setLocalScale(Vector3.one().scaled(gameSystem.getSCALE()));
        this.setParent(gameSystem);

        this.setLocalPosition(gameSystem.getPosVector(coordinate));
        Vector3 localPosVec = gameSystem.getPosVector(coordinate);
        Vector3 movePos = Vector3.add(gameSystem.getWorldPosition(), localPosVec);
        Vector3 up = gameSystem.getUp().normalized();

        movePos = Vector3.add(movePos, up.scaled(-0.2f)); // 아래로 좀 내리기

        this.setWorldPosition(movePos);
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

        Vector3 up = gameSystem.getUp().normalized();

        Vector3 forward = camera.getForward();
        Vector3 upValue = new Vector3(up).scaled(Vector3.dot(up, forward));
        Vector3 direction = Vector3.subtract(forward, upValue).normalized().negated();

        distance += speed * deltaTime;

        Vector3 v = this.getLocalPosition();
        float d = (float)Math.sqrt(Vector3.dot(v, v));

        direction = direction.scaled(distance);

        Vector3 localPosVec = gameSystem.getPosVector(coordinate);
        Vector3 movePos = Vector3.add(gameSystem.getWorldPosition(), Vector3.add(direction, localPosVec));

        movePos = Vector3.add(movePos, up.scaled(-0.2f)); // 아래로 좀 내리기

        this.setWorldPosition(movePos);
    }

    public void getScore(int direction){
        // 일정 거리보다 가깝다면
        float perfectLine = gameSystem.getZONEDISTANCE();

        if(this.DIRECTION != direction) return; // 드래그 한 방향이 다르면 삭제x

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

    public int getDirection(){
        return DIRECTION;
    }
}
