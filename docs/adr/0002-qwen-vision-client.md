# 0002. 제보 사진 인식에 Qwen vision 을 쓴다

- 상태: 제안
- 날짜: 2026-08-19

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

## 미검증 — 실제 키로 확인해야 하는 것

이 클라이언트는 **실호출 검증을 한 번도 하지 못한 상태로 들어왔다.** 아래는 문서로 확정하지 못했고,
전부 설정으로 열어 두었다.

1. `response_format: {"type":"json_object"}` 의 VL 모델 지원 여부. 텍스트 모델 위주로 문서화돼 있다.
   미지원이면 무시되거나 400 이다. 프롬프트에도 JSON 지시를 넣어 이중으로 방어했고 서버가 스키마를
   검증하지만, 어느 쪽이 동작하는지 확인이 필요하다.
2. `temperature: 0.0` 허용 여부. native API 는 `(0, 2)` 개구간을 요구한다. compatible-mode 가 `0` 을
   받는지 확인이 필요하다.
3. endpoint host. 문서가 리전별로 세 형태(`dashscope.aliyuncs.com`, `dashscope-intl.aliyuncs.com`,
   `{workspace}.{region}.maas.aliyuncs.com`)를 보여준다.
4. 모델 id `qwen-vl-plus` 의 계정 사용 가능 여부. `GET {url}/models` 로 확인.
5. DashScope 가 우리 S3 URL 을 실제로 가져올 수 있는지. 이 기능 전체가 여기 걸려 있다.
6. `message.content` 가 문자열이 아니라 파트 배열로 오는 provider 가 있다. 그러면 역직렬화가 실패한다.

확인 방법:

```bash
curl -X POST "$QWEN_VISION_URL/chat/completions" \
  -H "Authorization: Bearer $QWEN_API_KEY" -H 'Content-Type: application/json' \
  -d '{"model":"qwen-vl-plus","messages":[{"role":"user","content":[{"type":"image_url","image_url":{"url":"<우리 S3 URL>"}},{"type":"text","text":"이 사진의 가격표를 JSON 으로 읽어라"}]}],"temperature":0,"response_format":{"type":"json_object"}}'
```

## 결과

- 비용이 요청 수에 비례한다. 엔드포인트를 `ROLE_USER` 로 막았지만 사용자 단위 쿼터는 없다 — 한 계정이
  반복 호출하면 전체 쿼터를 소진시킬 수 있다. 실제 사용량을 보고 판단한다.
- `QWEN_API_KEY` 가 운영 시크릿으로 추가된다. 로그·에러 응답·이미지 URL 에 노출하지 않는다.
- 재시도는 총 2회(최초 1회 + 재시도 1회)다. read timeout 20s 이므로 최악 지연이 약 41s 다. 사용자가
  화면에서 기다리는 호출이라 그 이상 늘리지 않았다.
- 모델을 바꾸면 `qwen.vision.url`·`qwen.vision.model` 값만 바뀐다. `Qwen*` 클래스 접두는 이름일 뿐
  실제로는 OpenAI chat-completions 스키마다.
