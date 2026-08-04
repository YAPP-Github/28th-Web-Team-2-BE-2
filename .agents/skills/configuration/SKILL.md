---
name: configuration
description: Spring profile, Jasypt 비밀값, AWS S3와 Actuator 설정을 변경하거나 검증할 때 현재 저장소 규칙을 적용합니다.
---

# 설정

## 원칙

- 공통 기본값은 `application.yaml`, 운영 전용 값은 `application-prod.yaml`에 둔다.
- 실제 비밀값과 암호문을 저장소에 기록하지 않는다.
- JWT와 AWS 비밀값은 환경변수의 `ENC(...)`를 Jasypt로 복호화한다.
- `JASYPT_ENCRYPTOR_PASSWORD`는 암호문과 분리된 환경변수로 주입한다.
- AWS access key와 secret key가 모두 있으면 static credentials, 둘 다 없으면 AWS default provider chain을 쓴다. 하나만 있으면 설정 오류다.
- AWS bucket 또는 base URL이 없어도 앱은 기동하고 이미지 API 호출 때 `IMAGE_STORAGE_UNAVAILABLE`로 실패한다.

## 암호문 생성

```bash
JASYPT_INPUT='plain-value' \
JASYPT_ENCRYPTOR_PASSWORD='master-password' \
./gradlew -q jasyptEncrypt
```

출력된 암호문만 `ENC(...)` 환경변수 값으로 사용한다. 명령이나 로그에 평문을 남기지 않는다.

## 설정 변경 확인

1. `@ConfigurationProperties`가 기존 키와 일치하는지 확인한다.
2. 운영 필수값 때문에 local/test 기동이 깨지지 않는지 확인한다.
3. 공개 endpoint와 상세 health 노출 범위를 보안 테스트로 고정한다.
4. 미래 DB·캐시 방침은 [향후 인프라](references/future-infrastructure.md)를 참고하되 활성 의존성을 추가하지 않는다.

```bash
./gradlew check --no-daemon
```
