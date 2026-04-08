# 개발 가이드

## 환경

- **빌드 서버**: LXC 50170 (Proxmox)
- **소스 경로**: `/root/projects/hackerskeyboard`
- **JDK**: Java 17 Temurin (`/usr/lib/jvm/java-17-temurin`)
- **Gradle**: 8.14.3 (wrapper)
- **Android SDK**: `/opt/android-sdk`
- **Target SDK**: 36 (Android 16)
- **Min SDK**: 21

## 빌드

```bash
# LXC 50170 내부에서
export JAVA_HOME=/usr/lib/jvm/java-17-temurin

# 디버그
cd /root/projects/hackerskeyboard
./gradlew assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk

# 릴리즈 (서명 필요)
export SIGNING_STORE_PASSWORD=hackerskeyboard
export SIGNING_KEY_ALIAS=hackerskeyboard
export SIGNING_KEY_PASSWORD=hackerskeyboard
./gradlew assembleRelease
# → app/build/outputs/apk/release/app-release.apk
```

호스트에서 실행:
```bash
pct exec 50170 -- bash -c "export JAVA_HOME=/usr/lib/jvm/java-17-temurin && cd /root/projects/hackerskeyboard && ./gradlew assembleDebug"
```

## 키스토어

- 파일: `app/keystore.jks` (gitignore됨)
- alias: `hackerskeyboard`
- password: `hackerskeyboard` (store/key 동일)
- 유효기간: 10,000일
- 자체서명 인증서 (CN=dalsoop, Seoul, KR)

키스토어 재생성이 필요하면:
```bash
export JAVA_HOME=/usr/lib/jvm/java-17-temurin
$JAVA_HOME/bin/keytool -genkey -v \
  -keystore app/keystore.jks -keyalg RSA -keysize 2048 -validity 10000 \
  -alias hackerskeyboard \
  -dname 'CN=dalsoop,OU=dev,O=dalsoop,L=Seoul,ST=Seoul,C=KR' \
  -storepass hackerskeyboard -keypass hackerskeyboard
```

## GitHub 릴리즈 배포

호스트에서:
```bash
pct pull 50170 /root/projects/hackerskeyboard/app/build/outputs/apk/release/app-release.apk /tmp/hackerskeyboard-release.apk
gh release create vX.Y.Z /tmp/hackerskeyboard-release.apk \
  --repo dalsoop/hackerskeyboard \
  --title "vX.Y.Z — 설명" \
  --notes "변경 내용"
```

## 소스 구조

```
app/src/main/java/org/pocketworkstation/pckeyboard/
├── LatinIME.java          ← IME 메인 (입력 처리, 라이프사이클)
├── HangulComposer.java    ← 한글 자모 조합 엔진
├── KeyboardSwitcher.java  ← 키보드 레이아웃 전환
├── LatinKeyboard.java     ← 키보드 뷰 정의
├── LatinKeyboardView.java ← 키 터치/그리기
├── CandidateView.java     ← 자동완성 후보 UI
├── Suggest.java           ← 단어 추천
├── ClipboardHistory.java  ← 클립보드 기능
└── TextEntryState.java    ← 입력 상태 머신
```

## 한글 입력 아키텍처

### 핵심 컴포넌트

- **`HangulComposer`**: 자모 → 음절 조합. `process(code)` → commit할 완성 음절 반환, `getComposing()` → 현재 조합 중인 글자
- **`mHangulComposing`** (StringBuilder): IC에 `setComposingText()`로 보내는 현재 조합 텍스트
- **`mComposing`** (StringBuilder): 영문 예측 입력용 composing 버퍼

### 입력 흐름

```
키 입력 → onKey()
  ├─ isWordSeparator? → handleSeparator()
  └─ else → handleCharacter()
               ├─ isHangulJamo? → handleHangulCharacter()
               │     ├─ HangulComposer.process(code)
               │     ├─ 완성 음절 → finishComposingText + 새 조합 시작
               │     └─ 조합 중 → setComposingText()
               └─ else → 영문 처리 (mComposing + prediction)
```

### IME 라이프사이클과 composing 리셋

```
onStartInput()       ← 새 입력 필드 진입 (or 같은 필드 재시작)
                       여기서 모든 composing 상태 리셋 (한글 + 영문)
onStartInputView()   ← 키보드 뷰 표시
onUpdateSelection()   ← 커서/선택 변경 시 composing 상태 확인
onFinishInputView()   ← 키보드 뷰 숨김
onFinishInput()       ← 입력 필드 떠남
```

**중요**: `onStartInput()`에서 composing 리셋을 안 하면 카톡 등에서 메시지 전송 후 composing 텍스트가 다음 입력으로 leak됨. (v1.0.1에서 수정)

### 알려진 주의사항

1. **handleSeparator의 한글 Enter 처리**: 한글 composing 중 separator(마침표 등)를 치면 한글 commit 후 `sendDownUpKeyEvents(KEYCODE_ENTER)` + early return함. 마침표/쉼표 등 비-Enter separator도 Enter로 처리되는 문제 있음 (향후 수정 필요)
2. **setComposingText에 StringBuilder 직접 전달**: 일부 앱에서 mutable reference 문제 발생 가능. `.toString()` 사용 권장
3. **commitHangulComposing()**: `finishComposingText()`를 `reset()` 전에 호출해야 함. 순서 바꾸면 빈 텍스트가 commit됨
