package com.makhov.point;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

public class FieldInfo {
    public HitInfo current;
    public ConcurrentLinkedQueue<HitInfo> pastHitInfoes;
    public FieldState fieldState;

    public final static float radiusUnit = 4.0f;
    public final static int maxRadius = 10;
    public final static long growWait = 50;
    public final static long nextWait = 400;
    public final static long dyingWait = 1500;

    private long nextEventTime;
    private long points = 0;

    private float sw;
    private float sh;

    public void start(float sw, float sh) {
        this.sw = sw;
        this.sh = sh;

        nextEventTime = getTime() + nextWait;
        pastHitInfoes = new ConcurrentLinkedQueue<HitInfo>();
        fieldState = FieldState.EMPTY;
    }

    public void tick() {
        long time = getTime();
        if(wasClick){
            click(clickX, clickY, time);
            wasClick = false;
        }
        while (nextEventTime < time) {
            if (fieldState == FieldState.GROWING) {
                current.radius++;
                fieldState = current.radius == maxRadius ? FieldState.DISAPPEARING : FieldState.GROWING;
                nextEventTime += growWait;
            } else if (fieldState == FieldState.EMPTY) {
                current = generateNew(time);
                fieldState = FieldState.GROWING;
                nextEventTime += growWait;
            } else if (fieldState == FieldState.DISAPPEARING) {
                fieldState = FieldState.EMPTY;
                pastHitInfoes.add(current);
                current = null;
                nextEventTime += growWait;
            }
        }
        ArrayList<HitInfo> deadHitInfoes = new ArrayList<HitInfo>();
        for(HitInfo pastInfo : pastHitInfoes){
            if(pastInfo.birth+dyingWait<time){
                deadHitInfoes.add(pastInfo);
            }
        }
        for (HitInfo dead:deadHitInfoes){
            pastHitInfoes.remove(dead);
        }
    }

    private HitInfo generateNew(long curtime) {
        HitInfo result = new HitInfo();
        float maxw = sw - maxRadius * radiusUnit;
        float maxh = sh - maxRadius * radiusUnit;
        float tx = getRand(maxw);
        float ty = getRand(maxh);
        result.x = tx + maxRadius * radiusUnit / 2.0f;
        result.y = ty + maxRadius * radiusUnit / 2.0f;
        result.radius = 1;
        result.birth = curtime + maxRadius*growWait;

        return result;
    }

    private Random rand = new Random();

    private float getRand(float maximum) {
        return rand.nextFloat() * maximum;
    }


    private long getTime() {
        Calendar c = Calendar.getInstance();
        return c.getTimeInMillis();
    }

    public void render(ImageView img) {
        long time = getTime();
        Bitmap bmp = Bitmap.createBitmap((int) sw, (int) sh, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        for(HitInfo pastHit: pastHitInfoes){
            long livedTime = time-pastHit.birth;
            if(livedTime < dyingWait){
                int a = 255-(int) (255*livedTime/dyingWait);
                renderCircle(canvas, pastHit, a);
            }
        }
        if(current != null){
            renderCircle(canvas, current, 0xff);
        }
        String draw = Long.toString(points);
        Paint textPaint = new Paint();
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setColor(Color.BLACK);
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setTextSize(20);
        canvas.drawText(draw, 5, 25, textPaint);

        img.setImageBitmap(bmp);
    }

    private void renderCircle(Canvas c, HitInfo hitInfo, int a){
        Paint strokePaint = new Paint();
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setColor(Color.BLACK);

        Paint fillPaint = new Paint();
        fillPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        fillPaint.setColor(hitInfo.wasHit ? Color.argb(a, 23, 66, 254) : Color.argb(a, 255, 168, 57));

        c.drawCircle(hitInfo.x, hitInfo.y, hitInfo.radius*radiusUnit+1, fillPaint);
        c.drawCircle(hitInfo.x, hitInfo.y, hitInfo.radius*radiusUnit, strokePaint);
    }

    private boolean wasClick;
    private float clickX;
    private float clickY;
    public void clickUi(float x, float y){
        clickX = x;
        clickY = y;
        wasClick = true;
    }

    private void click(float x, float y, long time) {
        for(HitInfo hit:pastHitInfoes){
            if(!hit.wasHit && isHit(hit, x, y)){
                hit.wasHit = true;
                points++;
            }
        }
        HitInfo hit = current;
        if(hit != null && !hit.wasHit && isHit(hit, x, y)) {
            hit.wasHit = true;
            pastHitInfoes.add(hit);
            current = null;
            nextEventTime = time+nextWait;
            fieldState = FieldState.EMPTY;
            points++;
        }
    }

    private boolean isHit(HitInfo hit, float x, float y){
        float dist = calculateDist(hit.x, hit.y, x, y);
        float radius = hit.radius*radiusUnit;
        return dist<radius;
    }

    private static float calculateDist(float x1, float y1, float x2, float y2) {
        return (float) Math.sqrt((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1));
    }
}