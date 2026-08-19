package com.example.demo.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
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

    /**
     * report 가 item 의 Entity·Repository 를 직접 다루는 전환 경계.
     *
     * <p>§7 은 상대 도메인의 Entity·Repository 를 직접 가져오지 말고 공개 유스케이스를 호출하라고
     * 한다. {@code ItemNameJpaRepository} 는 그 규칙을 어긴다 — 이름으로 품목을 찾는 공개
     * 유스케이스가 item 쪽에 아직 없다. §9 가 허용하는 예외로 기록하고, item 에 그 유스케이스가
     * 생기면 이 규칙과 repository 를 함께 지운다.
     */
    @ArchTest
    static final ArchRule only_item_name_repository_reaches_into_item_domain = noClasses()
            .that()
            .resideInAPackage("com.example.demo.report.infrastructure")
            .and()
            .haveSimpleNameNotEndingWith("ItemNameJpaRepository")
            .and()
            .haveSimpleNameNotEndingWith("ItemCandidateQueryAdapter")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("com.example.demo.item.domain")
            .because("§7: 다른 도메인의 Entity 를 직접 가져오지 않는다. 예외는 위 javadoc 참조");

    @ArchTest
    static final ArchRule item_controller_implements_spec = classes()
            .that()
            .haveSimpleName(ItemController.class.getSimpleName())
            .should()
            .implement(ItemControllerSpec.class);
}
