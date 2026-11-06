package com.reelmonster;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * The screen inside the app: is the monster on, today's count, time lost and the last 7 days.
 * The layout is built in code (no XML layout files), and rebuilt every time you come back to
 * the app, so the numbers are always fresh.
 */
public class MainActivity extends Activity {

    private static final int CREAM = Color.parseColor("#FFF7EC");
    private static final int INK = Color.parseColor("#222222");
    private static final int GREY = Color.parseColor("#6B6B6B");
    private static final int GREEN = Color.parseColor("#4CAF50");
    private float dp;

    @Override
    protected void onResume() {
        super.onResume();
        dp = getResources().getDisplayMetrics().density;
        setContentView(buildScreen());
    }

    private View buildScreen() {
        LocalDate today = LocalDate.now();
        int todayCount = Stats.count(this, today);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(px(20), px(28), px(20), px(28));

        // Header: monster + title
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        MonsterView monster = new MonsterView(this);
        monster.setCount(todayCount);
        header.addView(monster);
        TextView title = text("Reel Monster", 28, INK, true);
        title.setPadding(px(14), 0, 0, 0);
        header.addView(title);
        page.addView(header);
        page.addView(space(16));

        // Is the monster switched on?
        LinearLayout status = card();
        if (isMonsterOn()) {
            status.addView(text("The monster is awake", 18, GREEN, true));
            status.addView(text("Open Instagram Reels or YouTube Shorts. The monster floats on top "
                    + "and counts every new reel by itself. Drag it anywhere.", 14, GREY, false));
        } else {
            status.addView(text("The monster is asleep", 18, INK, true));
            status.addView(text("Turn it on once: tap the button, open \"Reel Monster\" "
                    + "(sometimes under \"Installed apps\"), switch it on and tap Allow.", 14, GREY, false));
            Button turnOn = new Button(this);
            turnOn.setText("Turn on Reel Monster");
            turnOn.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                }
            });
            status.addView(turnOn);
        }
        page.addView(status);

        // Today
        LinearLayout todayCard = card();
        todayCard.addView(text("Today", 14, GREY, true));
        todayCard.addView(text(String.valueOf(todayCount), 56, INK, true));
        todayCard.addView(text("reels  ·  " + Stats.formatTime(Stats.millis(this, today))
                + " lost on Instagram + YouTube", 16, INK, false));
        page.addView(todayCard);

        // Last 7 days, with a bar for each day
        LinearLayout week = card();
        week.addView(text("Last 7 days", 14, GREY, true));
        int max = 1;
        for (int i = 0; i < 7; i++) max = Math.max(max, Stats.count(this, today.minusDays(i)));
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEE d MMM");
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            int c = Stats.count(this, day);
            week.addView(dayRow(i == 0 ? "Today" : day.format(fmt), c, max,
                    Stats.formatTime(Stats.millis(this, day))));
        }
        page.addView(week);

        page.addView(text("Works offline. Nothing leaves your phone.", 12, GREY, false));

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(CREAM);
        scroll.addView(page);
        return scroll;
    }

    /** One row: "Fri 6 Nov  [=====     ]  43 · 1h 05m" */
    private View dayRow(String label, int count, int max, String time) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, px(6), 0, px(6));

        TextView name = text(label, 14, INK, false);
        name.setWidth(px(90));
        row.addView(name);

        // The bar is two pieces side by side: coloured part = count, empty part = the rest.
        LinearLayout track = new LinearLayout(this);
        View filled = new View(this);
        filled.setBackgroundColor(Color.parseColor("#FF8A65"));
        track.addView(filled, new LinearLayout.LayoutParams(0, px(14), count));
        track.addView(new View(this), new LinearLayout.LayoutParams(0, px(14), max - count));
        row.addView(track, new LinearLayout.LayoutParams(0, px(14), 1));

        TextView value = text(count + " · " + time, 14, INK, false);
        value.setGravity(Gravity.END);
        value.setWidth(px(90));
        row.addView(value);
        return row;
    }

    /** True if the user switched the monster on in Accessibility settings. */
    private boolean isMonsterOn() {
        String on = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return on != null && on.contains(getPackageName() + "/");
    }

    // ---------- small helpers ----------

    private TextView text(String s, int sizeSp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sizeSp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(px(16), px(14), px(16), px(14));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(px(16));
        c.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = px(14);
        c.setLayoutParams(lp);
        return c;
    }

    private View space(int heightDp) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, px(heightDp)));
        return v;
    }

    private int px(int valueDp) {
        return (int) (valueDp * dp);
    }
}
