package com.reelmonster;

/** Laptop test for the auto on/off rules. Run by build.ps1. */
public class AutoSwitchTest {

    static int failures = 0;

    static void check(String name, Object expected, Object actual) {
        boolean ok = expected == null ? actual == null : expected.equals(actual);
        if (!ok) failures++;
        System.out.println((ok ? "PASS  " : "FAIL  ") + name + "  (expected " + expected + ", got " + actual + ")");
    }

    public static void main(String[] args) {
        String me = "com.reelmonster/com.reelmonster.MonsterService";
        String other = "com.other.app/.TheirService";

        // When should ScrollSense be on?
        check("on in Instagram", true, AutoSwitch.shouldBeOn("com.instagram.android"));
        check("on in YouTube", true, AutoSwitch.shouldBeOn("com.google.android.youtube"));
        check("off in GPay", false, AutoSwitch.shouldBeOn("com.google.android.apps.nbu.paisa.user"));
        check("off in its own app", false, AutoSwitch.shouldBeOn("com.reelmonster"));
        check("off when unknown", false, AutoSwitch.shouldBeOn(null));

        // Editing Android's list of switched-on accessibility services
        check("switch on, empty list", me, AutoSwitch.withService(null, me, true));
        check("switch on, keeps other", other + ":" + me, AutoSwitch.withService(other, me, true));
        check("switch on, already on (short name)", "com.reelmonster/.MonsterService",
                AutoSwitch.withService("com.reelmonster/.MonsterService", me, true));
        check("switch off, keeps other", other, AutoSwitch.withService(other + ":" + me, me, false));
        check("switch off, short name", "", AutoSwitch.withService("com.reelmonster/.MonsterService", me, false));
        check("switch off, already off", other, AutoSwitch.withService(other, me, false));

        System.out.println(failures == 0 ? "All auto switch tests passed." : failures + " test(s) FAILED.");
        System.exit(failures == 0 ? 0 : 1);
    }
}
