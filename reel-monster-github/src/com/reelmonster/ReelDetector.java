package com.reelmonster;

/**
 * The "brain" of automatic counting. It decides when a scroll event means "a NEW reel is on screen".
 *
 * It is plain Java (no Android code), so it can be tested on a laptop: see test/ReelDetectorTest.java.
 *
 * Instagram Reels and YouTube Shorts are vertical lists: one reel per screen, swipe up for the next.
 *
 * Instagram: when the list scrolls, Android sends a "scrolled" event (onScroll). Two kinds can arrive:
 *  1. With a position ("fromIndex" = which item is at the top). Then we count each position we
 *     have not reached before, so scrolling back to an old reel is not counted twice.
 *     (This is what Instagram sends on a real phone.)
 *  2. Without a position (fromIndex = -1). Then one swipe makes a quick burst of events, so we
 *     count one reel per burst.
 *
 * YouTube Shorts sends no scroll events. Instead, each short sits in its own page view, and a
 * swipe brings a different page view onto the screen. So we count each time the page changes
 * (onPageShown).
 */
public class ReelDetector {

    /** Scroll events closer together than this belong to the same swipe. */
    static final long SWIPE_GAP_MS = 700;

    private boolean seenList = false;   // have we seen a reel list since the user opened the app?
    private int listKey = 0;            // which list (a new screen of reels gets a new key)
    private int maxIndex = -1;          // furthest reel position reached in this list
    private long lastScrollMs = -1_000_000;
    private Integer lastPageKey = null; // which Shorts page was on screen last time (null = none yet)

    /** Is this view (by its resource name) the list that holds reels or shorts? */
    public static boolean isReelList(String viewId, String className) {
        if (viewId == null || className == null) return false;
        String id = viewId.toLowerCase();
        String cls = className.toLowerCase();
        boolean reelName = id.contains("clips") || id.contains("reel");
        boolean isList = cls.contains("pager") || cls.contains("recycler") || cls.contains("list");
        return reelName && isList;
    }

    /** Forget the current list, e.g. when the user leaves Instagram or YouTube. */
    public void reset() {
        seenList = false;
        maxIndex = -1;
        lastPageKey = null;
    }

    /**
     * Call this with a number that identifies the reel page now on screen (YouTube Shorts).
     * @return 1 if it is a different page from last time (a new short), otherwise 0
     */
    public int onPageShown(int pageKey) {
        if (lastPageKey != null && lastPageKey == pageKey) return 0;
        lastPageKey = pageKey;
        return 1;
    }

    /**
     * Call this for every scroll event of a reel list.
     * @param key       a number that identifies this particular list on screen
     * @param fromIndex position of the item at the top, or -1 if Android did not say
     * @param nowMs     current time in milliseconds
     * @return how many reels to add to the count (usually 0 or 1)
     */
    public int onScroll(int key, int fromIndex, long nowMs) {
        boolean newSwipe = nowMs - lastScrollMs > SWIPE_GAP_MS;
        lastScrollMs = nowMs;

        // First scroll in a list we have not seen yet: the reel that was already on screen counts.
        if (!seenList || key != listKey) {
            seenList = true;
            listKey = key;
            maxIndex = fromIndex;
            // With positions, the next reel is counted when its position arrives.
            // Without positions, this swipe already brings the next reel, so count both.
            return fromIndex >= 0 ? 1 : 2;
        }

        if (fromIndex >= 0) {
            if (fromIndex <= maxIndex) return 0;          // going back to a reel already counted
            int added = Math.min(fromIndex - maxIndex, 3); // a fast fling can skip a reel or two
            maxIndex = fromIndex;
            return added;
        }

        return newSwipe ? 1 : 0;
    }
}
