# 🐉 100-day-dragon-egg (드래곤 알 경주)

**100일간 드래곤 알을 소지한 플레이어가 승리**하는 Paper 플러그인. Minecraft **1.21**용.

게임 시작 후 **100일**(게임 시간, 2,400,000틱)이 지나면, 그 시점에 **드래곤 알을 인벤토리에 가진 플레이어**가 승리합니다.

## ✨ 주요 기능

- **플러그인 활성화 시 자동 시작** — 서버 로드와 함께 게임이 켜집니다 (원치 않으면 `onEnable`의 `startGame()` 주석 처리)
- **남은 시간 알림** — 매일마다 남은 일수를 브로드캐스트, **마지막 10일**부터 경고 + 벨 사운드, **마지막 1일** 긴급 경고
- **드래곤 알 소지 추적** — 인벤토리 변화를 실시간으로 추적해 현재 알 보유자를 파악
- **승리 연출** — 승리자 발표 + 타이틀, 승리 위치에 **엔더 드래곤 소환**, 번개/파티클/사운드 효과
- **무승부 처리** — 게임 종료 시 알을 가진 사람이 없으면 무승부 선언

## 🛠 요구사항

- **Minecraft:** 1.21 (Paper/Spigot)
- **Java:** 21
- **빌드:** Maven

## 💾 설치 / 빌드

```bash
mvn clean package
```

생성된 `target/*.jar` 를 서버 `plugins/` 폴더에 넣고 서버를 재시작합니다.

서버 실행 후 `/dragongame` 명령어로 게임을 관리할 수 있습니다.

## 🕹 명령어

| 명령어 | 설명 |
|--------|------|
| `/dragongame start` | 게임 시작 |
| `/dragongame stop` | 게임 강제 중단 |
| `/dragongame status` | 남은 시간/게임 상태 확인 |

**권한:** `dragongame.admin` — 기본값 `op`

## 📁 소스 구조

```
src/main/java/com/jeonensu/dragoneggrace/
├── DragonEggRacePlugin.java  # 메인 플러그인 + 타이머/승리 연출
├── DragonEggTracker.java     # 드래곤 알 소지자 추적
├── GameCommand.java          # /dragongame 명령어 처리
└── GameEventListener.java    # 인벤토리/아이템 이벤트
```

## 📦 부가 자료

- `AROWW.zip` — 리소스팩 (드래곤 알/화살 시각 효과용)

## 📄 라이선스

**GPL v3** — 자유로운 사용/수정/배포가 가능합니다.

> 버전: 1.0-beta — Paper API 1.21, Maven shade 빌드.