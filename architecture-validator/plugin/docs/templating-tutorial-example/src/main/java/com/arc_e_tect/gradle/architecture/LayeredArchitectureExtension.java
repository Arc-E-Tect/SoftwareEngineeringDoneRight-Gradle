package com.arc_e_tect.gradle.architecture;

import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;

import javax.inject.Inject;
import java.util.List;

public class LayeredArchitectureExtension {

    private final ListProperty<String> presentation;
    private final ListProperty<String> application;
    private final ListProperty<String> domain;
    private final ListProperty<String> infrastructure;

    @Inject
    public LayeredArchitectureExtension(ObjectFactory objects) {
        presentation = objects.listProperty(String.class).convention(List.of("..web..", "..api.."));
        application = objects.listProperty(String.class).convention(List.of("..application.."));
        domain = objects.listProperty(String.class).convention(List.of("..domain.."));
        infrastructure = objects.listProperty(String.class).convention(List.of("..infrastructure..", "..persistence.."));
    }

    public ListProperty<String> getPresentation() {
        return presentation;
    }

    public ListProperty<String> getApplication() {
        return application;
    }

    public ListProperty<String> getDomain() {
        return domain;
    }

    public ListProperty<String> getInfrastructure() {
        return infrastructure;
    }
}