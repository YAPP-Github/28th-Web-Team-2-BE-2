package com.example.demo.store.infrastructure;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;

class S3ImageStorageAdapterTest {

    @Test
    void S3_설정이_없으면_업로드를_실행하지_않고_실패한다() throws Exception {
        final S3ImageStorageAdapter adapter = new S3ImageStorageAdapter(mock(S3Client.class));
        set(adapter, "bucket", "");
        set(adapter, "baseUrl", "");

        assertThatThrownBy(() -> adapter.upload(new byte[] {1}, "image/png", "png"))
                .isInstanceOf(IllegalStateException.class);
    }

    private void set(final Object target, final String name, final String value) throws Exception {
        final Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
