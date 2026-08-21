package com.example.demo.user.domain;

public enum UserRank {
    SPROUT,
    ROOKIE,
    EXPERT,
    KING;

    public static UserRank fromReportCount(final long reportCount) {
        if (reportCount >= 15) {
            return KING;
        }
        if (reportCount >= 5) {
            return EXPERT;
        }
        if (reportCount >= 1) {
            return ROOKIE;
        }
        return SPROUT;
    }
}
