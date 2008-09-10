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

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * A ball region is a rectangular region that contains bouncing
 * balls, and possibly one animating line.  In its {@link #update(long)} method,
 * it will update all of its balls, the moving line.  It detects collisions
 * between the balls and the moving line, and when the line is complete, handles
 * splitting off a new region.
 */
public class BallRegion extends Shape2d {

    private float mLeft;
    private float mRight;
    private float mTop;
    private float mBottom;

    private List<Ball> mBalls;

    private AnimatingLine mAnimatingLine;

    /*
     * @param left The minimum x component
     * @param right The maximum x component
     * @param top The minimum y component
     * @param bottom The maximum y component
     * @param ballCapacity The number of balls the region might have (hint used to set up container)
     */
    public BallRegion(float left, float right, float top, float bottom, int ballCapacity) {
        mLeft = left;
        mRight = right;
        mTop = top;
        mBottom = bottom;
        mBalls = new ArrayList<Ball>(ballCapacity);
    }

    /**
     * Add a ball to the region.  It will also set the balls region to this region.
     * @param ball The ball
     */
    public void addBall(Ball ball) {
        ball.setRegion(this);
        mBalls.add(ball);
    }

    public float getLeft() {
        return mLeft;
    }

    public float getRight() {
        return mRight;
    }

    public float getTop() {
        return mTop;
    }

    public float getBottom() {
        return mBottom;
    }

    public List<Ball> getBalls() {
        return mBalls;
    }


    public AnimatingLine getAnimatingLine() {
        return mAnimatingLine;
    }

    public void setNow(long now) {
        // update the balls
        for (int i = 0; i < mBalls.size(); i++) {
            final Ball ball = mBalls.get(i);
            ball.setNow(now);
        }

        if (mAnimatingLine != null) {
            mAnimatingLine.setNow(now);
        }
    }

    /**
     * @return the area in the region in pixel*pixel
     */
    public float getArea() {
        return (mRight - mLeft) * (mBottom - mTop);
    }


    /**
     * Update the balls an (if it exists) the animating line in this region.
     * @param now in millis
     * @return A new region if a split has occured because the animating line
     *     finished.
     * @throws BallHitMovingLineException If any of the balls collides with
     *     the animating line.
     */
    public BallRegion update(long now) throws BallHitMovingLineException {

        // update the animating line
        final boolean newRegion =
                (mAnimatingLine != null && mAnimatingLine.update(now));

        // update the balls, look for collision
        for (int i = 0; i < mBalls.size(); i++) {
            final Ball ball = mBalls.get(i);
            ball.update(now);
            if (mAnimatingLine != null && ball.isIntersecting(mAnimatingLine)) {
                throw new BallHitMovingLineException(ball.getX(), ball.getY());
            }
        }

        // no collsion, new region means we need to split out the apropriate
        // balls into a new region
        if (newRegion) {
            BallRegion otherRegion = splitRegion(
                    mAnimatingLine.getDirection(),
                    mAnimatingLine.getPerpAxisOffset());
            mAnimatingLine = null;
            return otherRegion;
        } else {
            return null;
        }
    }

    /**
     * Return whether this region can start a line at a certain point.
     */
    public boolean canStartLineAt(float x, float y) {
        return mAnimatingLine == null && isPointWithin(x, y);
    }


    /**
     * Start a horizontal line at a point.
     * @param now What 'now' is.
     * @param x The x coordinate.
     * @param y The y coordinate.
     */
    public void startHorizontalLine(long now, float x, float y) {
        if (!canStartLineAt(x, y)) {
            throw new IllegalArgumentException(
                    "can't start line with point (" + x + "," + y + ")");
        }
        mAnimatingLine =
                new AnimatingLine(Direction.Horizontal, now, y, x, mLeft, mRight);
    }

    /**
     * Start a vertical line at a point.
     * @param now What 'now' is.
     * @param x The x coordinate.
     * @param y The y coordinate.
     */
    public void startVerticalLine(long now, float x, float y) {
        if (!canStartLineAt(x, y)) {
            throw new IllegalArgumentException(
                    "can't start line with point (" + x + "," + y + ")");
        }
        mAnimatingLine =
                new AnimatingLine(Direction.Vertical, now, x, y, mTop, mBottom);
    }

    /**
     * Splits this region at a certain offset, shrinking this one down and returning
     * the other region that makes up the rest.
     * @param direction The direction of the line.
     * @param perpAxisOffset The offset of the perpendicular axis of the line.
     * @return A new region containing a portion of the balls.
     */
    private BallRegion splitRegion(Direction direction, float perpAxisOffset) {

        if (direction == Direction.Horizontal) {
            BallRegion region = new BallRegion(mLeft, mRight, perpAxisOffset,
                    mBottom, mBalls.size() / 2);
            Iterator<Ball> it = mBalls.iterator();
            while (it.hasNext()) {
                Ball ball = it.next();
                if (ball.getY() > perpAxisOffset) {
                    it.remove();
                    region.addBall(ball);
                }
            }
            mBottom = perpAxisOffset;
            return region;
        } else  {
            assert(direction == Direction.Vertical);
            BallRegion region = new BallRegion(perpAxisOffset, mRight, mTop,
                    mBottom, mBalls.size() / 2);
            Iterator<Ball> it = mBalls.iterator();
            while (it.hasNext()) {
                Ball ball = it.next();
                if (ball.getX() > perpAxisOffset) {
                    it.remove();
                    region.addBall(ball);
                }
            }
            mRight = perpAxisOffset;
            return region;
        }
    }

}
