package com.arc_e_tect.gradle.architecture;

import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

import javax.inject.Inject;
import java.util.List;

public class HexagonalArchitectureExtension {

    private final ListProperty<String> inPorts;
    private final ListProperty<String> outPorts;
    private final ListProperty<String> domainModel;
    private final ListProperty<String> adapters;
    private final ListProperty<String> inboundAdapters;
    private final ListProperty<String> outboundAdapters;
    private final ListProperty<String> applicationServices;
    private final ListProperty<String> commonPackages;
    private final Property<Boolean> namingConventionsEnabled;

    @Inject
    public HexagonalArchitectureExtension(ObjectFactory objects) {
        inPorts = objects.listProperty(String.class).convention(List.of("..application.port.inbound.."));
        outPorts = objects.listProperty(String.class).convention(List.of("..application.port.outbound.."));
        domainModel = objects.listProperty(String.class).convention(List.of("..application.domain.."));
        adapters = objects.listProperty(String.class).convention(List.of("..adapter..", "..adapters.."));
        inboundAdapters = objects.listProperty(String.class).convention(List.of("..adapter.inbound..", "..adapters.inbound.."));
        outboundAdapters = objects.listProperty(String.class).convention(List.of("..adapter.outbound..", "..adapters.outbound.."));
        applicationServices = objects.listProperty(String.class)
                .convention(List.of("..application.domain.service..", "..application.service.."));
        commonPackages = objects.listProperty(String.class).convention(List.of("..application.common.."));
        namingConventionsEnabled = objects.property(Boolean.class).convention(false);
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

    public ListProperty<String> getInboundAdapters() {
        return inboundAdapters;
    }

    public ListProperty<String> getOutboundAdapters() {
        return outboundAdapters;
    }

    public ListProperty<String> getApplicationServices() {
        return applicationServices;
    }

    public ListProperty<String> getCommonPackages() {
        return commonPackages;
    }

    public Property<Boolean> getNamingConventionsEnabled() {
        return namingConventionsEnabled;
    }
}