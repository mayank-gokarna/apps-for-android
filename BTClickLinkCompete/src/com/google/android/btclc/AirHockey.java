
package com.google.android.btclc;

import com.google.android.btclc.Connection.OnConnectionLostListener;
import com.google.android.btclc.Connection.OnConnectionServiceReadyListener;
import com.google.android.btclc.Connection.OnIncomingConnectionListener;
import com.google.android.btclc.Connection.OnMaxConnectionsReachedListener;
import com.google.android.btclc.Connection.OnMessageReceivedListener;
import com.google.android.btclc.Demo_Ball.BOUNCE_TYPE;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.view.SurfaceHolder.Callback;

import java.util.ArrayList;

public class AirHockey extends Activity implements Callback {

    public static final String TAG = "AirHockey";

    private static final int SERVER_LIST_RESULT_CODE = 42;

    private AirHockey self;

    private int mType; // 0 = server, 1 = client

    private SurfaceView mSurface;

    private SurfaceHolder mHolder;

    private Paint bgPaint;

    private Paint ballPaint;

    private Paint paddlePaint;

    private PhysicsLoop pLoop;

    private ArrayList<Point> mPaddlePoints;

    private ArrayList<Long> mPaddleTimes;

    private int mPaddlePointWindowSize = 5;

    private int mPaddleRadius = 55;

    private Bitmap mPaddleBmp;

    private Demo_Ball mBall;

    private int mBallRadius = 40;

    private Connection mConnection;

    private OnMessageReceivedListener dataReceivedListener = new OnMessageReceivedListener() {
        public void OnMessageReceived(String device, String message) {

        }
    };

    private OnMaxConnectionsReachedListener maxConnectionsListener = new OnMaxConnectionsReachedListener() {
        public void OnMaxConnectionsReached() {

        }
    };

    private OnIncomingConnectionListener connectedListener = new OnIncomingConnectionListener() {
        public void OnIncomingConnection(String device) {

        }
    };

    private OnConnectionLostListener disconnectedListener = new OnConnectionLostListener() {
        public void OnConnectionLost(String device) {

        }
    };

    private OnConnectionServiceReadyListener serviceReadyListener = new OnConnectionServiceReadyListener() {
        public void OnConnectionServiceReady() {

        }
    };

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        self = this;
        mPaddleBmp = BitmapFactory.decodeResource(getResources(), R.drawable.paddlelarge);

        mPaddlePoints = new ArrayList<Point>();
        mPaddleTimes = new ArrayList<Long>();

        Intent startingIntent = getIntent();
        mType = startingIntent.getIntExtra("TYPE", 0);

        setContentView(R.layout.main);
        mSurface = (SurfaceView) findViewById(R.id.surface);
        mHolder = mSurface.getHolder();

        bgPaint = new Paint();
        bgPaint.setColor(Color.BLACK);

        ballPaint = new Paint();
        ballPaint.setColor(Color.GREEN);
        ballPaint.setAntiAlias(true);

        paddlePaint = new Paint();
        paddlePaint.setColor(Color.BLUE);
        paddlePaint.setAntiAlias(true);

        mConnection = new Connection(this, serviceReadyListener);
        mHolder.addCallback(self);

        WindowManager w = getWindowManager();
        Display d = w.getDefaultDisplay();
        int width = d.getWidth();
        int height = d.getHeight(); 
        mBall = new Demo_Ball(true, width, height-60);
    }

    @Override
    protected void onDestroy() {
        if (mConnection != null) {
            mConnection.shutdown();
        }
        super.onDestroy();
    }

    public void surfaceCreated(SurfaceHolder holder) {
        pLoop = new PhysicsLoop();
        pLoop.start();
    }

    private void draw() {
        Canvas canvas = null;
        try {
            canvas = mHolder.lockCanvas();
            if (canvas != null) {
                doDraw(canvas);
            }
        } finally {
            if (canvas != null) {
                mHolder.unlockCanvasAndPost(canvas);
            }
        }
    }

    private void doDraw(Canvas c) {
        c.drawRect(0, 0, c.getWidth(), c.getHeight(), bgPaint);

        if (mPaddleTimes.size() > 0) {
            Point p = mPaddlePoints.get(mPaddlePoints.size() - 1);

            // Debug circle
            // Point debugPaddleCircle = getPaddleCenter();
            // c.drawCircle(debugPaddleCircle.x, debugPaddleCircle.y,
            // mPaddleRadius, ballPaint);

            c.drawBitmap(mPaddleBmp, p.x - 60, p.y - 200, new Paint());
        }
        if (mBall == null) {
            return;
        }
        float x = mBall.getX();
        float y = mBall.getY();
        if ((x != -1) && (y != -1)) {
            float xv = mBall.getXVelocity();
            Bitmap bmp = BitmapFactory
                    .decodeResource(this.getResources(), R.drawable.android_right);
            if (xv < 0) {
                bmp = BitmapFactory.decodeResource(this.getResources(), R.drawable.android_left);
            }

            // Debug circle
            Point debugBallCircle = getBallCenter();
            c.drawCircle(debugBallCircle.x, debugBallCircle.y, mBallRadius, ballPaint);

            c.drawBitmap(bmp, x - 17, y - 23, new Paint());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        try {
            pLoop.safeStop();
        } finally {
            pLoop = null;
        }
    }

    private class PhysicsLoop extends Thread {
        private volatile boolean running = true;

        @Override
        public void run() {
            while (running) {
                try {
                    Thread.sleep(5);
                    draw();
                    handleCollision();
                    if (mBall != null) {
                        // Testing on just one screen for now
                        int position = mBall.update();
                        if (position != 0){
                            mBall.doRebound();
                        }
                    }
                } catch (InterruptedException ie) {
                    running = false;
                }
            }
        }

        public void safeStop() {
            running = false;
            interrupt();
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((resultCode == Activity.RESULT_OK) && (requestCode == SERVER_LIST_RESULT_CODE)) {

        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            Point p = new Point((int) event.getX(), (int) event.getY());
            mPaddlePoints.add(p);
            mPaddleTimes.add(System.currentTimeMillis());
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            Point p = new Point((int) event.getX(), (int) event.getY());
            mPaddlePoints.add(p);
            mPaddleTimes.add(System.currentTimeMillis());
            if (mPaddleTimes.size() > mPaddlePointWindowSize) {
                mPaddleTimes.remove(0);
                mPaddlePoints.remove(0);
            }
        } else {
            mPaddleTimes = new ArrayList<Long>();
            mPaddlePoints = new ArrayList<Point>();
        }
        return false;
    }

    public Point getBallCenter() {
        if (mBall == null) {
            return new Point(-1, -1);
        }
        int x = (int) mBall.getX();
        int y = (int) mBall.getY();
        return new Point(x + 10, y + 12);
    }

    public Point getPaddleCenter() {
        if (mPaddleTimes.size() > 0) {
            Point p = mPaddlePoints.get(mPaddlePoints.size() - 1);
            int x = p.x + 10;
            int y = p.y - 130;
            return new Point(x, y);
        } else {
            return new Point(-1, -1);
        }
    }

    public void handleCollision() {
        // TODO: Handle multiball case
        if (mBall == null) {
            return;
        }
        if (mPaddleTimes.size() < 1) {
            return;
        }

        Point ballCenter = getBallCenter();
        Point paddleCenter = getPaddleCenter();

        final int dy = ballCenter.y - paddleCenter.y;
        final int dx = ballCenter.x - paddleCenter.x;

        final float distance = dy * dy + dx * dx;

        if (distance < ((2 * mBallRadius) * (2 * mPaddleRadius))) {
            // Get paddle velocity
            float vX = 0;
            float vY = 0;
            Point endPoint = mPaddlePoints.get(mPaddlePoints.size() - 1);
            Point startPoint = mPaddlePoints.get(0);
            long timeDiff = mPaddleTimes.get(mPaddleTimes.size() - 1) - mPaddleTimes.get(0);
            if (timeDiff > 0) {
                vX = ((float) (endPoint.x - startPoint.x)) / timeDiff;
                vY = ((float) (endPoint.y - startPoint.y)) / timeDiff;
            }
            // Determine the bounce type
            BOUNCE_TYPE bounceType = BOUNCE_TYPE.TOP;
            if ((ballCenter.x < (paddleCenter.x - mPaddleRadius / 2))
                    && (ballCenter.y < (paddleCenter.y - mPaddleRadius / 2))) {
                bounceType = BOUNCE_TYPE.TOPLEFT;
            } else if ((ballCenter.x > (paddleCenter.x + mPaddleRadius / 2))
                    && (ballCenter.y < (paddleCenter.y - mPaddleRadius / 2))) {
                bounceType = BOUNCE_TYPE.TOPRIGHT;
            } else if ((ballCenter.x < (paddleCenter.x - mPaddleRadius / 2))
                    && (ballCenter.y > (paddleCenter.y + mPaddleRadius / 2))) {
                bounceType = BOUNCE_TYPE.BOTTOMLEFT;
            } else if ((ballCenter.x > (paddleCenter.x + mPaddleRadius / 2))
                    && (ballCenter.y > (paddleCenter.y + mPaddleRadius / 2))) {
                bounceType = BOUNCE_TYPE.BOTTOMRIGHT;
            } else if ((ballCenter.x < paddleCenter.x)
                    && (ballCenter.y > (paddleCenter.y - mPaddleRadius / 2))
                    && (ballCenter.y < (paddleCenter.y + mPaddleRadius / 2))) {
                bounceType = BOUNCE_TYPE.LEFT;
            } else if ((ballCenter.x > paddleCenter.x)
                    && (ballCenter.y > (paddleCenter.y - mPaddleRadius / 2))
                    && (ballCenter.y < (paddleCenter.y + mPaddleRadius / 2))) {
                bounceType = BOUNCE_TYPE.RIGHT;
            } else if ((ballCenter.x > (paddleCenter.x - mPaddleRadius / 2))
                    && (ballCenter.x < (paddleCenter.x + mPaddleRadius / 2))
                    && (ballCenter.y > paddleCenter.y)) {
                bounceType = BOUNCE_TYPE.RIGHT;
            }

            mBall.doBounce(bounceType, vX, vY);
        }

    }

}
