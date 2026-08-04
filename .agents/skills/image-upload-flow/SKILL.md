---
name: image-upload-flow
description: 공개 multipart 업로드와 S3 presigned PUT 발급이라는 두 이미지 업로드 계약을 구현하거나 검증할 때 사용합니다.
---

# 이미지 업로드 흐름

## 공통 제한

- PNG와 JPEG만 허용한다.
- 최대 크기는 5MB다.
- key는 `images/{UUID}.{png|jpg}`다.
- 영구 저장 URL은 `AWS_S3_BASE_URL + key`다. 만료되는 upload URL을 게시글이나 사용자 데이터에 저장하지 않는다.
- bucket/base URL 누락 또는 S3 실패는 내부 정보 없이 HTTP 503, `IMAGE_STORAGE_UNAVAILABLE`을 반환한다.

## 1. 서버 경유 multipart

`POST /api/images`, part 이름 `image`, 성공 시 HTTP 201과 `ImageUploadResponse(imageUrl)` 계약을 유지한다. application이 파일 metadata를 검증하고 infrastructure가 실제 S3 PUT을 수행한다.

## 2. 클라이언트 직접 업로드

`POST /api/images/presigned-url` 요청은 `filename`, `contentType`, `size`다. 응답은 다음 필드를 가진다.

- `uploadUrl`
- `imageUrl`
- `method`: 항상 `PUT`
- `expiresAt`: 발급 시점부터 10분
- `contentType`

Presigned 요청에는 `Content-Type`과 `Content-Length`를 포함한다. 클라이언트도 동일한 header와 길이로 PUT해야 한다. 현재 계약에는 Media DB 상태 머신이나 업로드 확인 API가 없으므로 새로 만들지 않는다.

## 검증

- 단위 테스트: 확장자, MIME, 크기, key, 만료, 503
- MockMvc: 공개 경로, 요청·응답 계약, HTTP 상태
- MinIO 통합: presigned PUT 후 HEAD, multipart 업로드 후 HEAD

```bash
./gradlew test --tests '*Image*Test' --no-daemon
```
