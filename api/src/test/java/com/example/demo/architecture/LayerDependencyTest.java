package com.example.demo.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideOutsideOfPackages;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.example.demo.auth.presentation.AuthController;
import com.example.demo.auth.presentation.spec.AuthControllerSpec;
import com.example.demo.image.presentation.ImageController;
import com.example.demo.image.presentation.spec.ImageControllerSpec;
import com.example.demo.item.presentation.ItemController;
import com.example.demo.item.presentation.spec.ItemControllerSpec;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
        packages = "com.example.demo",
        importOptions = ImportOption.DoNotIncludeTests.class)
class LayerDependencyTest {

    @ArchTest
    static final ArchRule domain_does_not_depend_on_outer_layers = noClasses()
            .that()
            .resideInAnyPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..presentation..", "..application..", "..infrastructure..");

    @ArchTest
    static final ArchRule application_does_not_depend_on_infrastructure = noClasses()
            .that()
            .resideInAnyPackage("..application..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..infrastructure..");

    @ArchTest
    static final ArchRule core_does_not_depend_on_selenium = noClasses()
            .that()
            .resideInAnyPackage("..application..", "..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.openqa.selenium..");

    @ArchTest
    static final ArchRule presentation_does_not_depend_on_infrastructure = noClasses()
            .that()
            .resideInAnyPackage("..presentation..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..infrastructure..");

    @ArchTest
    static final ArchRule controllers_do_not_depend_on_openapi = noClasses()
            .that()
            .haveSimpleNameEndingWith("Controller")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("io.swagger.v3..", "org.springdoc..");

    @ArchTest
    static final ArchRule auth_controller_implements_spec = classes()
            .that()
            .haveSimpleName(AuthController.class.getSimpleName())
            .should()
            .implement(AuthControllerSpec.class);

    @ArchTest
    static final ArchRule image_controller_implements_spec = classes()
            .that()
            .haveSimpleName(ImageController.class.getSimpleName())
            .should()
            .implement(ImageControllerSpec.class);

    @ArchTest
    static final ArchRule domain_does_not_depend_on_http_status = noClasses()
            .that()
            .resideInAnyPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework.http..")
            .because("ARCHITECTURE §8: Domain 은 HTTP 상태 코드에 의존하지 않는다");

    @ArchTest
    static final ArchRule item_controller_implements_spec = classes()
            .that()
            .haveSimpleName(ItemController.class.getSimpleName())
            .should()
            .implement(ItemControllerSpec.class);

    /**
     * {@code report} Application 이 다른 도메인의 Application 포트를 직접 참조하는 것을 허용한 예외다.
     *
     * <p>ARCHITECTURE §7 은 "사용하는 쪽 Application 이 포트와 Contract 를 소유한다"이므로 이 참조는 위반이다. §9 가
     * 전환 상태를 허용하되 "정확한 패키지, 이유, 제거 조건을 ArchUnit 규칙에 예외로 기록한다"를 요구하므로 여기 남긴다.
     *
     * <p><b>참조 지점</b>
     *
     * <ul>
     *   <li>{@code CreateUserReportUseCase} → {@code ItemExistencePort}, {@code PublicPriceQueryPort}
     *   <li>{@code GetRegionItemReportQueryUseCase} → {@code ItemExistencePort},
     *       {@code RegionReferenceRepository}
     *   <li>{@code GetMyReportQueryUseCase} → 위 둘
     *   <li>{@code GetMyWeeklyReportQueryUseCase} → {@code ItemExistencePort}
     * </ul>
     *
     * <p><b>이유</b> §7 정석대로 하면 report 쪽에 포트 + 어댑터를 도메인마다 만들어야 하고, 구현체가 하나뿐인 인터페이스가
     * 늘어난다(§9 가 만들지 말라고 한 형태다). 참조 지점이 4곳이라 한 번에 정리하는 편이 싸다.
     *
     * <p><b>제거 조건</b> report 도메인이 자기 포트와 어댑터를 갖게 되면 이 규칙을 지우고 금지로 바꾼다. 후속 이슈 #193.
     */
    @ArchTest
    static final ArchRule report_application_only_reaches_allowed_cross_domain_ports = noClasses()
            .that()
            .resideInAnyPackage("..report.application..")
            .should()
            .dependOnClassesThat(
                    resideInAnyPackage("..item..", "..user..", "..store..", "..region..")
                            .and(resideOutsideOfPackages(
                                    "..item.application.port..",
                                    "..item.domain..",
                                    "..user.application.port..")))
            .because("ARCHITECTURE §9: 기록된 전환 상태만 허용한다. 제거 조건은 위 javadoc 참고");
}
