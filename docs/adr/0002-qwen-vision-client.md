# 0002. 제보 사진 인식에 Qwen vision 을 쓴다

- 상태: 승인 (실호출 검증 완료, 1건 잔여)
- 날짜: 2026-08-19 (검증 2026-08-20)

## 배경

사용자가 제보 화면에서 올린 사진에서 품목·가격·수량을 읽어 입력값 후보를 제안한다. 외부 멀티모달
모델이 필요하고, 이는 되돌리기 어려운 의존성이라 기록해 둔다.

## 결정

DashScope 의 OpenAI 호환 endpoint(`/compatible-mode/v1/chat/completions`)로 Qwen vision 을 호출한다.
`external:qwen-client` gradle 서브프로젝트에 Feign 클라이언트를 두고, 기존 `kakao-client`·`kamis-client`
와 같은 구조를 따른다.

이미지는 base64 가 아니라 URL 로 넘긴다. 공식 문서에 나오는 형태이고, 5MB base64(약 6.7MB 본문)를
매 호출마다 싣는 것보다 가볍다. `images/` 접두사가 공개 읽기이므로 영구 URL 을 그대로 쓴다
(`0001` 이후의 S3 결정 참조 — `.infra/s3.tf`).

## 대안

- **base64 인라인**: 버킷 공개가 필요 없지만 본문이 커지고, VL 계열 지원 여부가 문서로 확정되지 않았다.
- **provider 직접 SDK**: OpenAI 호환 endpoint 를 쓰면 provider 교체 시 URL·model 값만 바뀐다. SDK 를
  물면 그 이점이 사라진다.
- **자체 OCR**: 가격표는 배치·서체가 제각각이고 품목명까지 읽어야 해서 규칙 기반으로는 어렵다.

## 실호출 검증 결과 (2026-08-20)

`QwenVisionLiveSmokeTest` 를 실제 키로 돌려 확인했다. 아래 5건이 해소됐고 1건이 남았다.
검증에 쓴 endpoint 는 `{workspaceId}.ap-southeast-1.maas.aliyuncs.com/compatible-mode/v1`,
모델은 `qwen-vl-plus` 다.

| 항목 | 결과 |
| --- | --- |
| 와이어 키가 snake_case 로 나가는지 | ✅ `image_url` 이 먹었다. 모델이 사진 내용을 정확히 읽었다 |
| `response_format: json_object` VL 지원 | ✅ 200, 순수 JSON 반환. 빼면 자유 문장이 오므로 효과가 있다 |
| `temperature: 0` 허용 | ✅ 거부되지 않았다 |
| endpoint host | ✅ workspace 형태로 200 |
| 모델 id `qwen-vl-plus` | ✅ 응답 `model` 필드에 그대로 |
| 응답 `message.content` 가 문자열인지 | ✅ 문자열 |
| 출력 잘림 | ✅ `finish_reason: stop`, 23 토큰 |

**남은 1건**: DashScope 가 우리 S3 URL 을 가져올 수 있는지. 버킷이 아직 없어서 확인하지 못했다.
`terraform apply` 후 `-Dqwen.live.imageUrl=<우리 URL>` 로 같은 테스트를 다시 돌린다. DashScope 는
`ap-southeast-1`, 버킷은 `ap-northeast-2` 라 리전 간 접근이다 — 공개 읽기이므로 될 것으로 보지만
확인 전까지 단정하지 않는다.

### 실측에서 새로 드러난 것

**토큰이 이미지 쪽에 몰린다.** `image_tokens: 1249` vs `text_tokens: 60`. 비용이 사실상 이미지
해상도에 비례한다. 지금은 5MB 원본을 그대로 올리므로, 업로드 전 리사이즈를 넣으면 비용이 크게
줄어들 여지가 있다. 실사용량을 보고 판단한다.

**계정에서 쓸 수 있는 모델이 `qwen-vl-plus` 보다 최신이다.** `/models` 응답에 `qwen3.8-27b`,
`qwen3.8-2.4t-a95b`, `qwen-image-3.0-pro`, `deepseek-v4-pro-0813`, `ZHIPU/GLM-5.3` 등이 있다.
`qwen-vl-plus` 로 동작은 하지만 가격표 OCR 정확도를 위해 최신 VL 모델을 비교해 볼 만하다
(`-Dqwen.live.model=` 로 같은 테스트를 돌리면 된다). 모델 id 는 설정값이므로 코드 변경이 없다.

## 확인 방법 — 남은 1건과 회귀

`QwenVisionLiveSmokeTest` 를 쓴다. 레포의 다른 live smoke 테스트와 같은 방식으로
기본값에서는 skip 되고 `-Dqwen.live=true` 일 때만 돈다.

```bash
QWEN_API_KEY=... ./gradlew :external:qwen-client:test \
    --tests '*QwenVisionLiveSmokeTest*' -Dqwen.live=true
```

세 테스트가 각각 (a) 프로덕션과 같은 본문이 200 을 받고 JSON 이 돌아오는지, (b) `response_format`
을 뺐을 때와 비교, (c) 계정에서 쓸 수 있는 모델 목록을 확인한다. host·model·이미지는 프로퍼티로
바꿀 수 있다.

```bash
-Dqwen.live.url=https://dashscope.aliyuncs.com/compatible-mode/v1
-Dqwen.live.model=qwen-vl-max
-Dqwen.live.imageUrl=https://<bucket>.s3.ap-northeast-2.amazonaws.com/images/<uuid>.jpg
```

버킷이 준비되면 마지막 프로퍼티로 남은 1건(우리 S3 URL 접근)까지 확인된다.

수동 확인이 필요하면:

```bash
curl -X POST "$QWEN_VISION_URL/chat/completions" \
  -H "Authorization: Bearer $QWEN_API_KEY" -H 'Content-Type: application/json' \
  -d '{"model":"qwen-vl-plus","messages":[{"role":"user","content":[{"type":"image_url","image_url":{"url":"<우리 S3 URL>"}},{"type":"text","text":"이 사진의 가격표를 JSON 으로 읽어라"}]}],"temperature":0,"response_format":{"type":"json_object"}}'
```

## 운영 시크릿 취급

키는 콘솔에서 CSV 로 내려온다. 그 파일은 평문이므로 **다운로드 폴더에 두지 않는다** — 비밀 관리자에
옮기고 원본을 지운다. 실수로 공유했으면 콘솔에서 폐기하고 재발급한다(키 값 자체는 되돌릴 수 없다).

CSV 에는 `workspaceId` 가 든 host 도 함께 온다. 이건 자격증명은 아니지만 계정을 식별하는 값이라
저장소에 커밋하지 않고 `QWEN_VISION_URL` 로만 주입한다.

## 결과

- 비용이 요청 수에 비례한다. 엔드포인트를 `ROLE_USER` 로 막았지만 사용자 단위 쿼터는 없다 — 한 계정이
  반복 호출하면 전체 쿼터를 소진시킬 수 있다. 실제 사용량을 보고 판단한다.
- `QWEN_API_KEY` 가 운영 시크릿으로 추가된다. 로그·에러 응답·이미지 URL 에 노출하지 않는다.
- 재시도는 총 2회(최초 1회 + 재시도 1회)다. read timeout 20s 이므로 최악 지연이 약 41s 다. 사용자가
  화면에서 기다리는 호출이라 그 이상 늘리지 않았다.
- 모델을 바꾸면 `qwen.vision.url`·`qwen.vision.model` 값만 바뀐다. `Qwen*` 클래스 접두는 이름일 뿐
  실제로는 OpenAI chat-completions 스키마다.
