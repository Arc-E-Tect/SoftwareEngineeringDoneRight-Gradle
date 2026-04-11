package com.arc_e_tect.utils.sedr.jacoco.marker;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.CONSTRUCTOR, TYPE, METHOD})
public @interface ExcludeFromJacocoGeneratedCodeCoverage {
    /**
     * Optional human-readable justification for why this element is excluded
     * from JaCoCo code-coverage measurement.
     * <p>Examples:
     * <pre>
     *   {@literal @}ExcludeFromJacocoGeneratedCodeCoverage("Spring Boot entry point – not unit-testable")
     *   {@literal @}ExcludeFromJacocoGeneratedCodeCoverage(justification = "Lombok-generated boilerplate")
     * </pre>
     */
    String justification() default "";
}
