package com.example.demo.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.example.demo.price")
class LayerDependencyTest {

    @ArchTest
    static final ArchRule domain_does_not_depend_on_presentation_or_infrastructure = noClasses()
            .that().resideInAnyPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..presentation..", "..infrastructure..");

    @ArchTest
    static final ArchRule presentation_does_not_depend_on_infrastructure = noClasses()
            .that().resideInAnyPackage("..presentation..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..infrastructure..").allowEmptyShould(true);

    @ArchTest
    static final ArchRule application_does_not_depend_on_infrastructure = noClasses()
            .that().resideInAnyPackage("..application..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..infrastructure..");
}
