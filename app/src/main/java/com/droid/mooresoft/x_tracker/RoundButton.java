package com.droid.mooresoft.x_tracker;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;

/**
 * Created by Ed on 6/16/15.
 */
public class RoundButton extends Button {

    public RoundButton(Context context) {
        super(context);
        init();
    }

    public RoundButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        applyAttributes(context, attrs);
        init();
    }

    public RoundButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        applyAttributes(context, attrs);
        init();
    }

    private int mIconResourceId, mCircleColor, mShadowDx, mShadowDy; // custom attributes

    private void applyAttributes(Context context, AttributeSet attrs) {
        final TypedArray a = context.getTheme().obtainStyledAttributes(attrs,
                R.styleable.RoundButton, 0, 0);
        try {
            mIconResourceId = a.getResourceId(R.styleable.RoundButton_round_button_icon, -1);
            mCircleColor = a.getColor(R.styleable.RoundButton_round_button_color, android.R.color.darker_gray);
            mShadowDx = a.getInt(R.styleable.RoundButton_round_button_shadow_dx, 10); // default to slight shadow
            mShadowDy = a.getInt(R.styleable.RoundButton_round_button_shadow_dy, 10);
        } finally {
            a.recycle();
        }
    }

    private int mRadius;
    private Point mCenter;

    private void init() {
        // clear the background
        setBackgroundColor(0x00ffffff);

        // circle will fill the view
        mRadius = Math.min(getWidth() - getPaddingLeft() - getPaddingRight(),
                getHeight() - getPaddingTop() - getPaddingBottom()) / 2;
        mRadius -= Math.max(mShadowDx, mShadowDy); // allow room for the shadow
        mCenter = new Point(getWidth() / 2, getHeight() / 2); // center of the button
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // check if this touch is within the button radius
        float x = Math.abs(event.getX() - mCenter.x),
                y = Math.abs(event.getY() - mCenter.y); // distance from the button center
        double x2 = Math.pow(x, 2), y2 = Math.pow(y, 2);
        int touchRadius = (int) Math.pow(x2 + y2, 0.5);
        
        if (touchRadius > mRadius) {
            return false; // don't treat this as a click
        } else {
            return super.onTouchEvent(event); // do normal touch stuff
        }
    }

    @Override
    public boolean performClick() {
        /// show a little animation when user taps on the view
        ValueAnimator valAnim = ValueAnimator.ofFloat(0, 1);
        valAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        valAnim.setDuration(300); // milliseconds
        valAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                // animate the fraction of the circle the overlay covers
                percentOfRadius = (float) animation.getAnimatedValue();
                invalidate(); // redraw the view
            }
        });
        valAnim.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                // do nothing
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                percentOfRadius = 0; // remove the animation overlay
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                percentOfRadius = 0; // remove the animation overlay
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
                // do nothing
            }
        });
        valAnim.start();

        return super.performClick(); // do normal click stuff
    }

    // overlay circle radius as fraction of full radius
    private float percentOfRadius = 0;

    @Override
    public void onDraw(final Canvas canvas) {
        canvas.drawColor(0x00ffffff); // clear the canvas

        // circle will fill the view
        mRadius = Math.min(getWidth() - getPaddingLeft() - getPaddingRight(),
                getHeight() - getPaddingTop() - getPaddingBottom()) / 2;
        mRadius -= Math.max(mShadowDx, mShadowDy); // allow room for the shadow
        mCenter = new Point(getWidth() / 2, getHeight() / 2); // center of the button

        // draw the shadow behind the circle
        final Paint mPaint = new Paint();
        int[] colors = {
                0xff000000, 0x88000000, 0x00000000 // black, faded black, transparent
        };
        float[] stops = {
                0, 0.9f, 1 // center, close to edge, outer edge
        };
        RadialGradient gradient = new RadialGradient(mCenter.x + mShadowDx, mCenter.y + mShadowDy,
                mRadius, colors, stops, Shader.TileMode.CLAMP);
        mPaint.setShader(gradient);
        mPaint.setAntiAlias(true); // sharpen edges
        mPaint.setDither(true); // improve color
        canvas.drawCircle(mCenter.x + mShadowDx, mCenter.y + mShadowDy, mRadius, mPaint);

        // draw the circle
        mPaint.setColor(mCircleColor);
        mPaint.setShader(null); // remove the radial gradient
        canvas.drawCircle(mCenter.x, mCenter.y, mRadius, mPaint);

        // draw the animation overlay circle if necessary
        if (percentOfRadius != 0) {
            gradient = new RadialGradient(mCenter.x, mCenter.y, mRadius * percentOfRadius, 0x00000000, // transparent
                    0x11000000, Shader.TileMode.CLAMP); // very faded black
            mPaint.setShader(gradient);
            canvas.drawCircle(mCenter.x, mCenter.y, mRadius * percentOfRadius, mPaint);
        }

        // draw icon if available
        if (mIconResourceId == -1) { // user did not provide an icon
            return;
        } else {
            // load and scale the bitmap to fit inside the button
            Bitmap b = BitmapFactory.decodeResource(getResources(), mIconResourceId);
            final Bitmap finalBitmap = scaleBitmap(b);
            mPaint.setShader(null); // remove radial gradient
            canvas.drawBitmap(finalBitmap, mCenter.x - finalBitmap.getWidth() / 2,
                    mCenter.y - finalBitmap.getHeight() / 2, mPaint);
        }
    }

    private Bitmap scaleBitmap(Bitmap b) {
        int bWidth = b.getWidth(), bHeight = b.getHeight(); // bitmap dimensions
        int vWidth = getWidth() - getPaddingLeft() - getPaddingRight(),
                vHeight = getHeight() - getPaddingTop() - getPaddingBottom(); // allowed view dimension
        int radius = Math.min(vWidth, vHeight) / 2; // background circle radius
        int innerSquareSide = (int) (radius * Math.pow(2, 0.5)); // largest square contained within the circle
        int padding = innerSquareSide / 10; // want a little padding around the icon
        innerSquareSide -= padding;

        // will only make the bitmap smaller if necessary, never larger
        float widthScale = 1, heightScale = 1;
        if (bWidth > innerSquareSide) {
            widthScale = (float) innerSquareSide / bWidth;
        }
        if (bHeight > innerSquareSide) {
            heightScale = (float) innerSquareSide / bHeight;
        }

        // use the min scale to ensure both dimensions fit within the view
        float finalScale = Math.min(widthScale, heightScale);
        int finalWidth = (int) (bWidth * finalScale),
                finalHeight = (int) (bHeight * finalScale);
        if (finalWidth <= 0 || finalHeight <= 0) {
            return b; // just give up
        }
        return Bitmap.createScaledBitmap(b, finalWidth, finalHeight, false);
    }
}
