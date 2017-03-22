package com.example.materialhintprogress.lib;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.Transformation;

import java.util.ArrayList;

/**
 * Created by hqc on 2015/9/25.
 */
public class StateMaterialProgressDrawable extends Drawable implements Animatable {

    private static final Interpolator LINEAR_INTERPOLATOR = new LinearInterpolator();
    private static final Interpolator END_CURVE_INTERPOLATOR = new EndCurveInterpolator();
    private static final Interpolator START_CURVE_INTERPOLATOR = new StartCurveInterpolator();

    // Maps to ProgressBar.Large style
    static final int LARGE = 0;
    // Maps to ProgressBar default style
    static final int DEFAULT = 1;

    // Maps to ProgressBar default style
    private static final int CIRCLE_DIAMETER = 40;
    private static final float CENTER_RADIUS = 15f; //should add up to 10 when + stroke_width
    private static final float STROKE_WIDTH = 2.5f;

    // Maps to ProgressBar.Large style
    private static final int CIRCLE_DIAMETER_LARGE = 56;
    private static final float CENTER_RADIUS_LARGE = 20f;
    private static final float STROKE_WIDTH_LARGE = 3f;

    /** 最大的延伸幅度,圆弧比例*/
    private static final float MAX_PROGRESS_ARC = .8f;
    private final int[] COLORS = new int[] {
            Color.BLACK
    };
    /** The duration of a single progress spin in milliseconds. */
    private static final int ANIMATION_DURATION = 1000 * 80 / 60;

    /** The number of points in the progress "star". */
    private static final float NUM_POINTS = 5f;
    /** The list of animators operating on this drawable. */
    private final ArrayList<Animation> mAnimators = new ArrayList<Animation>();

    /** The indicator ring, used to manage animation state. */
    private final StateRing mRing;

    /** Canvas rotation in degrees. */
    private float mRotation;

    private Resources mResources;
    private View mParent;
    private Animation mAnimation;
    private float mRotationCount;
    private double mWidth;
    private double mHeight;
    boolean mFinishing;

    /**圆圈 补满  最大所需要的时间 */
    private static  final  int  MAX_DRAW_CIRCLE_DURATION=250;
    /** 显示时间*/
    private static  final  int  SHOW_HINT_DURATION=500;


    public StateMaterialProgressDrawable(Context context,View parent){

        mParent = parent;
        mResources = context.getResources();

        mRing=new StateRing(mCallback);
        mRing.setColors(COLORS);

        updateSizes(DEFAULT);
    }


    private void setSizeParameters(double progressCircleWidth, double progressCircleHeight,
                                   double centerRadius, double strokeWidth) {
        final StateRing ring = mRing;
        final DisplayMetrics metrics = mResources.getDisplayMetrics();
        final float screenDensity = metrics.density;

        mWidth = progressCircleWidth * screenDensity;
        mHeight = progressCircleHeight * screenDensity;
        ring.setStrokeWidth((float) strokeWidth * screenDensity);
        ring.setCenterRadius(centerRadius * screenDensity);
        ring.setColorIndex(0);
        ring.setInsets((int) mWidth, (int) mHeight);
    }

    /**
     * Set the overall size for the progress spinner. This updates the radius
     * and stroke width of the ring.
     *
     * @param size One of {@link #DEFAULT} or
     *            {@link  #LARGE}
     */
    public void updateSizes(int size) {
        if (size == LARGE) {
            setSizeParameters(CIRCLE_DIAMETER_LARGE, CIRCLE_DIAMETER_LARGE, CENTER_RADIUS_LARGE,
                    STROKE_WIDTH_LARGE);
        } else {
            setSizeParameters(CIRCLE_DIAMETER, CIRCLE_DIAMETER, CENTER_RADIUS, STROKE_WIDTH);
        }
    }



    /**
     * Update the background color of the circle image view.
     */
    public void setBackgroundColor(int color) {
        mRing.setBackgroundColor(color);
    }

    /**
     * Set the colors used in the progress animation from color resources.
     * The first color will also be the color of the bar that grows in response
     * to a user swipe gesture.
     *
     * @param colors
     */
    public void setColorSchemeColors(int... colors) {
        mRing.setColors(colors);
        mRing.setColorIndex(0);
    }
    public void setRotation(float rotation) {
        mRotation = rotation;
        invalidateSelf();
    }

    @Override
    public int getIntrinsicHeight() {
        return (int) mHeight;
    }

    @Override
    public int getIntrinsicWidth() {
        return (int) mWidth;
    }

    private void applyFinishTranslation(float interpolatedTime, StateRing ring) {
        // shrink back down and complete a full rotation before
        // starting other circles
        // Rotation goes between [0..1].
        float targetRotation = (float) (Math.floor(ring.getStartingRotation() / MAX_PROGRESS_ARC)
                + 1f);
        final float startTrim = ring.getStartingStartTrim()
                + (ring.getStartingEndTrim() - ring.getStartingStartTrim()) * interpolatedTime;
        ring.setStartTrim(startTrim);
        final float rotation = ring.getStartingRotation()
                + ((targetRotation - ring.getStartingRotation()) * interpolatedTime);
        ring.setRotation(rotation);
    }

    /**
     * 设置，， 旋转的动画
     */
    private void setupAnimators() {
        final StateRing ring = mRing;
        final Animation animation = new Animation() {
            @Override
            public void applyTransformation(float interpolatedTime, Transformation t) {

                if (mFinishing) {
                    applyFinishTranslation(interpolatedTime, ring);
                }else {
                    // The minProgressArc is calculated from 0 to create an
                    // angle that
                    // matches the stroke width.
                    final float minProgressArc = (float) Math.toRadians(
                            ring.getStrokeWidth() / (2 * Math.PI * ring.getCenterRadius()));
                    final float startingEndTrim = ring.getStartingEndTrim();
                    final float startingTrim = ring.getStartingStartTrim();
                    final float startingRotation = ring.getStartingRotation();

                    // Offset the minProgressArc to where the endTrim is
                    // located.
                    final float minArc = MAX_PROGRESS_ARC - minProgressArc;

                    final float endTrim = startingEndTrim + (MAX_PROGRESS_ARC
                            * START_CURVE_INTERPOLATOR.getInterpolation(interpolatedTime));
                    ring.setEndTrim(endTrim);

                    final float startTrim = startingTrim + (MAX_PROGRESS_ARC
                            * END_CURVE_INTERPOLATOR.getInterpolation(interpolatedTime));
                    ring.setStartTrim(startTrim);
                        final float rotation = startingRotation + (0.25f * interpolatedTime);
                        ring.setRotation(rotation);
                        float groupRotation = ((720.0f / NUM_POINTS) * interpolatedTime)
                                + (720.0f * (mRotationCount / NUM_POINTS));
                        setRotation(groupRotation);


                }
            }

        };
        animation.setRepeatCount(Animation.INFINITE);
        animation.setRepeatMode(Animation.RESTART);
        animation.setInterpolator(LINEAR_INTERPOLATOR);
        animation.setAnimationListener(new Animation.AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
                mRotationCount = 0;
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                // do nothing
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

                ring.storeOriginals();
                ring.goToNextColor();
                ring.setStartTrim(ring.getEndTrim());
                if (mFinishing) {
                    // finished closing the last ring from the swipe gesture; go
                    // into progress mode
                    mFinishing = false;
                    animation.setDuration(ANIMATION_DURATION);
                } else {
                    mRotationCount = (mRotationCount + 1) % (NUM_POINTS);
                }
            }
        });
        mAnimation = animation;
    }

    /**
     * 开始设置， 圆环补满动画
     */
    private  void setupFillCircleAnimators(){
        final StateRing ring = mRing;
        final float startingEndTrim = ring.getEndTrim();
        final float startingTrim = ring.getStartTrim();
        final float noSwipeTrim=1-(startingEndTrim-startingTrim);
        final Animation animation = new Animation(){
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                super.applyTransformation(interpolatedTime, t);
               mRing.setEndTrim(startingEndTrim+interpolatedTime*noSwipeTrim);
            }
        };
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (mRing.getCurrrntHintMode() == StateRing.HINT_ERROR) {
                    mRing.setIsDrawX(true);
                    mRotation = 45;
                    setupXFirstAnimators();
                } else {
                    mRing.setIsDrawHook(true);
                    mRotation = 0;
                    setupHookFirstAnimators();
                }

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        animation.setDuration((long) (MAX_DRAW_CIRCLE_DURATION * noSwipeTrim));
        animation.setInterpolator(LINEAR_INTERPOLATOR);
        this.mAnimation=animation;
    }

    /**
     *   x 动画 开始的那一 笔画
     */
    private  void setupXFirstAnimators(){
        final StateRing ring = mRing;
        ring.setFirstStrokeTrim(0);
        ring.setSecondStrokeTrim(0);

        final Animation animation = new Animation(){
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                super.applyTransformation(interpolatedTime, t);
                ring.setFirstStrokeTrim(interpolatedTime);
            }
        };
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {

                setupXSecondAnimators();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        animation.setDuration((long) (MAX_DRAW_CIRCLE_DURATION ));
        animation.setInterpolator(LINEAR_INTERPOLATOR);
        this.mAnimation=animation;
        mParent.clearAnimation();
        mParent.startAnimation(mAnimation);
    }

    /**
     *  X动画 第二比
     */
    private  void setupXSecondAnimators(){
        final StateRing ring = mRing;
        ring.setSecondStrokeTrim(0);

        final Animation animation = new Animation(){
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                super.applyTransformation(interpolatedTime, t);
                ring.setSecondStrokeTrim(interpolatedTime);

            }
        };
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        animation.setDuration((long) (MAX_DRAW_CIRCLE_DURATION ));
        animation.setInterpolator(LINEAR_INTERPOLATOR);
        this.mAnimation=animation;
        mParent.clearAnimation();
        mParent.startAnimation(mAnimation);
    }

    /**
     *  draw hook , animation the first stroke
     */
    private  void setupHookFirstAnimators(){
        final StateRing ring = mRing;
        ring.setHookSecondStrokeTrim(0);
        ring.setHookFirstStrokeTrim(0);
        final Animation animation = new Animation(){
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                super.applyTransformation(interpolatedTime, t);
                ring.setHookFirstStrokeTrim(interpolatedTime);

            }
        };
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                setupHookSecondAnimators();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        animation.setDuration((long) (MAX_DRAW_CIRCLE_DURATION )/2);
        animation.setInterpolator(LINEAR_INTERPOLATOR);
        this.mAnimation=animation;
        mParent.clearAnimation();
        mParent.startAnimation(mAnimation);
    }

    /**
     *   draw hook , animation the second stroke
     */
    private  void setupHookSecondAnimators(){
        final StateRing ring = mRing;
        ring.setHookSecondStrokeTrim(0);

        final Animation animation = new Animation(){
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                super.applyTransformation(interpolatedTime, t);
                ring.setHookSecondStrokeTrim(interpolatedTime);

            }
        };
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        animation.setDuration((long) (MAX_DRAW_CIRCLE_DURATION *1.5));
        animation.setInterpolator(LINEAR_INTERPOLATOR);
        this.mAnimation=animation;
        mParent.clearAnimation();
        mParent.startAnimation(mAnimation);
    }

    @Override
    public void start() {
        setupAnimators();
        setRotation(0);
        mAnimation.reset();
        mRing.resetOriginals();
        mRing.storeOriginals();
        // Already showing some part of the ring
        if (mRing.getEndTrim() != mRing.getStartTrim()) {
            mFinishing = true;
            mAnimation.setDuration(ANIMATION_DURATION/2);
            mParent.startAnimation(mAnimation);
        } else {
            mRing.setColorIndex(0);
            mRing.resetOriginals();
            mAnimation.setDuration(ANIMATION_DURATION);
            mParent.startAnimation(mAnimation);
        }
        mRing.setIsDrawHook(false);
        mRing.setIsDrawX(false);
    }

    @Override
    public void stop() {
        mParent.clearAnimation();
        mRing.setColorIndex(0);
        mRing.resetOriginals();
    }

    @Override
    public boolean isRunning() {
        final ArrayList<Animation> animators = mAnimators;
        final int N = animators.size();
        for (int i = 0; i < N; i++) {
            final Animation animator = animators.get(i);
            if (animator.hasStarted() && !animator.hasEnded()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void draw(Canvas c) {
        final Rect bounds = getBounds();
        final int saveCount = c.save();

            c.rotate(mRotation, bounds.exactCenterX(), bounds.exactCenterY());
        mRing.draw(c, bounds);
        c.restoreToCount(saveCount);
    }

    /**
     * stop and show x hint
     */
    public  void stopForError(){
        mParent.clearAnimation();
        setupFillCircleAnimators();
        mParent.startAnimation(mAnimation);
        mRing.setCurrrntHintMode(StateRing.HINT_ERROR);
    }
    /**
     * stop and show hook hint
     */
    public  void stopForSucess(){
        mParent.clearAnimation();
        setupFillCircleAnimators();
        mParent.startAnimation(mAnimation);
        mRing.setCurrrntHintMode(StateRing.HINT_SUCCESS);
    }

    @Override
    public void setAlpha(int alpha) {
        mRing.setAlpha(alpha);
    }

    public int getAlpha() {
        return mRing.getAlpha();
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        mRing.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }



    private final Callback mCallback = new Callback() {
        @Override
        public void invalidateDrawable(Drawable d) {
            invalidateSelf();
        }

        @Override
        public void scheduleDrawable(Drawable d, Runnable what, long when) {
            scheduleSelf(what, when);
        }

        @Override
        public void unscheduleDrawable(Drawable d, Runnable what) {
            unscheduleSelf(what);
        }
    };



    /**
     *  The indicator ring, used to manage animation state.
     */
    private static class StateRing{

        private final float  one_sixteenth=0.0625f;
        private final RectF mTempBounds = new RectF();
        private final Paint mPaint = new Paint();

        private final Callback mCallback;

        private float mStartTrim = 0.0f;
        private float mEndTrim = 0.0f;
        private float mRotation = 0.0f;
        private float mStrokeWidth = 5.0f;
        private float mStrokeInset = 2.5f;

        private int[] mColors;
        // mColorIndex represents the offset into the available mColors that the
        // progress circle should currently display. As the progress circle is
        // animating, the mColorIndex moves by one to the next available color.
        private int mColorIndex;
        private float mStartingStartTrim;
        private float mStartingEndTrim;
        private float mStartingRotation;
        private double mRingCenterRadius;
        private int mAlpha=255;
        private final Paint mCirclePaint = new Paint();
        private int mBackgroundColor;

        // 画 x
        private  boolean isDrawX;
        private  float  firstStrokeTrim; //0-1;
        private  float  SecondStrokeTrim;
        // 画 勾
        private  boolean isDrawHook;
        private  float   hookFirstStrokeTrim; //0-1;
        private  float   hookSecondStrokeTrim;

        public static  final  int  HINT_ERROR=0;
        public static  final  int  HINT_SUCCESS=1;
        /** progress hint mode {@link #HINT_ERROR,#HINT_SUCCESS}*/
        private int  currrntHintMode;
//        /** 当 为true是，进度条停止转动，进行补满动画*/
//        private  boolean showHint;
//        /*** 当显示  提示图案时候，先要把圆环补满，在画提示图案  **/
//        private boolean circleIsDrawOver;


        public  StateRing(Callback callback ){
            mCallback = callback;

            mPaint.setStrokeCap(Paint.Cap.SQUARE);
            mPaint.setAntiAlias(true);
            mPaint.setStyle(Paint.Style.STROKE);
        }


        public boolean isDrawHook() {
            return isDrawHook;
        }

        public void setIsDrawHook(boolean isDrawHook) {
            this.isDrawHook = isDrawHook;
        }

        public boolean isDrawX() {
            return isDrawX;
        }

        public void setIsDrawX(boolean isDrawX) {
            this.isDrawX = isDrawX;
        }

        public int getCurrrntHintMode() {
            return currrntHintMode;
        }

        public void setCurrrntHintMode(int currrntHintMode) {
            this.currrntHintMode = currrntHintMode;
        }

        /**
         * set background color
         * @param color
         */
        public void setBackgroundColor(int color) {
            mBackgroundColor = color;
        }

        public float getFirstStrokeTrim() {
            return firstStrokeTrim;
        }

        public void setFirstStrokeTrim(float firstStrokeTrim) {
            this.firstStrokeTrim = firstStrokeTrim;
            invalidateSelf();
        }

        public float getSecondStrokeTrim() {
            return SecondStrokeTrim;
        }

        public void setSecondStrokeTrim(float secondStrokeTrim) {
            SecondStrokeTrim = secondStrokeTrim;
            invalidateSelf();
        }

        public float getHookFirstStrokeTrim() {
            return hookFirstStrokeTrim;
        }

        public void setHookFirstStrokeTrim(float hookFirstStrokeTrim) {
            this.hookFirstStrokeTrim = hookFirstStrokeTrim;
            invalidateSelf();
        }

        public float getHookSecondStrokeTrim() {
            return hookSecondStrokeTrim;
        }

        public void setHookSecondStrokeTrim(float hookSecondStrokeTrim) {
            this.hookSecondStrokeTrim = hookSecondStrokeTrim;
            invalidateSelf();
        }

        /**
         * draw progress for  every state
         * @param c
         * @param bounds
         */
        public  void draw(Canvas c, Rect bounds){

            final RectF arcBounds = mTempBounds;
            arcBounds.set(bounds);
            arcBounds.inset(mStrokeInset, mStrokeInset);

            final float startAngle = (mStartTrim + mRotation) * 360;


            final float endAngle = (mEndTrim + mRotation) * 360;
            float sweepAngle = endAngle - startAngle;
            mPaint.setColor(mColors[mColorIndex]);
            c.drawArc(arcBounds, startAngle, sweepAngle, false, mPaint);

            if (mAlpha < 255) {
                mCirclePaint.setColor(mBackgroundColor);
                mCirclePaint.setAlpha(255 - mAlpha);
                c.drawCircle(bounds.exactCenterX(), bounds.exactCenterY(), bounds.width() / 2,
                        mCirclePaint);
            }

            if(isDrawX){
                drawX(c,bounds);
            }
            if(isDrawHook){
                drawHook(c,bounds);
            }
        }

        /**
         * draw  x  hint
         * @param c
         * @param bounds
         */
        private void drawX(Canvas c,Rect bounds){

            float centerX=bounds.exactCenterX();
            float centerY=bounds.exactCenterY();
            //  16分之 8
            float three_fourth= one_sixteenth * 8;
            Path path=new Path();
            if(firstStrokeTrim>0) {
                float fxs= (float) (centerX- mRingCenterRadius*three_fourth);
                float sxe=(float) (centerX+ mRingCenterRadius*three_fourth);
                float xRange=sxe-fxs;
                path.moveTo(fxs, centerY);
                path.lineTo(fxs + xRange * firstStrokeTrim, centerY);
                c.drawPath(path, mPaint);
                path.reset();
            }
            if(SecondStrokeTrim>0){
                float fys= (float) (centerY- mRingCenterRadius*three_fourth);
                float sye=(float) (centerY+ mRingCenterRadius*three_fourth);
                float yRange=sye-fys;
                path.moveTo(centerX, fys);
                path.lineTo(centerX, fys + yRange * SecondStrokeTrim);

                c.drawPath(path, mPaint);

            }
        }

        /**
         * draw  right  hook
         */
        private  void  drawHook(Canvas c,Rect bounds){

              // 16 分之一x
              float offsetx=bounds.exactCenterX()*one_sixteenth;

              float midPointX=bounds.exactCenterX()-offsetx;
              float midPointY= (float) (bounds.exactCenterY()+mRingCenterRadius*one_sixteenth*4);

              float firstPointX=midPointX*14*one_sixteenth-offsetx;
              float firstPointY=bounds.exactCenterY();

              float endPointX= (float) (1.5*midPointX-offsetx);
              float endPointY= (float) (bounds.exactCenterY()-mRingCenterRadius*0.3333);

              Path path= new Path();
              if(hookFirstStrokeTrim>0){
                   float xRange=midPointX-firstPointX;
                   float yRange=midPointY-firstPointY;

                   path.moveTo(firstPointX,firstPointY);
                   path.lineTo(firstPointX+hookFirstStrokeTrim*xRange,firstPointY+yRange*hookFirstStrokeTrim);
                   c.drawPath(path,mPaint);
                   path.reset();
              }
             if(hookSecondStrokeTrim>0){
                 float xRange=endPointX-midPointX;
                 float yRange=endPointY-midPointY;

                 path.moveTo(midPointX,midPointY);
                 path.lineTo(midPointX+hookSecondStrokeTrim*xRange,midPointY+yRange*hookSecondStrokeTrim);
                 c.drawPath(path,mPaint);
             }

        }



        /**
         * Set the colors the progress spinner alternates between.
         *
         * @param colors Array of integers describing the colors. Must be non-<code>null</code>.
         */
        public void setColors( int[] colors) {
            mColors = colors;
            // if colors are reset, make sure to reset the color index as well
            setColorIndex(0);
        }

        /**
         * @param index Index into the color array of the color to display in
         *            the progress spinner.
         */
        public void setColorIndex(int index) {
            mColorIndex = index;
        }

        /**
         * Proceed to the next available ring color. This will automatically
         * wrap back to the beginning of colors.
         */
        public void goToNextColor() {
            mColorIndex = (mColorIndex + 1) % (mColors.length);
        }

        public void setColorFilter(ColorFilter filter) {
            mPaint.setColorFilter(filter);
            invalidateSelf();
        }

        /**
         * @param alpha Set the alpha of the progress spinner and associated arrowhead.
         */
        public void setAlpha(int alpha) {
            mAlpha = alpha;
        }

        /**
         * @return Current alpha of the progress spinner and arrowhead.
         */
        public int getAlpha() {
            return mAlpha;
        }

        /**
         * @param strokeWidth Set the stroke width of the progress spinner in pixels.
         */
        public void setStrokeWidth(float strokeWidth) {
            mStrokeWidth = strokeWidth;
            mPaint.setStrokeWidth(strokeWidth);
            invalidateSelf();
        }

        @SuppressWarnings("unused")
        public float getStrokeWidth() {
            return mStrokeWidth;
        }

        @SuppressWarnings("unused")
        public void setStartTrim(float startTrim) {
            mStartTrim = startTrim;
            invalidateSelf();
        }

        @SuppressWarnings("unused")
        public float getStartTrim() {
            return mStartTrim;
        }

        public float getStartingStartTrim() {
            return mStartingStartTrim;
        }

        public float getStartingEndTrim() {
            return mStartingEndTrim;
        }

        @SuppressWarnings("unused")
        public void setEndTrim(float endTrim) {
            mEndTrim = endTrim;
            invalidateSelf();
        }

        @SuppressWarnings("unused")
        public float getEndTrim() {
            return mEndTrim;
        }

        @SuppressWarnings("unused")
        public void setRotation(float rotation) {
            mRotation = rotation;
            invalidateSelf();
        }

        @SuppressWarnings("unused")
        public float getRotation() {
            return mRotation;
        }

        public void setInsets(int width, int height) {
            final float minEdge = (float) Math.min(width, height);
            float insets;
            if (mRingCenterRadius <= 0 || minEdge < 0) {
                insets = (float) Math.ceil(mStrokeWidth / 2.0f);
            } else {
                insets = (float) (minEdge / 2.0f - mRingCenterRadius);
            }
            mStrokeInset = insets;
        }

        @SuppressWarnings("unused")
        public float getInsets() {
            return mStrokeInset;
        }

        /**
         * @param centerRadius Inner radius in px of the circle the progress
         *            spinner arc traces.
         */
        public void setCenterRadius(double centerRadius) {
            mRingCenterRadius = centerRadius;
        }

        public double getCenterRadius() {
            return mRingCenterRadius;
        }



        /**
         * @return The amount the progress spinner is currently rotated, between [0..1].
         */
        public float getStartingRotation() {
            return mStartingRotation;
        }

        /**
         * If the start / end trim are offset to begin with, store them so that
         * animation starts from that offset.
         */
        public void storeOriginals() {
            mStartingStartTrim = mStartTrim;
            mStartingEndTrim = mEndTrim;
            mStartingRotation = mRotation;
        }

        /**
         * Reset the progress spinner to default rotation, start and end angles.
         */
        public void resetOriginals() {
            mStartingStartTrim = 0;
            mStartingEndTrim = 0;
            mStartingRotation = 0;
            setStartTrim(0);
            setEndTrim(0);
            setRotation(0);
        }

        private void invalidateSelf() {
            mCallback.invalidateDrawable(null);
        }

    }



    /**
     * Squishes the interpolation curve into the second half of the animation.
     */
    private static class EndCurveInterpolator extends AccelerateDecelerateInterpolator {
        @Override
        public float getInterpolation(float input) {
            return super.getInterpolation(Math.max(0, (input - 0.5f) * 2.0f));
        }
    }

    /**
     * Squishes the interpolation curve into the first half of the animation.
     */
    private static class StartCurveInterpolator extends AccelerateDecelerateInterpolator {
        @Override
        public float getInterpolation(float input) {
            return super.getInterpolation(Math.min(1, input * 2.0f));
        }
    }



}
