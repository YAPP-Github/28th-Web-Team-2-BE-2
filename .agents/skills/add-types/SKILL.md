---
name: add-types
description: Java 소스의 적격한 var 지역 변수만 실제 반환 타입으로 바꾸고 컴파일로 검증할 때 사용합니다.
---

# 명시적 타입 추가

## 범위

사용자가 지정한 Java 파일 또는 diff에서 `var`로 선언한 지역 변수만 찾는다. 메서드 호출이나 생성식의 반환 타입을 IDE 추측이 아니라 선언부에서 확인한 뒤 명시적 타입으로 바꾼다.

## 제외

- lambda parameter와 반복문의 타입 추론
- 익명 클래스처럼 명시 타입이 더 불명확해지는 경우
- 타입 변경과 무관한 포맷·이름·구조 리팩터링
- 사용자가 수정 중인 범위 밖 파일

## 절차

1. `rg -n '\bvar\b'`로 후보를 찾는다.
2. 각 식의 컴파일 시점 반환 타입과 generic parameter를 확인한다.
3. 최소 diff로 교체한다.
4. Spotless와 관련 compile/test를 실행한다.

```bash
./gradlew spotlessCheck compileJava compileTestJava --no-daemon
```
