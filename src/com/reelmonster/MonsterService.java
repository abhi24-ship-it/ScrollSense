package com.reelmonster;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * The monster. Android runs this in the background once you switch it on in
 * Settings > Accessibility. It does three jobs:
 *  1. Shows the floating bubble when Instagram or YouTube is in front, hides it otherwise.
 *  2. Watches for scroll events in the reels/shorts list and counts each new reel (ReelDetector).
 *  3. Measures how long Instagram/YouTube stay open ("time lost") and saves it (Stats).
 * After a phone restart Android starts it again by itself, and the saved count is read back.
 */
public class MonsterService extends AccessibilityService {

    private static final String TAG = "ReelMonster"; // see logs with: adb logcat -s ReelMonster
    private static final String INSTAGRAM = "com.instagram.android";
    private static final String YOUTUBE = "com.google.android.youtube";
    private static final Set<String> TARGET_APPS = new HashSet<>(Arrays.asList(INSTAGRAM, YOUTUBE));
    private static final String SHORTS_PAGE = "com.google.android.youtube:id/reel_player_page_container";

    private final ReelDetector detector = new ReelDetector();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private WindowManager windowManager;
    private WindowManager.LayoutParams params;
    private LinearLayout bubble;   // the floating window: monster + speech
    private MonsterView monster;
    private TextView speech;

    private boolean shown = false;
    private long shownSinceMs;     // when the bubble appeared (for "time lost")
    private float dp;

    private final Runnable hideSpeech = new Runnable() {
        @Override public void run() { speech.setVisibility(View.GONE); }
    };

    // Save the time every 30 s, so a sudden restart loses at most 30 s.
    private final Runnable saveTimeTick = new Runnable() {
        @Override public void run() {
            saveTime();
            handler.postDelayed(this, 30_000);
        }
    };

    // Screen off = phone in pocket, stop the clock. Unlocked = check which app is in front again.
    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                hideBubble();
            } else {
                AccessibilityNodeInfo root = getRootInActiveWindow();
                if (root != null && root.getPackageName() != null) {
                    onAppInFront(root.getPackageName().toString());
                }
            }
        }
    };

    @Override
    protected void onServiceConnected() {
        dp = getResources().getDisplayMetrics().density;
        windowManager = getSystemService(WindowManager.class);
        buildBubble();
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(screenReceiver, filter);
        Log.d(TAG, "Monster service started. Today's count = " + Stats.count(this, LocalDate.now()));

        // If Android (re)started us while Instagram or YouTube is already open, show the bubble now
        // instead of waiting for the next app switch.
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root != null && root.getPackageName() != null) {
            onAppInFront(root.getPackageName().toString());
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getPackageName() == null) return;
        String pkg = event.getPackageName().toString();

        int type = event.getEventType();
        if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            // Our own bubble can send this event too; only our app's screen should count as "in front".
            boolean ourScreen = MainActivity.class.getName().equals(String.valueOf(event.getClassName()));
            onAppInFront(ourScreen ? "reel monster screen" : pkg);
        } else if (TARGET_APPS.contains(pkg)) {
            if (type == AccessibilityEvent.TYPE_VIEW_SCROLLED && pkg.equals(INSTAGRAM)) onScrolled(event);
            // These apps change their screen many times a second (subtitles, progress bar),
            // so look at the screen at most once every 400 ms.
            if (!checkPending) {
                checkPending = true;
                handler.postDelayed(lookAtScreen, 400);
            }
        }
    }

    private boolean checkPending = false;

    /**
     * Runs shortly after Instagram or YouTube changed something on screen:
     *  1. Makes sure the bubble is showing if one of them is really in front (in case we missed the
     *     "app switched" event, e.g. right after Android restarted the monster).
     *  2. On YouTube, checks which Shorts page is on screen.
     */
    private final Runnable lookAtScreen = new Runnable() {
        @Override public void run() {
            checkPending = false;
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root == null || root.getPackageName() == null) return;
            String front = root.getPackageName().toString();
            onAppInFront(front);
            if (shown && front.equals(YOUTUBE)) checkShortsPage(root);
        }
    };

    /** YouTube Shorts: find the Shorts page that fills the screen; a different page = a new short. */
    private void checkShortsPage(AccessibilityNodeInfo root) {
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        AccessibilityNodeInfo best = null;
        int bestHeight = 0;
        for (AccessibilityNodeInfo page : root.findAccessibilityNodeInfosByViewId(SHORTS_PAGE)) {
            Rect r = new Rect();
            page.getBoundsInScreen(r);
            if (page.isVisibleToUser() && r.height() > bestHeight) {
                best = page;
                bestHeight = r.height();
            }
        }
        // Only count when one page covers most of the screen (not halfway through a swipe).
        if (best == null || bestHeight < screenHeight / 2) return;
        countReels(detector.onPageShown(best.hashCode()), "shorts page " + best.hashCode());
    }

    /** Decide whether the bubble should be visible for the app now in front. */
    private void onAppInFront(String pkg) {
        // Ignore windows that sit on top of the app without replacing it:
        // our own bubble, the notification shade and the keyboard.
        if (pkg.equals(getPackageName()) || pkg.equals("com.android.systemui") || isKeyboard(pkg)) {
            return;
        }
        if (TARGET_APPS.contains(pkg)) {
            showBubble();
        } else if (shown) {
            Log.d(TAG, "now in front: " + pkg);
            hideBubble();
        }
    }

    private boolean isKeyboard(String pkg) {
        String ime = Settings.Secure.getString(getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
        return ime != null && ime.startsWith(pkg + "/");
    }

    private void onScrolled(AccessibilityEvent event) {
        AccessibilityNodeInfo source = event.getSource();
        if (source == null) return;
        String id = findViewId(source);
        String cls = event.getClassName() == null ? null : event.getClassName().toString();

        // Logged so we can tune the detection on a real phone (adb logcat -s ReelMonster).
        Log.d(TAG, "scroll pkg=" + event.getPackageName() + " id=" + id + " class=" + cls
                + " from=" + event.getFromIndex() + " to=" + event.getToIndex()
                + " items=" + event.getItemCount());

        if (!ReelDetector.isReelList(id, cls)) return;
        countReels(detector.onScroll(source.hashCode(), event.getFromIndex(), SystemClock.elapsedRealtime()),
                "instagram position " + event.getFromIndex());
    }

    /** Save the new count, update the monster and make it say something. */
    private void countReels(int added, String why) {
        if (added <= 0) return;
        int total = Stats.addReels(this, added);
        Log.d(TAG, "counted +" + added + " -> " + total + " (" + why + ")");
        monster.setCount(total);
        say(MonsterView.funnyLine(total));
    }

    /** The view's own name, or its parent's (some lists have an unnamed inner part). */
    private String findViewId(AccessibilityNodeInfo node) {
        for (int i = 0; i < 3 && node != null; i++) {
            if (node.getViewIdResourceName() != null) return node.getViewIdResourceName();
            node = node.getParent();
        }
        return null;
    }

    // ---------- The floating bubble ----------

    private void buildBubble() {
        monster = new MonsterView(this);

        speech = new TextView(this);
        speech.setTextColor(Color.parseColor("#222222"));
        speech.setTextSize(13);
        speech.setMaxWidth((int) (200 * dp));
        speech.setPadding((int) (10 * dp), (int) (6 * dp), (int) (10 * dp), (int) (6 * dp));
        GradientDrawable round = new GradientDrawable();
        round.setColor(Color.WHITE);
        round.setCornerRadius(14 * dp);
        round.setStroke((int) (2 * dp), Color.parseColor("#222222"));
        speech.setBackground(round);
        speech.setVisibility(View.GONE);

        bubble = new LinearLayout(this);
        bubble.setOrientation(LinearLayout.VERTICAL);
        bubble.setGravity(Gravity.START); // left-aligned, so the monster doesn't move when it talks
        bubble.addView(monster);   // monster on top, speech underneath, so the monster never jumps
        bubble.addView(speech);

        SharedPreferences pos = getSharedPreferences("bubble", MODE_PRIVATE);
        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, // draw over other apps
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,         // don't steal the keyboard
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = pos.getInt("x", (int) (16 * dp));
        params.y = pos.getInt("y", (int) (220 * dp));

        // Drag the monster with your finger; its position is remembered.
        monster.setOnTouchListener(new View.OnTouchListener() {
            float startRawX, startRawY;
            int startX, startY;
            @Override public boolean onTouch(View v, MotionEvent e) {
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startRawX = e.getRawX(); startRawY = e.getRawY();
                        startX = params.x; startY = params.y;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = startX + (int) (e.getRawX() - startRawX);
                        params.y = startY + (int) (e.getRawY() - startRawY);
                        windowManager.updateViewLayout(bubble, params);
                        return true;
                    case MotionEvent.ACTION_UP:
                        pos.edit().putInt("x", params.x).putInt("y", params.y).apply();
                        return true;
                }
                return false;
            }
        });
    }

    private void showBubble() {
        if (shown) return;
        shown = true;
        shownSinceMs = SystemClock.elapsedRealtime();
        monster.setCount(Stats.count(this, LocalDate.now()));
        windowManager.addView(bubble, params);
        handler.postDelayed(saveTimeTick, 30_000);
        Log.d(TAG, "bubble shown");
    }

    private void hideBubble() {
        if (!shown) return;
        saveTime();
        shown = false;
        handler.removeCallbacks(saveTimeTick);
        handler.removeCallbacks(hideSpeech);
        speech.setVisibility(View.GONE);
        windowManager.removeView(bubble);
        detector.reset(); // next visit starts a fresh reel list
        Log.d(TAG, "bubble hidden");
    }

    /** Add the time since the last save to today's "time lost". */
    private void saveTime() {
        if (!shown) return;
        long now = SystemClock.elapsedRealtime();
        Stats.addMillis(this, now - shownSinceMs);
        shownSinceMs = now;
    }

    /** Show a speech bubble under the monster for 3.5 seconds. */
    private void say(String line) {
        speech.setText(line);
        speech.setVisibility(View.VISIBLE);
        handler.removeCallbacks(hideSpeech);
        handler.postDelayed(hideSpeech, 3500);
    }

    @Override
    public void onInterrupt() { }

    @Override
    public void onDestroy() {
        hideBubble();
        unregisterReceiver(screenReceiver);
        super.onDestroy();
    }
}
