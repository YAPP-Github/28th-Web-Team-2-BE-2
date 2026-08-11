package com.example.demo.auth.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RefreshTokenHasherTest {

    @Test
    void Refresh_Token은_SHA256_hex로_해시한다() {
        assertThat(new RefreshTokenHasher().hash("refresh-token"))
                .isEqualTo("0eb17643d4e9261163783a420859c92c7d212fa9624106a12b510afbec266120");
    }
}
