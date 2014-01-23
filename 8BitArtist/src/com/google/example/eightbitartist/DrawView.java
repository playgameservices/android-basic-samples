/*
 * Copyright (C) 2013 Google Inc.
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

package com.google.example.eightbitartist;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

// This View is the canvas on which the user can paint their masterpiece of
// beautifully pixelated art Every time the user touches this view, the corresponding
// pixel's color will be changed to the currently active drawing color.
public class DrawView extends View implements OnTouchListener, ColorChooser.ColorChooserListener {

    public static final int GRID_SIZE = 10;
    public static final String TAG = "DrawView";

    public short grid[][];

    public double mHeightInPixels;

    public short mSelectedColor = 1;

    private int lastGridX = -1, lastGridY = -1;

    // These are the four colors provided for painting.
    // If years of classic has taught me anything, these
    // are enough colors for anything. Anything at all.
    public static int colorMap[] = {
            0xFF000000, 0xFF0000FF, 0xFFFF0000,
            0xFF00FF00
    };

    // Some temporary variables so we don't allocate while rendering
    public Rect mRect = new Rect();
    public Paint p = new Paint();

    public Boolean keepAnimating = false;

    private DrawingActivity mDrawingActivity;

    public DrawView(Context context, AttributeSet attrs) {
        super(context, attrs);

        grid = new short[GRID_SIZE][GRID_SIZE];

        setOnTouchListener(this);

        setAnimating(true);
    }

    public void setActivity(DrawingActivity da) {
        mDrawingActivity = da;
    }

    public void setAnimating(Boolean val) {
        keepAnimating = val;
        if (val) {
            invalidate();
        }
    }

    // Convert back from screenspace
    public int sp(double screenSpaceCoordinate) {
        return (int) Math.round(screenSpaceCoordinate * mHeightInPixels);
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Assume this is a square (as we will make it so in onMeasure()
        // and figure out how many pixels there are.
        mHeightInPixels = this.getHeight();

        // Now, draw with 0,0 in upper left and 9,9 in lower right
        for (int x = 0; x < GRID_SIZE; x++) {
            for (int y = 0; y < GRID_SIZE; y++) {
                p.setColor(colorMap[grid[x][y]]);

                mRect.top = sp(y * 1.0 / GRID_SIZE);
                mRect.left = sp(x * 1.0 / GRID_SIZE);
                mRect.right = sp((x + 1.0) / GRID_SIZE);
                mRect.bottom = sp((y + 1.0) / GRID_SIZE);

                canvas.drawRect(mRect, p);
            }
        }

        if (keepAnimating) {
            invalidate();
        }
    }

    // We would like a square working area, so at layout time, we'll figure out how much room we
    // have grow to fit the larger of the parent's measure. This way we'll completely fill the
    // parent in one dimension.
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
        int parentHeight = MeasureSpec.getSize(heightMeasureSpec);

        if (parentWidth > parentHeight) {
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(parentHeight,
                    MeasureSpec.EXACTLY);
        } else {
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(parentWidth,
                    MeasureSpec.EXACTLY);
        }

        setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public boolean onTouch(View arg0, MotionEvent me) {
        if (mDrawingActivity.mMyRole == DrawingActivity.ROLE_GUESSER
                || mDrawingActivity.mMyRole == DrawingActivity.ROLE_NOTHING) {
            // If I'm guessing or watching a replay, I'm not drawing.
            return false;
        }

        switch (me.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                // Find where the touch event, which is in pixels, maps
                // to our 10x10 grid. (0,0) is in the upper left, (9, 9)
                // is in the lower right.
                int gridX = (int) Math.floor(1.0 * me.getX() / mHeightInPixels
                        * GRID_SIZE);
                int gridY = (int) Math.floor(1.0 * me.getY() / mHeightInPixels
                        * GRID_SIZE);

                Log.d(TAG, "You touched " + gridX + " " + gridY + "/" + me.getY());

                if (gridX < GRID_SIZE && gridY < GRID_SIZE && gridX >= 0
                        && gridY >= 0) {
                    grid[gridX][gridY] = mSelectedColor;

                    // Don't double-hit
                    if (lastGridX != gridX || gridY != lastGridY) {
                        mDrawingActivity
                                .emitDrawEvent(gridX, gridY, mSelectedColor);
                        lastGridX = gridX;
                        lastGridY = gridY;
                    }
                }

                return true;
        }

        return false;
    }

    public void setMacroPixel(int gridX, int gridY, short colorIndex) {
        // paint that pixel with the currently selected color
        grid[gridX][gridY] = colorIndex;
    }

    // When you hit the "Clear" button, you want to send
    public void clear(boolean sendMessage) {
        lastGridX = -1;
        lastGridY = -1;

        for (int x = 0; x < GRID_SIZE; x++) {
            for (int y = 0; y < GRID_SIZE; y++) {
                grid[x][y] = 0;
            }
        }

        if (sendMessage) {
            mDrawingActivity.emitClearEvent();
        }
    }

    // Methods for ColorChooserListener
    @Override
    public void setColor(short color) {
        mSelectedColor = color;
    }

    @Override
    public short getColor() {
        return mSelectedColor;
    }

    @Override
    public int[] getColorList() {
        return DrawView.colorMap;
    }
}
