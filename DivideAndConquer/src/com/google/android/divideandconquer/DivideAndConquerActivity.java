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

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Stack;

/**
 * The activity for the game.  Listens for callbacks from the game engine, and
 * response appropriately, such as bringing up a 'game over' dialog when a ball
 * hits a moving line and there is only one life left.
 */
public class DivideAndConquerActivity extends Activity
        implements DivideAndConquerView.BallEventCallBack,
        NewGameCallback,
        DialogInterface.OnCancelListener,
        Eula.OnEulaAgreedTo {

    private boolean mAlreadyAgreedToEula = false;
    private boolean mEngineReady = false;

    /**
     * Each level has a different background color and ball color.
     */
    static class LevelStyle {
        private final int mBgColor;
        private final int mBallColor;

        LevelStyle(int bgColor, int ballColor) {
            mBgColor = 0xFF000000 | bgColor;
            mBallColor = 0xFF000000 |ballColor;
        }
    }

    /**
     * The styles we cycle through when leveling up.
     */
    static LevelStyle[] LEVEL_STYLES = new LevelStyle[] {
            new LevelStyle(0xFFD162FF, 0xFF76A7FF),
            new LevelStyle(0xFF76A7FF, 0xFF75FFE9),
            new LevelStyle(0xFF7E33, 0x8DBDD9),
            new LevelStyle(0x61FF6E, 0xD9B4D6),
            new LevelStyle(0x2BA7FF, 0xFF842B),
    };

    private static final int NEW_GAME_NUM_BALLS = 1;
    private static final double LEVEL_UP_THRESHOLD = 0.8;
    private static final int TRY_AGAIN_PAUSE = 1000;
    private static final int COLLISION_VIBRATE_MILLIS = 200;

    private boolean mVibrateOn;
    
    private int mNumBalls = NEW_GAME_NUM_BALLS;
    
    private DivideAndConquerView mBallsView;

    private static final int WELCOME_DIALOG = 20;
    private static final int GAME_OVER_DIALOG = 21;
    private WelcomeDialog mWelcomeDialog;
    private GameOverDialog mGameOverDialog;

    private TextView mLivesLeft;
    private TextView mPercentContained;
    private int mNumLives;
    private Vibrator mVibrator;
    private TextView mLevelInfo;
    private int mNumLivesStart = 5;

    private Toast mCurrentToast;

// usefull for playing around with colors
//    private int theindex = 0;
//    public boolean onKeyUp(int i, KeyEvent event) {
//        if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT) {
//            theindex++;
//            theindex = theindex % LEVEL_STYLES.length;
//            mBallsView.setColors(
//                    LEVEL_STYLES[theindex].mBgColor,
//                    LEVEL_STYLES[theindex].mBallColor
//                    );
//            return true;
//        } else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT) {
//            theindex--;
//            if (theindex < 0) theindex = LEVEL_STYLES.length - 1;
//            mBallsView.setColors(
//                    LEVEL_STYLES[theindex].mBgColor,
//                    LEVEL_STYLES[theindex].mBallColor
//                    );
//            return true;
//        }
//        return false;
//    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAlreadyAgreedToEula = Eula.show(this);

        // Turn off the title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.main);
        mBallsView = (DivideAndConquerView) findViewById(R.id.ballsView);
        mBallsView.setCallback(this);

        mPercentContained = (TextView) findViewById(R.id.percentContained);
        mLevelInfo = (TextView) findViewById(R.id.levelInfo);
        mLivesLeft = (TextView) findViewById(R.id.livesLeft);

        // we'll vibrate when the ball hits the moving line
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    }

    /** {@inheritDoc} */
    public void onEngineReady(BallEngine ballEngine) {
        // display 10 balls bouncing around for visual effect
        ballEngine.reset(SystemClock.elapsedRealtime(), 10);
        mBallsView.setColors(
                LEVEL_STYLES[0].mBgColor,
                LEVEL_STYLES[0].mBallColor
                );
        mBallsView.setMode(DivideAndConquerView.Mode.Bouncing);

        mEngineReady = true;

        // show the welcome dialog
        if (mAlreadyAgreedToEula) {
            showDialog(WELCOME_DIALOG);
        }
    }

    /** {@inheritDoc} */
    public void onEulaAgreedTo() {
        if (mEngineReady) {
            showDialog(WELCOME_DIALOG);
        } 
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == WELCOME_DIALOG) {
            mWelcomeDialog = new WelcomeDialog(this, this);
            mWelcomeDialog.setOnCancelListener(this);
            return mWelcomeDialog;
        } else if (id == GAME_OVER_DIALOG) {
            mGameOverDialog = new GameOverDialog(this, this);
            mGameOverDialog.setOnCancelListener(this);
            return mGameOverDialog;
        }
        return null;
    }

    @Override
    protected void onPause() {
        super.onPause();
        mBallsView.setMode(DivideAndConquerView.Mode.PausedByUser);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mVibrateOn = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(Preferences.KEY_VIBRATE, true);

        mNumLivesStart = Preferences.getCurrentDifficulty(this).getLivesToStart();
    }

    private static final int MENU_NEW_GAME = Menu.FIRST;
    private static final int MENU_SETTINGS = Menu.FIRST + 1;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        menu.add(0, MENU_NEW_GAME, MENU_NEW_GAME, "New Game");
        menu.add(0, MENU_SETTINGS, MENU_SETTINGS, "Settings");

        return true;        
    }

    /**
     * We pause the game while the menu is open; this remembers what it was
     * so we can restore when the menu closes
     */
    Stack<DivideAndConquerView.Mode> mRestoreMode = new Stack<DivideAndConquerView.Mode>();

    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        saveMode();
        mBallsView.setMode(DivideAndConquerView.Mode.Paused);
        return super.onMenuOpened(featureId, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        switch (item.getItemId()) {
            case MENU_NEW_GAME:
                cancelToasts();
                onNewGame();
                break;
            case MENU_SETTINGS:
                final Intent intent = new Intent();
                intent.setClass(this, Preferences.class);
                startActivity(intent);
                break;
        }

        mRestoreMode.pop(); // don't want to restore when an action was taken

        return true;
    }

    @Override
    public void onOptionsMenuClosed(Menu menu) {
        super.onOptionsMenuClosed(menu);
        restoreMode();
    }


    private void saveMode() {
        // don't want to restore to a state where user can't resume game.
        final DivideAndConquerView.Mode mode = mBallsView.getMode();
        final DivideAndConquerView.Mode toRestore = mode == DivideAndConquerView.Mode.Paused ? DivideAndConquerView.Mode.PausedByUser : mode;
        mRestoreMode.push(toRestore);
    }

    private void restoreMode() {
        if (!mRestoreMode.isEmpty()) {
            mBallsView.setMode(mRestoreMode.pop());
        }
    }

    /** {@inheritDoc} */
    public void onBallHitsMovingLine(final BallEngine ballEngine, float x, float y) {
        saveMode();
        mBallsView.setMode(DivideAndConquerView.Mode.Paused);
        if (--mNumLives == 0) {
            // vibrate three times
            if (mVibrateOn) {
                mVibrator.vibrate(
                    new long[]{0l, COLLISION_VIBRATE_MILLIS,
                                   50l, COLLISION_VIBRATE_MILLIS,
                                   50l, COLLISION_VIBRATE_MILLIS},
                        -1);
            }
            showDialog(GAME_OVER_DIALOG);
        } else {
            if (mVibrateOn) {
                mVibrator.vibrate(COLLISION_VIBRATE_MILLIS);
            }
            mBallsView.postDelayed(new Runnable() {
                public void run() {
                    ballEngine.reset(SystemClock.elapsedRealtime(), mNumBalls);
                    updateLivesDisplay(mNumLives, true);
                    updatePercentDisplay(0);
                    restoreMode();
                }
            }, TRY_AGAIN_PAUSE);
        }
    }

    /** {@inheritDoc} */
    public void onNewRegion(final BallEngine ballEngine) {
        final float percentageFilled = ballEngine.getPercentageFilled();
        updatePercentDisplay(percentageFilled);
        if (percentageFilled > LEVEL_UP_THRESHOLD) {
            levelUp(ballEngine);
        }
    }

    /**
     * Go to the next level
     * @param ballEngine The ball engine.
     */
    private void levelUp(final BallEngine ballEngine) {
        mNumBalls++;

        updatePercentDisplay(0);
        updateLevelDisplay(mNumBalls);
        ballEngine.reset(SystemClock.elapsedRealtime(), mNumBalls);
        updateLevelColors();
        mBallsView.setMode(DivideAndConquerView.Mode.Bouncing);
        showToast("level " + mNumBalls);
    }

    private void showToast(String text) {
        cancelToasts();
        mCurrentToast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
        mCurrentToast.show();
    }

    private void cancelToasts() {
        if (mCurrentToast != null) {
            mCurrentToast.cancel();
            mCurrentToast = null;
        }
    }

    /**
     * Update the header that displays how much of the space has been contained.
     * @param amountFilled The fraction, between 0 and 1, that is filled.
     */
    private void updatePercentDisplay(float amountFilled) {
        final int prettyPercent = (int) (amountFilled *100);
        mPercentContained.setText(
                getString(R.string.percent_contained, prettyPercent));
    }

    /** {@inheritDoc} */
    public void onNewGame() {
        mNumBalls = NEW_GAME_NUM_BALLS;
        mNumLives = mNumLivesStart;
        updatePercentDisplay(0);
        updateLivesDisplay(mNumLives, false);
        updateLevelDisplay(mNumBalls);
        updateLevelColors();
        mBallsView.getEngine().reset(SystemClock.elapsedRealtime(), mNumBalls);
        mBallsView.setMode(DivideAndConquerView.Mode.Bouncing);
    }

    /**
     * Update the background color and ball color appropriate to the current level.
     */
    private void updateLevelColors() {
        final int index = (mNumBalls - 1) % LEVEL_STYLES.length;
        mBallsView.setColors(
                LEVEL_STYLES[index].mBgColor,
                LEVEL_STYLES[index].mBallColor
                );
    }

    /**
     * Update the header displaying the current level
     */
    private void updateLevelDisplay(int numBalls) {
        mLevelInfo.setText(getString(R.string.level, numBalls));
    }

    /**
     * Update the display showing the number of lives left.
     * @param numLives The number of lives left.
     * @param showToast Whether to show a toast with the number of lives left
     *   too.
     */
    void updateLivesDisplay(int numLives, boolean showToast) {
        String text = (numLives == 1) ?
                getString(R.string.one_life_left) : getString(R.string.lives_left, numLives);
        mLivesLeft.setText(text);
        if (showToast) {
            showToast(text);
        }
    }

    /** {@inheritDoc} */
    public void onCancel(DialogInterface dialog) {
        if (dialog == mWelcomeDialog || dialog == mGameOverDialog) {
            // user hit back, they're done
            finish();
        }
    }
}
