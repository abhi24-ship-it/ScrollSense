package com.reelmonster;

/**
 * Which logo stage to show for today's reel count, and the 3 popup lines.
 *
 * The handwritten note set the stages at 20, 60 and 80 reels for a limit of 80. Since you can now
 * choose your own limit, the stages keep the same proportions:
 *   stage 2 at a quarter of the limit, stage 3 at three quarters, stage 4 at the limit itself.
 * For the default limit of 80 that is exactly 20, 60 and 80.
 *
 * Plain Java (no Android code), so it can be tested on a laptop: see test/LogoStageTest.java.
 */
public class LogoStage {

    /** Count where stage 2 starts (a quarter of the limit, at least 1). */
    static int quarter(int limit) {
        return Math.max(1, Math.round(limit * 0.25f));
    }

    /** Count where stage 3 starts (three quarters of the limit, after stage 2, before the limit). */
    static int threeQuarters(int limit) {
        int t = Math.round(limit * 0.75f);
        return Math.max(quarter(limit), Math.min(t, limit));
    }

    public static int forCount(int count, int limit) {
        if (count >= limit) return 4;                 // black face, red sharp eyes (limit reached)
        if (count >= threeQuarters(limit)) return 3;  // red face, black sharp eyes
        if (count >= quarter(limit)) return 2;        // dark blue face, black calm eyes
        return 1;                                     // cyan face, black calm eyes
    }

    /**
     * The only things ScrollSense says: one short popup when the count passes a stage.
     * @param before the count before the new reel(s)
     * @param after  the count after
     * @param limit  your daily limit
     * @return the line to show, or null to say nothing
     */
    public static String milestoneLine(int before, int after, int limit) {
        if (before < limit && after >= limit) return "Son";
        if (before < threeQuarters(limit) && after >= threeQuarters(limit)) return "Pack it up";
        if (before < quarter(limit) && after >= quarter(limit)) return "Do you even remember the first reel?";
        return null;
    }
}
