package org.pocketworkstation.pckeyboard;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.Log;

/**
 * Detects foldable device screen states based on screen width (dp).
 * No Jetpack WindowManager dependency — uses Configuration only.
 */
public class FoldableSupport {
    private static final String TAG = "HK/Foldable";

    // Galaxy Z Fold cover screen: ~360dp width
    // Normal phone portrait: ~360-420dp
    // Galaxy Z Fold inner screen portrait: ~600dp+
    // Galaxy Z Fold inner screen landscape: ~800dp+
    private static final int NARROW_SCREEN_MAX_DP = 400;
    private static final int WIDE_SCREEN_MIN_DP = 580;

    public enum ScreenClass {
        NARROW,  // Cover screen or very small device
        NORMAL,  // Standard phone
        WIDE     // Unfolded inner screen or tablet
    }

    private ScreenClass mCurrentScreen = ScreenClass.NORMAL;
    private int mLastWidthDp = 0;

    /**
     * Update screen class from current configuration.
     * @return true if screen class changed
     */
    public boolean update(Configuration conf) {
        int widthDp = conf.screenWidthDp;
        if (widthDp == mLastWidthDp) return false;

        mLastWidthDp = widthDp;
        ScreenClass prev = mCurrentScreen;

        if (widthDp <= NARROW_SCREEN_MAX_DP) {
            mCurrentScreen = ScreenClass.NARROW;
        } else if (widthDp >= WIDE_SCREEN_MIN_DP) {
            mCurrentScreen = ScreenClass.WIDE;
        } else {
            mCurrentScreen = ScreenClass.NORMAL;
        }

        boolean changed = (prev != mCurrentScreen);
        if (changed) {
            Log.i(TAG, "Screen class: " + prev + " -> " + mCurrentScreen
                    + " (width=" + widthDp + "dp)");
        }
        return changed;
    }

    public ScreenClass getScreenClass() {
        return mCurrentScreen;
    }

    public int getWidthDp() {
        return mLastWidthDp;
    }

    public boolean isNarrow() {
        return mCurrentScreen == ScreenClass.NARROW;
    }

    public boolean isWide() {
        return mCurrentScreen == ScreenClass.WIDE;
    }

    /**
     * Suggest keyboard height adjustment for current screen.
     * Returns a delta to add to the user's configured height percentage.
     * Narrow screens get taller keyboards (bigger keys).
     * Wide screens get slightly shorter keyboards (more screen space).
     */
    public int getHeightAdjustment() {
        switch (mCurrentScreen) {
            case NARROW: return 8;   // +8% height for bigger keys on cover screen
            case WIDE:   return -5;  // -5% on unfolded (keys already wide enough)
            default:     return 0;
        }
    }

    /**
     * Suggest keyboard mode override for current screen.
     * Returns -1 if no override (use user preference).
     * Narrow screens use the dedicated narrow layout (kbd_qwerty_narrow),
     * so no mode override is needed.
     */
    public int getKeyboardModeOverride() {
        return -1;  // Narrow layout handles cover screen; no mode override needed
    }
}
