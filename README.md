# Tricky Tower 3차 발표 README

## 1. 간단한 게임 소개

Tricky Tower는 중력 기반 물리 엔진을 활용해 블록을 쌓아 올리는 전략형 퍼즐 게임입니다.

## 2. 현재 진행 상황 (진행 정도 표시)

* **물리 엔진 구현**: 100%
* **AABB 충돌 처리**: 100%
* **터치 입력 (드래그·회전)**: 100%
* **씬 전환 및 레벨 시스템**: 100%
* **UI 레이아웃 및 UX 폴리싱**: 100%
* **아트 에셋 통합**: 100%
* **오디오 통합**: 100%

## 3. Git 커밋 빈도 (github-insights-commits)

주차별 커밋 수를 통해 개발 활동을 시각화합니다.

| 주차 (ISO Year‑Week) |        기간        | 커밋 수 (예시) |
| :----------------: | :--------------: | :-------: |
|         1주차        |    4/7 – 4/13    |     0     |
|         2주차        |    4/14 – 4/20   |     2     |
|         3주차        |    4/21 – 4/27   |     5     |
|         4주차        |    4/28 – 5/4    |     3     |
|         5주차        |    5/5 – 5/11    |     0     |
|         6주차        |    5/12 – 5/18   |     9     |
|         7주차        |    5/19 – 5/25   |     4     |
|         8주차        |    5/26 – 6/1    |     1     |
|         9주차        |     6/2 – 6/8    |     0     |
|        10주차        |    6/9 – 6/15    |     3     |
|        11주차        | 6/16 – 6/16 (부분) |     1     |

## 4. 목표 변경 사항

물리 부분에서 시간을 많이 소모하여 10개의 스테이지에서 5개의 스테이지로 변경.

## 5. 사용된 기술

* **안드로이드(Java 11) 개발 환경**
  `compileSdk = 35`, `minSdk = 24` 설정과 함께 Java 11 호환성으로 개발됨.

* **JBox2D 물리엔진 활용**
  의존성 목록에 `org.jbox2d:jbox2d-library:2.2.1.1`을 추가하여 블록 물리 동작을 구현.

## 6. 참고한 것

* **배경 이미지와 BGM**
  트리키타워와 메이플스토리의 배경이미지, 메이플스토리의 BGM을 사용.
  리소스 폴더에 hnesis, elinia 등 지역명 기반 이미지와 음악 파일 존재.

* **외부 라이브러리 참고**
  README에서 JBox2D 물리엔진 사용이 명시되어 있음.

## 7. 수업내용에서 차용한 것

* **자체 2D 게임 프레임워크 (a2dg)**
  수업시간에 제작한 프레임워크 차용

## 8. 아쉬운 것

* 시간이 남았다면 다른 모드를 만들고 싶었지만 한가지 모드밖에 만들지 못했다.
* 팔기 위해서 보충할 것은 배경과 블럭을 구분을 더욱 시켜 시각적으로 잘 보이게 만들고, 다른 게임에 없는 특이한 기능을 추가해야 될 것 같다.
* 가끔 빠르게 내려간 블럭의 히트박스가 겹쳐서 블럭이 합쳐지는 현상이 있었는데 이를 해결하지 못했다.

## 9. 수업에 대한 내용

* 이번 수업을 들으며 모바일 게임을 만드는 것을 기대하였는데, 이를 이루게 되어서 너무 좋았다.
