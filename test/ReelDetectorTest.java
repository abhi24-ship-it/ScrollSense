package com.reelmonster;

/**
 * Laptop test for the counting logic (no phone needed). It replays fake scroll events that look
 * like the ones Instagram and YouTube send, and checks the count. Run by build.ps1.
 */
public class ReelDetectorTest {

    static int failures = 0;

    static void check(String name, int expected, int actual) {
        boolean ok = expected == actual;
        if (!ok) failures++;
        System.out.println((ok ? "PASS  " : "FAIL  ") + name + "  (expected " + expected + ", got " + actual + ")");
    }

    static void check(String name, boolean expected, boolean actual) {
        check(name, expected ? 1 : 0, actual ? 1 : 0);
    }

    /** One swipe from reel i to reel i+1, with positions: several events while dragging, one after. */
    static int swipeWithIndex(ReelDetector d, int key, int i, long[] clock) {
        int added = 0;
        for (int e = 0; e < 5; e++) { added += d.onScroll(key, i, clock[0]); clock[0] += 60; }
        added += d.onScroll(key, i + 1, clock[0]);
        clock[0] += 5000; // watch the reel for 5 s
        return added;
    }

    /** One swipe without positions (fromIndex = -1): a quick burst of 6 events. */
    static int swipeNoIndex(ReelDetector d, int key, long[] clock) {
        int added = 0;
        for (int e = 0; e < 6; e++) { added += d.onScroll(key, -1, clock[0]); clock[0] += 60; }
        clock[0] += 5000;
        return added;
    }

    public static void main(String[] args) {
        long[] clock = {0};

        // 1. 20 reels in a list that reports positions (like YouTube Shorts): 19 swipes.
        ReelDetector d = new ReelDetector();
        int count = 0;
        for (int i = 0; i < 19; i++) count += swipeWithIndex(d, 1, i, clock);
        check("20 reels with positions", 20, count);

        // 2. Scrolling back to old reels does not count them again: 0..5, back to 3, then up to 7.
        d = new ReelDetector();
        count = 0;
        for (int i = 0; i < 5; i++) count += swipeWithIndex(d, 1, i, clock);   // reels 0..5
        count += d.onScroll(1, 4, clock[0]); count += d.onScroll(1, 3, clock[0] += 60);
        clock[0] += 5000;
        for (int i = 3; i < 7; i++) count += swipeWithIndex(d, 1, i, clock);   // up to reel 7
        check("going back is not counted twice (reels 0..7)", 8, count);

        // 3. 20 reels in a list that does NOT report positions (Instagram may do this): 19 swipes.
        d = new ReelDetector();
        count = 0;
        for (int i = 0; i < 19; i++) count += swipeNoIndex(d, 2, clock);
        check("20 reels without positions", 20, count);

        // 4. After leaving the app, the next visit starts fresh.
        d = new ReelDetector();
        count = 0;
        for (int i = 0; i < 4; i++) count += swipeWithIndex(d, 1, i, clock);  // reels 0..4 = 5
        d.reset();
        for (int i = 0; i < 2; i++) count += swipeWithIndex(d, 1, i, clock);  // reels 0..2 = 3
        check("reset after leaving the app", 8, count);

        // 5. A brand-new reel list on screen (different key) starts fresh too.
        d = new ReelDetector();
        count = 0;
        for (int i = 0; i < 4; i++) count += swipeWithIndex(d, 1, i, clock);  // 5
        for (int i = 0; i < 2; i++) count += swipeWithIndex(d, 9, i, clock);  // 3
        check("new reel list starts fresh", 8, count);

        // 6. YouTube Shorts: 20 shorts = the on-screen page changes 19 times. Like the real
        //    phone, the app recycles 3 page views (A, B, C, A, B, C...) and checks the screen
        //    several times while each short plays.
        d = new ReelDetector();
        count = 0;
        int[] pages = {101, 202, 303};
        for (int s = 0; s < 20; s++) {
            for (int check = 0; check < 8; check++) count += d.onPageShown(pages[s % 3]);
        }
        check("20 YouTube shorts", 20, count);

        // 7. After leaving YouTube and coming back to the same page, it counts once again.
        d.reset();
        check("same page after leaving counts once", 1, d.onPageShown(303) + d.onPageShown(303));

        // 8. Which views count as the reel list.
        check("Instagram reels pager",
                true, ReelDetector.isReelList("com.instagram.android:id/clips_viewer_view_pager", "androidx.viewpager.widget.ViewPager"));
        check("YouTube shorts list",
                true, ReelDetector.isReelList("com.google.android.youtube:id/reel_recycler", "androidx.recyclerview.widget.RecyclerView"));
        check("normal feed list is ignored",
                false, ReelDetector.isReelList("android:id/list", "android.widget.ListView"));
        check("a text view named 'reel_title' is ignored",
                false, ReelDetector.isReelList("com.google.android.youtube:id/reel_title", "android.widget.TextView"));
        check("unnamed view is ignored", false, ReelDetector.isReelList(null, "android.widget.ListView"));

        System.out.println(failures == 0 ? "All logic tests passed." : failures + " test(s) FAILED.");
        System.exit(failures == 0 ? 0 : 1);
    }
}
