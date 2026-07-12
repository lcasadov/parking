package com.aleatica.parking.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Verifica las reglas de arquitectura hexagonal de la capa de persistencia
 * (change hexagonal-persistence, #89). Se amplía por agregado a medida que se migran.
 *
 * <p>Reglas por agregado migrado:
 * <ul>
 *   <li>El dominio ({@code ..<agg>.domain..}) no depende de JPA/Hibernate/Spring Data.</li>
 *   <li>La aplicación ({@code ..<agg>.application..}) no depende de {@code JpaRepository}
 *       ni de las entidades JPA de infraestructura.</li>
 * </ul>
 */
class HexagonalArchitectureTest {

    private static final JavaClasses PRODUCTION_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.aleatica.parking");

    // Paquetes de framework de PERSISTENCIA prohibidos en dominio/aplicación.
    // Nota (decisión D4 de design.md): se permiten los value types de paginación
    // `org.springframework.data.domain.{Pageable,Page,Sort}` en la frontera; lo que
    // se prohíbe es el framework de repositorio/ORM (JpaRepository, CrudRepository…).
    private static final String[] PERSISTENCE_FRAMEWORK = {
            "jakarta.persistence..",
            "org.hibernate..",
            "org.springframework.data.repository..",
            "org.springframework.data.jpa..",
    };

    // ---- request (migrado) ----

    @Test
    void request_domain_is_free_of_persistence_framework() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..request.domain..")
                .should().dependOnClassesThat().resideInAnyPackage(PERSISTENCE_FRAMEWORK);
        rule.check(PRODUCTION_CLASSES);
    }

    @Test
    void request_application_does_not_depend_on_spring_data_or_jpa_entities() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..request.application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework.data.repository..",
                        "org.springframework.data.jpa..",
                        "..request.infrastructure..");
        rule.check(PRODUCTION_CLASSES);
    }

    // ---- release (migrado) ----

    @Test
    void release_domain_is_free_of_persistence_framework() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..release.domain..")
                .should().dependOnClassesThat().resideInAnyPackage(PERSISTENCE_FRAMEWORK);
        rule.check(PRODUCTION_CLASSES);
    }

    @Test
    void release_application_does_not_depend_on_spring_data_or_jpa_entities() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..release.application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework.data.repository..",
                        "org.springframework.data.jpa..",
                        "..release.infrastructure..");
        rule.check(PRODUCTION_CLASSES);
    }

    // ---- fixedassignment (migrado) ----

    @Test
    void fixedassignment_domain_is_free_of_persistence_framework() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..fixedassignment.domain..")
                .should().dependOnClassesThat().resideInAnyPackage(PERSISTENCE_FRAMEWORK);
        rule.check(PRODUCTION_CLASSES);
    }

    @Test
    void fixedassignment_application_does_not_depend_on_spring_data_or_jpa_entities() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..fixedassignment.application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework.data.repository..",
                        "org.springframework.data.jpa..",
                        "..fixedassignment.infrastructure..");
        rule.check(PRODUCTION_CLASSES);
    }

    // NOTA: al migrar cada agregado adicional (desk,
    // parkingspace, employee, visitor, auditlog, loginlog, emailoutbox) se añaden
    // aquí sus dos reglas equivalentes. Al completar los 11, sustituir por una
    // regla global sobre "..domain.." y "..application..".
}
