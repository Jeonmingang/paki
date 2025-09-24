# MIGRATION to 2.0.0

본 버전은 1.9.9 기반에서 **스테이지 자동 중앙 추첨, 이징 애니메이션, BGM 중첩 방지, 다이아/금 블럭 인터랙션, 랭킹/방송**을 추가/개선했습니다.

## 중요한 변경점
- `plugin.yml`의 `api-version`을 `1.16`으로 교정.
- `config.yml` 스키마 확장
  - `stages[*].cup`, `stages[*].advanceChance`, `stages[*].bgm`, `stages[*].effects`, `stages[*].announceLevel` 추가.
  - `autoAdvance.enabled`, `autoAdvance.intervalTicks` 추가.
  - `defaultWorld` 기본값 `bskyblock_world` 추가.
- 구슬 애니메이션: 1틱 스케줄러 + `easeInOutQuad` 적용, `drawingNow` 게이트로 동시 1건 보장.
- 중앙 실패 시 채팅 알림: `&7[꽝] &f#<슬롯> (&l왼쪽/오른쪽&r 1칸)` 형식.
- BGM: 스테이지/시작 시 `stopSound` 후 `playSound` (1.16 API).
- 다이아 블럭 = 배출구, 금블럭 우클릭 = 지급 버튼.
- 서버 브로드캐스트 및 `/파칭코 랭킹` 구현 (ranks.yml 사용).

## 하위 호환성
- 1.9.9의 핵심 동작(구슬 지급/애니/스테이지 흐름)을 유지하면서 상기 기능을 추가했습니다.
- 구 버전 config는 자동 마이그레이션 되지 않으므로 제공된 `config.yml` 템플릿을 참고하여 병합하세요.

## 설정 가이드
- `autoAdvance.intervalTicks`: 스테이지 상태일 때 중앙(다음 스테이지) 추첨 주기. 20 = 1초.
- 확률 표기는 `"1/3"` 또는 `0.33` 둘 다 지원.

## 빌드/배포
- Java 8, Spigot 1.16.5, CatServer 호환.
- `maven-shade-plugin` 제거, `maven-jar-plugin`으로 `com/minkang/ultimate/**`, `plugin.yml`, `config.yml`, `machines.yml`만 포함.



### 2.0.1
- pom.xml에서 Vault/Citizens 제거(코드 상 사용 없음) → 저장소 단일화(spigotmc-repo).
- 위로 인해 발생한 `VaultAPI`/`citizensapi` 의존성 해상도 실패 빌드에러 해소.


### 2.1.0
- 기계별 오퍼레이터 락(구슬 투입자 고정) 추가: 게임 종료 시까지 전용 사용.
- 자동 중앙 추첨의 행동 주체를 '오퍼레이터'로 변경.
- /파칭코 설정 구슬 <기계번호>: 손에 든 아이템으로 전용 구슬 설정.
- /파칭코 강제해제 <기계번호>, /파칭코 상태 <기계번호> 추가.
- GOLD_BLOCK 지급/DIAMOND_BLOCK 출구 동작 유지.


### 2.1.1
- MachineManager: 제네릭 캡쳐 에러(getOrDefault) 제거, 안전 파싱으로 변경.
- BgmController: stopSound(SoundCategory) 호출 제거 → 1.16.5 호환 API로 정리.


### 2.2.0
- 금블럭 우클릭 시 **손의 전용구슬이면 '투입'**, 아니면 **지급 버튼**으로 동작.
- '전용구슬' 인식 기준을 **이름+로어 완전 일치(색코드 무시)**로 강화.
- 스테이지 '진입' 로직 추가:
  - 구슬 상승 연출 → `entry.centralChance` 성공 시 **숫자 3개 스핀(Title)** → `entry.matchChance` 일치 시 **스테이지 1 진입**.
  - 실패 시 `&7[꽝] &f#<좌/우 1칸>` 알림 및 짧은 좌/우 연출.
- `config.yml` 확장: `entry.centralChance`, `entry.matchChance`, `visuals.entryRiseTicks`, `visuals.stepTicks`.
- `/파칭코 설치 <id>`: 스크린샷과 유사한 구조물 자동 생성(석탄/금/다이아 + 유리/철창/호퍼).
- `/파칭코 삭제 <id>`: 레코드 삭제 안내(구조물 삭제는 안전상 자동 수행하지 않음).
