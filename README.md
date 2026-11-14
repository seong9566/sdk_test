# sdk_test

Pleos Connect(AAOS) 환경 확인용 PoC 앱입니다. WebView에 카카오 지도 JS API를 띄우고, 차량의 위치를 실시간 마커로 표시합니다. 웹소켓 중계 서버를 통해 다른 참가자의 위치도 함께 그립니다.

## 구성

| 경로 | 설명 |
| --- | --- |
| `app/src/main/java/.../MapActivity.kt` | WebView + Kakao Maps. AAOS 표준 `LocationManager` 좌표를 JS로 넘겨 내 마커를 갱신하고, 중계 서버로 위치를 보냄 |
| `app/src/main/java/.../LocationRelay.kt` | OkHttp 웹소켓 클라이언트. 위치 송수신, 끊기면 3초 뒤 재연결 |
| `app/src/main/java/.../PlaceholderActivity.kt` | 런처 화면. 3분할 화면(차량 상태 \| Pleos 내비 \| 이 앱)의 오른쪽 칸 자리표시 |
| `app/src/main/assets/kakao_map.html` | 지도 페이지. `updateLocation` · `updatePeer` · `removePeer` 등 JS 함수 제공 |
| `app/src/debug/` | debug 빌드 전용 네트워크 설정. `10.0.2.2`(에뮬레이터에서 본 호스트 PC)만 평문 `ws://` 허용 |
| `relay/server.js` | 위치 공유 중계 서버. 받은 위치를 보낸 사람을 뺀 모두에게 전달하고, 연결이 끊기면 `leave`를 알림 |
| `relay/fake-peer.js` | 가짜 상대 차량. 강남역 주변을 원을 그리며 1초마다 위치 전송 |

## 설정

프로젝트 루트의 `local.properties`(커밋되지 않음)에 값을 넣습니다.

```properties
KAKAO_JS_KEY=카카오_JavaScript_키
# 선택. 기본값은 ws://10.0.2.2:8080 (에뮬레이터 → 호스트 PC의 로컬 중계 서버)
RELAY_URL=ws://10.0.2.2:8080
```

카카오는 요청 출처(도메인)를 검사합니다. 앱은 `https://localhost`를 baseUrl로 페이지를 로드하므로, [카카오 개발자 콘솔](https://developers.kakao.com)의 Web 플랫폼에 `https://localhost`를 등록해야 합니다.

## 실행

### 1. 중계 서버

Node.js 22 이상이 필요합니다(`fake-peer.js`가 내장 `WebSocket`을 사용).

```bash
cd relay
npm install
npm run server          # ws://0.0.0.0:8080 (포트는 PORT 환경변수로 변경)
```

다른 터미널에서 가짜 상대 차량을 띄우면 지도에 상대 마커가 나타납니다.

```bash
cd relay
npm run peer            # 또는 node fake-peer.js [ws://localhost:8080] [id]
```

### 2. 앱

```bash
./gradlew installDebug
```

런처 아이콘은 `PlaceholderActivity`를 엽니다. 지도 화면은 adb로 직접 띄웁니다.

```bash
adb shell am start -n com.example.sdk_test/.MapActivity
```

에뮬레이터에서는 Extended controls → Location으로 위치를 바꿔 마커 이동을 확인할 수 있습니다. 로그는 `PleosMapPoC` 태그로 남습니다.

```bash
adb logcat -s PleosMapPoC
```

## 메시지 형식

중계 서버와 주고받는 JSON 메시지입니다.

```json
{ "type": "loc", "id": "car-1a2b3c", "lat": 37.4979, "lng": 127.0276 }
{ "type": "leave", "id": "car-1a2b3c" }
```

## 참고

- PoC입니다. 중계 서버는 위치를 저장하지 않으며 인증도 없습니다. 실서비스에는 `wss`(TLS), 인증, 위치정보법상 동의·기록 처리가 필요합니다.
- release 빌드는 평문 통신을 허용하지 않으므로 `RELAY_URL`을 `wss://` 주소로 지정해야 합니다.
- minSdk 28, targetSdk/compileSdk 37
