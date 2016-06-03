package com.thanhta.hellogame;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;


public class GamePanel extends SurfaceView implements SurfaceHolder.Callback
{
    public static final int WIDTH = 856;
    public static final int HEIGHT = 480;
    public static final int MOVESPEED = -5;
    private MainThread thread;
    private Background bg;
    private Player player;
    private long smokeStarttime ;
    private ArrayList<Smokepuff> smoke;

    public GamePanel(Context context)
    {
        super(context);


        //add the callback to the surfaceholder to intercept events
        getHolder().addCallback(this);

        thread = new MainThread(getHolder(), this);

        //make gamePanel focusable so it can handle events
        setFocusable(true);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height){}

    @Override
    public void surfaceDestroyed(SurfaceHolder holder){
        boolean retry = true;
//        while(retry)
//        {
//            try{thread.setRunning(false);
//                thread.join();
//
//            }catch(InterruptedException e){e.printStackTrace();}
//            retry = false;
//        }
        int counter = 0;
        while (retry && counter<1000){
            //sometime is take multiple times to setRunning a thread
            counter++;
            try{
                thread.setRunning(false);
                thread.join();
                retry = false;
            } catch (InterruptedException e){e.printStackTrace();}
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder){
        //create background and player
        bg = new Background(BitmapFactory.decodeResource(getResources(), R.drawable.grassbg1));
        player = new Player(BitmapFactory.decodeResource(getResources(), R.drawable.helicopter), 65, 25, 3);
        //create smoke
        smoke = new ArrayList<Smokepuff>();
        smokeStarttime = System.nanoTime();
        //we can safely start the game loop
        thread.setRunning(true);
        thread.start();
    }
    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        if(event.getAction()==MotionEvent.ACTION_DOWN){
            if(!player.getPlaying())
            {
                player.setPlaying(true);
            }
            else
            {
                player.setUp(true);
            }
            return true;
        }
        if(event.getAction()==MotionEvent.ACTION_UP)
        {
            player.setUp(false);
            return true;
        }

        return super.onTouchEvent(event);
    }

    public void update()
    {
        if(player.getPlaying()) {
            bg.update();
            player.update();
            //smoke come out delay
            long elapsed = (System.nanoTime()- smokeStarttime)/1000000;
            if (elapsed >120) {
                smoke.add(new Smokepuff(player.getX(),player.getY()+10));
                smokeStarttime = System.nanoTime();
            }

            //remove the smoke after it off the screen
            for (int i = 0; i<smoke.size(); i++){
                smoke.get(i).update();
                if (smoke.get(i).getX()<-10) {
                    smoke.remove(i);
                }
            }
        }
    }
    @Override
    public void draw(Canvas canvas)
    {
        final float scaleFactorX = getWidth()/(WIDTH*1.f);
        final float scaleFactorY = getHeight()/(HEIGHT*1.f);

        if(canvas!=null) {
            final int savedState = canvas.save();
            canvas.scale(scaleFactorX, scaleFactorY);
            bg.draw(canvas);
            player.draw(canvas);
            //draw every smoke each time
            for (Smokepuff sp: smoke){
                sp.draw(canvas);
            }
            //return back to save state (if not it will keep scaling bigger and bigger
            canvas.restoreToCount(savedState);
        }
    }
}