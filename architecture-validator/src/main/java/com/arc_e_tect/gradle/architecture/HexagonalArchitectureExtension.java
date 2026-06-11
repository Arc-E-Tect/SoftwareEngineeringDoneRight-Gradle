package com.arc_e_tect.gradle.architecture;

import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;

import javax.inject.Inject;
import java.util.List;

public class HexagonalArchitectureExtension {

    private final ListProperty<String> inPorts;
    private final ListProperty<String> outPorts;
    private final ListProperty<String> domainModel;
    private final ListProperty<String> adapters;
    private final ListProperty<String> applicationServices;
    private final ListProperty<String> commonPackages;

    @Inject
    public HexagonalArchitectureExtension(ObjectFactory objects) {
        inPorts = objects.listProperty(String.class).convention(List.of("..application.port.in.."));
        outPorts = objects.listProperty(String.class).convention(List.of("..application.port.out.."));
        domainModel = objects.listProperty(String.class).convention(List.of("..application.domain.."));
        adapters = objects.listProperty(String.class).convention(List.of("..adapter..", "..adapters.."));
        applicationServices = objects.listProperty(String.class)
                .convention(List.of("..application.domain.service..", "..application.service.."));
        commonPackages = objects.listProperty(String.class).convention(List.of("..application.common.."));
    }

    public ListProperty<String> getInPorts() {
        return inPorts;
    }

    public ListProperty<String> getOutPorts() {
        return outPorts;
    }

    public ListProperty<String> getDomainModel() {
        return domainModel;
    }

    public ListProperty<String> getAdapters() {
        return adapters;
    }

    public ListProperty<String> getApplicationServices() {
        return applicationServices;
    }

    public ListProperty<String> getCommonPackages() {
        return commonPackages;
    }
}