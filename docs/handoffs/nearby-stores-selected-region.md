# 선택 지역 좌표로 주변 가게를 조회하는 프론트엔드 전달 사항

백엔드는 `GET /api/v1/regions/search` 응답에 선택한 법정동의 `latitude`, `longitude`를 추가한다. 프론트엔드는 이 값을 보관해 초기 주변 가게 조회에 사용해야 한다. `GET /api/v1/stores/nearby`의 요청 계약은 바뀌지 않는다.

## 지역 검색 응답

```http
GET /api/v1/regions/search?keyword=성성동
```

```json
{
  "code": "SUCCESS",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "searchResults": [
      {
        "regionId": "4413310500",
        "regionName": "충청남도 천안시 서북구 성성동",
        "latitude": 36.8358,
        "longitude": 127.1324
      }
    ]
  }
}
```

- `regionId`는 앞자리 `0`이 보존되어야 하므로 문자열로 유지한다.
- `latitude`, `longitude`는 숫자다. 카카오 주소 검색의 `y`를 `latitude`, `x`를 `longitude`로 변환한 값이다.
- 프론트엔드는 카카오 API를 직접 호출하거나 REST API 키를 노출하지 않는다.

## 적용 방법

1. 동 검색 결과를 선택할 때 `regionId`, `regionName`, `latitude`, `longitude`를 하나의 선택 지역 상태로 저장한다.
2. 가게 화면의 최초 조회와 지역 변경 후 조회 모두 선택 지역의 좌표로 호출한다.

   ```http
   GET /api/v1/stores/nearby?latitude=36.8358&longitude=127.1324
   ```

3. 기존 선택 지역에 `regionId`, `regionName`만 남아 있으면 `regionName`으로 지역 검색을 다시 호출한다. 응답 중 `regionId`가 같은 항목의 좌표만 사용한다.
4. 일치 항목이 없거나 지역 검색에 실패하면 주변 가게 API를 호출하지 않는다. 사용자에게 동네를 다시 선택하도록 안내한다.
5. 광진구 고정 좌표 또는 지도 기본 중심 좌표는 지도 표시용으로만 남길 수 있다. 어떤 경우에도 `/api/v1/stores/nearby` 요청의 대체 좌표로 사용하지 않는다.

## 프론트엔드 검증 항목

- 광진구가 아닌 법정동을 선택하면 해당 검색 결과의 좌표가 주변 가게 요청에 전달된다.
- 기존 선택 지역 상태는 `regionId`가 일치하는 검색 결과로 좌표를 복원한다.
- 좌표 복원 실패 시 주변 가게 요청을 보내지 않고 재선택 상태를 표시한다.
- `GET /api/v1/regions/search`의 새 좌표 필드가 없거나 `null`인 경우를 정상 값으로 처리하지 않는다.

## 범위 외

이번 백엔드 변경은 선택 지역 좌표를 제공하는 것까지다. 프론트엔드의 상태 저장, 초기 조회 호출, 화면 오류 상태 변경은 별도 프론트엔드 작업이 필요하다.
