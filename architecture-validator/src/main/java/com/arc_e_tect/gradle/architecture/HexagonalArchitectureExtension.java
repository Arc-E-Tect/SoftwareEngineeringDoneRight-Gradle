package com.arc_e_tect.gradle.architecture;

import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

import javax.inject.Inject;
import java.util.List;

/**
 * Nested {@code hexagonalArchitecture {}} configuration block for the built-in hexagonal rule
 * pack, exposed via {@link ArchitectureValidatorExtension#getHexagonalArchitecture()}.
 *
 * <p>Every property here is a package-pattern list matched against classes under
 * {@link ArchitectureValidatorExtension#getBasePackage()}, each with a real default reflecting a
 * conventional hexagonal-architecture layout - override only the ones that don't fit.</p>
 */
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

    /**
     * Creates the extension, with every property already set to its default. Instantiated by
     * Gradle's extension-creation infrastructure.
     *
     * @param objects Gradle's object factory
     */
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

    /**
     * Inbound port package patterns.
     *
     * @return mutable list property of inbound port package patterns
     */
    public ListProperty<String> getInPorts() {
        return inPorts;
    }

    /**
     * Outbound port package patterns.
     *
     * @return mutable list property of outbound port package patterns
     */
    public ListProperty<String> getOutPorts() {
        return outPorts;
    }

    /**
     * Domain model package patterns.
     *
     * @return mutable list property of domain model package patterns
     */
    public ListProperty<String> getDomainModel() {
        return domainModel;
    }

    /**
     * Adapter package patterns, matching both inbound and outbound adapters not already covered by
     * {@link #getInboundAdapters()}/{@link #getOutboundAdapters()}.
     *
     * @return mutable list property of adapter package patterns
     */
    public ListProperty<String> getAdapters() {
        return adapters;
    }

    /**
     * Inbound adapter package patterns.
     *
     * @return mutable list property of inbound adapter package patterns
     */
    public ListProperty<String> getInboundAdapters() {
        return inboundAdapters;
    }

    /**
     * Outbound adapter package patterns.
     *
     * @return mutable list property of outbound adapter package patterns
     */
    public ListProperty<String> getOutboundAdapters() {
        return outboundAdapters;
    }

    /**
     * Application service package patterns.
     *
     * @return mutable list property of application service package patterns
     */
    public ListProperty<String> getApplicationServices() {
        return applicationServices;
    }

    /**
     * Shared/common package patterns, excluded from layer-boundary rules that would otherwise
     * flag code every layer is allowed to depend on.
     *
     * @return mutable list property of common package patterns
     */
    public ListProperty<String> getCommonPackages() {
        return commonPackages;
    }

    /**
     * Whether naming-convention rules (e.g. adapter classes ending in a conventional suffix) are
     * enabled, in addition to the layer-boundary rules.
     *
     * @return mutable property for the naming-conventions-enabled flag
     */
    public Property<Boolean> getNamingConventionsEnabled() {
        return namingConventionsEnabled;
    }
}
