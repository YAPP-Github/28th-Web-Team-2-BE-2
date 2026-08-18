package com.example.demo.image.application.port;

/**
 * 저장된 이미지를 외부에서 한시적으로 읽을 수 있는 URL을 만든다.
 *
 * <p>버킷을 공개하지 않기 때문에 영구 URL만으로는 외부 모델이 이미지를 가져갈 수 없다. 짧은 만료
 * 읽기 URL을 따로 발급해 필요한 순간에만 접근을 허용한다.
 */
public interface ImageReadUrlPort {

    /**
     * @param imageUrl 업로드가 돌려준 영구 URL
     * @return 만료되는 읽기 전용 URL
     */
    String presignedReadUrl(String imageUrl);
}
