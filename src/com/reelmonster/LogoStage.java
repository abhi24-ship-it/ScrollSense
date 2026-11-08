package com.reelmonster;

/**
 * Which logo stage to show for today's reel count. Rules from the handwritten note
 * (logo/source/handwritten_stage_rules.jpg); the stage switches AT 20, 60 and 80.
 *
 * Plain Java (no Android code), so it can be tested on a laptop: see test/LogoStageTest.java.
 */
public class LogoStage {

    public static int forCount(int count) {
        if (count < 20) return 1;   // cyan face, black calm eyes
        if (count < 60) return 2;   // indigo face, black calm eyes
        if (count < 80) return 3;   // red face, black sharp eyes
        return 4;                   // black face, red sharp eyes (the 80-reel limit is reached)
    }

    /**
     * The only things ScrollSense says: one short popup when the count passes 20, 60 or 80.
     * @param before the count before the new reel(s)
     * @param after  the count after
     * @return the line to show, or null to say nothing
     */
    public static String milestoneLine(int before, int after) {
        if (before < 80 && after >= 80) return "Son";
        if (before < 60 && after >= 60) return "Pack it up";
        if (before < 20 && after >= 20) return "Do you even remember the first reel?";
        return null;
    }
}
