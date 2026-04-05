package org.pocketworkstation.pckeyboard;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages clipboard history for the keyboard.
 * Stores the last N clipboard entries and provides them for quick pasting.
 */
public class ClipboardHistory {
    private static final int MAX_ITEMS = 10;
    private final List<String> mHistory = new ArrayList<>();
    private ClipboardManager mClipboardManager;
    private boolean mListening = false;

    private final ClipboardManager.OnPrimaryClipChangedListener mListener =
            new ClipboardManager.OnPrimaryClipChangedListener() {
                @Override
                public void onPrimaryClipChanged() {
                    if (mClipboardManager == null) return;
                    ClipData clip = mClipboardManager.getPrimaryClip();
                    if (clip == null || clip.getItemCount() == 0) return;
                    CharSequence text = clip.getItemAt(0).getText();
                    if (text == null || text.length() == 0) return;
                    String str = text.toString().trim();
                    if (str.isEmpty()) return;
                    // Remove duplicate if exists
                    mHistory.remove(str);
                    // Add to front
                    mHistory.add(0, str);
                    // Trim to max
                    while (mHistory.size() > MAX_ITEMS) {
                        mHistory.remove(mHistory.size() - 1);
                    }
                }
            };

    public void start(Context context) {
        if (mListening) return;
        mClipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (mClipboardManager != null) {
            mClipboardManager.addPrimaryClipChangedListener(mListener);
            mListening = true;
            // Seed with current clipboard content
            mListener.onPrimaryClipChanged();
        }
    }

    public void stop() {
        if (mClipboardManager != null && mListening) {
            mClipboardManager.removePrimaryClipChangedListener(mListener);
            mListening = false;
        }
    }

    public List<String> getItems() {
        return mHistory;
    }

    public String getCurrentClip() {
        if (mClipboardManager == null) return null;
        ClipData clip = mClipboardManager.getPrimaryClip();
        if (clip == null || clip.getItemCount() == 0) return null;
        CharSequence text = clip.getItemAt(0).getText();
        return text != null ? text.toString() : null;
    }

    public boolean hasItems() {
        return !mHistory.isEmpty();
    }

    public void clear() {
        mHistory.clear();
    }
}
