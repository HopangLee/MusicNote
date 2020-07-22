package com.example.musicnote;

import android.app.Activity;
import android.content.Context;
import android.media.Image;
import android.media.MediaPlayer;
import android.provider.ContactsContract;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.w3c.dom.Text;

import static androidx.constraintlayout.widget.Constraints.TAG;

public class MusicUi{
    private MediaPlayer currentMediaPlayer;
    private ProgressBar musicBar;
    private TextView titleText;
    private ImageView playBtn;
    private ImageView album;
    String[] title = {"How You Like That - 블랙핑크", "DNA - 방탄소년단","빨간맛 - 레드벨벳"};
    int[] fileRoot = {R.drawable.blackpink_howyoulikethat, R.drawable.bts_dna, R.drawable.redvelvet_redflavor};
    MediaPlayer[] mediaPlayer = new MediaPlayer[3];

    private final Activity mActivity;

    // 해야할일: MediaPlayer도 MusicUi.java로 옮기기
    MusicUi(Activity mActivity, Context context, ProgressBar musicBar, TextView titleText, ImageView playBtn, ImageView album){
        this.musicBar = musicBar;
        this.titleText = titleText;
        this.playBtn = playBtn;
        this.album = album;
        mediaPlayer[0] = MediaPlayer.create(context, R.raw.blackpink);
        mediaPlayer[1] = MediaPlayer.create(context, R.raw.bts);
        mediaPlayer[2] = MediaPlayer.create(context, R.raw.red_velvet);
        this.mActivity = mActivity;
    }

    public void setMediaPlayer(int number){
        this.currentMediaPlayer = mediaPlayer[number];
        currentMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                musicStop();
            }
        });
        musicBar.setMax(currentMediaPlayer.getDuration());
        musicBar.setProgress(currentMediaPlayer.getCurrentPosition());
        titleText.setText(title[number]);
        album.setImageResource(fileRoot[number]);
    }

    public void musicPlay(){
        if(currentMediaPlayer != null) {
            currentMediaPlayer.start();
            playBtn.setImageResource(android.R.drawable.ic_media_pause);

            Thread musicThread = new Thread(new Runnable() {
                @Override
                public void run() { // Thread 로 작업할 내용을 구현
                    while(currentMediaPlayer.isPlaying()){
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        mActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                musicBar.setProgress(currentMediaPlayer.getCurrentPosition());
                            }
                        });
                    }
                    //musicBar.setProgress(currentMediaPlayer.getCurrentPosition());
                }

            });
            musicThread.start(); // 쓰레드 시작
        }
    }

    public void musicPause(){
        if(currentMediaPlayer != null) {
            currentMediaPlayer.pause();
            playBtn.setImageResource(android.R.drawable.ic_media_play);
        }
    }

    public void musicStop(){
        if(currentMediaPlayer != null) {
            currentMediaPlayer.pause();
            currentMediaPlayer.seekTo(0);
            playBtn.setImageResource(android.R.drawable.ic_media_play);
            musicBar.setProgress(0);
        }
    }

    public MediaPlayer getMediaPlayer(){
        return currentMediaPlayer;
    }

    public boolean isPlaying(int i){
        return mediaPlayer[i].isPlaying();
    }
}
