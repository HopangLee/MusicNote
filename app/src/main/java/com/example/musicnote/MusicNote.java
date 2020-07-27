package com.example.musicnote;

import android.view.MotionEvent;

import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.HitTestResult;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;

import java.util.Random;

public class MusicNote extends Node {

    private Vector3 direction; // 날아갈 방향
    private float speed; // 속도
    final float AVERAGE = 5f;
    final float MINSPEED = 3f;

    MusicNote(AnchorNode parent, ModelRenderable modelRenderable){
        this.setRenderable(modelRenderable);
        this.setLocalScale(new Vector3(0.5f, 0.5f, 0.5f));
        this.setParent(parent);

        Random rand = new Random();
        speed = (float)rand.nextGaussian() + AVERAGE; // 가우시안 평균 이동
        speed = Float.max(MINSPEED, speed); // 최소 속도 설정

        float theta = rand.nextFloat() * (float)Math.PI * 2;
        float pi = rand.nextFloat() * (float) Math.PI;

        direction = new Vector3((float)(Math.sin(pi) * Math.cos(theta)), (float)(Math.sin(pi) * Math.sin(theta)), (float)(Math.cos(pi)));
    }

    // 노트 anchornode를 중심으로 이동
    @Override
    public void onUpdate(FrameTime frameTime) {
        super.onUpdate(frameTime);

        float deltaTime = frameTime.getDeltaSeconds();

        Vector3 moveVec = direction.scaled(speed * deltaTime); // delta 시간 곱해주기
        Vector3 newVec = Vector3.add(this.getLocalPosition(), moveVec); // 이동 벡터

        this.setLocalPosition(newVec);
    }

    // 터치했을 때 이펙트와 함께 사라짐(점수 오르는 것도?)
    @Override
    public void onTap(HitTestResult hitTestResult, MotionEvent motionEvent) {

    }
}
