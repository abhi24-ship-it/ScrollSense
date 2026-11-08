package com.reelmonster;

/** Laptop test for the logo stage thresholds (switch AT 20, 60, 80). Run by build.ps1. */
public class LogoStageTest {

    public static void main(String[] args) {
        int[][] cases = {
            {0, 1}, {1, 1}, {19, 1},
            {20, 2}, {59, 2},
            {60, 3}, {79, 3},
            {80, 4}, {81, 4}, {500, 4},
        };
        int failures = 0;
        for (int[] c : cases) {
            int got = LogoStage.forCount(c[0]);
            boolean ok = got == c[1];
            if (!ok) failures++;
            System.out.println((ok ? "PASS  " : "FAIL  ") + c[0] + " reels -> stage " + got + " (expected " + c[1] + ")");
        }
        // The popups: only when the count passes 20, 60 or 80 (also if it jumps past it)
        Object[][] lines = {
            {18, 19, null}, {19, 20, "Do you even remember the first reel?"}, {20, 21, null},
            {59, 60, "Pack it up"}, {58, 61, "Pack it up"}, {60, 61, null},
            {79, 80, "Son"}, {80, 81, null},
        };
        for (Object[] c : lines) {
            String got = LogoStage.milestoneLine((Integer) c[0], (Integer) c[1]);
            boolean ok = got == null ? c[2] == null : got.equals(c[2]);
            if (!ok) failures++;
            System.out.println((ok ? "PASS  " : "FAIL  ") + c[0] + " -> " + c[1] + " says " + got);
        }
        System.out.println(failures == 0 ? "All logo stage tests passed." : failures + " test(s) FAILED.");
        System.exit(failures == 0 ? 0 : 1);
    }
}
