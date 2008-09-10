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

/**
 * Thrown when a collision is detected within an update of a
 * {@link BallEngine}; a ball hit a moving line and it's game
 * over dude.
 */
public class BallHitMovingLineException extends Exception {

    final float mX;
    final float mY;

    /**
     * @param x The x coord of the collision.
     * @param y The y coord of the collision.
     */
    public BallHitMovingLineException(float x, float y) {
        mX = x;
        mY = y;
    }

    public float getX() {
        return mX;
    }

    public float getY() {
        return mY;
    }
}
