package com.reelmonster;

import android.content.Context;
import android.content.SharedPreferences;

import java.time.LocalDate;

/**
 * Saves the numbers on the phone's storage, one entry per day, e.g.
 *   count_2026-11-06 = 43        (reels watched that day)
 *   millis_2026-11-06 = 3900000  (time spent in Instagram/YouTube that day)
 * SharedPreferences is a small file kept by Android, so the numbers survive a phone restart.
 */
public class Stats {

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("stats", Context.MODE_PRIVATE);
    }

    public static int count(Context c, LocalDate day) {
        return prefs(c).getInt("count_" + day, 0);
    }

    public static long millis(Context c, LocalDate day) {
        return prefs(c).getLong("millis_" + day, 0);
    }

    /** Adds reels to today's count and returns the new total. */
    public static int addReels(Context c, int reels) {
        LocalDate today = LocalDate.now();
        int total = count(c, today) + reels;
        prefs(c).edit().putInt("count_" + today, total).commit(); // commit = write to disk now
        return total;
    }

    /** Adds time spent in Instagram/YouTube to today. */
    public static void addMillis(Context c, long ms) {
        if (ms <= 0) return;
        LocalDate today = LocalDate.now();
        prefs(c).edit().putLong("millis_" + today, millis(c, today) + ms).commit();
    }

    /** 3_900_000 ms -> "1h 05m", 720_000 -> "12m", 40_000 -> "0m". */
    public static String formatTime(long ms) {
        long minutes = ms / 60_000;
        if (minutes < 60) return minutes + "m";
        return (minutes / 60) + "h " + String.format("%02d", minutes % 60) + "m";
    }
}
