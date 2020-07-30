package com.example.musicnote;

import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;

public class GameNote extends Node {
    ArSceneView arSceneView; // 카메라 위치 알기 위함
    GameSystem gameSystem; // up Vector와 parnet의 위치 => SetNotePosition으로 위치 갱신
    boolean isRight; // 오른쪽 생성 노드인지 왼쪽 생성 노드인지 확인 (true => right, false => left)

    GameNote(ArSceneView arSceneView, GameSystem gameSystem, boolean isRight){
        this.arSceneView = arSceneView;
        this.gameSystem = gameSystem;
        this.isRight = isRight;
    }

    @Override
    public void onUpdate(FrameTime frameTime) {

    }
}
