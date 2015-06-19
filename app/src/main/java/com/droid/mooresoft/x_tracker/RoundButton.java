package com.droid.mooresoft.x_tracker;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.Toast;

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
        getAttributes(context, attrs);
        init();
    }

    public RoundButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        getAttributes(context, attrs);
        init();
    }

    private int mIconResourceId, mCircleColor, mShadowDx, mShadowDy; // custom attributes

    private void getAttributes(Context context, AttributeSet attrs) {
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

    private void init() {
        // clear the background
        setBackgroundColor(0x00ffffff);
    }

    @Override
    public boolean performClick() {
        // TODO: animate
        return super.performClick(); // do normal click stuff
    }

    @Override
    public void onDraw(final Canvas canvas) {
        canvas.drawColor(0x00ffffff); // clear the canvas

        // circle will fill the view
        int radius = Math.min(getWidth() - getPaddingLeft() - getPaddingRight(),
                getHeight() - getPaddingTop() - getPaddingBottom()) / 2;
        radius -= Math.max(mShadowDx, mShadowDy); // allow room for the shadow
        Point center = new Point(getWidth() / 2, getHeight() / 2); // center of the button

        // draw the shadow behind the circle
        final Paint mPaint = new Paint();
        int[] colors = {
                0xff000000, 0x88000000, 0x00000000 // black, faded black, transparent
        };
        float[] stops = {
                0, 0.9f, 1 // center, close to edge, outer edge
        };
        RadialGradient gradient = new RadialGradient(center.x + mShadowDx, center.y + mShadowDy,
                radius, colors, stops, Shader.TileMode.CLAMP);
        mPaint.setShader(gradient);
        mPaint.setAntiAlias(true); // sharpen edges
        mPaint.setDither(true); // improve color
        canvas.drawCircle(center.x + mShadowDx, center.y + mShadowDy, radius, mPaint);

        // draw the circle
        mPaint.setColor(mCircleColor);
        mPaint.setShader(null); // remove the radial gradient
        canvas.drawCircle(center.x, center.y, radius, mPaint);

        if (mIconResourceId == -1) { // user did not provide an icon
            return;
        } else {
            // load and scale the bitmap to fit inside the button
            Bitmap b = BitmapFactory.decodeResource(getResources(), mIconResourceId);
            final Bitmap finalBitmap = scaleBitmap(b);
            final Paint iconPaint = new Paint();
            iconPaint.setAntiAlias(true);
            iconPaint.setDither(true);
            canvas.drawBitmap(finalBitmap, center.x - finalBitmap.getWidth() / 2,
                    center.y - finalBitmap.getHeight() / 2, iconPaint);
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
