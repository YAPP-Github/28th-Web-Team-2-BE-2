package com.example.demo.image.application.port;

/**
 * 우리 저장소가 발급한 이미지 URL인지 확인한다.
 *
 * <p>{@code images/} 접두사는 공개 읽기이므로 외부에 넘길 URL을 따로 서명할 필요가 없다. 다만
 * 검증은 남는다 — 임의 URL을 그대로 외부 모델에 넘기면 사용자가 우리 비용으로 아무 호스트나
 * 가져오게 만들 수 있다.
 */
public interface ImageUrlPort {

    /**
     * @param imageUrl 업로드가 돌려준 영구 URL
     * @return 같은 URL. 우리 저장소의 것이 아니거나 key 형식을 벗어나면 거부한다.
     */
    String requireOwnedUrl(String imageUrl);
}
