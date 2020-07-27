package com.example.musicnote;

import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.rendering.ModelRenderable;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Handler;

import androidx.annotation.Nullable;

public class AlbumNode extends Node {
    private AnchorNode parent;
    private ModelRenderable[] musicNotes;
    private int[] timerArray;
    private Handler mHandler; // 주기적으로 음표 생성위함
    private Timer timer;
    private float time = 0;
    private TimerTask TT = new TimerTask() {
        @Override
        public void run() {
            // 반복실행할 구문
            time += 0.1f; // 0.1초 추가
        }
    };
    private int index = 0; // timerArray의 index

    private float delay = 1f; // 생성되고 누르기 전까지의 시간

    AlbumNode(AnchorNode parent, ModelRenderable albumModel, int[] timerArray, ModelRenderable[] musicNotes){
        this.setRenderable(albumModel);
        this.setParent(parent);
        this.parent = parent;
        this.timerArray = timerArray;
        this.musicNotes = musicNotes;
    }

    // 음악을 시작했을 때 돌기 및 거리에 따른 크기 증가 및 시간에 따른 음표 오브젝트 생성
    @Override
    public void onUpdate(FrameTime frameTime) {
        super.onUpdate(frameTime);
        // Animation hasn't been set up.

        // 특정 시간에 음표 생성 (아직 딜레이 적용x)
        if(index < timerArray.length && time == timerArray[index]){
            Random rand = new Random();
            int i = rand.nextInt(musicNotes.length);

            MusicNote m = new MusicNote(parent, musicNotes[i]);

            index++;
        }

    }

    // 뮤직게임 스타트 (음표 오브젝트 생성)
    public void startGame(){
        timer.schedule(TT, 0, 100); // 0.1초마다 실행
    }
}
