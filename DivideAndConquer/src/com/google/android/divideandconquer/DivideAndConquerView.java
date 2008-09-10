/*
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.divideandconquer;

import android.view.View;
import android.view.MotionEvent;
import android.view.KeyEvent;
import android.content.Context;
import android.graphics.Paint;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.RectF;
import android.os.SystemClock;
import android.os.Debug;
import android.util.TypedValue;
import android.util.AttributeSet;

/**
 * Handles the visual display and touch input for the game.
 */
public class DivideAndConquerView extends View {

    private int mBackgroundColor = 0xFFFFE942;
    private int mBallColor = 0xFF4258FF;

    static final int BORDER_WIDTH = 10;
    static final float BALL_RADIUS = 4f;
    static final float BALL_SPEED = 80f;

    private static final boolean PROFILE_DRAWING = false;
    private boolean mDrawingProfilingStarted = false;

    private final Paint mPaint;
    private BallEngine mEngine;

    private Mode mMode = Mode.Paused;

    private BallEventCallBack mCallback;

    // interface for starting a line
    private DirectionPoint mDirectionPoint = null;

    /**
     * Callback notifying of events related to the ball engine.
     */
    static interface BallEventCallBack {

        /**
         * The engine has its dimensions and is ready to go.
         * @param ballEngine The ball engine.
         */
        void onEngineReady(BallEngine ballEngine);

        /**
         * A ball has hit a moving line.
         * @param ballEngine The engine.
         * @param x The x coordinate of the ball.
         * @param y The y coordinate of the ball.
         */
        void onBallHitsMovingLine(BallEngine ballEngine, float x, float y);

        /**
         * A line made it to the edges of its region, splitting off a new region.
         * @param ballEngine The engine.
         */
        void onNewRegion(BallEngine ballEngine);
    }

    /**
     * @return The ball engine associated with the game.
     */
    public BallEngine getEngine() {
        return mEngine;
    }

    /**
     * Keeps track of the mode of this view.
     */
    enum Mode {

        /**
         * The balls are bouncing around.
         */
        Bouncing,

        /**
         * The animation has stopped and the balls won't move around.  The user
         * may not unpause it; this is used to temporarily stop games between
         * levels, or when the game is over and the activity places a dialog up.
         */
        Paused,

        /**
         * Same as {@link #Paused}, but paints the word 'touch to unpause' on
         * the screen, so the user knows he/she can unpause the game.
         */
        PausedByUser
    }

    public DivideAndConquerView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStrokeWidth(2);
        mPaint.setColor(Color.BLACK);

        // so we can see the back key
        setFocusableInTouchMode(true);

        setBackgroundColor(mBackgroundColor);
    }

    /**
     * Set the colors used to draw the background and the balls.
     * @param bgColor The color for the background.
     * @param ballColor The color for the balls.
     */
    public void setColors(int bgColor, int ballColor) {
        mBackgroundColor = bgColor;
        mBallColor = ballColor;
        setBackgroundColor(mBackgroundColor);
    }

    /**
     * Set the callback that will be notified of events related to the ball
     * engine.
     * @param callback The callback.
     */
    public void setCallback(BallEventCallBack callback) {
        mCallback = callback;
    }

    @Override
    protected void onSizeChanged(int i, int i1, int i2, int i3) {
        super.onSizeChanged(i, i1, i2,
                i3);

        // this should only happen once when the activity is first launched.
        // we could be smarter about saving / restoring across activity
        // lifecycles, but for now, this is good enough to handle in game play,
        // and most cases of navigating away with the home key and coming back.
        mEngine = new BallEngine(
                BORDER_WIDTH, getWidth() - BORDER_WIDTH,
                BORDER_WIDTH, getHeight() - BORDER_WIDTH,
                BALL_SPEED,
                BALL_RADIUS);
        mCallback.onEngineReady(mEngine);
    }


    /**
     * @return the current mode of operation.
     */
    public Mode getMode() {
        return mMode;
    }

    /**
     * Set the mode of operation.
     * @param mode The mode.
     */
    public void setMode(Mode mode) {
        mMode = mode;

        if (mMode == Mode.Bouncing && mEngine != null) {
            // when starting up again, the engine needs to know what 'now' is.
            mEngine.setNow(SystemClock.elapsedRealtime());
            invalidate();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // the first time the user hits back while the balls are moving,
        // we'll pause the game.  but if they hit back again, we'll do the usual
        // (exit the activity)
        if (keyCode == KeyEvent.KEYCODE_BACK && mMode == Mode.Bouncing) {
            setMode(Mode.PausedByUser);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {

        if (mMode == Mode.PausedByUser) {
            // touching unpauses when the game was paused by the user.
            setMode(Mode.Bouncing);
            return true;
        } else if (mMode == Mode.Paused) {
            return false;
        }

        final float x = motionEvent.getX();
        final float y = motionEvent.getY();
        switch(motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (mEngine.canStartLineAt(x, y)) {
                    mDirectionPoint =
                            new DirectionPoint(x, y);
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                if (mDirectionPoint != null) {
                    mDirectionPoint.updateEndPoint(x, y);
                } else if (mEngine.canStartLineAt(x, y)) {
                    mDirectionPoint =
                        new DirectionPoint(x, y);
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (mDirectionPoint != null) {
                    switch (mDirectionPoint.getDirection()) {
                        case Unknown:
                            // do nothing
                            break;
                        case Horizonal:
                            mEngine.startHorizontalLine(SystemClock.elapsedRealtime(),
                                    mDirectionPoint.getX(), mDirectionPoint.getY());
                            if (PROFILE_DRAWING) {
                                if (!mDrawingProfilingStarted) {
                                    Debug.startMethodTracing("BallsDrawing");
                                    mDrawingProfilingStarted = true;
                                }
                            }
                            break;
                        case Vertical:
                            mEngine.startVerticalLine(SystemClock.elapsedRealtime(),
                                    mDirectionPoint.getX(), mDirectionPoint.getY());
                            if (PROFILE_DRAWING) {
                                if (!mDrawingProfilingStarted) {
                                    Debug.startMethodTracing("BallsDrawing");
                                    mDrawingProfilingStarted = true;
                                }
                            }
                            break;
                    }
                }
                mDirectionPoint = null;
                return true;
            case MotionEvent.ACTION_CANCEL:
                mDirectionPoint = null;
                return true;
        }
        return false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        boolean newRegion = false;

        if (mMode == Mode.Bouncing) {
            try {
                newRegion = mEngine.update(SystemClock.elapsedRealtime());
            } catch (BallHitMovingLineException e) {
                mCallback.onBallHitsMovingLine(mEngine, e.getX(), e.getY());
            }

            if (newRegion) {
                mCallback.onNewRegion(mEngine);

                // reset back to full alpha bg color
                setBackgroundColor(mBackgroundColor);
            }

            if (PROFILE_DRAWING) {
                if (newRegion && mDrawingProfilingStarted) {
                    mDrawingProfilingStarted = false;
                    Debug.stopMethodTracing();
                }
            }
        }

        for (int i = 0; i < mEngine.getRegions().size(); i++) {
            BallRegion region = mEngine.getRegions().get(i);
            drawRegion(canvas, region);
        }

        if (mMode == Mode.PausedByUser) {
            drawPausedText(canvas);
        } else if (mMode == Mode.Bouncing) {
            // keep em' bouncing!
            invalidate();
        }
    }

    /**
     * Pain the text instructing the user how to unpause the game.
     */
    private void drawPausedText(Canvas canvas) {
        mPaint.setColor(Color.BLACK);
        mPaint.setAntiAlias(true);
        mPaint.setTextSize(
                    TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_SP,
                            20,
                            getResources().getDisplayMetrics()));
        final String unpauseInstructions = getContext().getString(R.string.unpause_instructions);
        canvas.drawText(unpauseInstructions, getWidth() / 5, getHeight() / 2, mPaint);
        mPaint.setAntiAlias(false);
    }

    private RectF mRectF = new RectF();

    /**
     * Draw a ball region.
     */
    private void drawRegion(Canvas canvas, BallRegion region) {

        // draw fill rect to offset against background
        mPaint.setColor(Color.WHITE);

        mRectF.set(region.getLeft(), region.getTop(),
                region.getRight(), region.getBottom());
        canvas.drawRect(mRectF, mPaint);

        // draw an outline
        mPaint.setColor(Color.BLACK);
        final float minX = region.getLeft();
        final float maxX = region.getRight();
        final float minY = region.getTop();
        final float maxY = region.getBottom();
        canvas.drawLine(minX, minY, maxX, minY, mPaint); // top line
        canvas.drawLine(minX, minY, minX, maxY, mPaint); // left line
        canvas.drawLine(minX, maxY, maxX, maxY, mPaint); // bottom line
        canvas.drawLine(maxX, minY, maxX, maxY, mPaint); // right line

        // draw each ball
        mPaint.setColor(mBallColor);
        mPaint.setShadowLayer(1, 1, 2, Color.BLACK);
        mPaint.setAntiAlias(true);
        for (Ball ball : region.getBalls()) {
            canvas.drawCircle(ball.getX(), ball.getY(), BALL_RADIUS, mPaint);
        }
        mPaint.clearShadowLayer();
        mPaint.setAntiAlias(false);

        // draw the animating line
        final AnimatingLine al = region.getAnimatingLine();
        if (al != null) {
            drawAnimatingLine(canvas, al);
        }
    }

    private static int scaleToBlack(int component, float percentage) {
        return (int) ((1f - percentage*0.4f) * component);
    }

    /**
     * Draw an animating line.
     */
    private void drawAnimatingLine(Canvas canvas, AnimatingLine al) {

        final float perc = al.getPercentageDone();
        final int color = mBackgroundColor;
        mPaint.setColor(Color.argb(
                0xFF,
                scaleToBlack(Color.red(color), perc),
                scaleToBlack(Color.green(color), perc),
                scaleToBlack(Color.blue(color), perc)
        ));
        switch (al.getDirection()) {
            case Horizontal:
                canvas.drawLine(
                        al.getStart(), al.getPerpAxisOffset(),
                        al.getEnd(), al.getPerpAxisOffset(),
                        mPaint);
                break;
            case Vertical:
                canvas.drawLine(
                        al.getPerpAxisOffset(), al.getStart(),
                        al.getPerpAxisOffset(), al.getEnd(),
                        mPaint);
                break;
        }
    }
}
