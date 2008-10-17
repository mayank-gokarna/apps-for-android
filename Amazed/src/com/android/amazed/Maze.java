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

import java.io.InputStream;
import java.lang.reflect.Field;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;

/*
 * Maze drawn on screen, each new level is loaded once the previous level has been completed.
 */
public class Maze
{
	// application context
	static Context context;
	
	// maze tile size and dimension
	final static int TILE_SIZE = 16;
	final static int MAZE_COLS = 20;
	final static int MAZE_ROWS = 26;
	
	// tile types
	final static int PATH_TILE = 0;
	final static int VOID_TILE = 1;
	final static int EXIT_TILE = 2;
	
	// tile colors
	final static int VOID_COLOR = Color.BLACK;
	
	// maze level data
	static int[] mazeData; 
	
	// number of level
	final static int MAX_LEVELS = 10;
	
	// current tile attributes
	Rect tileRect = new Rect();
	int row;
	int col;
	int x;
	int y;
	
	// tile bitmaps
	Bitmap imgPath;
	Bitmap imgExit;
	
	/*
	 * Maze constructor.
	 * @param context Application context used to load images.
	 */
	Maze(Context context)
	{
		this.context = context;
		
		// load bitmaps.
		imgPath = BitmapFactory.decodeResource(context.getResources(), R.drawable.path);
		imgExit = BitmapFactory.decodeResource(context.getResources(), R.drawable.exit);		
	}
	
	/*
	 * Load specified maze level.
	 * @param level Maze level to be loaded.
	 */
	void load(int level)
	{		
		// maze data is stored in the res/raw folder as level1.txt, level2.txt etc....
		String levelNum = "level" + level;
		
		try
		{
			// construct our maze data array.
			mazeData = new int[MAZE_ROWS*MAZE_COLS];
			// attempt to load maze data.
			InputStream is = loadRawInput(levelNum);
			
			// we need to loop through the input stream and load each tile for the current maze.
			for (int i=0;i<mazeData.length;i++)
			{
				// data is stored in unicode so we need to convert it.
				mazeData[i] = Character.getNumericValue(is.read());
				
				// skip the "," and white space in our human readable file.
				is.read();
				is.read();
			}
		}
		catch (Exception e)
		{
			Log.i("Maze","load exception: " + e);
		}
		
	}
	
	/*
	 * Draw the maze.
	 * @param canvas Canvas object to draw too.
	 * @param paint Paint object used to draw with.
	 */
	public void draw(Canvas canvas, Paint paint)
	{
	
		// loop through our maze and draw each tile individually.
		for (int i=0; i<mazeData.length; i++)
		{
			// calculate the row and column of the current tile.
			row = i / (MAZE_COLS);
			col = i % (MAZE_COLS);
			
			// convert the row and column into actual x,y co-ordinates so we can draw it on screen.
			x = col*TILE_SIZE;
			y = row*TILE_SIZE;
		
			// draw the actual tile based on type.
			if (mazeData[i] == PATH_TILE)
				canvas.drawBitmap(imgPath, x, y, paint);
			else if (mazeData[i] == EXIT_TILE)
				canvas.drawBitmap(imgExit, x, y, paint);
			else if (mazeData[i] == VOID_TILE)
			{
				// since our "void" tile is purely black lets draw a rectangle instead of using an image.
				
				// tile attributes we are going to paint.
				tileRect.left = x;
				tileRect.top = y;
				tileRect.right = x+TILE_SIZE;
				tileRect.bottom = y+TILE_SIZE;
			
				paint.setColor(VOID_COLOR);		
				canvas.drawRect(tileRect, paint);
			}
		}

	}
	
	/*
	 * Determine which cell the marble currently occupies.
	 * @param x Current x co-ordinate.
	 * @param y Current y co-ordinate.
	 * @return The actual cell occupied by the marble.
	 */
	public int getCellType(int x, int y)
	{
		// convert the x,y co-ordinate into row and col values.
		int cellCol = x/TILE_SIZE;
		int cellRow = y/TILE_SIZE;
		
		// location is the row,col coordinate converted so we know where in the maze array to look.
		int location = 0;
		
		// if we are beyond the 1st row need to multiple by the number of columns.
		if (cellRow > 0)
			location = cellRow * MAZE_COLS;
		
		// add the column location.
		location += cellCol;
		
		return mazeData[location];
	}
	
	/*
	 * Android version of the standard java InputStream.
	 * @param file Name of file to load.
	 * @return InputStream containing file.
	 */
	public static InputStream loadRawInput(String file) 
	{
		InputStream is = null;
		try {
			Field f = R.raw.class.getDeclaredField(file);
			int id = f.getInt(null);
			is = context.getResources().openRawResource(id);
		} catch (Exception e) {
			Log.i("Maze","loadRawInput exception: " + e);
		}
		return is;
	}
}