package com.reelmonster;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Decides when ScrollSense (the accessibility service) should be switched ON, and edits Android's
 * list of switched-on accessibility services.
 *
 * ScrollSense is OFF everywhere except Instagram and YouTube, so payment apps like GPay never see
 * an app that can read the screen.
 *
 * Plain Java (no Android code), so it can be tested on a laptop: see test/AutoSwitchTest.java.
 */
public class AutoSwitch {

    public static final String INSTAGRAM = "com.instagram.android";
    public static final String YOUTUBE = "com.google.android.youtube";
    public static final Set<String> TARGET_APPS = new HashSet<>(Arrays.asList(INSTAGRAM, YOUTUBE));

    /** ON only while Instagram or YouTube is the app in front. */
    public static boolean shouldBeOn(String appInFront) {
        return appInFront != null && TARGET_APPS.contains(appInFront);
    }

    /**
     * Android keeps the switched-on accessibility services as one piece of text, separated by ":",
     * e.g. "com.other.app/.TheirService:com.reelmonster/com.reelmonster.MonsterService".
     * Returns that text with our service added (on = true) or removed (on = false).
     * Any other services in the list are kept exactly as they were.
     */
    public static String withService(String list, String service, boolean on) {
        List<String> result = new ArrayList<>();
        boolean found = false;
        if (list != null) {
            for (String entry : list.split(":")) {
                if (entry.isEmpty()) continue;
                if (fullName(entry).equals(fullName(service))) {
                    found = true;
                    if (!on) continue; // leave it out = switch it off
                }
                result.add(entry);
            }
        }
        if (on && !found) result.add(service);
        return String.join(":", result);
    }

    /** "com.reelmonster/.MonsterService" and "com.reelmonster/com.reelmonster.MonsterService" are the same. */
    static String fullName(String entry) {
        int slash = entry.indexOf('/');
        if (slash < 0) return entry;
        String pkg = entry.substring(0, slash);
        String cls = entry.substring(slash + 1);
        return pkg + "/" + (cls.startsWith(".") ? pkg + cls : cls);
    }
}
