package com.example.demo.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class UserRankTest {

    @ParameterizedTest(name = "제보 수 {0}건이면 {1} 등급이다")
    @CsvSource({
        "0, SPROUT",
        "1, ROOKIE",
        "4, ROOKIE",
        "5, EXPERT",
        "14, EXPERT",
        "15, KING",
        "16, KING"
    })
    void 제보_수_경계에_따라_등급을_계산한다(final long reportCount, final UserRank expected) {
        assertThat(UserRank.fromReportCount(reportCount)).isEqualTo(expected);
    }
}
