package com.example.materialhintprogress.lib;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;


/**
 * Created by hqc on 2015/9/23.
 */
public class StateCircleProgress extends ImageView{

    private  StateMaterialProgressDrawable materialProgressDrawable;

    public StateCircleProgress(Context context) {
        this(context, null);
    }

    public StateCircleProgress(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StateCircleProgress(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        materialProgressDrawable=new StateMaterialProgressDrawable(context,this);
        materialProgressDrawable.updateSizes(StateMaterialProgressDrawable.LARGE);
//        materialProgressDrawable.setAlpha(180);
//        materialProgressDrawable.setBackgroundColor(Color.GREEN);
        materialProgressDrawable.setColorSchemeColors(Color.GRAY,Color.BLUE,Color.GREEN,Color.YELLOW);
        setImageDrawable(materialProgressDrawable);
    }

//    @Override
//    protected void onVisibilityChanged(View changedView, int visibility) {
//        super.onVisibilityChanged(changedView, visibility);
//        if (visibility == VISIBLE) {
//            mDrawable.start();
//
//        } else {
//            mDrawable.stop();
//        }
//    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
//        materialProgressDrawable.setBounds(0,0,w,h);
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
//        return who == mDrawable || super.verifyDrawable(who);
        return super.verifyDrawable(who);
    }


    public  void  start(){
        materialProgressDrawable.start();
    }

    public  void  stop(){
        materialProgressDrawable.stop();
    }


    public  void  showError(){
        materialProgressDrawable.stopForError();
    }

    public  void  showSucess(){
        materialProgressDrawable.stopForSucess();
    }
}
