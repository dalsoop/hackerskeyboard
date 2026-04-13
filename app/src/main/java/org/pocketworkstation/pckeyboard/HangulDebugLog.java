package org.pocketworkstation.pckeyboard;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Ring buffer logger for Hangul composition debugging.
 * Captures state transitions, IC calls, and errors.
 * Viewable from keyboard settings.
 */
public class HangulDebugLog {
    private static final int MAX_ENTRIES = 200;
    private static final String[] STATES = {"NONE", "CHO", "JUNG", "JONG"};

    private static final ArrayList<String> sLog = new ArrayList<>();
    private static boolean sEnabled = true;

    public static void setEnabled(boolean enabled) {
        sEnabled = enabled;
        if (enabled) {
            log("DEBUG", "디버그 로그 시작");
        }
    }

    public static boolean isEnabled() {
        return sEnabled;
    }

    public static void log(String tag, String msg) {
        if (!sEnabled) return;
        String ts = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(new Date());
        synchronized (sLog) {
            sLog.add(ts + " [" + tag + "] " + msg);
            while (sLog.size() > MAX_ENTRIES) {
                sLog.remove(0);
            }
        }
    }

    /** Log Hangul composer state transition */
    public static void state(int oldState, int newState, int keyCode, String composing) {
        if (!sEnabled) return;
        String old = (oldState >= 0 && oldState < STATES.length) ? STATES[oldState] : "?";
        String nw = (newState >= 0 && newState < STATES.length) ? STATES[newState] : "?";
        char ch = (keyCode >= 0x3131 && keyCode <= 0x3163) ? (char) keyCode : '?';
        log("STATE", old + "→" + nw + " key=" + ch + "(0x" + Integer.toHexString(keyCode) + ") composing=\"" + composing + "\"");
    }

    /** Log IC (InputConnection) call */
    public static void ic(String method, String arg) {
        if (!sEnabled) return;
        log("IC", method + "(" + arg + ")");
    }

    /** Log commit */
    public static void commit(String text) {
        if (!sEnabled) return;
        log("COMMIT", "\"" + text + "\"");
    }

    /** Log error */
    public static void error(String msg) {
        log("ERROR", msg);
    }

    /** Log key event */
    public static void key(String action, int keyCode) {
        if (!sEnabled) return;
        log("KEY", action + " code=" + keyCode);
    }

    /** Get all log entries */
    public static List<String> getEntries() {
        synchronized (sLog) {
            return new ArrayList<>(sLog);
        }
    }

    /** Get log as single string */
    public static String getText() {
        StringBuilder sb = new StringBuilder();
        synchronized (sLog) {
            for (String entry : sLog) {
                sb.append(entry).append('\n');
            }
        }
        return sb.toString();
    }

    /** Clear log */
    public static void clear() {
        synchronized (sLog) {
            sLog.clear();
        }
    }

    /** Get entry count */
    public static int size() {
        synchronized (sLog) {
            return sLog.size();
        }
    }
}
