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
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

// A view that acts as a palette to let players choose colors.
public class ColorChooser extends View implements OnTouchListener {

    public static final String TAG = "ColorChooser";

    // Allow me to retrieve a list of colors, and someone to send selection events to.
    public interface ColorChooserListener {
        // Set selected color
        public void setColor(short color);

        // Find out what color is selected
        public short getColor();

        // Get list of colors to render---can be any length
        public int[] getColorList();
    };

    ColorChooserListener mListener;

    private final Paint p = new Paint();
    private final RectF mRect = new RectF();

    public ColorChooser(Context context, AttributeSet attrs) {
        super(context, attrs);

        setOnTouchListener(this);
    }

    public void setDrawView(ColorChooserListener ccl) {
        mListener = ccl;

        invalidate();
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (mListener == null) {
            Log.d(TAG, "You appear to have not set a ColorChooser listener.");
            return;
        }

        canvas.drawColor(0xFF000000);

        // draw each of the color swatches in the palette
        for (int i = 0; i < mListener.getColorList().length; i++) {
            p.setColor(DrawView.colorMap[i]);

            mRect.top = (i * getHeight()) / DrawView.colorMap.length;
            mRect.bottom = ((i + 1) * getHeight()) / DrawView.colorMap.length;
            mRect.left = 0;
            mRect.right = getWidth();

            canvas.drawRoundRect(mRect, 5, 5, p);

            if (mListener.getColor() == i) {
                p.setColor(0xFFFFFFFF);
                mRect.top = (i * getHeight()) / DrawView.colorMap.length + 10;
                mRect.bottom = ((i + 1) * getHeight())
                        / DrawView.colorMap.length - 10;
                mRect.left = 10;
                mRect.right = getWidth() - 10;

                canvas.drawRoundRect(mRect, 5, 5, p);
            }
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        if (mListener == null) {
            Log.d(TAG, "You appear to have not set a ColorChooser listener.");
            return false;
        }

        mListener.setColor((short) Math.floor(event.getY()
                / ((getHeight() + 1.0) / DrawView.colorMap.length)));

        invalidate();

        return false;
    }
}
