package com.example.demo.report.application.port;

import com.example.demo.report.application.contract.ExtractedPriceTag;

/**
 * 사진에서 가격표 정보를 읽는 출력 포트.
 *
 * <p>Qwen이라는 사실을 Application 밖으로 내보내지 않는다. 모델 교체가 어댑터 교체로 끝난다.
 */
public interface ImageAnalysisPort {

    /**
     * @param imageUrl 저장된 이미지의 영구 URL. 외부 모델이 읽을 수 있게 만드는 책임은 어댑터에 있다.
     */
    ExtractedPriceTag analyze(String imageUrl);
}
