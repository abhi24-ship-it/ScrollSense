package com.reelmonster;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * The screen inside the app: setup status, today's count, time lost and the last 7 days.
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

        // Header: logo + title
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        MonsterView monster = new MonsterView(this);
        monster.setCount(todayCount);
        header.addView(monster);
        TextView title = text("ScrollSense", 28, INK, true);
        title.setPadding(px(14), 0, 0, 0);
        header.addView(title);
        page.addView(header);
        page.addView(space(16));

        // Auto mode: the watcher switches ScrollSense on only in Instagram and YouTube
        WatcherService.start(this);
        boolean usageOk = hasUsageAccess();
        boolean switchOk = WatcherService.canSwitch(this);
        LinearLayout status = card();
        if (usageOk && switchOk) {
            status.addView(text("Auto mode is on", 18, GREEN, true));
            status.addView(text("ScrollSense sleeps everywhere and wakes up only inside Instagram and "
                    + "YouTube. Payment apps like GPay never see it.", 14, GREY, false));
        } else {
            status.addView(text("Finish setup (one time)", 18, INK, true));
            status.addView(text((usageOk ? "✓ " : "1. ") + "Usage access: lets ScrollSense see which "
                    + "app is open (app names only, never the screen).", 14, usageOk ? GREEN : GREY, false));
            if (!usageOk) {
                Button allow = new Button(this);
                allow.setText("Allow usage access");
                allow.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                    }
                });
                status.addView(allow);
            }
            TextView grant = text((switchOk ? "✓ " : "2. ") + "Auto on/off: plug the phone into the laptop "
                    + "and run this once:\nadb shell pm grant com.reelmonster "
                    + "android.permission.WRITE_SECURE_SETTINGS", 14, switchOk ? GREEN : GREY, false);
            grant.setTextIsSelectable(true);
            grant.setPadding(0, px(8), 0, 0);
            status.addView(grant);
        }
        page.addView(status);

        // Today
        LinearLayout todayCard = card();
        todayCard.addView(text("Today", 14, GREY, true));
        todayCard.addView(text(String.valueOf(todayCount), 56, INK, true));
        todayCard.addView(text("reels  ·  " + Stats.formatTime(Stats.millis(this, today))
                + " lost on Instagram + YouTube", 16, INK, false));
        int dailyLimit = Stats.dailyLimit(this);
        int left = dailyLimit - todayCount;
        TextView limit = left > 0
                ? text("Daily limit: " + dailyLimit + " reels · " + left + " left", 14, GREY, false)
                : text("Limit reached: Reels and Shorts are closed until tomorrow. Chats still work.",
                        14, Color.parseColor("#D84315"), true);
        limit.setPadding(0, px(8), 0, 0);
        todayCard.addView(limit);
        page.addView(todayCard);

        // Set your own daily limit
        LinearLayout limitCard = card();
        limitCard.addView(text("Daily reel limit", 14, GREY, true));
        LinearLayout limitRow = new LinearLayout(this);
        limitRow.setGravity(Gravity.CENTER_VERTICAL);
        final EditText limitBox = new EditText(this);
        limitBox.setInputType(InputType.TYPE_CLASS_NUMBER);
        limitBox.setText(String.valueOf(dailyLimit));
        limitBox.setTextSize(20);
        limitBox.setTextColor(INK);
        limitRow.addView(limitBox, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        Button save = new Button(this);
        save.setText("Save");
        save.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                String typed = limitBox.getText().toString().trim();
                if (typed.isEmpty() || typed.length() > 3 || Integer.parseInt(typed) < 1) {
                    Toast.makeText(MainActivity.this, "Type a number from 1 to " + Stats.MAX_LIMIT,
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                Stats.setDailyLimit(MainActivity.this, Integer.parseInt(typed));
                Toast.makeText(MainActivity.this, "Daily limit set to " + Stats.dailyLimit(MainActivity.this)
                        + " reels", Toast.LENGTH_SHORT).show();
                setContentView(buildScreen()); // refresh the numbers and the logo
            }
        });
        limitRow.addView(save);
        limitCard.addView(limitRow);
        limitCard.addView(text("The logo changes at a quarter of it, at three quarters, and at the limit, "
                + "where Reels and Shorts close until midnight.", 13, GREY, false));
        page.addView(limitCard);

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

    /** True once "Usage access" is allowed for ScrollSense in Settings. */
    @SuppressWarnings("deprecation")
    private boolean hasUsageAccess() {
        android.app.AppOpsManager ops = getSystemService(android.app.AppOpsManager.class);
        int mode = ops.checkOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), getPackageName());
        return mode == android.app.AppOpsManager.MODE_ALLOWED;
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
