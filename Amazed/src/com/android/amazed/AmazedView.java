/*
 * Copyright (C) 2008 Jason Tomlinson.
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

package com.android.amazed;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

/*
 * Custom view used to draw the maze and marble.  Responds to accelerometer updates to roll the marble around the screen.
 */
public class AmazedView extends View
{
	Marble marble;
	Maze maze;
	Context context;
	Activity activity;
	
	// canvas we paint to.
	Canvas mCanvas;

	Paint paint;
	Typeface font = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);
	int textPadding = 10;
	
	// game states
	final static int NULL_STATE		= -1;
	final static int GAME_INIT		= 0;
	final static int GAME_RUNNING 	= 1;
	final static int GAME_OVER 		= 2;
	final static int GAME_COMPLETE 	= 3;
	final static int GAME_LANDSCAPE	= 4;
	// current state of the game
	static int currentState 		= NULL_STATE;
	
	// this prevents the user from dying instantly when they start a level if the device is tilted.
	boolean bWarning = false;
	
	// screen dimensions
	int canvasWidth = 0;
	int canvasHeight = 0;
	// are we running in portrait mode.
	boolean bPortrait;
	
	// current level
	int level = 1;

	// timing used for scoring.
	long totalTime = 0;
	long startTime = 0;
	long endTime = 0;

	// sensor manager used to control the accelerometer sensor.
	public SensorManager mSensorManager;
	// accelerometer sensor values.
	float accelX = 0;
	float accelY = 0;
	float accelZ = 0;	// this is never used but just in-case future versions make use of it.
	
	//accelerometer buffer, currently set to 0 so even the slightest movement will roll the marble.
	float sensorBuffer = 0;
	
	// http://code.google.com/android/reference/android/hardware/SensorManager.html#SENSOR_ACCELEROMETER
	// for an explanation on the values reported by SENSOR_ACCELEROMETER.
	public final SensorListener sensorAccelerometer = new SensorListener()
	{
		// method called whenever new sensor values are reported.
		public void onSensorChanged(int sensor, float[] values)
		{
			// grab the values required to respond to user movement.
			accelX = values[0];
			accelY = values[1];
			accelZ = values[2];
		}

		// reports when the accuracy of sensor has change
		// SENSOR_STATUS_ACCURACY_HIGH 	 = 3 
		// SENSOR_STATUS_ACCURACY_LOW 	 = 1
		// SENSOR_STATUS_ACCURACY_MEDIUM = 2
		// SENSOR_STATUS_UNRELIABLE		 = 0   //calibration required.
		public void onAccuracyChanged(int sensor, int accuracy) 
		{
			// currently not used
		}
	};
	
	/*
	 * Custom view constructor.
	 * @param context Application context
	 * @param activity Activity controlling the view
	 */
	public AmazedView(Context context, Activity activity) 
	{
		super(context);
		this.context = context;
		this.activity = activity;
		
		// init paint and make is look "nice" with anti-aliasing.
		paint = new Paint();
		paint.setTextSize(14);
		paint.setTypeface(font);	
		paint.setAntiAlias(true);
	
		// setup accelerometer sensor manager.
		mSensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
		// register our accelerometer so we can receive values.  SENSOR_DELAY_GAME is the recommended rate for games
		mSensorManager.registerListener(sensorAccelerometer, SensorManager.SENSOR_ACCELEROMETER, SensorManager.SENSOR_DELAY_GAME);

		// init our maze and marble.
		maze = new Maze(context);
		marble = new Marble(this);
		
		// set the starting state of the game.
		switchGameState(GAME_INIT);	
	}

	protected void onSizeChanged(int w, int h, int oldw, int oldh)
	{
		super.onSizeChanged(w, h, oldw, oldh);
		 
		// get new screen dimensions.
		canvasWidth = w;
		canvasHeight = h;
		 
		// are we in portrait or landscape mode now?
		// you could use bPortrait = !bPortrait however in the future who know's how many different ways a device screen
		// may be rotated.
		if (canvasHeight > canvasWidth)
			bPortrait = true;
		else
		{
			bPortrait = false;
			switchGameState(GAME_LANDSCAPE);
		}
	}
	
	/*
	 * Called every cycle, used to process current game state.
	 */
	public void gameTick()
	{
		// very basic state machine, makes a good foundation for a more complex game.
		switch (currentState)
		{
			case GAME_INIT:
				// prepare a new game for the user.
				initNewGame();
				switchGameState(GAME_RUNNING);
			
			case GAME_RUNNING:
				// update our marble.
				if (!bWarning)
					updateMarble();
				break;
		}
		
		// redraw the screen once our tick function is complete.
		invalidate();	
	}
	
	/*
	 * Reset game variables in preparation for a new game.
	 */
	public void initNewGame()
	{
		marble.lives = 5;
		totalTime = 0;
		level = 0;
		initLevel();
	}
	
	/*
	 * Initialize the next level.
	 */
	public void initLevel()
	{
		if (level < maze.MAX_LEVELS)
		{
			// setup the next level.
			bWarning = true;
			level++;
			maze.load(level);
			marble.init();
		}
		else
		{
			// user has finished the game, update state machine.
			switchGameState(GAME_COMPLETE);
		}
	}

	/*
	 * Called from gameTick(), update marble x,y based on latest values obtained from the Accelerometer sensor.  AccelX and
	 *  accelY are values received from the accelerometer, higher values represent the device tilted at a more acute angle.
	 */
	public void updateMarble()
	{
		// we CAN give ourselves a buffer to stop the marble from rolling even though we think the device is "flat".
		if (accelX > sensorBuffer || accelX < -sensorBuffer)
			marble.updateX(accelX);
		if (accelY > sensorBuffer || accelY < -sensorBuffer)
			marble.updateY(accelY);

	
		// check which cell the marble is currently occupying.
		if (maze.getCellType(marble.getX(), marble.getY()) == maze.VOID_TILE)
		{
			// user entered the "void".
			if (marble.lives > 0)
			{
				// user still has some lives remaining, restart the level.
				marble.lives--;
				marble.init();
				bWarning = true;
			}
			else
			{
				// user has no more lives left, end of game.
				endTime = System.currentTimeMillis();
				totalTime += endTime - startTime;
				switchGameState(GAME_OVER);
			}
		
		}
		else if (maze.getCellType(marble.getX(), marble.getY()) == maze.EXIT_TILE)
		{
			// user has reached the exit tiles, prepare the next level.
			endTime = System.currentTimeMillis();
			totalTime += endTime - startTime;
			initLevel();
		}
	}
	
	public boolean onTouchEvent(MotionEvent event) 
	{
		// we only want to handle down events .
		if (event.getAction() == MotionEvent.ACTION_DOWN)
		{
			if (currentState == GAME_OVER || currentState == GAME_COMPLETE)
			{
				// re-start the game.
				currentState = GAME_INIT;
			}
			else if (currentState == GAME_RUNNING)
			{
				// in-game, remove the pop-up text so user can play.
				bWarning = false;
				startTime = System.currentTimeMillis();
			}
		}
		return true;
	} 
	
	public boolean onKeyDown(int keyCode, KeyEvent event) 
	{
		// quit application if user presses the back key.
		if (keyCode == KeyEvent.KEYCODE_BACK)
		    cleanUp();
		
	    return true;
	}

	public void onDraw(Canvas canvas)
	{
		// update our canvas reference.
		mCanvas = canvas;
		
		// clear the screen.
		paint.setColor(Color.WHITE);
		mCanvas.drawRect(0, 0, canvasWidth, canvasHeight, paint);
		
		// simple state machine, draw screen depending on the current state.
		switch(currentState)
		{
			case GAME_RUNNING:
				// draw our maze first since everything else appears "on top" of it.
				maze.draw(mCanvas, paint);
		
				// draw our marble and hud.
				marble.draw(mCanvas, paint);
				drawHUD();
				break;
				
			case GAME_OVER:
				drawGameOver();
				break;
				
			case GAME_COMPLETE:
				drawGameComplete();
				break;
				
			case GAME_LANDSCAPE:
				drawLandscapeMode();
				break;
		}
		
		gameTick();
		
	}
	
	/*
	 * Called from onDraw(), draws the in-game HUD
	 */
	public void drawHUD()
	{
		paint.setColor(Color.BLACK);
		String hudText = context.getString(R.string.time) + ": " + (totalTime/1000);
		mCanvas.drawText(hudText, textPadding, 440, paint);
		hudText = context.getString(R.string.level) + ": " + level;
		mCanvas.drawText(hudText, (canvasWidth-paint.measureText(hudText))/2, 440, paint);
		hudText = context.getString(R.string.lives) + ": " + marble.lives;
		mCanvas.drawText(hudText, canvasWidth-paint.measureText(hudText)-textPadding, 440, paint); 
		
		// do we need to display the warning message to save the user from possibly dying instantly.
		if (bWarning)
		{
			paint.setColor(Color.BLUE);
			mCanvas.drawRect(0,canvasHeight/2-15, canvasWidth, canvasHeight/2+5, paint);
			paint.setColor(Color.WHITE);
			hudText = context.getString(R.string.tap_screen);
			mCanvas.drawText(hudText, (canvasWidth-paint.measureText(hudText))/2, canvasHeight/2, paint);			
		}
	}

	/*
	 * Called from onDraw(), draws the game over screen.
	 */
	public void drawGameOver()
	{	
		paint.setColor(Color.BLACK);
		String text = context.getString(R.string.game_over);
		mCanvas.drawText(text, (canvasWidth-paint.measureText(text))/2, canvasHeight/2, paint);
		text = context.getString(R.string.total_time) + ": " + (totalTime/1000) + "s";
		mCanvas.drawText(text, (canvasWidth-paint.measureText(text))/2, canvasHeight/2+paint.getFontSpacing(), paint);
		text = context.getString(R.string.game_over_msg_a) + " " + (level-1) + " " + context.getString(R.string.game_over_msg_b);
		mCanvas.drawText(text, (canvasWidth-paint.measureText(text))/2, canvasHeight/2+(paint.getFontSpacing()*2), paint);
		text = context.getString(R.string.restart);
		mCanvas.drawText(text, (canvasWidth-paint.measureText(text))/2, canvasHeight-(paint.getFontSpacing()*3), paint);
	}
	
	/*
	 * Called from onDraw(), draws the game complete screen.
	 */
	public void drawGameComplete()
	{
		paint.setColor(Color.BLACK);
		String text = context.getString(R.string.game_complete);
		mCanvas.drawText(text, (canvasWidth-paint.measureText(text))/2, canvasHeight/2, paint);
		text = context.getString(R.string.total_time) + ": " + (totalTime/1000) + "s";
		mCanvas.drawText(text, (canvasWidth-paint.measureText(text))/2, canvasHeight/2+paint.getFontSpacing(), paint);
		text = context.getString(R.string.restart);
		mCanvas.drawText(text, (canvasWidth-paint.measureText(text))/2, canvasHeight-(paint.getFontSpacing()*3), paint);
	}

	/*
	 * Called from onDraw(), displays a message asking the user to return the device back to portrait mode.
	 */
	public void drawLandscapeMode()
	{
		paint.setColor(Color.WHITE);
		mCanvas.drawRect(0, 0, canvasWidth, canvasHeight, paint);
		paint.setColor(Color.BLACK);
		String text = context.getString(R.string.landscape_mode);
		mCanvas.drawText(text, (canvasWidth-paint.measureText(text))/2, canvasHeight/2, paint);
	
	}
	
	/*
	 * Updates the current game state with a new state.  At the moment this is very basic however if the game was to
	 * get more complicated the code required for changing game states could grow quickly.  
	 * @param newState New game state 
	 */
	public void switchGameState(int newState)
	{
		currentState = newState;
	}

	/*
	 * Register the accelerometer sensor so we can use it in-game.
	 */
	public void registerListener()
	{
	    mSensorManager.registerListener(sensorAccelerometer, SensorManager.SENSOR_ACCELEROMETER, SensorManager.SENSOR_DELAY_GAME);
   	}

	/*
	 * Unregister the accelerometer sensor otherwise it will continue to operate and report values.
	 */
	public void unregisterListener() 
	{
    	mSensorManager.unregisterListener(sensorAccelerometer);
	}
	
	/*
	 * Clean up the custom view and exit the application.
	 */
	public void cleanUp()
	{
		marble = null;
		maze = null;
		unregisterListener();
		activity.finish();
	}
	
}
