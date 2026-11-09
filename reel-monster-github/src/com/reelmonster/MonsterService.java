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
import android.view.accessibility.AccessibilityWindowInfo;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.time.LocalDate;
import java.util.Set;

/**
 * The ScrollSense service. It is an accessibility service, and WatcherService switches it on only while
 * Instagram or YouTube is open (it is off everywhere else). While on, it does three jobs:
 *  1. Shows the floating bubble when Instagram or YouTube is in front, hides it otherwise.
 *  2. Watches for scroll events in the reels/shorts list and counts each new reel (ReelDetector).
 *  3. Measures how long Instagram/YouTube stay open ("time lost") and saves it (Stats).
 * Every time it is switched on, the saved count is read back, so nothing is lost.
 */
public class MonsterService extends AccessibilityService {

    private static final String TAG = "ScrollSense"; // see logs with: adb logcat -s ScrollSense
    private static final String INSTAGRAM = AutoSwitch.INSTAGRAM;
    private static final String YOUTUBE = AutoSwitch.YOUTUBE;
    private static final Set<String> TARGET_APPS = AutoSwitch.TARGET_APPS;
    private static final String SHORTS_PAGE = "com.google.android.youtube:id/reel_player_page_container";
    private static final String INSTAGRAM_REELS = "com.instagram.android:id/clips_viewer_view_pager";

    private final ReelDetector detector = new ReelDetector();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private WindowManager windowManager;
    private WindowManager.LayoutParams params;
    private LinearLayout bubble;   // the floating window: logo + popup
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
                String front = packageOf(focusedAppRoot());
                if (front != null) onAppInFront(front);
            }
        }
    };

    @Override
    protected void onServiceConnected() {
        dp = getResources().getDisplayMetrics().density;
        windowManager = getSystemService(WindowManager.class);
        buildBubble();
        buildBlockScreen();
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(screenReceiver, filter);
        Log.d(TAG, "ScrollSense service started. Today's count = " + Stats.count(this, LocalDate.now()));

        // If Android (re)started us while Instagram or YouTube is already open, show the bubble now
        // instead of waiting for the next app switch.
        String front = packageOf(focusedAppRoot());
        if (front != null) onAppInFront(front);
    }

    private static final String OUR_SCREEN = "ScrollSense screen";

    /**
     * The app window that really has focus (the one you are using), or null if unknown.
     * A small YouTube picture-in-picture window or our own bubble never has focus.
     */
    private AccessibilityNodeInfo focusedAppRoot() {
        for (AccessibilityWindowInfo w : getWindows()) {
            if (w.getType() == AccessibilityWindowInfo.TYPE_APPLICATION && w.isFocused()) return w.getRoot();
        }
        return null;
    }

    private static String packageOf(AccessibilityNodeInfo node) {
        return node == null || node.getPackageName() == null ? null : node.getPackageName().toString();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getPackageName() == null) return;
        String pkg = event.getPackageName().toString();

        int type = event.getEventType();
        if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (MainActivity.class.getName().equals(String.valueOf(event.getClassName()))) {
                onAppInFront(OUR_SCREEN);
            } else if (TARGET_APPS.contains(pkg)) {
                // YouTube also sends this when it shrinks into a picture-in-picture window over
                // another app, so only show the bubble if Instagram/YouTube really has focus.
                String focused = packageOf(focusedAppRoot());
                if (focused == null || focused.equals(pkg)) onAppInFront(pkg);
            } else {
                onAppInFront(pkg);
            }
        }
        if (TARGET_APPS.contains(pkg)) {
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
     *  1. Shows or hides the bubble for the app that really has focus (this also fixes things if
     *     we missed an "app switched" event, e.g. right after Android restarted ScrollSense).
     *  2. If today's limit is used up and the Reels/Shorts player is open, blocks it.
     *  3. On YouTube, checks which Shorts page is on screen.
     */
    private final Runnable lookAtScreen = new Runnable() {
        @Override public void run() {
            checkPending = false;
            AccessibilityNodeInfo root = focusedAppRoot();
            String front = packageOf(root);
            if (front == null) return;
            onAppInFront(front.equals(getPackageName()) ? OUR_SCREEN : front);
            if (!shown || !TARGET_APPS.contains(front)) {
                stopBlocking();
                return;
            }
            boolean player = reelPlayerOnScreen(root, front);
            if (player && Stats.limitReached(MonsterService.this)) {
                startBlocking();
            } else {
                stopBlocking();
                if (front.equals(YOUTUBE)) checkShortsPage(root);
            }
            // While blocking, keep checking every second so the block screen goes away
            // as soon as the player is closed.
            if (blocking && !checkPending) {
                checkPending = true;
                handler.postDelayed(this, 1000);
            }
        }
    };

    // ---------- Daily limit ----------

    private LinearLayout blockScreen;               // full-screen "Reels are closed" cover
    private WindowManager.LayoutParams blockParams;
    private boolean blocking = false;
    private boolean pressedBackThisVisit = false;
    private long lastBackMs = -1_000_000;

    /** Is the full-screen Reels (Instagram) or Shorts (YouTube) player open? */
    private boolean reelPlayerOnScreen(AccessibilityNodeInfo root, String app) {
        String playerId = app.equals(INSTAGRAM) ? INSTAGRAM_REELS : SHORTS_PAGE;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        for (AccessibilityNodeInfo node : root.findAccessibilityNodeInfosByViewId(playerId)) {
            Rect r = new Rect();
            node.getBoundsInScreen(r);
            if (node.isVisibleToUser() && r.height() >= screenHeight / 2) return true;
        }
        return false;
    }

    /**
     * Limit reached and the Reels/Shorts player is open:
     *  - cover the screen with a block screen, so the reels can't be watched or scrolled;
     *  - press Back ONCE for the user, which normally leaves the player (to the app's Home tab,
     *    or back to the chat the reel was opened from). Only once, because pressing Back again
     *    while the app is still closing the player would leave the whole app.
     * Chats, Home, Search and profiles don't use the player, so they keep working.
     */
    private void startBlocking() {
        if (!blocking) {
            blocking = true;
            windowManager.addView(blockScreen, blockParams);
            Log.d(TAG, "limit reached -> block screen on");
        }
        long now = SystemClock.elapsedRealtime();
        if (!pressedBackThisVisit && now - lastBackMs > 3000) {
            pressedBackThisVisit = true;
            lastBackMs = now;
            performGlobalAction(GLOBAL_ACTION_BACK);
            Log.d(TAG, "pressed Back once to leave the player");
        }
    }

    /** The player is gone (or the app was left): remove the block screen. */
    private void stopBlocking() {
        if (blocking) {
            blocking = false;
            windowManager.removeView(blockScreen);
            Log.d(TAG, "block screen off");
        }
        pressedBackThisVisit = false;
    }

    private void buildBlockScreen() {
        TextView message = new TextView(this);
        message.setText("Son\n\nReels and Shorts are closed until tomorrow.");
        message.setTextColor(Color.WHITE);
        message.setTextSize(20);
        message.setGravity(Gravity.CENTER);

        android.widget.Button leave = new android.widget.Button(this);
        leave.setText("Leave Reels");
        leave.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { performGlobalAction(GLOBAL_ACTION_BACK); }
        });

        blockScreen = new LinearLayout(this);
        blockScreen.setOrientation(LinearLayout.VERTICAL);
        blockScreen.setGravity(Gravity.CENTER);
        blockScreen.setPadding((int) (32 * dp), 0, (int) (32 * dp), 0);
        blockScreen.setBackgroundColor(Color.parseColor("#F2141414")); // almost solid dark
        blockScreen.setClickable(true); // swallow every touch, so nothing behind can be scrolled
        blockScreen.addView(message);
        LinearLayout.LayoutParams buttonSpace = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        buttonSpace.topMargin = (int) (24 * dp);
        blockScreen.addView(leave, buttonSpace);

        blockParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
    }

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

        // Logged so we can tune the detection on a real phone (adb logcat -s ScrollSense).
        Log.d(TAG, "scroll pkg=" + event.getPackageName() + " id=" + id + " class=" + cls
                + " from=" + event.getFromIndex() + " to=" + event.getToIndex()
                + " items=" + event.getItemCount());

        if (!ReelDetector.isReelList(id, cls)) return;
        countReels(detector.onScroll(source.hashCode(), event.getFromIndex(), SystemClock.elapsedRealtime()),
                "instagram position " + event.getFromIndex());
    }

    /** Save the new count, update the bubble and show a popup if needed. */
    private void countReels(int added, String why) {
        if (added <= 0) return;
        if (Stats.limitReached(this)) { // already at the limit: no more reels today
            startBlocking();
            return;
        }
        int total = Stats.addReels(this, added);
        Log.d(TAG, "counted +" + added + " -> " + total + " (" + why + ")");
        monster.setCount(total);
        int limit = Stats.dailyLimit(this);
        String line = LogoStage.milestoneLine(total - added, total, limit); // only at 1/4, 3/4 and the limit
        if (line != null) say(line);
        if (total >= limit) startBlocking(); // that was the last one
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
        bubble.setGravity(Gravity.START); // left-aligned, so the logo doesn't move when the popup shows
        bubble.addView(monster);   // logo on top, popup underneath, so the logo never jumps
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

        // Drag the bubble with your finger; its position is remembered.
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
        stopBlocking();
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

    /** Show a short popup under the logo for 2 seconds. */
    private void say(String line) {
        speech.setText(line);
        speech.setVisibility(View.VISIBLE);
        handler.removeCallbacks(hideSpeech);
        handler.postDelayed(hideSpeech, 2000);
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
