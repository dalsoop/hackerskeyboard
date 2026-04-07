# Hacker's Keyboard 한국어 포크 — 개발 가이드

## 개발 환경

### LXC (50170: android-dev)
```bash
phs infra lxc-enter --vmid 50170
```

### 빌드 도구
| 도구 | 버전 | 경로 |
|------|------|------|
| JDK | Temurin 17 | `/usr/lib/jvm/java-17-temurin` |
| JDK | Temurin 11 (레거시) | `/usr/lib/jvm/java-11-temurin` |
| Android SDK | API 36 | `/opt/android-sdk` |
| Gradle | 8.14.3 (wrapper) | `./gradlew` |
| NDK | r21e | `/opt/android-sdk/ndk-bundle` |
| Build Tools | 26.0.3 + 35.0.0 | `/opt/android-sdk/build-tools/` |

### 빌드 명령
```bash
# LXC 접속
phs infra lxc-enter --vmid 50170

# 소스 위치
cd /root/projects/hackerskeyboard

# 빌드
export JAVA_HOME=/usr/lib/jvm/java-17-temurin
./gradlew assembleDebug

# APK 위치
app/build/outputs/apk/debug/app-debug.apk
```

### 호스트에서 빌드 (한방)
```bash
# 소스 복사 + 빌드 + APK 회수
tar -C /root/hackerskeyboard -cf - app/src/main/java/org/pocketworkstation/pckeyboard/LatinIME.java | \
  pct exec 50170 -- tar -C /root/projects/hackerskeyboard -xf -

pct exec 50170 -- bash -c 'export JAVA_HOME=/usr/lib/jvm/java-17-temurin && \
  cd /root/projects/hackerskeyboard && ./gradlew assembleDebug 2>&1 | tail -5'

pct pull 50170 /root/projects/hackerskeyboard/app/build/outputs/apk/debug/app-debug.apk \
  /root/hackerskeyboard/app-debug-ko.apk
```

### 전체 소스 복사 (PR 머지 등 대규모 변경 후)
```bash
tar -C /root/hackerskeyboard -cf - . --exclude='.git' --exclude='app-debug-ko.apk' | \
  pct exec 50170 -- bash -c 'rm -rf /root/projects/hackerskeyboard && \
  mkdir -p /root/projects/hackerskeyboard && \
  tar -C /root/projects/hackerskeyboard -xf -'

pct exec 50170 -- bash -c 'export JAVA_HOME=/usr/lib/jvm/java-17-temurin && \
  cd /root/projects/hackerskeyboard && chmod +x gradlew && ./gradlew assembleDebug'
```

## 핵심 파일

### Java
| 파일 | 역할 |
|------|------|
| `LatinIME.java` | 메인 IME 서비스 — 한글 조합, 키 핸들링, 클립보드, 단축키 |
| `HangulComposer.java` | 한글 자모→음절 조합 엔진 (96 테스트 통과) |
| `ClipboardHistory.java` | 클립보드 히스토리 관리 (최근 10개) |
| `InputLanguageSelection.java` | 언어 목록 (ko 등록, BLACKLIST 제거) |
| `KeyboardSwitcher.java` | 키보드 모드 전환, 캐시 |
| `LatinKeyboardBaseView.java` | 키 렌더링, Ctrl 라벨 표시 |
| `GlobalKeyboardSettings.java` | ctrlActive 등 글로벌 설정 |
| `Keyboard.java` | 키 정의, XML 파싱 |
| `PointerTracker.java` | 멀티터치, modifier 키 감지 |

### 레이아웃 XML
| 파일 | 역할 |
|------|------|
| `xml-ko/kbd_qwerty.xml` | 한국어 4행 레이아웃 |
| `xml-ko/kbd_full.xml` | 한국어 5행 레이아웃 |
| `xml/kbd_full.xml` | 기본 5행 (영어 등) |
| `xml/kbd_qwerty.xml` | 기본 4행 |
| `xml/kbd_full_fn.xml` | Fn 오버레이 (5행) |
| `xml/kbd_compact_fn.xml` | Fn 오버레이 (4행) |
| `xml/popup_emoji.xml` | 이모지 팝업 (30개) |
| `layout/candidates.xml` | 액션 툴바 + 추천 단어 |

### 리소스
| 파일 | 역할 |
|------|------|
| `values-ko/strings.xml` | 한국어 번역 |
| `values-ko/donottranslate-keymap.xml` | 5행 한글 키맵 |
| `values-ko/donottranslate-altchars.xml` | 한글 팝업 대체문자 |
| `values/keycodes.xml` | 키코드 (key_next_language, key_clipboard 등) |
| `values/strings.xml` | 영어 기본 문자열 |

## 한글 조합 엔진 (HangulComposer.java)

### 상태 머신
```
S_NONE → S_CHO (초성) → S_JUNG (중성) → S_JONG (종성)
                ↑                              ↓
                └──────── 모음 입력 시 분리 ────┘
```

### 핵심 메서드
- `process(int code)` — 자모 입력 처리, 커밋할 텍스트 반환
- `commit()` — 현재 조합 확정, 상태 리셋
- `backspace()` — 자모 단위 삭제
- `getComposing()` — 현재 조합 중 텍스트
- `isComposing()` — 조합 중 여부
- `isHangulJamo(int code)` — 자모 판별

### IC 호출 패턴
```java
// 음절 전환 (handleHangulCharacter)
ic.setComposingText(mHangulComposing.toString(), 1);  // .toString() 필수!
ic.finishComposingText();
// 새 음절
ic.setComposingText(mHangulComposing.toString(), 1);

// Enter/스페이스 (handleSeparator)
ic.finishComposingText();  // reset 전에 호출!
mHangulComposer.reset();
sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER);
return;  // early return
```

### ⚠️ StringBuilder 주의
`setComposingText`에 **반드시 `.toString()`** 전달:
```java
// ❌ 버그: IC가 참조를 유지 → setLength(0)시 IC composing도 빈 문자열
ic.setComposingText(mHangulComposing, 1);

// ✅ 올바름: String 불변 복사본
ic.setComposingText(mHangulComposing.toString(), 1);
```

## 테스트

### 한글 조합 테스트 (LXC에서 실행)
```bash
pct exec 50170 -- bash -c 'cd /tmp && \
  /usr/lib/jvm/java-17-temurin/bin/javac TestAllEdgeCases.java && \
  /usr/lib/jvm/java-17-temurin/bin/java TestAllEdgeCases'
```

테스트 파일: `/tmp/TestAllEdgeCases.java` (148개), `/tmp/TestKakao.java` (187개)

### 테스트 항목
- 기본 음절 조합
- 겹받침 (ㄳㄵㄶㄺㄻㄼㄽㄾㄿㅀㅄ)
- 복합 모음 (ㅘㅙㅚㅝㅞㅟㅢ)
- 받침 분리
- 백스페이스 분해
- 연속 전송 (카톡 시뮬레이션)
- StringBuilder 참조 안전
- Ctrl 라벨 매핑
- Shift+Arrow 선택

## 머지된 업스트림 PR
| PR | 내용 |
|---|------|
| #978 | SDK 21~36, Gradle 8.x |
| #867 | AndroidX 마이그레이션 |
| #944 | Material Design UI |
| #691 | 팝업 커스텀 + AutoCaps 수정 |

## 릴리즈
```bash
# 빌드 + 푸시 + 릴리즈
git add -A && git commit -m "message" && git push origin master
gh release create v1.41.1-koXX --repo dalsoop/hackerskeyboard \
  --target master --title "제목" --notes "내용" \
  /root/hackerskeyboard/app-debug-ko.apk
```

## GitHub
- 포크: https://github.com/dalsoop/hackerskeyboard
- 원본: https://github.com/klausw/hackerskeyboard
