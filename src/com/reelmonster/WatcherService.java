package com.reelmonster;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

/**
 * The watcher. It keeps ScrollSense (MonsterService) switched OFF, and switches it ON only while
 * Instagram or YouTube is the app in front. So GPay, banking apps and every other app never see
 * an app that can read the screen.
 *
 * How it knows the app in front: once a second it asks Android's "usage access" which app was
 * last opened. That only gives app names, never what is on the screen.
 * How it switches ScrollSense: it edits Android's list of switched-on accessibility services,
 * which needs the WRITE_SECURE_SETTINGS permission (given once from the laptop, see README).
 *
 * It runs as a "foreground service" (with a quiet notification) so Android doesn't stop it,
 * and it pauses while the screen is off.
 */
public class WatcherService extends Service {

    private static final String TAG = "ScrollSense";
    private static final String CHANNEL = "watcher";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private UsageStatsManager usage;
    private String appInFront;          // last app opened, according to usage access
    private long lastCheckMs;
    private boolean watching = false;

    /** Start the watcher (safe to call many times). */
    public static void start(Context context) {
        context.startForegroundService(new Intent(context, WatcherService.class));
    }

    private final Runnable check = new Runnable() {
        @Override public void run() {
            checkAppInFront();
            handler.postDelayed(this, 1000);
        }
    };

    // Screen off: switch ScrollSense off and stop checking. Phone unlocked: start checking again.
    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                stopWatching();
                setServiceOn(WatcherService.this, false);
            } else {
                startWatching();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        usage = getSystemService(UsageStatsManager.class);
        showNotification();
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(screenReceiver, filter);
        lastCheckMs = System.currentTimeMillis() - 60_000; // first check looks back one minute
        startWatching();
        Log.d(TAG, "watcher started");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY; // if Android stops it, start it again
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        stopWatching();
        unregisterReceiver(screenReceiver);
        super.onDestroy();
    }

    private void startWatching() {
        if (watching) return;
        watching = true;
        handler.post(check);
    }

    private void stopWatching() {
        watching = false;
        handler.removeCallbacks(check);
    }

    /** Ask usage access which app was opened last, then switch ScrollSense on or off. */
    @SuppressWarnings("deprecation") // MOVE_TO_FOREGROUND: the name that also works on Android 8-9
    private void checkAppInFront() {
        long now = System.currentTimeMillis();
        UsageEvents events = usage.queryEvents(lastCheckMs - 2000, now); // small overlap, nothing missed
        lastCheckMs = now;
        UsageEvents.Event event = new UsageEvents.Event();
        while (events.hasNextEvent()) {
            events.getNextEvent(event);
            if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                appInFront = event.getPackageName(); // the newest one wins
            }
        }
        setServiceOn(this, AutoSwitch.shouldBeOn(appInFront));
    }

    /** Switch ScrollSense (the accessibility service) on or off. Does nothing if already so. */
    static void setServiceOn(Context context, boolean on) {
        ContentResolver settings = context.getContentResolver();
        String service = new ComponentName(context, MonsterService.class).flattenToString();
        String current = Settings.Secure.getString(settings, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        String updated = AutoSwitch.withService(current, service, on);
        if (updated.equals(current == null ? "" : current)) return;
        try {
            Settings.Secure.putString(settings, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, updated);
            if (on) Settings.Secure.putInt(settings, Settings.Secure.ACCESSIBILITY_ENABLED, 1);
            Log.d(TAG, "ScrollSense switched " + (on ? "ON" : "OFF"));
        } catch (SecurityException e) {
            Log.d(TAG, "can't switch ScrollSense: WRITE_SECURE_SETTINGS not given yet");
        }
    }

    /** True once the one-time laptop command has been run (see README). */
    static boolean canSwitch(Context context) {
        return context.checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS)
                == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    private void showNotification() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel(
                CHANNEL, "ScrollSense watcher", NotificationManager.IMPORTANCE_MIN));
        Notification notification = new Notification.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setContentTitle("ScrollSense")
                .setContentText("Sleeping. Wakes up only in Instagram and YouTube.")
                .setOngoing(true)
                .build();
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(1, notification);
        }
    }
}
