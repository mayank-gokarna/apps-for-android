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

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

/*
 * Marble drawn in the maze.
 */
public class Marble{
	
	// View controlling the marble.
	View view;
	
	// marble attributes 
	// x,y are private because we need boundary checking on any new values to make sure they are valid.
	private int x = 0;
	private int y = 0;
	int radius = 8;
	int color = Color.WHITE;
	int lives = 5;
	
	/*
	 * Marble constructor.
	 * @param view View controlling the marble
	 */
	public Marble(View view)
	{		
		this.view = view;
		init();
	}

	/*
	 * Setup marble starting co-ords.
	 */
	public void init()
	{
		x = radius * 6;
		y = radius * 6;
	}
	
	/*
	 * Draw the marble.
	 * @param canvas Canvas object to draw too.
	 * @param paint Paint object used to draw with.
	 */
	public void draw(Canvas canvas, Paint paint)
	{
		paint.setColor(color);
		canvas.drawCircle(x, y, radius, paint);
	}
	
	/*
	 * Attempt to update the marble with a new x value, boundary checking enabled to make sure the new co-ordinate is valid.
	 * @param newX Incremental value to add onto current x co-ordinate.
	 */
	public void updateX(float newX)
	{
		x += newX;
		
		// boundary checking, don't want the marble rolling off-screen.
		if (x+radius >= view.getWidth())
			x = view.getWidth()-radius;
		else if (x-radius < 0)
			x = radius;
		
	}
	
	/*
	 * Attempt to update the marble with a new y value, boundary checking enabled to make sure the new co-ordinate is valid.
	 * @param newY Incremental value to add onto current y co-ordinate.
	 */
	public void updateY(float newY)
	{
		y -= newY;	
		
		// boundary checking, don't want the marble rolling off-screen.
		if (y+radius >= view.getHeight())
			y = view.getHeight()-radius;
		else if (y-radius < 0)
			y = radius;
	}
	
	/*
	 * @return Current x co-ordinate.
	 */
	public int getX()
	{
		return x;
	}
	
	/*
	 * @return Current y co-ordinate.
	 */
	public int getY()
	{
		return y;
	}
}
