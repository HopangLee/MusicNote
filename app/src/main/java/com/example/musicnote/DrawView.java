package com.example.musicnote;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class DrawView extends View {
    /*
    ArrayList<DrawImage> arr = new ArrayList<DrawImage>();
    */

    Paint[] paint = new Paint[2];
    //Path[] path = new Path[2];

    HashMap<Integer, Path>[] pathMap = new HashMap[2];
    HashMap<Integer, Point> previousPointMap;

    public DrawView(Context context){
        super(context);
        paint[0] = new Paint();
        paint[0].setStyle(Paint.Style.STROKE);
        paint[0].setStrokeWidth(20f);
        //paint[0].setStrokeJoin(Paint.Join.ROUND);
        paint[0].setStrokeCap(Paint.Cap.ROUND);
        paint[0].setPathEffect(new CornerPathEffect(10));
        paint[0].setAntiAlias(true);
        paint[0].setColor(Color.BLUE);
        paint[0].setAlpha(125);


        //path[0] = new Path();

        paint[1] = new Paint();
        paint[1].setStyle(Paint.Style.STROKE);
        paint[1].setStrokeWidth(20f);
        //paint[1].setStrokeJoin(Paint.Join.ROUND);
        paint[1].setStrokeCap(Paint.Cap.ROUND);
        paint[1].setPathEffect(new CornerPathEffect(10));
        paint[1].setAntiAlias(true);
        paint[1].setColor(Color.RED);
        paint[1].setAlpha(125);
        //path[1] = new Path();
    }

    public DrawView(Context context, AttributeSet attrs){
        super(context, attrs);
        paint[0] = new Paint();
        paint[0].setStyle(Paint.Style.STROKE);
        paint[0].setStrokeWidth(20f);
        //paint[0].setStrokeJoin(Paint.Join.ROUND);
        paint[0].setStrokeCap(Paint.Cap.ROUND);
        paint[0].setPathEffect(new CornerPathEffect(10));
        paint[0].setAntiAlias(true);
        paint[0].setColor(Color.BLUE);
        paint[0].setAlpha(125);

        //path[0] = new Path();

        paint[1] = new Paint();
        paint[1].setStyle(Paint.Style.STROKE);
        paint[1].setStrokeWidth(20f);
        //paint[1].setStrokeJoin(Paint.Join.ROUND);
        paint[1].setStrokeCap(Paint.Cap.ROUND);
        paint[1].setPathEffect(new CornerPathEffect(10));
        paint[1].setAntiAlias(true);
        paint[1].setColor(Color.RED);
        paint[1].setAlpha(125);
        //path[1] = new Path();
    }

    public DrawView(Context context, AttributeSet attrs, int defStyleAttr){
        super(context, attrs, defStyleAttr);
        paint[0] = new Paint();
        paint[0].setStyle(Paint.Style.STROKE);
        paint[0].setStrokeWidth(20f);
        //paint[0].setStrokeJoin(Paint.Join.ROUND);
        paint[0].setStrokeCap(Paint.Cap.ROUND);
        paint[0].setPathEffect(new CornerPathEffect(10));
        paint[0].setAntiAlias(true);
        paint[0].setColor(Color.BLUE);
        paint[0].setAlpha(125);

        //path[0] = new Path();

        paint[1] = new Paint();
        paint[1].setStyle(Paint.Style.STROKE);
        paint[1].setStrokeWidth(20f);
        //paint[1].setStrokeJoin(Paint.Join.ROUND);
        paint[1].setStrokeCap(Paint.Cap.ROUND);
        paint[1].setPathEffect(new CornerPathEffect(10));
        paint[1].setAntiAlias(true);
        paint[1].setColor(Color.RED);
        paint[1].setAlpha(125);
        //path[1] = new Path();
    }

    @Override
    public void onDraw(Canvas c) {
        //c.drawPath(path[0], paint[0]);
        //c.drawPath(path[1], paint[1]);
    }
}
