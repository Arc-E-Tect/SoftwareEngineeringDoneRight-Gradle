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
    private final ListProperty<String> domainServices;
    private final ListProperty<String> adapters;
    private final ListProperty<String> inboundAdapters;
    private final ListProperty<String> outboundAdapters;
    private final ListProperty<String> configurationPackages;
    private final ListProperty<String> portDataTypePackages;
    private final ListProperty<String> commonPackages;
    private final ListProperty<String> domainAllowedPackages;
    private final ListProperty<String> frameworkDenylistPackages;
    private final ListProperty<String> inboundAdapterDenylistPackages;
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
        domainModel = objects.listProperty(String.class).convention(List.of("..application.domain.model.."));
        domainServices = objects.listProperty(String.class).convention(List.of("..application.domain.service.."));
        adapters = objects.listProperty(String.class).convention(List.of("..adapter..", "..adapters.."));
        inboundAdapters = objects.listProperty(String.class).convention(List.of("..adapter.inbound..", "..adapters.inbound.."));
        outboundAdapters = objects.listProperty(String.class).convention(List.of("..adapter.outbound..", "..adapters.outbound.."));
        configurationPackages = objects.listProperty(String.class).convention(List.of("..configuration.."));
        portDataTypePackages = objects.listProperty(String.class)
                .convention(List.of("..command..", "..query..", "..result.."));
        commonPackages = objects.listProperty(String.class).convention(List.of("..application.common.."));
        domainAllowedPackages = objects.listProperty(String.class)
                .convention(List.of("java.lang..", "java.time..", "java.util..", "java.math.."));
        frameworkDenylistPackages = objects.listProperty(String.class).convention(List.of());
        inboundAdapterDenylistPackages = objects.listProperty(String.class).convention(List.of());
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
     * Domain model package patterns - the innermost ring, holding pure value objects/entities.
     * Kept disjoint from {@link #getDomainServices()} by default (siblings under
     * {@code application.domain}, not one nested inside the other) so the domain-model-isolation
     * and domain-service-isolation rules never both match the same class.
     *
     * @return mutable list property of domain model package patterns
     */
    public ListProperty<String> getDomainModel() {
        return domainModel;
    }

    /**
     * Domain service package patterns - the layer that implements use-case orchestration and
     * cross-aggregate business rules on top of the domain model. Distinct from
     * {@link #getDomainModel()} (siblings, not nested) so the two isolation rules stay mutually
     * exclusive.
     *
     * @return mutable list property of domain service package patterns
     */
    public ListProperty<String> getDomainServices() {
        return domainServices;
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
     * Shared/common package patterns, excluded from layer-boundary rules that would otherwise
     * flag code every layer is allowed to depend on.
     *
     * @return mutable list property of common package patterns
     */
    public ListProperty<String> getCommonPackages() {
        return commonPackages;
    }

    /**
     * Package patterns nested inside a port package that hold pure data-transfer types
     * (commands, queries, results) rather than the port interfaces themselves - excluded from the
     * "ports must be interfaces" and port-naming-convention rules, which apply only to the port
     * contracts.
     *
     * @return mutable list property of port data-type package patterns
     */
    public ListProperty<String> getPortDataTypePackages() {
        return portDataTypePackages;
    }

    /**
     * Package patterns holding Spring (or other DI-framework) wiring/configuration classes. Only
     * classes in these packages - plus domain service classes themselves - may reference domain
     * service implementations directly; every other class must go through a port instead.
     *
     * @return mutable list property of configuration package patterns
     */
    public ListProperty<String> getConfigurationPackages() {
        return configurationPackages;
    }

    /**
     * JDK package patterns the domain model is allowed to depend on, in addition to its own
     * {@link #getDomainModel()} packages. Enforced by the {@code domain_must_only_depend_on_domain_or_jdk_core}
     * rule, which requires every non-JDK dependency of a domain class to resolve back into
     * {@link #getDomainModel()} - keeping the domain layer free of adapters, application
     * services, ports, and any third-party or framework dependency by construction, rather than by
     * naming a growing list of things it must avoid.
     *
     * @return mutable list property of JDK package patterns allowed from the domain model
     */
    public ListProperty<String> getDomainAllowedPackages() {
        return domainAllowedPackages;
    }

    /**
     * Framework/library package patterns the domain model, domain services, and ports must never
     * depend on (directly or transitively), enforced by
     * {@code core_application_layer_must_have_no_denylisted_dependencies}. Empty by default so
     * the rule is a no-op until a project opts in with its own framework list (e.g.
     * {@code 'org.springframework..', 'jakarta..', 'org.hibernate..'}).
     *
     * @return mutable list property of denylisted framework package patterns
     */
    public ListProperty<String> getFrameworkDenylistPackages() {
        return frameworkDenylistPackages;
    }

    /**
     * Additional package patterns inbound adapters must never depend on. The built-in
     * {@code inbound_adapters_must_access_application_through_inbound_ports} rule always blocks
     * direct dependencies on the domain model, outbound ports, outbound adapters, and
     * configuration; this list lets a project extend that deny-list for its own libraries.
     *
     * @return mutable list property of additional inbound-adapter denylisted package patterns
     */
    public ListProperty<String> getInboundAdapterDenylistPackages() {
        return inboundAdapterDenylistPackages;
    }

    /**
     * Whether naming-convention rules (bidirectional suffix/package checks, e.g. inbound port
     * interfaces must end in {@code UseCase}/{@code InputPort} and vice versa) are enabled, in
     * addition to the layer-boundary rules.
     *
     * @return mutable property for the naming-conventions-enabled flag
     */
    public Property<Boolean> getNamingConventionsEnabled() {
        return namingConventionsEnabled;
    }
}
