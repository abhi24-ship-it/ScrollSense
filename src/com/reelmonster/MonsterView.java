package com.reelmonster;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import java.util.Random;

/**
 * Draws the monster with simple shapes (circles and ovals), plus a badge with the count.
 * The more reels, the bigger the monster, the wider its mouth and the "angrier" its colour.
 * It also picks the funny line the monster says.
 */
public class MonsterView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float dp;
    private int count = 0;

    public MonsterView(Context context) {
        super(context);
        dp = context.getResources().getDisplayMetrics().density;
    }

    public void setCount(int count) {
        this.count = count;
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

    private int bodyColor() {
        if (count < 10) return Color.parseColor("#7ED957"); // happy green
        if (count < 30) return Color.parseColor("#FFB347"); // orange
        if (count < 60) return Color.parseColor("#FF6B6B"); // red
        return Color.parseColor("#9B5DE5");                 // giant purple
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float g = growth();

        // Ears, then body
        paint.setColor(bodyColor());
        canvas.drawCircle(w * 0.27f, w * 0.24f, w * 0.10f, paint);
        canvas.drawCircle(w * 0.73f, w * 0.24f, w * 0.10f, paint);
        canvas.drawCircle(w * 0.5f, w * 0.56f, w * 0.40f, paint);

        // Eyes
        paint.setColor(Color.WHITE);
        canvas.drawCircle(w * 0.37f, w * 0.48f, w * 0.10f, paint);
        canvas.drawCircle(w * 0.63f, w * 0.48f, w * 0.10f, paint);
        paint.setColor(Color.parseColor("#222222"));
        canvas.drawCircle(w * 0.38f, w * 0.50f, w * 0.045f, paint);
        canvas.drawCircle(w * 0.64f, w * 0.50f, w * 0.045f, paint);

        // Mouth: opens wider as the count rises
        float mouthW = w * (0.12f + 0.08f * g);
        float mouthH = w * (0.04f + 0.09f * g);
        paint.setColor(Color.parseColor("#6B1F1F"));
        canvas.drawOval(new RectF(w * 0.5f - mouthW, w * 0.72f - mouthH,
                w * 0.5f + mouthW, w * 0.72f + mouthH), paint);

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

    // ---------- Funny lines ----------

    private static final String[][] LINES = {
        // 1-4 reels
        {"Nom! One reel, how cute.", "Just a snack. Still hungry.", "Ooh, another one!"},
        // 5-14
        {"Yum! Your evening tastes great.", "Crunchy reels, my favourite.", "Keep 'em coming, human."},
        // 15-29
        {"Burp. Your homework says hi.", "I'm getting chonky...", "Reel number {n}. Just saying."},
        // 30-59
        {"I can't feel my legs. MORE!", "Your to-do list is crying.", "{n} reels! I'm basically a whale."},
        // 60+
        {"I am the size of your regrets.", "Do you even remember reel 1?", "{n}?! Go drink some water."},
    };
    private static final Random random = new Random();

    public static String funnyLine(int count) {
        int tier = count < 5 ? 0 : count < 15 ? 1 : count < 30 ? 2 : count < 60 ? 3 : 4;
        String[] options = LINES[tier];
        return options[random.nextInt(options.length)].replace("{n}", String.valueOf(count));
    }
}
