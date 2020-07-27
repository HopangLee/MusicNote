package com.example.musicnote;

import android.media.MediaPlayer;
import android.util.Log;

import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Handler;

import androidx.annotation.Nullable;

public class AlbumNode extends Node {
    private AnchorNode parent;
    private ModelRenderable[] musicNotes;
    private int[] timerArray; // 밀리세컨드 단위
    private float time = 0f;
    private int index = 0; // timerArray의 index
    private float delay = 1f; // 생성되고 누르기 전까지의 시간
    private MediaPlayer mediaPlayer;
    private ArSceneView arSceneView;

    AlbumNode(AnchorNode parent, ModelRenderable albumModel, int[] timerArray, ModelRenderable[] musicNotes, MediaPlayer mediaPlayer, ArSceneView arSceneView){
        this.setRenderable(albumModel);
        this.setLocalScale(new Vector3(0.25f, 0.25f, 0.25f));
        this.setParent(parent);
        this.parent = parent;
        this.timerArray = timerArray;
        this.musicNotes = musicNotes;
        this.mediaPlayer = mediaPlayer;
        this.arSceneView = arSceneView;
        Log.i("AlbumNode", "is created");

    }

    // 음악을 시작했을 때 돌기 및 거리에 따른 크기 증가 및 시간에 따른 음표 오브젝트 생성
    @Override
    public void onUpdate(FrameTime frameTime) {
        super.onUpdate(frameTime);
        // Animation hasn't been set up.
        Log.i("Time : ", time + "ms");
        if(mediaPlayer.isPlaying()) {
            time = mediaPlayer.getCurrentPosition(); // 밀리 세컨드로 받아옴

            // 특정 시간에 음표 생성 (아직 딜레이 적용x)
            if (index < timerArray.length && time >= timerArray[index]) {
                Random rand = new Random();
                int i = rand.nextInt(musicNotes.length);

                MusicNote m = new MusicNote(parent, musicNotes[i], arSceneView);

                index++;
            }
        }


    }

    // 뮤직게임 중지 (음표 오브젝트 생성x)
    public void stopGame(){
        time = 0f;
        index = 0;
    }
}
