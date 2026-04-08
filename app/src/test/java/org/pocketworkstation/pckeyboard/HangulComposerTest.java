package org.pocketworkstation.pckeyboard;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * HangulComposer 단위 테스트
 * - 자모 조합 (두벌식)
 * - 복합 자모 (겹받침, 이중모음)
 * - 음절 분리 (받침 + 모음 → 초성 이동)
 * - 백스페이스
 * - 상태 전이
 */
public class HangulComposerTest {

    // Compatibility Jamo constants
    static final int ㄱ = 0x3131, ㄲ = 0x3132, ㄴ = 0x3134, ㄷ = 0x3137;
    static final int ㄸ = 0x3138, ㄹ = 0x3139, ㅁ = 0x3141, ㅂ = 0x3142;
    static final int ㅃ = 0x3143, ㅅ = 0x3145, ㅆ = 0x3146, ㅇ = 0x3147;
    static final int ㅈ = 0x3148, ㅉ = 0x3149, ㅊ = 0x314A, ㅋ = 0x314B;
    static final int ㅌ = 0x314C, ㅍ = 0x314D, ㅎ = 0x314E;
    static final int ㅏ = 0x314F, ㅐ = 0x3150, ㅑ = 0x3151, ㅒ = 0x3152;
    static final int ㅓ = 0x3153, ㅔ = 0x3154, ㅕ = 0x3155, ㅖ = 0x3156;
    static final int ㅗ = 0x3157, ㅛ = 0x315B, ㅜ = 0x315C, ㅠ = 0x3160;
    static final int ㅡ = 0x3161, ㅣ = 0x3163;

    private HangulComposer composer;

    @Before
    public void setUp() {
        composer = new HangulComposer();
    }

    // === isHangulJamo 판별 ===

    @Test
    public void isHangulJamo_consonants() {
        assertTrue(HangulComposer.isHangulJamo(ㄱ));
        assertTrue(HangulComposer.isHangulJamo(ㅎ));
    }

    @Test
    public void isHangulJamo_vowels() {
        assertTrue(HangulComposer.isHangulJamo(ㅏ));
        assertTrue(HangulComposer.isHangulJamo(ㅣ));
    }

    @Test
    public void isHangulJamo_nonJamo() {
        assertFalse(HangulComposer.isHangulJamo('a'));
        assertFalse(HangulComposer.isHangulJamo('.'));
        assertFalse(HangulComposer.isHangulJamo(0x3164)); // ㅤ (filler)
        assertFalse(HangulComposer.isHangulJamo(0xAC00)); // 가 (syllable, not jamo)
    }

    // === 기본 상태 ===

    @Test
    public void initialState_notComposing() {
        assertFalse(composer.isComposing());
        assertEquals("", composer.getComposing());
    }

    // === 초성 단독 입력 ===

    @Test
    public void singleConsonant_composing() {
        String commit = composer.process(ㄱ);
        assertNull(commit);
        assertTrue(composer.isComposing());
        assertEquals("ㄱ", composer.getComposing());
    }

    @Test
    public void consecutiveConsonants_commitFirst() {
        composer.process(ㄱ);
        String commit = composer.process(ㄴ);
        assertEquals("ㄱ", commit);
        assertEquals("ㄴ", composer.getComposing());
    }

    // === 모음 단독 입력 ===

    @Test
    public void singleVowel_composing() {
        String commit = composer.process(ㅏ);
        assertNull(commit);
        assertTrue(composer.isComposing());
        assertEquals("ㅏ", composer.getComposing());
    }

    @Test
    public void consecutiveVowels_commitFirst() {
        composer.process(ㅏ);
        String commit = composer.process(ㅓ);
        assertEquals("ㅏ", commit);
        assertEquals("ㅓ", composer.getComposing());
    }

    // === 초성 + 중성 → 음절 ===

    @Test
    public void choJung_syllable() {
        composer.process(ㄱ);  // ㄱ
        String commit = composer.process(ㅏ);  // 가
        assertNull(commit);
        assertEquals("가", composer.getComposing());
    }

    @Test
    public void choJung_variousSyllables() {
        // 나
        composer.process(ㄴ);
        composer.process(ㅏ);
        assertEquals("나", composer.getComposing());

        composer.reset();

        // 하
        composer.process(ㅎ);
        composer.process(ㅏ);
        assertEquals("하", composer.getComposing());
    }

    // === 초성 + 중성 + 종성 ===

    @Test
    public void choJungJong_syllable() {
        composer.process(ㅎ);  // ㅎ
        composer.process(ㅏ);  // 하
        String commit = composer.process(ㄴ);  // 한
        assertNull(commit);
        assertEquals("한", composer.getComposing());
    }

    @Test
    public void han_gul() {
        // "한글" 입력
        composer.process(ㅎ);  // ㅎ
        composer.process(ㅏ);  // 하
        composer.process(ㄴ);  // 한

        // ㅡ 입력 → ㄴ이 다음 초성으로 이동, "한" commit
        String commit = composer.process(ㅡ);
        assertEquals("하", commit);  // "한" commit

        // 현재 composing: 느 (ㄴ+ㅡ)
        // 아, 실제로는 "글" 만들려면 ㄱ+ㅡ+ㄹ 이어야 함
        // 한글 테스트 다시
    }

    @Test
    public void hangul_fullWord() {
        // "한" = ㅎ+ㅏ+ㄴ
        composer.process(ㅎ);
        composer.process(ㅏ);
        composer.process(ㄴ);
        assertEquals("한", composer.getComposing());

        // "글" 시작: ㄱ 입력 → 받침 ㄴ+ㄱ 시도, 겹받침 없으므로 "한" commit
        String commit1 = composer.process(ㄱ);
        assertEquals("한", commit1);
        assertEquals("ㄱ", composer.getComposing());

        composer.process(ㅡ);  // 그
        assertEquals("그", composer.getComposing());

        composer.process(ㄹ);  // 글
        assertEquals("글", composer.getComposing());
    }

    // === 받침 → 모음 입력 시 음절 분리 ===

    @Test
    public void jongToCho_split() {
        // "간" 입력 후 ㅏ → "가" commit, composing "나"
        composer.process(ㄱ);  // ㄱ
        composer.process(ㅏ);  // 가
        composer.process(ㄴ);  // 간
        String commit = composer.process(ㅏ);  // → "가" commit, composing "나"
        assertEquals("가", commit);
        assertEquals("나", composer.getComposing());
    }

    // === 겹받침 ===

    @Test
    public void compoundJong_gs() {
        // ㄱ+ㅏ+ㄱ+ㅅ = 갃 (ㄱ+ㅏ+ㄳ)
        composer.process(ㄱ);
        composer.process(ㅏ);
        composer.process(ㄱ);
        String commit = composer.process(ㅅ);
        assertNull(commit);
        assertEquals("갃", composer.getComposing());
    }

    @Test
    public void compoundJong_lg() {
        // ㄹ+ㄱ → ㄺ
        composer.process(ㄱ);
        composer.process(ㅏ);
        composer.process(ㄹ);
        composer.process(ㄱ);
        assertEquals("갉", composer.getComposing());
        // 실제: 갉 (가+ㄺ)
    }

    @Test
    public void compoundJong_split_onVowel() {
        // ㄱ+ㅏ+ㄱ+ㅅ = 갃 (ㄳ 겹받침)
        // + ㅏ → "각" commit, composing "사"
        composer.process(ㄱ);
        composer.process(ㅏ);
        composer.process(ㄱ);
        composer.process(ㅅ);  // 갃
        String commit = composer.process(ㅏ);  // → "각" commit, "사" composing
        assertEquals("각", commit);
        assertEquals("사", composer.getComposing());
    }

    @Test
    public void compoundJong_bs() {
        // ㅂ+ㅅ → ㅄ
        composer.process(ㄱ);
        composer.process(ㅏ);
        composer.process(ㅂ);
        composer.process(ㅅ);
        // 갑+ㅅ → 값 (ㅄ)
        String composing = composer.getComposing();
        assertEquals("값", composing);
    }

    @Test
    public void compoundJong_lm() {
        // ㄹ+ㅁ → ㄻ
        composer.process(ㄱ);
        composer.process(ㅏ);
        composer.process(ㄹ);
        composer.process(ㅁ);
        String composing = composer.getComposing();
        assertEquals("갊", composer.getComposing());
    }

    // === 이중 모음 ===

    @Test
    public void compoundJung_wa() {
        // ㅗ+ㅏ → ㅘ
        composer.process(ㄱ);
        composer.process(ㅗ);  // 고
        String commit = composer.process(ㅏ);  // 과
        assertNull(commit);
        assertEquals("과", composer.getComposing());
    }

    @Test
    public void compoundJung_we() {
        // ㅜ+ㅓ → ㅝ
        composer.process(ㄱ);
        composer.process(ㅜ);  // 구
        composer.process(ㅓ);  // 궈
        assertEquals("궈", composer.getComposing());
    }

    @Test
    public void compoundJung_wi() {
        // ㅜ+ㅣ → ㅟ
        composer.process(ㄱ);
        composer.process(ㅜ);
        composer.process(ㅣ);
        assertEquals("귀", composer.getComposing());
    }

    @Test
    public void compoundJung_ui() {
        // ㅡ+ㅣ → ㅢ
        composer.process(ㅇ);
        composer.process(ㅡ);
        composer.process(ㅣ);
        assertEquals("의", composer.getComposing());
    }

    @Test
    public void compoundJung_wae() {
        // ㅗ+ㅐ → ㅙ
        composer.process(ㄱ);
        composer.process(ㅗ);
        composer.process(ㅐ);
        assertEquals("괘", composer.getComposing());
    }

    @Test
    public void compoundJung_incompatible_commits() {
        // ㅏ+ㅓ → 합성 불가, "ㅏ" commit
        composer.process(ㄱ);
        composer.process(ㅏ);  // 가
        String commit = composer.process(ㅓ);  // 합성 불가 → "가" commit, composing "ㅓ"
        assertEquals("가", commit);
        assertEquals("ㅓ", composer.getComposing());
    }

    // === 종성이 될 수 없는 자음 (ㄸ, ㅃ, ㅉ) ===

    @Test
    public void ddSsJj_cannotBeJong() {
        // ㄱ+ㅏ+ㄸ → "가" commit, composing "ㄸ"
        composer.process(ㄱ);
        composer.process(ㅏ);
        String commit = composer.process(ㄸ);
        assertEquals("가", commit);
        assertEquals("ㄸ", composer.getComposing());
    }

    @Test
    public void ssangBieup_cannotBeJong() {
        composer.process(ㄱ);
        composer.process(ㅏ);
        String commit = composer.process(ㅃ);
        assertEquals("가", commit);
        assertEquals("ㅃ", composer.getComposing());
    }

    @Test
    public void ssangJieut_cannotBeJong() {
        composer.process(ㄱ);
        composer.process(ㅏ);
        String commit = composer.process(ㅉ);
        assertEquals("가", commit);
        assertEquals("ㅉ", composer.getComposing());
    }

    // === 백스페이스 ===

    @Test
    public void backspace_fromCho() {
        composer.process(ㄱ);
        assertTrue(composer.backspace());
        assertFalse(composer.isComposing());
    }

    @Test
    public void backspace_fromJung_withCho() {
        composer.process(ㄱ);
        composer.process(ㅏ);  // 가
        assertTrue(composer.backspace());
        assertEquals("ㄱ", composer.getComposing());
    }

    @Test
    public void backspace_fromJung_standalone() {
        composer.process(ㅏ);
        assertTrue(composer.backspace());
        assertFalse(composer.isComposing());
    }

    @Test
    public void backspace_fromJong() {
        composer.process(ㄱ);
        composer.process(ㅏ);
        composer.process(ㄴ);  // 간
        assertTrue(composer.backspace());
        assertEquals("가", composer.getComposing());
    }

    @Test
    public void backspace_fromCompoundJong() {
        // ㄱ+ㅏ+ㄱ+ㅅ = 갃 (ㄳ)
        composer.process(ㄱ);
        composer.process(ㅏ);
        composer.process(ㄱ);
        composer.process(ㅅ);
        assertTrue(composer.backspace());
        assertEquals("각", composer.getComposing());
    }

    @Test
    public void backspace_fromCompoundJung() {
        // ㅗ+ㅏ = ㅘ
        composer.process(ㄱ);
        composer.process(ㅗ);
        composer.process(ㅏ);  // 과
        assertTrue(composer.backspace());
        assertEquals("고", composer.getComposing());
    }

    @Test
    public void backspace_notComposing() {
        assertFalse(composer.backspace());
    }

    // === reset / commit ===

    @Test
    public void reset_clearsState() {
        composer.process(ㄱ);
        composer.process(ㅏ);
        composer.reset();
        assertFalse(composer.isComposing());
        assertEquals("", composer.getComposing());
    }

    @Test
    public void commit_returnsTextAndResets() {
        composer.process(ㄱ);
        composer.process(ㅏ);
        composer.process(ㄴ);
        String committed = composer.commit();
        assertEquals("한".equals(committed) ? committed : "간", committed);
        // 실제: "간"
        assertEquals("간", committed);
        assertFalse(composer.isComposing());
    }

    @Test
    public void commit_empty() {
        String committed = composer.commit();
        assertEquals("", committed);
    }

    // === 실제 단어 입력 시뮬레이션 ===

    @Test
    public void realWord_annyeong() {
        StringBuilder result = new StringBuilder();
        // 안 = ㅇ+ㅏ+ㄴ
        composer.process(ㅇ);
        composer.process(ㅏ);
        composer.process(ㄴ);
        // ㄴ → 받침. 다음 ㅕ 입력 시 ㄴ이 초성으로 이동
        String c1 = composer.process(ㅕ);
        result.append(c1);  // "아" commit
        // composing: 녀
        composer.process(ㅇ);  // → "녀" commit? 아니, 녀+ㅇ → 녕?
        // ㅇ은 받침이 될 수 있으므로 "녕"
        assertEquals("녕", composer.getComposing());
    }

    @Test
    public void realWord_gamsahamnida() {
        StringBuilder result = new StringBuilder();

        // 감 = ㄱ+ㅏ+ㅁ
        composer.process(ㄱ);
        composer.process(ㅏ);
        composer.process(ㅁ);
        assertEquals("감", composer.getComposing());

        // 사 시작: ㅅ → "감" commit
        String c1 = composer.process(ㅅ);
        assertEquals("감", c1);
        result.append(c1);

        composer.process(ㅏ);  // 사
        assertEquals("사", composer.getComposing());
    }

    // === 겹받침 분리 후 다음 음절 ===

    @Test
    public void compoundJong_lb_split() {
        // ㄹ+ㅂ → ㄼ, then ㅏ → ㄹ남기고 ㅂ+ㅏ=바
        composer.process(ㄱ);
        composer.process(ㅏ);
        composer.process(ㄹ);
        composer.process(ㅂ);  // 갈+ㅂ → ㄼ
        String commit = composer.process(ㅏ);
        // "갈" commit, composing "바"
        assertEquals("갈", commit);
        assertEquals("바", composer.getComposing());
    }

    @Test
    public void compoundJong_nh_split() {
        // ㄴ+ㅎ → ㄶ, then ㅏ → ㄴ남기고 ㅎ+ㅏ=하
        composer.process(ㅁ);
        composer.process(ㅏ);
        composer.process(ㄴ);
        composer.process(ㅎ);  // 만+ㅎ → ㄶ
        String commit = composer.process(ㅏ);
        assertEquals("만", commit);
        assertEquals("하", composer.getComposing());
    }

    // === 연속 음절 ===

    @Test
    public void multiSyllable_sequence() {
        StringBuilder committed = new StringBuilder();

        // 나 = ㄴ+ㅏ
        composer.process(ㄴ);
        composer.process(ㅏ);

        // 라 시작: ㄹ → "나" commit (ㄹ은 받침 가능이지만 다음 모음 봐야 함)
        // 실제: ㄹ은 받침이 되므로 "날" composing
        composer.process(ㄹ);
        assertEquals("날", composer.getComposing());

        // ㅏ → ㄹ 초성으로 이동, "나" commit
        String c = composer.process(ㅏ);
        assertEquals("나", c);
        committed.append(c);
        assertEquals("라", composer.getComposing());

        // 최종 commit
        committed.append(composer.commit());
        assertEquals("나라", committed.toString());
    }
}
