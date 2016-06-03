package com.thanhta.hellogame;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.Random;


public class GamePanel extends SurfaceView implements SurfaceHolder.Callback
{
    public static final int WIDTH = 856;
    public static final int HEIGHT = 480;
    public static final int MOVESPEED = -5;
    private MainThread thread;
    private Background bg;
    private Player player;
    private long smokeStartTime;
    private long missileStartTime;
    private ArrayList<Smokepuff> smoke;
    private ArrayList<Missile> missiles;
    private Random rand = new Random();

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
        //create everything here
        bg = new Background(BitmapFactory.decodeResource(getResources(), R.drawable.grassbg1));
        player = new Player(BitmapFactory.decodeResource(getResources(), R.drawable.helicopter), 65, 25, 3);
        smoke = new ArrayList<>();
        missiles = new ArrayList<>();
        smokeStartTime = System.nanoTime();
        missileStartTime = System.nanoTime();

        //safely start the game loop
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
            //add missile on timer
            long missilesElapsed = (System.nanoTime()- missileStartTime)/1000000;
            if (missilesElapsed> (2000- player.getScore()/4)){
                //first missile always goes down in the middle
                if (missiles.size()==0){
                    //use paint.net to find out about width and height of the missile
                    missiles.add(new Missile(BitmapFactory.decodeResource(getResources(),
                            R.drawable.missile),WIDTH+10, HEIGHT/2, 45, 15, player.getScore(), 13));
                } else {
                    //random position for missile from 0 to the height of the screen
                    missiles.add(new Missile(BitmapFactory.decodeResource(getResources(),
                            R.drawable.missile), WIDTH+10, (int)(rand.nextDouble()*((HEIGHT))), 45,15,player.getScore(), 13));
                }
                missileStartTime =System.nanoTime();
            }
            //loop through every missiles
            for (int i =0; i<missiles.size(); i++){
                //update missile
                missiles.get(i).update();
                //check for collision and stop
                if (collision(missiles.get(i),player)){
                    missiles.remove(i);
                    player.setPlaying(false);
                    break;
                }
                //remove missile when off the screen
                if (missiles.get(i).getX()<-100){
                    missiles.remove(i);
                }
            }

            //smoke come out delay
            long elapsed = (System.nanoTime()- smokeStartTime)/1000000;
            if (elapsed >120) {
                smoke.add(new Smokepuff(player.getX(),player.getY()+10));
                smokeStartTime = System.nanoTime();
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
    public boolean collision (GameObject a, GameObject b){
        if (Rect.intersects(a.getRectangle(),b.getRectangle())){
            return true;
        }
        return false;
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
            //draw every missiles
            for (Missile m: missiles){
                m.draw(canvas);
            }
            //return back to save state (if not it will keep scaling bigger and bigger
            canvas.restoreToCount(savedState);
        }
    }
}