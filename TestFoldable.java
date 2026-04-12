/**
 * Comprehensive test for FoldableSupport logic.
 * Run: javac TestFoldable.java && java TestFoldable
 */
public class TestFoldable {
    static final int NARROW_MAX = 400;
    static final int WIDE_MIN = 580;

    enum ScreenClass { NARROW, NORMAL, WIDE }

    // --- Mirror FoldableSupport logic ---
    static ScreenClass classify(int widthDp) {
        if (widthDp <= NARROW_MAX) return ScreenClass.NARROW;
        if (widthDp >= WIDE_MIN) return ScreenClass.WIDE;
        return ScreenClass.NORMAL;
    }

    static int heightAdj(ScreenClass sc) {
        switch (sc) {
            case NARROW: return 8;
            case WIDE:   return -5;
            default:     return 0;
        }
    }

    static int modeOverride(ScreenClass sc) {
        return sc == ScreenClass.NARROW ? 0 : -1;
    }

    static int clampHeight(int base, ScreenClass sc) {
        return Math.max(15, Math.min(75, base + heightAdj(sc)));
    }

    // --- Stateful simulator (mirrors FoldableSupport.update) ---
    static ScreenClass currentScreen = ScreenClass.NORMAL;
    static int lastWidthDp = 0;

    static boolean update(int widthDp) {
        if (widthDp == lastWidthDp) return false;
        lastWidthDp = widthDp;
        ScreenClass prev = currentScreen;
        currentScreen = classify(widthDp);
        return prev != currentScreen;
    }

    static void resetState() {
        currentScreen = ScreenClass.NORMAL;
        lastWidthDp = 0;
    }

    // --- Test framework ---
    static int pass = 0, fail = 0;
    static String section = "";

    static void section(String name) {
        section = name;
        System.out.println("\n[" + name + "]");
    }

    static void check(String name, boolean condition) {
        if (condition) { pass++; }
        else { fail++; System.out.println("  FAIL: " + section + " / " + name); }
    }

    public static void main(String[] args) {
        System.out.println("=== FoldableSupport Comprehensive Test ===");

        // ============================================================
        // 1. Real device widths — portrait
        // ============================================================
        section("Galaxy Z Fold 6 cover portrait: 360dp");
        check("NARROW", classify(360) == ScreenClass.NARROW);

        section("Galaxy Z Fold 5 cover portrait: 374dp");
        check("NARROW", classify(374) == ScreenClass.NARROW);

        section("Galaxy Z Fold 4 cover portrait: 356dp");
        check("NARROW", classify(356) == ScreenClass.NARROW);

        section("Galaxy Z Fold 3 cover portrait: 349dp");
        check("NARROW", classify(349) == ScreenClass.NARROW);

        section("Galaxy Z Flip 5 cover (외부): 183dp");
        check("NARROW", classify(183) == ScreenClass.NARROW);

        section("Galaxy Z Fold 6 inner portrait: 600dp");
        check("WIDE", classify(600) == ScreenClass.WIDE);

        section("Galaxy Z Fold 5 inner portrait: 600dp");
        check("WIDE", classify(600) == ScreenClass.WIDE);

        section("Galaxy Z Fold 4 inner portrait: 585dp");
        check("WIDE", classify(585) == ScreenClass.WIDE);

        section("Pixel Fold inner portrait: 589dp");
        check("WIDE", classify(589) == ScreenClass.WIDE);

        section("OnePlus Open inner portrait: 599dp");
        check("WIDE", classify(599) == ScreenClass.WIDE);

        // ============================================================
        // 2. Real device widths — landscape
        // ============================================================
        section("Galaxy Z Fold 6 cover landscape: 822dp");
        check("WIDE", classify(822) == ScreenClass.WIDE);

        section("Galaxy Z Fold 6 inner landscape: 841dp");
        check("WIDE", classify(841) == ScreenClass.WIDE);

        section("Galaxy Z Fold 3 cover landscape: 768dp");
        check("WIDE", classify(768) == ScreenClass.WIDE);

        // ============================================================
        // 3. Normal phones (should all be NORMAL)
        // ============================================================
        section("Galaxy S24 Ultra: 412dp");
        check("NORMAL", classify(412) == ScreenClass.NORMAL);

        section("Galaxy S23: 411dp");
        check("NORMAL", classify(411) == ScreenClass.NORMAL);

        section("Pixel 8: 411dp");
        check("NORMAL", classify(411) == ScreenClass.NORMAL);

        section("Pixel 8 Pro: 448dp");
        check("NORMAL", classify(448) == ScreenClass.NORMAL);

        section("iPhone-class width: 430dp");
        check("NORMAL", classify(430) == ScreenClass.NORMAL);

        section("Old phone: 360dp (nexus 5)");
        check("NARROW", classify(360) == ScreenClass.NARROW);

        section("Budget phone: 401dp");
        check("NORMAL", classify(401) == ScreenClass.NORMAL);

        // ============================================================
        // 4. Tablets
        // ============================================================
        section("Galaxy Tab S9: 753dp");
        check("WIDE", classify(753) == ScreenClass.WIDE);

        section("iPad-class: 810dp");
        check("WIDE", classify(810) == ScreenClass.WIDE);

        section("10-inch tablet: 600dp");
        check("WIDE", classify(600) == ScreenClass.WIDE);

        section("Small tablet: 580dp");
        check("WIDE", classify(580) == ScreenClass.WIDE);

        // ============================================================
        // 5. Multi-window / split-screen widths
        // ============================================================
        section("Split-screen half of Fold inner: 300dp");
        check("NARROW", classify(300) == ScreenClass.NARROW);

        section("Split-screen 2/3 of Fold inner: 400dp");
        check("NARROW", classify(400) == ScreenClass.NARROW);

        section("Split-screen on tablet: 500dp");
        check("NORMAL", classify(500) == ScreenClass.NORMAL);

        section("Freeform small window: 250dp");
        check("NARROW", classify(250) == ScreenClass.NARROW);

        section("Pop-up window: 320dp");
        check("NARROW", classify(320) == ScreenClass.NARROW);

        // ============================================================
        // 6. Boundary values
        // ============================================================
        section("Exact boundaries");
        check("0dp -> NARROW", classify(0) == ScreenClass.NARROW);
        check("1dp -> NARROW", classify(1) == ScreenClass.NARROW);
        check("399dp -> NARROW", classify(399) == ScreenClass.NARROW);
        check("400dp -> NARROW", classify(400) == ScreenClass.NARROW);
        check("401dp -> NORMAL", classify(401) == ScreenClass.NORMAL);
        check("450dp -> NORMAL", classify(450) == ScreenClass.NORMAL);
        check("500dp -> NORMAL", classify(500) == ScreenClass.NORMAL);
        check("579dp -> NORMAL", classify(579) == ScreenClass.NORMAL);
        check("580dp -> WIDE", classify(580) == ScreenClass.WIDE);
        check("581dp -> WIDE", classify(581) == ScreenClass.WIDE);
        check("9999dp -> WIDE", classify(9999) == ScreenClass.WIDE);

        // ============================================================
        // 7. Height adjustments per class
        // ============================================================
        section("Height adjustments");
        check("NARROW +8", heightAdj(ScreenClass.NARROW) == 8);
        check("NORMAL +0", heightAdj(ScreenClass.NORMAL) == 0);
        check("WIDE -5", heightAdj(ScreenClass.WIDE) == -5);

        // ============================================================
        // 8. Mode overrides per class
        // ============================================================
        section("Mode overrides");
        check("NARROW -> force 4-row (0)", modeOverride(ScreenClass.NARROW) == 0);
        check("NORMAL -> no override (-1)", modeOverride(ScreenClass.NORMAL) == -1);
        check("WIDE -> no override (-1)", modeOverride(ScreenClass.WIDE) == -1);

        // ============================================================
        // 9. Height clamping — all base heights × all screen classes
        // ============================================================
        section("Height clamping: base=15 (minimum)");
        check("NARROW 15+8=23", clampHeight(15, ScreenClass.NARROW) == 23);
        check("NORMAL 15+0=15", clampHeight(15, ScreenClass.NORMAL) == 15);
        check("WIDE 15-5=15 (clamped)", clampHeight(15, ScreenClass.WIDE) == 15);

        section("Height clamping: base=20");
        check("NARROW 20+8=28", clampHeight(20, ScreenClass.NARROW) == 28);
        check("WIDE 20-5=15", clampHeight(20, ScreenClass.WIDE) == 15);

        section("Height clamping: base=30");
        check("NARROW 30+8=38", clampHeight(30, ScreenClass.NARROW) == 38);
        check("WIDE 30-5=25", clampHeight(30, ScreenClass.WIDE) == 25);

        section("Height clamping: base=40 (default)");
        check("NARROW 40+8=48", clampHeight(40, ScreenClass.NARROW) == 48);
        check("NORMAL 40+0=40", clampHeight(40, ScreenClass.NORMAL) == 40);
        check("WIDE 40-5=35", clampHeight(40, ScreenClass.WIDE) == 35);

        section("Height clamping: base=50");
        check("NARROW 50+8=58", clampHeight(50, ScreenClass.NARROW) == 58);
        check("WIDE 50-5=45", clampHeight(50, ScreenClass.WIDE) == 45);

        section("Height clamping: base=60");
        check("NARROW 60+8=68", clampHeight(60, ScreenClass.NARROW) == 68);
        check("WIDE 60-5=55", clampHeight(60, ScreenClass.WIDE) == 55);

        section("Height clamping: base=70");
        check("NARROW 70+8=75 (clamped)", clampHeight(70, ScreenClass.NARROW) == 75);
        check("WIDE 70-5=65", clampHeight(70, ScreenClass.WIDE) == 65);

        section("Height clamping: base=75 (maximum)");
        check("NARROW 75+8=75 (clamped)", clampHeight(75, ScreenClass.NARROW) == 75);
        check("NORMAL 75+0=75", clampHeight(75, ScreenClass.NORMAL) == 75);
        check("WIDE 75-5=70", clampHeight(75, ScreenClass.WIDE) == 70);

        section("Height clamping: extremes");
        check("base=10 WIDE -> 15 (floor)", clampHeight(10, ScreenClass.WIDE) == 15);
        check("base=12 WIDE -> 15 (floor)", clampHeight(12, ScreenClass.WIDE) == 15);
        check("base=73 NARROW -> 75 (ceil)", clampHeight(73, ScreenClass.NARROW) == 75);

        // ============================================================
        // 10. Stateful transitions — update() change detection
        // ============================================================
        section("State: initial update");
        resetState();
        check("first call returns true", update(360));
        check("state is NARROW", currentScreen == ScreenClass.NARROW);

        section("State: same width repeated");
        check("same width -> false", !update(360));
        check("still NARROW", currentScreen == ScreenClass.NARROW);

        section("State: fold -> unfold");
        check("360->600 returns true", update(600));
        check("now WIDE", currentScreen == ScreenClass.WIDE);

        section("State: unfold -> fold");
        check("600->360 returns true", update(360));
        check("now NARROW", currentScreen == ScreenClass.NARROW);

        section("State: NARROW -> NORMAL");
        check("360->412 returns true", update(412));
        check("now NORMAL", currentScreen == ScreenClass.NORMAL);

        section("State: NORMAL -> NORMAL (different width)");
        check("412->430 returns false (same class)", !update(430));
        check("still NORMAL", currentScreen == ScreenClass.NORMAL);

        section("State: NORMAL -> WIDE");
        check("430->600 returns true", update(600));
        check("now WIDE", currentScreen == ScreenClass.WIDE);

        section("State: WIDE -> WIDE (different width)");
        check("600->841 returns false (same class)", !update(841));
        check("still WIDE", currentScreen == ScreenClass.WIDE);

        section("State: WIDE -> NARROW (skip NORMAL)");
        check("841->350 returns true", update(350));
        check("now NARROW", currentScreen == ScreenClass.NARROW);

        // ============================================================
        // 11. Rapid fold/unfold sequence (simulate real usage)
        // ============================================================
        section("Rapid fold/unfold sequence");
        resetState();
        int[] sequence = {360, 360, 600, 600, 360, 412, 412, 600, 360, 600};
        boolean[] expected = {true, false, true, false, true, true, false, true, true, true};
        ScreenClass[] expectedClass = {
            ScreenClass.NARROW, ScreenClass.NARROW, ScreenClass.WIDE, ScreenClass.WIDE,
            ScreenClass.NARROW, ScreenClass.NORMAL, ScreenClass.NORMAL, ScreenClass.WIDE,
            ScreenClass.NARROW, ScreenClass.WIDE
        };
        for (int i = 0; i < sequence.length; i++) {
            boolean changed = update(sequence[i]);
            check("seq[" + i + "] " + sequence[i] + "dp changed=" + expected[i],
                  changed == expected[i]);
            check("seq[" + i + "] class=" + expectedClass[i],
                  currentScreen == expectedClass[i]);
        }

        // ============================================================
        // 12. updateKeyboardOptions simulation
        //     (full pipeline: user pref + foldable override)
        // ============================================================
        section("Full pipeline: cover screen + user wants 5-row");
        {
            int userMode = 2; // 5-row full
            int userHeight = 40;
            ScreenClass sc2 = classify(360); // cover
            int foldMode = modeOverride(sc2);
            int finalMode = foldMode >= 0 ? foldMode : userMode;
            int finalHeight = clampHeight(userHeight, sc2);
            check("mode forced to 4-row (0)", finalMode == 0);
            check("height 48", finalHeight == 48);
        }

        section("Full pipeline: unfolded + user wants 4-row");
        {
            int userMode = 0; // 4-row qwerty
            int userHeight = 40;
            ScreenClass sc2 = classify(600); // unfolded
            int foldMode = modeOverride(sc2);
            int finalMode = foldMode >= 0 ? foldMode : userMode;
            int finalHeight = clampHeight(userHeight, sc2);
            check("mode stays user pref (0)", finalMode == 0);
            check("height 35", finalHeight == 35);
        }

        section("Full pipeline: normal phone + user wants compact");
        {
            int userMode = 1; // compact
            int userHeight = 45;
            ScreenClass sc2 = classify(412); // normal phone
            int foldMode = modeOverride(sc2);
            int finalMode = foldMode >= 0 ? foldMode : userMode;
            int finalHeight = clampHeight(userHeight, sc2);
            check("mode stays user pref (1)", finalMode == 1);
            check("height unchanged 45", finalHeight == 45);
        }

        section("Full pipeline: cover + landscape (rotated cover)");
        {
            // Cover landscape gives wide width but short height
            int userMode = 2;
            int userHeight = 35;
            ScreenClass sc2 = classify(822); // cover landscape
            int foldMode = modeOverride(sc2);
            int finalMode = foldMode >= 0 ? foldMode : userMode;
            int finalHeight = clampHeight(userHeight, sc2);
            check("mode stays user pref (2)", finalMode == 2);
            check("height 30", finalHeight == 30);
        }

        section("Full pipeline: split-screen on Fold inner");
        {
            int userMode = 2;
            int userHeight = 40;
            ScreenClass sc2 = classify(300); // half of inner screen
            int foldMode = modeOverride(sc2);
            int finalMode = foldMode >= 0 ? foldMode : userMode;
            int finalHeight = clampHeight(userHeight, sc2);
            check("mode forced to 4-row (0)", finalMode == 0);
            check("height 48", finalHeight == 48);
        }

        // ============================================================
        // Summary
        // ============================================================
        System.out.println("\n========================================");
        System.out.println("  TOTAL: " + pass + " passed, " + fail + " failed");
        System.out.println("========================================");
        System.exit(fail > 0 ? 1 : 0);
    }
}
