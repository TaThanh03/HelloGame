package com.thanhta.hellogame;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.lang.reflect.Type;
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
    private ArrayList<TopBorder> topBorders;
    private ArrayList<BotBorder> botBorders;
    private int maxBorderHeight;
    private int minBorderHeight;
    private boolean topDown =true;
    private boolean botDown =true;
    //to control difficulty progression
    private int progressDenom = 20;
    private boolean newGameCreated;
    private Random rand = new Random();

    private Explosion explosion;
    private long startReset;
    private boolean reset;
    //disappear determine draw the player or not
    private boolean disappear;
    private boolean started;
    private int best;


    public GamePanel(Context context)
    {
        super(context);
        //add the callback to the surfaceholder to intercept events
        getHolder().addCallback(this);
        //make gamePanel focusable so it can handle events
        setFocusable(true);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height){}

    @Override
    public void surfaceDestroyed(SurfaceHolder holder){
        boolean retry = true;
        int counter = 0;
        while (retry && counter<1000){
            //sometime is take multiple times to setRunning a thread
            counter++;
            try{
                thread.setRunning(false);
                thread.join();
                retry = false;
                //set thread to null so garbage collector can pick up objects ò that thread
                thread = null;
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
        topBorders = new ArrayList<>();
        botBorders = new ArrayList<>();
        smokeStartTime = System.nanoTime();
        missileStartTime = System.nanoTime();
        //safely start the game loop
        thread = new MainThread(getHolder(), this);
        thread.setRunning(true);
        thread.start();
    }
    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        if(event.getAction()==MotionEvent.ACTION_DOWN){
            if(!player.getPlaying() && newGameCreated && reset)
            {
                player.setPlaying(true);
                player.setUp(true);
            }
            if(player.getPlaying())
            {
                //draw the explosion only if the game is already started
                if (!started) started=true;
                reset = false;
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
            if (botBorders.isEmpty()){
                player.setPlaying(false);
                return;
            }
            if (topBorders.isEmpty()){
                player.setPlaying(false);
                return;
            }
            bg.update();
            player.update();
            // calculate the threshold of the height's border base on the score
            // max and min border's height are updated,
            // border switched direction when either max or min is met
            maxBorderHeight = 30+player.getScore()/progressDenom;
            //cap max border height so borders can only take up total 1/2 screen
            if (maxBorderHeight > HEIGHT/4){ maxBorderHeight=HEIGHT/4;}
            minBorderHeight = 5+player.getScore()/progressDenom;
            //check border collision
            for (int i = 0; i<topBorders.size(); i++){
                if (collision(topBorders.get(i),player)){
                    player.setPlaying(false);
                }
            }
            for (int i=0; i<botBorders.size();i++){
                if (collision(botBorders.get(i),player)){
                    player.setPlaying(false);
                }
            }
            // update border
            this.updateTopBorder();
            this.updateBottomBorder();
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
                    //make sure it doesn't go thought borders
                    missiles.add(new Missile(BitmapFactory.decodeResource(getResources(),
                            R.drawable.missile), WIDTH+10,
                            (int) (rand.nextDouble()*(HEIGHT-(maxBorderHeight*2))+maxBorderHeight),
                            45, 15, player.getScore(), 13));
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
        //if collision, the getPlaying() is false, everything will frezze and jump to this
        else {
            player.resetDYA();
            if (!reset){
                newGameCreated = false;
                startReset = System.nanoTime();
                reset = true;
                disappear = true;
                explosion = new Explosion(BitmapFactory.decodeResource(getResources(),R.drawable.explosion),
                        player.getX(),player.getY()-30,100,100,25);
            }
            explosion.update();
            //how long the procedure will wait to reset again?
            long resetElapsed = System.nanoTime()-startReset/1000000;
            if (resetElapsed > 250000000 && !newGameCreated){
                newGame();
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
            if (!disappear) {
                player.draw(canvas);
            }
            //draw every smoke each time
            for (Smokepuff sp: smoke){
                sp.draw(canvas);
            }
            //draw every missiles
            for (Missile m: missiles){
                m.draw(canvas);
            }
            for (TopBorder tb: topBorders){
                tb.draw(canvas);
            }
            for (BotBorder bb: botBorders){
                bb.draw(canvas);
            }
            //draw explosion
            if (started){
                explosion.draw(canvas);
            }
            drawText(canvas);
            //return back to save state (if not it will keep scaling bigger and bigger
            canvas.restoreToCount(savedState);
        }
    }

    public void updateTopBorder(){
        //every 50 points, insert randomly placed top blocks that break the pattern
        if (player.getScore()%50 ==0){
            topBorders.add(new TopBorder(BitmapFactory.decodeResource(getResources(),R.drawable.brick),
                    topBorders.get(topBorders.size()-1).getX()+20,
                    0,
                    (int)(rand.nextDouble()*maxBorderHeight)+1 ));
        }
        //update top border
        for (int i=0; i<topBorders.size(); i++){
            topBorders.get(i).update();
            //if border off screen -> remove
            if (topBorders.get(i).getX()<-20){
                topBorders.remove(i);
                //calculate topdown which determines the direction of border (up or down)
                if (topBorders.get(topBorders.size()-1).getHeight()>=maxBorderHeight){
                    topDown = false;
                }
                if (topBorders.get(topBorders.size()-1).getHeight()<=minBorderHeight){
                    topDown = true;
                }
                //new border added will have larger height
                if (topDown){
                    topBorders.add(new TopBorder(BitmapFactory.decodeResource(getResources(),R.drawable.brick),
                            topBorders.get(topBorders.size()-1).getX()+20,
                            0,
                            topBorders.get(topBorders.size()-1).getHeight()+1));
                }
                //new border added will have smaller height
                else {
                    topBorders.add(new TopBorder(BitmapFactory.decodeResource(getResources(),R.drawable.brick),
                            topBorders.get(topBorders.size()-1).getX()+20,
                            0,
                            topBorders.get(topBorders.size()-1).getHeight()-1));
                }
            }
        }
    }
    public void updateBottomBorder(){
        //every 40 points, insert randomly placed bottom blocks that break the pattern
        if (player.getScore()%40 ==0){
            botBorders.add(new BotBorder(BitmapFactory.decodeResource(getResources(),R.drawable.brick),
                    botBorders.get(botBorders.size()-1).getX()+20,
                    (int)(rand.nextDouble()*maxBorderHeight)+HEIGHT-maxBorderHeight));
        }
        //update bottom border
        for (int i =0; i<botBorders.size(); i++){
            botBorders.get(i).update();
            if (botBorders.get(i).getX()<-20) {
                botBorders.remove(i);
                if (botBorders.get(botBorders.size() - 1).getY() <= HEIGHT- maxBorderHeight) {
                    botDown = true ;
                }
                if (botBorders.get(botBorders.size() - 1).getY() >= HEIGHT- minBorderHeight) {
                    botDown = false;
                }
                if (botDown) {
                    botBorders.add(new BotBorder(BitmapFactory.decodeResource(getResources(), R.drawable.brick),
                            botBorders.get(botBorders.size() - 1).getX() + 20,
                            botBorders.get(botBorders.size() - 1).getY() + 1));
                } else {
                    botBorders.add(new BotBorder(BitmapFactory.decodeResource(getResources(), R.drawable.brick),
                            botBorders.get(botBorders.size() - 1).getX() + 20,
                            botBorders.get(botBorders.size() - 1).getY() - 1));
                }
            }
        }
    }
    public void newGame() {
        disappear =false;

        botBorders.clear();
        topBorders.clear();
        missiles.clear();
        smoke.clear();

        player.resetScore();
        player.resetDYA();
        player.setY(HEIGHT/2);
        minBorderHeight = 5;
        maxBorderHeight = 30;

        if (player.getScore()>best){
            best = player.getScore();
        }

        //create initial borders
        for (int i=0; i*20<WIDTH+40; i++){
            if (i == 0) {
                topBorders.add(new TopBorder(BitmapFactory.decodeResource(getResources(),R.drawable.brick),
                        i*20, 0, 10));
            } else {
                topBorders.add(new TopBorder(BitmapFactory.decodeResource(getResources(),R.drawable.brick),
                        i*20, 0, topBorders.get(i-1).getHeight()+1));
            }
        }
        for (int i=0; i*20<WIDTH+40; i++){
            if (i == 0) {
                botBorders.add(new BotBorder(BitmapFactory.decodeResource(getResources(),R.drawable.brick),
                        i*20, HEIGHT-minBorderHeight));
            } else {
                botBorders.add(new BotBorder(BitmapFactory.decodeResource(getResources(),R.drawable.brick),
                        i*20, botBorders.get(i-1).getY()-1));
            }
        }
        newGameCreated = true;
    }
    public void drawText(Canvas canvas){
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(30);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Distance: "+ (player.getScore()*3), 10, HEIGHT-10, paint );
        canvas.drawText("Best: "+ best, WIDTH-215, HEIGHT-10, paint );
        if (!player.getPlaying() && newGameCreated && reset){
            Paint paint1 = new Paint();
            paint1.setTextSize(40);
            paint1.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            canvas.drawText("PRESS TO START",WIDTH/2-50,HEIGHT/2, paint1);
            paint1.setTextSize(20);
            canvas.drawText("PRESS AND HOLD TO GO UP",WIDTH/2-50, HEIGHT/2+20, paint1);
            canvas.drawText("RELEASE TO GO DOWN",WIDTH/2-50, HEIGHT/2+40, paint1);
        }
    }
}