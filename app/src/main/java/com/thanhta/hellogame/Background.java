package com.thanhta.hellogame;

import android.graphics.Bitmap;
import android.graphics.Canvas;

/**
 * Created by Thanh Ta on 5/30/2016.
 */
public class Background {
    private Bitmap image;
    private int x, y, dx;

    public Background(Bitmap res){
        image = res;
        dx = GamePanel.MOVESPEED;
    }
    public void update(){
        x += dx;
        //if the background goes off the screen, reset it to the begin
        if(x<-GamePanel.WIDTH){
            x=0;
        }
    }
    public void draw(Canvas canvas){
        canvas.drawBitmap(image, x, y, null);
        //draw another image next to the first image to make background look continuous
        if(x<0){
            canvas.drawBitmap(image, x+GamePanel.WIDTH, y, null );
        }
    }
}
