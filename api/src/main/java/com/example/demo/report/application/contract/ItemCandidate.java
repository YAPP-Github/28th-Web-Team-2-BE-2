package com.example.demo.report.application.contract;

/**
 * 인식된 품목명에 대응하는 품목 후보.
 *
 * <p>{@code item} 도메인의 Entity를 그대로 가져오지 않는다({@code docs/ARCHITECTURE.md} §7).
 * report가 필요한 세 값만 가진 Contract를 report가 소유하고, Adapter가 변환해 채운다.
 *
 * <p>{@code defaultUnit}을 함께 싣는 이유가 이 기능의 핵심 제약이다. 제보 저장 API는
 * {@code unit}이 {@code items.default_unit}과 문자열까지 같아야 통과시킨다
 * ({@code CreateUserReportUseCase.validateUnit}). 즉 단위는 모델이 추측할 값이 아니라
 * 품목이 결정하는 값이다.
 */
public record ItemCandidate(Long itemId, String name, String defaultUnit) {}
