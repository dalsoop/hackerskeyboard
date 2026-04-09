package org.pocketworkstation.pckeyboard;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * HangulComposer 심화 테스트
 * - 모든 초성/중성/종성 개별 확인
 * - 모든 겹받침/이중모음 조합
 * - 상태 전이 매트릭스
 * - 연속 백스페이스
 * - 긴 문장 입력 시뮬레이션
 * - 비정상 입력 시퀀스
 */
public class HangulComposerExhaustiveTest {

    // Compatibility Jamo
    static final int ㄱ = 0x3131, ㄲ = 0x3132, ㄴ = 0x3134, ㄷ = 0x3137;
    static final int ㄸ = 0x3138, ㄹ = 0x3139, ㅁ = 0x3141, ㅂ = 0x3142;
    static final int ㅃ = 0x3143, ㅅ = 0x3145, ㅆ = 0x3146, ㅇ = 0x3147;
    static final int ㅈ = 0x3148, ㅉ = 0x3149, ㅊ = 0x314A, ㅋ = 0x314B;
    static final int ㅌ = 0x314C, ㅍ = 0x314D, ㅎ = 0x314E;
    static final int ㅏ = 0x314F, ㅐ = 0x3150, ㅑ = 0x3151, ㅒ = 0x3152;
    static final int ㅓ = 0x3153, ㅔ = 0x3154, ㅕ = 0x3155, ㅖ = 0x3156;
    static final int ㅗ = 0x3157, ㅘ = 0x3158, ㅙ = 0x3159, ㅚ = 0x315A;
    static final int ㅛ = 0x315B, ㅜ = 0x315C, ㅝ = 0x315D, ㅞ = 0x315E;
    static final int ㅟ = 0x315F, ㅠ = 0x3160, ㅡ = 0x3161, ㅢ = 0x3162;
    static final int ㅣ = 0x3163;

    // All 19 choseong in order
    static final int[] ALL_CHO = {ㄱ,ㄲ,ㄴ,ㄷ,ㄸ,ㄹ,ㅁ,ㅂ,ㅃ,ㅅ,ㅆ,ㅇ,ㅈ,ㅉ,ㅊ,ㅋ,ㅌ,ㅍ,ㅎ};
    static final String[] CHO_STR = {"ㄱ","ㄲ","ㄴ","ㄷ","ㄸ","ㄹ","ㅁ","ㅂ","ㅃ","ㅅ","ㅆ","ㅇ","ㅈ","ㅉ","ㅊ","ㅋ","ㅌ","ㅍ","ㅎ"};

    // All 21 jungseong as compatibility jamo codes
    static final int[] ALL_JUNG = {ㅏ,ㅐ,ㅑ,ㅒ,ㅓ,ㅔ,ㅕ,ㅖ,ㅗ,0x3158,0x3159,0x315A,ㅛ,ㅜ,0x315D,0x315E,0x315F,ㅠ,ㅡ,0x3162,ㅣ};

    private HangulComposer composer;

    @Before
    public void setUp() {
        composer = new HangulComposer();
    }

    // ===== 모든 초성 19개 개별 입력 =====

    @Test
    public void allChoseong_individually() {
        for (int i = 0; i < ALL_CHO.length; i++) {
            composer.reset();
            String commit = composer.process(ALL_CHO[i]);
            assertNull("초성 " + CHO_STR[i] + " 단독 입력 시 commit 없어야 함", commit);
            assertTrue("초성 " + CHO_STR[i] + " 입력 후 composing 상태", composer.isComposing());
            assertEquals(CHO_STR[i], composer.getComposing());
        }
    }

    // ===== 모든 초성 + ㅏ → 음절 =====

    @Test
    public void allChoseong_withA() {
        String[] expected = {"가","까","나","다","따","라","마","바","빠","사","싸","아","자","짜","차","카","타","파","하"};
        for (int i = 0; i < ALL_CHO.length; i++) {
            composer.reset();
            composer.process(ALL_CHO[i]);
            String commit = composer.process(ㅏ);
            assertNull("초성+ㅏ는 commit 없어야 함: " + CHO_STR[i], commit);
            assertEquals(CHO_STR[i] + "+ㅏ", expected[i], composer.getComposing());
        }
    }

    // ===== 모든 모음 21개 단독 입력 =====

    @Test
    public void allJungseong_standalone() {
        String[] expected = {"ㅏ","ㅐ","ㅑ","ㅒ","ㅓ","ㅔ","ㅕ","ㅖ","ㅗ","ㅘ","ㅙ","ㅚ","ㅛ","ㅜ","ㅝ","ㅞ","ㅟ","ㅠ","ㅡ","ㅢ","ㅣ"};
        for (int i = 0; i < ALL_JUNG.length; i++) {
            composer.reset();
            // 복합 모음은 단독으로 입력 불가 (ㅘ 등은 compatibility jamo로 직접 입력 안 됨)
            // isHangulJamo 범위 내이고 isVowel이면 테스트
            if (HangulComposer.isHangulJamo(ALL_JUNG[i])) {
                composer.process(ALL_JUNG[i]);
                assertTrue("모음 " + expected[i] + " 입력 후 composing", composer.isComposing());
            }
        }
    }

    // ===== 종성이 될 수 있는 자음 (16개) / 될 수 없는 자음 (3개: ㄸ,ㅃ,ㅉ) =====

    @Test
    public void jongseongCapable_consonants() {
        // 종성 가능: ㄱ,ㄲ,ㄴ,ㄷ,ㄹ,ㅁ,ㅂ,ㅅ,ㅆ,ㅇ,ㅈ,ㅊ,ㅋ,ㅌ,ㅍ,ㅎ
        int[] canBeJong = {ㄱ,ㄲ,ㄴ,ㄷ,ㄹ,ㅁ,ㅂ,ㅅ,ㅆ,ㅇ,ㅈ,ㅊ,ㅋ,ㅌ,ㅍ,ㅎ};
        for (int jamo : canBeJong) {
            composer.reset();
            composer.process(ㄱ); // 초성
            composer.process(ㅏ); // 가
            String commit = composer.process(jamo);
            assertNull("종성 가능 자음은 commit 없어야 함: " + (char)jamo, commit);
            // 받침이 붙은 음절이어야 함
            assertTrue(composer.isComposing());
        }
    }

    @Test
    public void jongseongIncapable_consonants() {
        int[] cannotBeJong = {ㄸ, ㅃ, ㅉ};
        String[] names = {"ㄸ", "ㅃ", "ㅉ"};
        for (int i = 0; i < cannotBeJong.length; i++) {
            composer.reset();
            composer.process(ㄱ);
            composer.process(ㅏ); // 가
            String commit = composer.process(cannotBeJong[i]);
            assertEquals(names[i] + "은 종성 불가 → 가 commit", "가", commit);
            assertEquals(names[i], composer.getComposing());
        }
    }

    // ===== 모든 겹받침 11개 조합 =====

    @Test
    public void allCompoundJong_gs() {
        // ㄱ+ㅅ → ㄳ
        assertCompoundJong(ㄱ, ㅅ);
    }

    @Test
    public void allCompoundJong_nj() {
        // ㄴ+ㅈ → ㄵ
        assertCompoundJong(ㄴ, ㅈ);
    }

    @Test
    public void allCompoundJong_nh() {
        // ㄴ+ㅎ → ㄶ
        assertCompoundJong(ㄴ, ㅎ);
    }

    @Test
    public void allCompoundJong_lg() {
        assertCompoundJong(ㄹ, ㄱ);
    }

    @Test
    public void allCompoundJong_lm() {
        assertCompoundJong(ㄹ, ㅁ);
    }

    @Test
    public void allCompoundJong_lb() {
        assertCompoundJong(ㄹ, ㅂ);
    }

    @Test
    public void allCompoundJong_ls() {
        assertCompoundJong(ㄹ, ㅅ);
    }

    @Test
    public void allCompoundJong_lt() {
        assertCompoundJong(ㄹ, ㅌ);
    }

    @Test
    public void allCompoundJong_lp() {
        assertCompoundJong(ㄹ, ㅍ);
    }

    @Test
    public void allCompoundJong_lh() {
        assertCompoundJong(ㄹ, ㅎ);
    }

    @Test
    public void allCompoundJong_bs() {
        assertCompoundJong(ㅂ, ㅅ);
    }

    private void assertCompoundJong(int first, int second) {
        composer.reset();
        composer.process(ㅇ);   // 초성
        composer.process(ㅏ);   // 아
        assertNull(composer.process(first));   // 받침 1
        assertNull(composer.process(second));  // 겹받침
        assertTrue(composer.isComposing());
        // 글자가 하나여야 함 (음절)
        assertEquals(1, composer.getComposing().length());
    }

    // ===== 모든 겹받침 분리 (모음 입력 시) =====

    @Test
    public void allCompoundJong_splitOnVowel_gs() {
        assertCompoundJongSplit(ㄱ, ㅅ, "악"); // 아+ㄱ 남기고, ㅅ+ㅏ=사
    }

    @Test
    public void allCompoundJong_splitOnVowel_nj() {
        assertCompoundJongSplit(ㄴ, ㅈ, "안");
    }

    @Test
    public void allCompoundJong_splitOnVowel_nh() {
        assertCompoundJongSplit(ㄴ, ㅎ, "안");
    }

    @Test
    public void allCompoundJong_splitOnVowel_lg() {
        assertCompoundJongSplit(ㄹ, ㄱ, "알");
    }

    @Test
    public void allCompoundJong_splitOnVowel_lm() {
        assertCompoundJongSplit(ㄹ, ㅁ, "알");
    }

    @Test
    public void allCompoundJong_splitOnVowel_lb() {
        assertCompoundJongSplit(ㄹ, ㅂ, "알");
    }

    @Test
    public void allCompoundJong_splitOnVowel_ls() {
        assertCompoundJongSplit(ㄹ, ㅅ, "알");
    }

    @Test
    public void allCompoundJong_splitOnVowel_lt() {
        assertCompoundJongSplit(ㄹ, ㅌ, "알");
    }

    @Test
    public void allCompoundJong_splitOnVowel_lp() {
        assertCompoundJongSplit(ㄹ, ㅍ, "알");
    }

    @Test
    public void allCompoundJong_splitOnVowel_lh() {
        assertCompoundJongSplit(ㄹ, ㅎ, "알");
    }

    @Test
    public void allCompoundJong_splitOnVowel_bs() {
        assertCompoundJongSplit(ㅂ, ㅅ, "압");
    }

    private void assertCompoundJongSplit(int first, int second, String expectedCommit) {
        composer.reset();
        composer.process(ㅇ);
        composer.process(ㅏ);
        composer.process(first);
        composer.process(second);
        String commit = composer.process(ㅏ); // 모음 → 분리
        assertEquals(expectedCommit, commit);
        assertTrue(composer.isComposing());
        // 두 번째 자음+ㅏ 음절이 composing
        assertEquals(1, composer.getComposing().length());
    }

    // ===== 모든 이중모음 7개 =====

    @Test
    public void compoundJung_all_oa()  { assertCompoundJung(ㅗ, ㅏ, "과"); }
    @Test
    public void compoundJung_all_oae() { assertCompoundJung(ㅗ, ㅐ, "괘"); }
    @Test
    public void compoundJung_all_oi()  { assertCompoundJung(ㅗ, ㅣ, "괴"); }
    @Test
    public void compoundJung_all_ue()  { assertCompoundJung(ㅜ, ㅓ, "궈"); }
    @Test
    public void compoundJung_all_uye() { assertCompoundJung(ㅜ, ㅔ, "궤"); }
    @Test
    public void compoundJung_all_ui()  { assertCompoundJung(ㅜ, ㅣ, "귀"); }
    @Test
    public void compoundJung_all_eui() { assertCompoundJung(ㅡ, ㅣ, "긔"); }

    private void assertCompoundJung(int first, int second, String expected) {
        composer.reset();
        composer.process(ㄱ);
        composer.process(first);
        String commit = composer.process(second);
        assertNull(commit);
        assertEquals(expected, composer.getComposing());
    }

    // ===== 이중모음 불가 조합 → commit =====

    @Test
    public void compoundJung_invalid_aa()    { assertInvalidCompoundJung(ㅏ, ㅏ); }
    @Test
    public void compoundJung_invalid_eo()    { assertInvalidCompoundJung(ㅓ, ㅏ); }
    @Test
    public void compoundJung_invalid_yu_a()  { assertInvalidCompoundJung(ㅠ, ㅏ); }
    @Test
    public void compoundJung_invalid_o_eo()  { assertInvalidCompoundJung(ㅗ, ㅓ); }
    @Test
    public void compoundJung_invalid_u_a()   { assertInvalidCompoundJung(ㅜ, ㅏ); }

    private void assertInvalidCompoundJung(int first, int second) {
        composer.reset();
        composer.process(ㄱ);
        composer.process(first);
        String commit = composer.process(second);
        assertNotNull("합성 불가 이중모음은 commit 발생해야 함", commit);
    }

    // ===== 겹받침 불가 조합 → commit =====

    @Test
    public void compoundJong_invalid_gn() {
        // ㄱ+ㄴ → 겹받침 불가, commit
        composer.reset();
        composer.process(ㅇ);
        composer.process(ㅏ);
        composer.process(ㄱ);
        String commit = composer.process(ㄴ);
        assertNotNull("ㄱ+ㄴ 겹받침 불가 → commit", commit);
    }

    @Test
    public void compoundJong_invalid_nm() {
        composer.reset();
        composer.process(ㅇ);
        composer.process(ㅏ);
        composer.process(ㄴ);
        String commit = composer.process(ㅁ);
        assertNotNull("ㄴ+ㅁ 겹받침 불가 → commit", commit);
    }

    @Test
    public void compoundJong_invalid_sg() {
        composer.reset();
        composer.process(ㅇ);
        composer.process(ㅏ);
        composer.process(ㅅ);
        String commit = composer.process(ㄱ);
        assertNotNull("ㅅ+ㄱ 겹받침 불가 → commit", commit);
    }

    // ===== 연속 백스페이스 — 완전 삭제 =====

    @Test
    public void backspace_fullSequence_choJungJong() {
        composer.process(ㄱ);
        composer.process(ㅏ);
        composer.process(ㄴ);  // 간
        assertTrue(composer.backspace());  // 가
        assertEquals("가", composer.getComposing());
        assertTrue(composer.backspace());  // ㄱ
        assertEquals("ㄱ", composer.getComposing());
        assertTrue(composer.backspace());  // empty
        assertFalse(composer.isComposing());
        assertFalse(composer.backspace()); // nothing to delete
    }

    @Test
    public void backspace_compoundJong_thenJong_thenJung_thenCho() {
        // ㄱ+ㅏ+ㄹ+ㅂ = 갈+ㄼ
        composer.process(ㄱ);
        composer.process(ㅏ);
        composer.process(ㄹ);
        composer.process(ㅂ);

        assertTrue(composer.backspace());  // ㄼ → ㄹ
        assertTrue(composer.backspace());  // 갈 → 가
        assertEquals("가", composer.getComposing());
        assertTrue(composer.backspace());  // 가 → ㄱ
        assertEquals("ㄱ", composer.getComposing());
        assertTrue(composer.backspace());  // ㄱ → empty
        assertFalse(composer.isComposing());
    }

    @Test
    public void backspace_compoundJung_decompose() {
        // ㄱ+ㅗ+ㅏ = 과 (ㅘ)
        composer.process(ㄱ);
        composer.process(ㅗ);
        composer.process(ㅏ);
        assertEquals("과", composer.getComposing());

        assertTrue(composer.backspace());  // ㅘ → ㅗ
        assertEquals("고", composer.getComposing());

        assertTrue(composer.backspace());  // 고 → ㄱ
        assertEquals("ㄱ", composer.getComposing());
    }

    // ===== 백스페이스 후 재입력 =====

    @Test
    public void backspace_thenResume() {
        composer.process(ㄱ);
        composer.process(ㅏ);
        composer.process(ㄴ);  // 간
        composer.backspace();   // 가
        composer.process(ㅁ);   // 감
        assertEquals("감", composer.getComposing());
    }

    @Test
    public void backspace_toEmpty_thenNewInput() {
        composer.process(ㄱ);
        composer.backspace();
        assertFalse(composer.isComposing());

        composer.process(ㄴ);
        assertTrue(composer.isComposing());
        assertEquals("ㄴ", composer.getComposing());
    }

    // ===== reset 후 재입력 =====

    @Test
    public void reset_thenNewInput() {
        composer.process(ㄱ);
        composer.process(ㅏ);
        composer.reset();

        composer.process(ㄴ);
        composer.process(ㅏ);
        assertEquals("나", composer.getComposing());
    }

    @Test
    public void multipleResets() {
        for (int i = 0; i < 10; i++) {
            composer.process(ㄱ);
            composer.process(ㅏ);
            composer.reset();
        }
        assertFalse(composer.isComposing());
        composer.process(ㅎ);
        assertEquals("ㅎ", composer.getComposing());
    }

    // ===== 헬퍼 =====

    private void appendIfNotNull(StringBuilder sb, String s) {
        if (s != null) sb.append(s);
    }

    private String typeSequence(int[] input) {
        StringBuilder result = new StringBuilder();
        composer.reset();
        for (int code : input) {
            appendIfNotNull(result, composer.process(code));
        }
        result.append(composer.commit());
        return result.toString();
    }

    // ===== 긴 문장 시뮬레이션 =====

    @Test
    public void sentence_gamsahamnida() {
        assertEquals("감사합니다", typeSequence(new int[]{
            ㄱ,ㅏ,ㅁ, ㅅ,ㅏ, ㅎ,ㅏ,ㅂ, ㄴ,ㅣ, ㄷ,ㅏ
        }));
    }

    @Test
    public void sentence_annyeonghaseyo() {
        assertEquals("안녕하세요", typeSequence(new int[]{
            ㅇ,ㅏ,ㄴ, ㄴ,ㅕ,ㅇ, ㅎ,ㅏ, ㅅ,ㅔ, ㅇ,ㅛ
        }));
    }

    @Test
    public void sentence_saranghae() {
        assertEquals("사랑해", typeSequence(new int[]{
            ㅅ,ㅏ, ㄹ,ㅏ,ㅇ, ㅎ,ㅐ
        }));
    }

    @Test
    public void sentence_daehanminguk() {
        assertEquals("대한민국", typeSequence(new int[]{
            ㄷ,ㅐ, ㅎ,ㅏ,ㄴ, ㅁ,ㅣ,ㄴ, ㄱ,ㅜ,ㄱ
        }));
    }

    @Test
    public void sentence_hangugeo() {
        assertEquals("한국어", typeSequence(new int[]{
            ㅎ,ㅏ,ㄴ, ㄱ,ㅜ,ㄱ, ㅇ,ㅓ
        }));
    }

    @Test
    public void sentence_hwaiting() {
        // 화이팅 = ㅎ+ㅗ+ㅏ+ㅇ+ㅣ+ㅌ+ㅣ+ㅇ
        assertEquals("화이팅", typeSequence(new int[]{
            ㅎ,ㅗ,ㅏ, ㅇ,ㅣ, ㅌ,ㅣ,ㅇ
        }));
    }

    @Test
    public void sentence_gwiyeoun() {
        // 귀여운 = ㄱ+ㅜ+ㅣ+ㅕ+ㅇ+ㅜ+ㄴ
        assertEquals("귀여운", typeSequence(new int[]{
            ㄱ,ㅜ,ㅣ, ㅇ,ㅕ,ㅇ,ㅜ,ㄴ
        }));
    }

    // ===== 쌍자음 처리 =====

    @Test
    public void ssangConsonant_cho_gg() {
        composer.process(ㄲ);
        composer.process(ㅏ);
        assertEquals("까", composer.getComposing());
    }

    @Test
    public void ssangConsonant_jong_gg() {
        // ㄲ은 종성 가능 (index 2)
        composer.process(ㄱ);
        composer.process(ㅏ);
        String commit = composer.process(ㄲ);
        assertNull(commit); // 종성으로 붙음
        assertTrue(composer.isComposing());
    }

    @Test
    public void ssangConsonant_jong_ss() {
        // ㅆ은 종성 가능 (index 20)
        composer.process(ㄱ);
        composer.process(ㅏ);
        String commit = composer.process(ㅆ);
        assertNull(commit);
        assertTrue(composer.isComposing());
    }

    // ===== 단순 종성 → 모음 분리 (모든 단순 종성) =====

    @Test
    public void simpleJong_splitOnVowel_g() {
        assertSimpleJongSplit(ㄱ, "아", "가");
    }

    @Test
    public void simpleJong_splitOnVowel_n() {
        assertSimpleJongSplit(ㄴ, "아", "나");
    }

    @Test
    public void simpleJong_splitOnVowel_d() {
        assertSimpleJongSplit(ㄷ, "아", "다");
    }

    @Test
    public void simpleJong_splitOnVowel_l() {
        assertSimpleJongSplit(ㄹ, "아", "라");
    }

    @Test
    public void simpleJong_splitOnVowel_m() {
        assertSimpleJongSplit(ㅁ, "아", "마");
    }

    @Test
    public void simpleJong_splitOnVowel_b() {
        assertSimpleJongSplit(ㅂ, "아", "바");
    }

    @Test
    public void simpleJong_splitOnVowel_s() {
        assertSimpleJongSplit(ㅅ, "아", "사");
    }

    @Test
    public void simpleJong_splitOnVowel_ng() {
        assertSimpleJongSplit(ㅇ, "아", "아");
    }

    @Test
    public void simpleJong_splitOnVowel_j() {
        assertSimpleJongSplit(ㅈ, "아", "자");
    }

    @Test
    public void simpleJong_splitOnVowel_ch() {
        assertSimpleJongSplit(ㅊ, "아", "차");
    }

    @Test
    public void simpleJong_splitOnVowel_k() {
        assertSimpleJongSplit(ㅋ, "아", "카");
    }

    @Test
    public void simpleJong_splitOnVowel_t() {
        assertSimpleJongSplit(ㅌ, "아", "타");
    }

    @Test
    public void simpleJong_splitOnVowel_p() {
        assertSimpleJongSplit(ㅍ, "아", "파");
    }

    @Test
    public void simpleJong_splitOnVowel_h() {
        assertSimpleJongSplit(ㅎ, "아", "하");
    }

    private void assertSimpleJongSplit(int jong, String expectedCommit, String expectedComposing) {
        composer.reset();
        composer.process(ㅇ);
        composer.process(ㅏ);
        composer.process(jong);
        String commit = composer.process(ㅏ); // 모음 → 분리
        assertEquals(expectedCommit, commit);
        assertEquals(expectedComposing, composer.getComposing());
    }

    // ===== 빠른 교대 입력 (자음-모음-자음-모음) =====

    @Test
    public void rapidAlternating() {
        StringBuilder result = new StringBuilder();
        // ㄱㅏㄴㅏㄷㅏㄹㅏ = 가나다라
        composer.process(ㄱ); composer.process(ㅏ);
        // ㄴ → 종성 "간"
        composer.process(ㄴ);
        // ㅏ → split "가" commit, "나" composing
        result.append(composer.process(ㅏ));
        // ㄷ → 종성 "난"
        composer.process(ㄷ);
        // ㅏ → split "나" commit, "다" composing
        result.append(composer.process(ㅏ));
        // ㄹ → 종성 "달"
        composer.process(ㄹ);
        // ㅏ → split "다" commit, "라" composing
        result.append(composer.process(ㅏ));
        result.append(composer.commit());

        assertEquals("가나다라", result.toString());
    }

    // ===== 모음만 연속 =====

    @Test
    public void vowelOnly_sequence() {
        StringBuilder result = new StringBuilder();
        composer.process(ㅏ);
        result.append(composer.process(ㅓ)); // "ㅏ"
        result.append(composer.process(ㅗ)); // "ㅓ"
        result.append(composer.commit());    // "ㅗ"
        assertEquals("ㅏㅓㅗ", result.toString());
    }

    // ===== 자음만 연속 =====

    @Test
    public void consonantOnly_sequence() {
        StringBuilder result = new StringBuilder();
        composer.process(ㄱ);
        result.append(composer.process(ㄴ)); // "ㄱ"
        result.append(composer.process(ㄷ)); // "ㄴ"
        result.append(composer.commit());    // "ㄷ"
        assertEquals("ㄱㄴㄷ", result.toString());
    }

    // ===== 이중모음 + 종성 + 분리 복합 시나리오 =====

    @Test
    public void complex_compoundJungThenJong() {
        // 관 = ㄱ+ㅗ+ㅏ+ㄴ = ㄱ+ㅘ+ㄴ
        composer.process(ㄱ);
        composer.process(ㅗ);
        composer.process(ㅏ); // ㅘ
        assertEquals("과", composer.getComposing());
        composer.process(ㄴ); // 관
        assertEquals("관", composer.getComposing());
    }

    @Test
    public void complex_compoundJungJongSplit() {
        // 관 → ㅏ 입력 → "과" commit, "나" composing
        composer.process(ㄱ);
        composer.process(ㅗ);
        composer.process(ㅏ);
        composer.process(ㄴ); // 관
        String commit = composer.process(ㅏ);
        assertEquals("과", commit);
        assertEquals("나", composer.getComposing());
    }

    // ===== 종성 후 같은 자음 다시 (겹받침 불가) =====

    @Test
    public void sameJong_twice_noCombine() {
        // ㅇ+ㅏ+ㄱ+ㄱ → ㄱ+ㄱ 겹받침 불가 → "악" commit, "ㄱ" composing
        composer.process(ㅇ);
        composer.process(ㅏ);
        composer.process(ㄱ);
        String commit = composer.process(ㄱ);
        assertEquals("악", commit);
        assertEquals("ㄱ", composer.getComposing());
    }

    // ===== commit() 반복 호출 =====

    @Test
    public void commit_twice_secondEmpty() {
        composer.process(ㄱ);
        composer.process(ㅏ);
        String first = composer.commit();
        assertEquals("가", first);
        String second = composer.commit();
        assertEquals("", second);
    }

    // ===== isHangulJamo 경계값 =====

    @Test
    public void isHangulJamo_boundaries() {
        assertFalse(HangulComposer.isHangulJamo(0x3130)); // just before range
        assertTrue(HangulComposer.isHangulJamo(0x3131));   // ㄱ (first)
        assertTrue(HangulComposer.isHangulJamo(0x3163));   // ㅣ (last)
        assertFalse(HangulComposer.isHangulJamo(0x3164));  // ㅤ (filler, out)
        assertFalse(HangulComposer.isHangulJamo(0));
        assertFalse(HangulComposer.isHangulJamo(-1));
        assertFalse(HangulComposer.isHangulJamo(0xFFFF));
    }
}
