package com.reelmonster;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/**
 * Draws the floating bubble: the logo picture for the current stage (see LogoStage), plus a badge
 * with the count. The more reels, the bigger it gets.
 *
 * The 4 stage pictures (res/drawable-nodpi/logo_stage1..4.png) are made from one master logo
 * by logo/make_logo_stages.py.
 */
public class MonsterView extends View {

    private static final int[] STAGE_PICTURES = {
        R.drawable.logo_stage1, R.drawable.logo_stage2, R.drawable.logo_stage3, R.drawable.logo_stage4,
    };

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint picturePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final float dp;
    private int count = 0;
    private int stage = 0;   // which picture is loaded (1-4)
    private Bitmap picture;

    public MonsterView(Context context) {
        super(context);
        dp = context.getResources().getDisplayMetrics().density;
        setCount(0);
    }

    public void setCount(int count) {
        this.count = count;
        int newStage = LogoStage.forCount(count);
        if (newStage != stage) { // only load a picture when the stage changes
            stage = newStage;
            picture = BitmapFactory.decodeResource(getResources(), STAGE_PICTURES[stage - 1]);
        }
        requestLayout(); // size may change
        invalidate();    // redraw
    }

    /** 0.0 at 0 reels, 1.0 at 60+ reels. */
    private float growth() {
        return Math.min(count, 60) / 60f;
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int size = (int) ((64 + 56 * growth()) * dp); // grows from 64dp to 120dp
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();

        // The logo picture for the current stage, scaled to the view
        canvas.drawBitmap(picture, null, new RectF(0, 0, w, w), picturePaint);

        // Count badge in the top-right corner
        float r = w * 0.17f;
        float cx = w - r - 1, cy = r + 1;
        paint.setColor(Color.parseColor("#222222"));
        canvas.drawCircle(cx, cy, r, paint);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(cx, cy, r - 2 * dp, paint);
        String text = String.valueOf(count);
        paint.setColor(Color.parseColor("#222222"));
        paint.setFakeBoldText(true);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(text.length() <= 2 ? r * 1.1f : r * 0.8f);
        canvas.drawText(text, cx, cy - (paint.descent() + paint.ascent()) / 2, paint);
    }
}
