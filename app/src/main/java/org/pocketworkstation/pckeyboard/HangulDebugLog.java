package org.pocketworkstation.pckeyboard;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Ring buffer logger for Hangul composition debugging.
 * Captures state transitions, IC calls, errors, and uncaught exceptions.
 * Viewable from keyboard settings.
 */
public class HangulDebugLog {
    private static final int MAX_ENTRIES = 200;
    private static Thread.UncaughtExceptionHandler sOriginalHandler;
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

    /** Log exception with stack trace */
    public static void exception(String context, Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        String trace = sw.toString();
        // 스택 트레이스에서 핵심 라인만 추출 (최대 8줄)
        String[] lines = trace.split("\n");
        StringBuilder brief = new StringBuilder();
        brief.append(context).append(": ").append(t.getClass().getSimpleName())
             .append(": ").append(t.getMessage());
        int count = 0;
        for (String line : lines) {
            if (line.contains("pckeyboard") && count < 8) {
                brief.append("\n  ").append(line.trim());
                count++;
            }
        }
        log("EXCEPTION", brief.toString());
    }

    /** Install global uncaught exception handler */
    public static void installCrashHandler() {
        sOriginalHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            StringWriter sw = new StringWriter();
            throwable.printStackTrace(new PrintWriter(sw));
            log("CRASH", thread.getName() + ": " + throwable.getClass().getSimpleName()
                    + ": " + throwable.getMessage());
            String[] lines = sw.toString().split("\n");
            int count = 0;
            for (String line : lines) {
                if (count < 15) {
                    log("CRASH", "  " + line.trim());
                    count++;
                }
            }
            // 원래 핸들러 호출 (시스템 크래시 리포팅)
            if (sOriginalHandler != null) {
                sOriginalHandler.uncaughtException(thread, throwable);
            }
        });
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
