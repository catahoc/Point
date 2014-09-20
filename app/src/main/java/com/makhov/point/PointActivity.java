package com.makhov.point;

import android.app.Activity;
import android.graphics.Point;
import android.os.Bundle;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.lang.reflect.Field;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Created by hgzsdfk on 20.09.2014.
 */
public class PointActivity extends Activity {
    private ImageView imageContainer;
    private FieldInfo fieldInfo = new FieldInfo();

    @Override
    public void onCreate(Bundle bundle){
        super.onCreate(bundle);
// remove title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.point_layout);
        imageContainer = (ImageView)findViewById(R.id.imageContainer);
        imageContainer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                float x = motionEvent.getX();
                float y = motionEvent.getY();
                onClick(x, y);
                return false;
            }
        });


        startFieldInfoWithWindowSize(fieldInfo);
        startThread();
    }

    private void onClick(float x, float y) {
        fieldInfo.clickUi(x, y);
    }

    private void startFieldInfoWithWindowSize(FieldInfo targetFieldInfo){
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        targetFieldInfo.start(width, height);
    }

    @Override
    public void onStop(){
        super.onStop();
        stopThread();
    }

    private Thread thread;
    private Semaphore semaphore;

    private void startThread() {
        thread = new Thread(new Runnable() {
            Runnable renderingRunnable = new Runnable() {
                @Override
                public void run() {
                    render();
                }
            };
            @Override
            public void run() {
                try {
                    while(true) {
                        if(semaphore.tryAcquire(16, TimeUnit.MILLISECONDS)){
                            return;
                        }
                        else{
                            loop();
                            runOnUiThread(renderingRunnable);
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        semaphore = new Semaphore(1, true);
        semaphore.tryAcquire();
        thread.start();
    }

    private void render() {
        fieldInfo.render(imageContainer);
    }

    private void loop() {
        fieldInfo.tick();
    }

    private void stopThread() {
        semaphore.release();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        thread = null;
    }
}
