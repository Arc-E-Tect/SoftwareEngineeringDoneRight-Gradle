package com.arc_e_tect.gradle.architecture;

import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;

import javax.inject.Inject;
import java.util.List;

public class LayeredArchitectureExtension {

    private final ListProperty<String> presentation;
    private final ListProperty<String> applicationServices;
    private final ListProperty<String> domain;
    private final ListProperty<String> infrastructure;

    @Inject
    public LayeredArchitectureExtension(ObjectFactory objects) {
        presentation = objects.listProperty(String.class).convention(List.of("..web..", "..api.."));
        // Unlike the built-in hexagonal template - which folds application and domain
        // services into a single `domainServices` concept - a layered architecture keeps
        // Application Services as its own rule-governed layer. See LayeredArchitectureTest.java.template.
        applicationServices = objects.listProperty(String.class).convention(List.of("..application.."));
        domain = objects.listProperty(String.class).convention(List.of("..domain.."));
        infrastructure = objects.listProperty(String.class).convention(List.of("..infrastructure..", "..persistence.."));
    }

    public ListProperty<String> getPresentation() {
        return presentation;
    }

    public ListProperty<String> getApplicationServices() {
        return applicationServices;
    }

    public ListProperty<String> getDomain() {
        return domain;
    }

    public ListProperty<String> getInfrastructure() {
        return infrastructure;
    }
}