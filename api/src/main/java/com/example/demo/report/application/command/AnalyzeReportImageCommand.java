package com.example.demo.report.application.command;

/**
 * 인식 요청 입력.
 *
 * @param imageUrl 업로드 API가 돌려준 영구 URL
 * @param itemId 사용자가 이미 품목을 골랐다면 그 ID. 없으면 {@code null}
 */
public record AnalyzeReportImageCommand(String imageUrl, Long itemId) {

    public boolean hasSelectedItem() {
        return itemId != null;
    }
}
