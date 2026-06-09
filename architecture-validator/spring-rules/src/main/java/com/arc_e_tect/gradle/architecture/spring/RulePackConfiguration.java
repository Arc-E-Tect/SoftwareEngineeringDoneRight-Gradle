package com.arc_e_tect.gradle.architecture.spring;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class RulePackConfiguration {

    private RulePackConfiguration() {
    }

    static String basePackage() {
        return property("architectureValidator.basePackage", "");
    }

    static String[] inPorts() {
        return packages("architectureValidator.inPorts");
    }

    static String[] outPorts() {
        return packages("architectureValidator.outPorts");
    }

    static String[] domainModel() {
        return packages("architectureValidator.domainModel");
    }

    static String[] adapters() {
        return packages("architectureValidator.adapters");
    }

    static String[] applicationServices() {
        return packages("architectureValidator.applicationServices");
    }

    static String[] merge(String[] dynamicPackages, String... fixedPackages) {
        return Stream.concat(Arrays.stream(fixedPackages), Arrays.stream(dynamicPackages))
                .toArray(String[]::new);
    }

    private static String property(String key, String defaultValue) {
        return System.getProperty(key, defaultValue);
    }

    private static String[] packages(String key) {
        List<String> values = Arrays.stream(property(key, "").split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toList());
        return values.toArray(new String[0]);
    }
}