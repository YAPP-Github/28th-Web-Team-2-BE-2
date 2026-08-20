---
name: image-upload-flow
description: 서버 경유 multipart 이미지 업로드 계약을 구현하거나 검증할 때 사용합니다.
---

# 이미지 업로드 흐름

## 경로와 인증

경로를 `/api/v1` 아래에 둔다. `ResponseWrapper`가 `/api/v1/**`만 envelope으로 감싸므로 그 밖에
두면 같은 서비스에서 응답 모양이 갈린다.

업로드는 `ROLE_USER`를 요구한다. 무인증 업로드는 우리 버킷에 임의 파일을 쌓는 통로가
되고, 제보 작성 자체가 이미 `ROLE_USER`다.

## 공통 제한

- PNG와 JPEG만 허용한다.
- 최대 크기는 5MB다.
- key는 `images/{UUID}.{png|jpg}`다.
- 영구 저장 URL은 `AWS_S3_BASE_URL + key`다. 이 값을 제보의 `photoUrl`로 저장한다.
- `images/` 접두사는 공개 읽기다. 영구 URL을 그대로 `img src`에 쓸 수 있다.
- bucket/base URL 누락 또는 S3 실패는 내부 정보 없이 HTTP 503, `IMAGE_STORAGE_UNAVAILABLE`을 반환한다.

## 업로드

`POST /api/v1/images`, part 이름 `image`, 성공 시 HTTP 201과 `ImageUploadResponse(imageUrl)` 계약을 유지한다. application이 파일 metadata를 검증하고 infrastructure가 실제 S3 PUT을 수행한다.

## 클라이언트 직접 업로드는 두지 않는다

presigned PUT 발급 엔드포인트를 만들었다가 지웠다. 쓰는 클라이언트가 없었고, 5MB 사진은 서버를
거쳐도 충분하다. 브라우저 직접 업로드가 실제로 필요해지면 그때 되살린다 — 그때는 S3 CORS 설정도
함께 필요하다.

## 검증

- 단위 테스트: 확장자, MIME, 크기, key, 만료, 503
- MockMvc: 인증(ROLE_USER), 요청·응답 계약, HTTP 상태
- 실제 S3 왕복은 아직 검증하지 않았다. testcontainers 에 MinIO 를 추가할지는 `AGENTS.md` 의
  기술 추가 방침과 함께 결정한다.

```bash
./gradlew test --tests '*Image*Test' --no-daemon
```
