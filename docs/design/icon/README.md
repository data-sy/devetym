# 안드로이드 런처 아이콘 소스

`androidApp/src/main/res/`의 adaptive 런처 아이콘 PNG를 생성하는 소스 SVG.
iOS용 마스터 lockup(edge-to-edge)을 안드로이드 adaptive icon 규격
(108dp 캔버스 / 중앙 66dp 세이프존)에 맞게 재구성한 것.

## 파일 → 산출물 매핑

| 소스 SVG | 생성 대상 (`mipmap-*/`) | 비고 |
|---|---|---|
| `icon-android-foreground.svg` | `ic_launcher_foreground.png` | 투명 배경 + lockup 0.62배·중앙정렬(세이프존) |
| `icon-android-legacy-square.svg` | `ic_launcher.png` | API≤25 정사각 폴백(초록 배경 구움) |
| `icon-android-legacy-round.svg` | `ic_launcher_round.png` | API≤25 원형 폴백 |
| `icon-android-monochrome.svg` | `ic_launcher_monochrome.png` | Android 13+ 테마 아이콘(단색 실루엣) |

배경(full-bleed 단색 `#2E5D3A`)은 PNG가 아니라
`androidApp/src/main/res/drawable/ic_launcher_background.xml` 벡터가 담당.
adaptive 정의는 `mipmap-anydpi-v26/ic_launcher{,_round}.xml`.

## 재생성 방법

밀도별 픽셀: foreground/monochrome(108dp) = 108·162·216·324·432,
legacy(48dp) = 48·72·96·144·192.

```sh
# 예: foreground xxxhdpi
rsvg-convert -w 432 -h 432 icon-android-foreground.svg \
  -o ../../../androidApp/src/main/res/mipmap-xxxhdpi/ic_launcher_foreground.png
```

## 마스터 lockup

"개발/어원/사전" 원본 lockup(icon.svg·icon-dark.svg 등)은 별도 design repo에 있음:
`~/dev-etymology/docs/design/icon/assets/v2/`. 여기 SVG들은 그 lockup을
안드로이드 규격으로 파생한 것.

## 주의 — 세이프존

foreground/monochrome/legacy-round는 lockup을 `scale(0.62)`로 축소해
66dp 세이프존 안에 둔다. 이 값을 키우면 원형 마스크에서 글자 모서리가 잘린다.
bare `@color` 배경을 쓰면 Android 16 런처가 인셋+민트 플레이트를 그리므로
반드시 full-bleed drawable 배경을 유지할 것.
