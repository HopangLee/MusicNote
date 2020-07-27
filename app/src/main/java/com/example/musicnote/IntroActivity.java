package com.example.musicnote;

import android.content.Intent;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.AbsoluteSizeSpan;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class IntroActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        Button music_noteBtn = (Button) findViewById(R.id.musicNote_btn);
        ImageView image_bof = (ImageView)findViewById(R.id.bof_logo);

        SpannableStringBuilder spannable = new SpannableStringBuilder("#두근두근_행사장 가는 길\nAR 음악노트");
        spannable.setSpan(new AbsoluteSizeSpan(40),0, 14, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new AbsoluteSizeSpan(75),15, 22, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        music_noteBtn.setText(spannable, TextView.BufferType.EDITABLE);

        music_noteBtn.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), InfoActivity.class);
                startActivity(intent);
            }

        });

    }
}
