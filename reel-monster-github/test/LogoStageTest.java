package com.reelmonster;

/** Laptop test for the logo stages and popups, with the default and with custom limits. Run by build.ps1. */
public class LogoStageTest {

    static int failures = 0;

    static void check(String name, Object expected, Object actual) {
        boolean ok = expected == null ? actual == null : expected.equals(actual);
        if (!ok) failures++;
        System.out.println((ok ? "PASS  " : "FAIL  ") + name + "  (expected " + expected + ", got " + actual + ")");
    }

    public static void main(String[] args) {
        // Default limit 80: stages switch AT 20, 60, 80 (the handwritten note)
        int[][] stages80 = {{0, 1}, {19, 1}, {20, 2}, {59, 2}, {60, 3}, {79, 3}, {80, 4}, {500, 4}};
        for (int[] c : stages80) check("limit 80: " + c[0] + " reels -> stage", c[1], LogoStage.forCount(c[0], 80));

        // Custom limit 40: quarter = 10, three quarters = 30
        int[][] stages40 = {{9, 1}, {10, 2}, {29, 2}, {30, 3}, {39, 3}, {40, 4}};
        for (int[] c : stages40) check("limit 40: " + c[0] + " reels -> stage", c[1], LogoStage.forCount(c[0], 40));

        // Tiny limits still go 1 -> ... -> 4 and reach stage 4 exactly at the limit
        check("limit 1: 0 reels", 1, LogoStage.forCount(0, 1));
        check("limit 1: 1 reel", 4, LogoStage.forCount(1, 1));
        check("limit 3: 2 reels", 3, LogoStage.forCount(2, 3));
        check("limit 3: 3 reels", 4, LogoStage.forCount(3, 3));

        // Popups (limit 80): only when the count passes 20, 60 or 80, also if it jumps past
        check("80: 19->20", "Do you even remember the first reel?", LogoStage.milestoneLine(19, 20, 80));
        check("80: 18->19", null, LogoStage.milestoneLine(18, 19, 80));
        check("80: 20->21", null, LogoStage.milestoneLine(20, 21, 80));
        check("80: 59->60", "Pack it up", LogoStage.milestoneLine(59, 60, 80));
        check("80: 58->61", "Pack it up", LogoStage.milestoneLine(58, 61, 80));
        check("80: 79->80", "Son", LogoStage.milestoneLine(79, 80, 80));
        check("80: 80->81", null, LogoStage.milestoneLine(80, 81, 80));
        // Popups (limit 40)
        check("40: 9->10", "Do you even remember the first reel?", LogoStage.milestoneLine(9, 10, 40));
        check("40: 29->30", "Pack it up", LogoStage.milestoneLine(29, 30, 40));
        check("40: 39->40", "Son", LogoStage.milestoneLine(39, 40, 40));

        System.out.println(failures == 0 ? "All logo stage tests passed." : failures + " test(s) FAILED.");
        System.exit(failures == 0 ? 0 : 1);
    }
}
