package com.example.demo.image.application.port;

import com.example.demo.image.application.command.UploadImageCommand;
import com.example.demo.image.domain.ImageKey;

/**
 * 이미지 저장소 출력 포트. S3라는 사실을 Application 밖으로 내보내지 않는다.
 *
 * <p>영구 URL 조립도 어댑터 책임으로 둔다. base URL은 배포 환경 설정값이고, Application이 그걸
 * 알면 저장소 교체가 어려워진다.
 */
public interface ImageStoragePort {

    /** 서버가 직접 PUT한다. 성공하면 제보에 저장할 영구 URL을 돌려준다. */
    String uploadAndReturnUrl(ImageKey key, UploadImageCommand command);

}
