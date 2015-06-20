package com.droid.mooresoft.x_tracker;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.TextView;

/**
 * Created by Ed on 6/19/15.
 */
public class ShadowedTextView extends TextView {

    public ShadowedTextView(Context context) {
        super(context);
        init();
    }

    public ShadowedTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        applyAttributes(context, attrs);
        init();
    }

    public ShadowedTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        applyAttributes(context, attrs);
        init();
    }

    private int mShadowDx, mShadowDy; // custom attributes

    private void applyAttributes(Context context, AttributeSet attrs) {
        final TypedArray a = context.getTheme().obtainStyledAttributes(attrs,
                R.styleable.ShadowedTextView, 0, 0);
        try {
            // default to slight shadow
            mShadowDx = a.getInt(R.styleable.ShadowedTextView_shadowed_text_view_shadow_dx, 10);
            mShadowDy = a.getInt(R.styleable.ShadowedTextView_shadowed_text_view_shadow_dy, 10);
        } finally {
            a.recycle();
        }
    }

    private void init() {
        // force center gravity
        setGravity(Gravity.CENTER);
    }

    @Override
    public void onDraw(final Canvas canvas) {
        if (mShadowDx != 0 || mShadowDy != 0) { // draw a shadow behind the text
            final Paint shadowPaint = new Paint();
            shadowPaint.setColor(0x33000000); // very faded black
            shadowPaint.setAntiAlias(true);
            shadowPaint.setDither(true);

            String text = getText().toString(); // user text
            shadowPaint.setTextAlign(Paint.Align.CENTER);
            shadowPaint.setTextSize(getTextSize());
            Rect bounds = new Rect(); // text bounding box
            shadowPaint.getTextBounds(text, 0, text.length(), bounds);

            // draw the shadow
            canvas.drawText(text, (float) getWidth() / 2 + mShadowDx,
                    (float) getHeight() / 2 + bounds.height() / 2 + mShadowDy, shadowPaint);
        }

        super.onDraw(canvas); // draw all the normal stuff
    }
}
