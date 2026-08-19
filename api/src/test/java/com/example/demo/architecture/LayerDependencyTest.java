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

    @ArchTest
    static final ArchRule item_controller_implements_spec = classes()
            .that()
            .haveSimpleName(ItemController.class.getSimpleName())
            .should()
            .implement(ItemControllerSpec.class);
}
