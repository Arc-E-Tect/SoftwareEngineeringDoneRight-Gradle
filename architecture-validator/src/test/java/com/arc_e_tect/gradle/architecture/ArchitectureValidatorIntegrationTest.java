package com.arc_e_tect.gradle.architecture;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ArchitectureValidatorIntegration")
class ArchitectureValidatorIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("should fail build when architecture tests violate rules")
    void shouldFailBuildWhenArchitectureViolationsExist() throws IOException {
        Path projectDir = createProjectWithFailingArchitectureTest("fail-on-violation", false);

        BuildResult result = createRunner(projectDir)
                .withArguments("testArchitecture", "--stacktrace")
                .buildAndFail();

        assertThat(result.getOutput()).contains("Architecture validation failed with");
        assertThat(result.getOutput()).contains("failing rule");
    }

    @Test
    @DisplayName("should report violations and pass when ignoreFailures is true")
    void shouldReportViolationsAndPassWhenIgnoreFailuresIsTrue() throws IOException {
        Path projectDir = createProjectWithFailingArchitectureTest("ignore-failures", true);

        BuildResult result = createRunner(projectDir)
                .withArguments("testArchitecture", "--stacktrace")
                .build();

        assertThat(result.task(":testArchitecture").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
        assertThat(result.getOutput()).contains("FAILED");

        Path xmlReportDir = projectDir.resolve("build/reports/architecture-validator/xml");
        assertThat(xmlReportDir).isDirectory();
        String mergedXml = readAllXml(xmlReportDir);
        assertThat(mergedXml).contains("failures=");
        assertThat(mergedXml).doesNotContain("failures=\"0\"");
    }

    @Test
    @DisplayName("should pass for empty hexagonal package structure")
    void shouldPassForEmptyHexagonalPackageStructure() throws IOException {
        Path projectDir = createProjectWithEmptyHexagonalPackageStructure("empty-hexagonal-packages");

        BuildResult result = createRunner(projectDir)
                .withArguments("testArchitecture", "--stacktrace")
                .build();

        assertThat(result.task(":testArchitecture").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
        assertThat(result.getOutput()).doesNotContain("outbound_ports_reside_in_correct_package FAILED");
    }

    @Test
    @DisplayName("should fail when port interfaces are outside configured port packages")
    void shouldFailWhenPortInterfacesAreOutsideConfiguredPortPackages() throws IOException {
        Path projectDir = createProjectWithMisplacedPortInterfaces("misplaced-port-interfaces");

        BuildResult result = createRunner(projectDir)
                .withArguments("testArchitecture", "--stacktrace")
                .buildAndFail();

        assertThat(result.getOutput()).contains("inbound_ports_reside_in_correct_package FAILED");
    }

    @Test
    @DisplayName("should allow adapters to implement outbound ports that use domain objects")
    void shouldAllowAdaptersToImplementOutboundPortsThatUseDomainObjects() throws IOException {
        Path projectDir = createProjectWithAdapterUsingDomainObjectsThroughPort("adapter-uses-domain-through-port");

        BuildResult result = createRunner(projectDir)
                .withArguments("testArchitecture", "--stacktrace")
                .build();

        assertThat(result.task(":testArchitecture").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
        assertThat(result.getOutput()).doesNotContain("FAILED");
    }

    @Test
    @DisplayName("should allow inbound ports to use command and query data types")
    void shouldAllowInboundPortsToUseCommandAndQueryDataTypes() throws IOException {
        Path projectDir = createProjectWithInboundPortUsingCommandAndQuery("inbound-port-uses-command-and-query");

        BuildResult result = createRunner(projectDir)
                .withArguments("testArchitecture", "--stacktrace")
                .build();

        assertThat(result.task(":testArchitecture").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
        assertThat(result.getOutput()).doesNotContain("inbound_ports_must_be_interfaces FAILED");
    }

    @Test
    @DisplayName("should forward generic and hexagonal rule pack properties to the architecture test JVM")
    void shouldForwardGenericAndHexagonalRulePackPropertiesToArchitectureTestJvm() throws IOException {
        Path projectDir = createProjectWithRulePackPropertyForwarding("rule-pack-property-forwarding");

        BuildResult result = createRunner(projectDir)
                .withArguments("testArchitecture", "--stacktrace")
                .build();

        assertThat(result.task(":testArchitecture").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
        assertThat(result.getOutput()).contains("FAILED");

        Path xmlReportDir = projectDir.resolve("build/reports/architecture-validator/xml");
        assertThat(xmlReportDir).isDirectory();
        String mergedXml = readAllXml(xmlReportDir);
        assertThat(mergedXml).contains("rulesDisabled=SomeClass.someMethod");
        assertThat(mergedXml).contains("namingConventionsEnabled=true");
    }

    @Test
    @DisplayName("should fail when inbound adapters depend on service implementations directly")
    void shouldFailWhenInboundAdaptersDependOnServiceImplementationsDirectly() throws IOException {
        Path projectDir = createProjectWithAdapterDependingOnServiceImplementation("adapter-depends-on-service");

        BuildResult result = createRunner(projectDir)
                .withArguments("testArchitecture", "--stacktrace")
                .buildAndFail();

        assertThat(result.getOutput()).contains("adapters_must_not_depend_on_domain_services_directly FAILED");
    }

    @Test
    @DisplayName("should fail when a domain model uses a source-retained annotation outside its allow-list")
    void shouldFailWhenDomainModelUsesSourceRetainedAnnotationOutsideAllowList() throws IOException {
        Path projectDir = createProjectWithSourceRetainedDomainAnnotation("source-retained-domain-annotation");

        BuildResult result = createRunner(projectDir)
                .withArguments("testArchitecture", "--stacktrace")
                .buildAndFail();

        assertThat(result.getOutput()).contains("source_annotation_dependencies_must_obey_core_allow_lists");
        String mergedXml = readAllXml(projectDir.resolve("build/reports/architecture-validator/xml"));
        assertThat(mergedXml)
                .contains("domain_must_only_depend_on_domain_or_jdk_core")
                .contains("lombok.Getter");
    }

    private Path createProjectWithFailingArchitectureTest(String projectName, boolean ignoreFailures) throws IOException {
        Path projectDir = tempDir.resolve(projectName);
        Files.createDirectories(projectDir);

        write(projectDir.resolve("settings.gradle"), """
                pluginManagement {
                    repositories {
                        gradlePluginPortal()
                    }
                }

                rootProject.name = '%s'
                """.formatted(projectName));

        write(projectDir.resolve("build.gradle"), """
                plugins {
                    id 'java'
                    id 'com.arc-e-tect.architecture-validator'
                }

                group = 'com.example.archtest'
                version = '0.0.1'

                repositories {
                    mavenCentral()
                }

                architectureValidator {
                    basePackage = 'com.example.archtest'
                    useBuiltInHexagonalRulePack = false
                    ignoreFailures = %s
                }
                """.formatted(ignoreFailures));

        write(projectDir.resolve("src/main/java/com/example/archtest/Dummy.java"), """
                package com.example.archtest;

                public class Dummy {
                }
                """);

        write(projectDir.resolve("src/testArchitecture/java/com/example/archtest/ManualArchitectureTest.java"), """
                package com.example.archtest;

                import org.junit.jupiter.api.Test;
                import static org.junit.jupiter.api.Assertions.fail;

                class ManualArchitectureTest {
                    @Test
                    void shouldFlagArchitectureViolation() {
                        fail("Intentional architecture rule violation for smoke test");
                    }
                }
                """);

        return projectDir;
    }

    private Path createProjectWithSourceRetainedDomainAnnotation(String projectName) throws IOException {
        Path projectDir = tempDir.resolve(projectName);
        Files.createDirectories(projectDir);

        write(projectDir.resolve("settings.gradle"), """
                pluginManagement {
                    repositories {
                        gradlePluginPortal()
                    }
                }

                rootProject.name = '%s'
                """.formatted(projectName));

        write(projectDir.resolve("build.gradle"), """
                plugins {
                    id 'java'
                    id 'com.arc-e-tect.architecture-validator'
                }

                group = 'com.example.archtest'
                version = '0.0.1'

                repositories {
                    mavenCentral()
                }

                architectureValidator {
                    basePackage = 'com.example.archtest'
                }
                """);

        write(projectDir.resolve("src/main/java/lombok/Getter.java"), """
                package lombok;

                import java.lang.annotation.ElementType;
                import java.lang.annotation.Retention;
                import java.lang.annotation.RetentionPolicy;
                import java.lang.annotation.Target;

                @Retention(RetentionPolicy.SOURCE)
                @Target(ElementType.TYPE)
                public @interface Getter {
                }
                """);

        write(projectDir.resolve("src/main/java/com/example/archtest/application/domain/model/Order.java"), """
                package com.example.archtest.application.domain.model;

                import lombok.Getter;

                @Getter
                public record Order(String id) {
                }
                """);

        return projectDir;
    }

    private Path createProjectWithEmptyHexagonalPackageStructure(String projectName) throws IOException {
        Path projectDir = tempDir.resolve(projectName);
        Files.createDirectories(projectDir);

        write(projectDir.resolve("settings.gradle"), """
                pluginManagement {
                    repositories {
                        gradlePluginPortal()
                    }
                }

                rootProject.name = '%s'
                """.formatted(projectName));

        write(projectDir.resolve("build.gradle"), """
                plugins {
                    id 'java'
                    id 'com.arc-e-tect.architecture-validator'
                }

                group = 'com.example.archtest'
                version = '0.0.1'

                repositories {
                    mavenCentral()
                }

                architectureValidator {
                    basePackage = 'com.example.archtest'
                    useBuiltInHexagonalRulePack = true
                    ignoreFailures = false
                    hexagonalArchitecture {
                        outboundAdapters = ['..adapter.outbound..']
                    }
                }
                """);

        Files.createDirectories(projectDir.resolve("src/main/java/com/example/archtest/application/port/inbound"));
        Files.createDirectories(projectDir.resolve("src/main/java/com/example/archtest/application/port/outbound"));
        Files.createDirectories(projectDir.resolve("src/main/java/com/example/archtest/application/domain"));
        Files.createDirectories(projectDir.resolve("src/main/java/com/example/archtest/application/service"));
        Files.createDirectories(projectDir.resolve("src/main/java/com/example/archtest/adapter/inbound"));
        Files.createDirectories(projectDir.resolve("src/main/java/com/example/archtest/adapter/outbound"));
        Files.createDirectories(projectDir.resolve("src/main/java/com/example/archtest/application/common"));

        return projectDir;
    }

    private Path createProjectWithInboundPortUsingCommandAndQuery(String projectName) throws IOException {
        Path projectDir = tempDir.resolve(projectName);
        Files.createDirectories(projectDir);

        write(projectDir.resolve("settings.gradle"), """
                pluginManagement {
                    repositories {
                        gradlePluginPortal()
                    }
                }

                rootProject.name = '%s'
                """.formatted(projectName));

        write(projectDir.resolve("build.gradle"), """
                plugins {
                    id 'java'
                    id 'com.arc-e-tect.architecture-validator'
                }

                group = 'com.example.archtest'
                version = '0.0.1'

                repositories {
                    mavenCentral()
                }

                architectureValidator {
                    basePackage = 'com.example.archtest'
                    hexagonalArchitecture {
                        inPorts = ['..application.port.in..']
                    }
                }
                """);

        write(projectDir.resolve("src/main/java/com/example/archtest/application/port/in/command/CreateOrderCommand.java"), """
                package com.example.archtest.application.port.in.command;

                public record CreateOrderCommand(String customerId) {
                }
                """);

        write(projectDir.resolve("src/main/java/com/example/archtest/application/port/in/query/FindOrderQuery.java"), """
                package com.example.archtest.application.port.in.query;

                public record FindOrderQuery(String orderId) {
                }
                """);

        write(projectDir.resolve("src/main/java/com/example/archtest/application/port/in/OrderUseCase.java"), """
                package com.example.archtest.application.port.in;

                import com.example.archtest.application.port.in.command.CreateOrderCommand;
                import com.example.archtest.application.port.in.query.FindOrderQuery;

                public interface OrderUseCase {
                    void create(CreateOrderCommand command);
                    String find(FindOrderQuery query);
                }
                """);

        return projectDir;
    }

    private Path createProjectWithRulePackPropertyForwarding(String projectName) throws IOException {
        Path projectDir = tempDir.resolve(projectName);
        Files.createDirectories(projectDir);

        write(projectDir.resolve("settings.gradle"), """
                pluginManagement {
                    repositories {
                        gradlePluginPortal()
                    }
                }

                rootProject.name = '%s'
                """.formatted(projectName));

        write(projectDir.resolve("build.gradle"), """
                plugins {
                    id 'java'
                    id 'com.arc-e-tect.architecture-validator'
                }

                group = 'com.example.archtest'
                version = '0.0.1'

                repositories {
                    mavenCentral()
                }

                architectureValidator {
                    basePackage = 'com.example.archtest'
                    ignoreFailures = true
                    failOnViolation = false
                    rulesDisabled = ['SomeClass.someMethod']
                    hexagonalArchitecture {
                        namingConventionsEnabled = true
                    }
                }
                """);

        write(projectDir.resolve("src/main/java/com/example/archtest/Dummy.java"), """
                package com.example.archtest;

                public class Dummy {
                }
                """);

        write(projectDir.resolve("src/testArchitecture/java/com/example/archtest/PropertyForwardingArchitectureTest.java"), """
                package com.example.archtest;

                import org.junit.jupiter.api.Test;

                import static org.junit.jupiter.api.Assertions.fail;

                class PropertyForwardingArchitectureTest {
                    @Test
                    void shouldExposeConfiguredProperties() {
                        String rulesDisabled = System.getProperty("architectureValidator.rules.disabled");
                        String namingConventionsEnabled = System.getProperty("architectureValidator.namingConventions.enabled");

                        if (!"SomeClass.someMethod".equals(rulesDisabled)
                                || !"true".equals(namingConventionsEnabled)) {
                            fail("rulesDisabled=" + rulesDisabled
                                    + "; namingConventionsEnabled=" + namingConventionsEnabled);
                        }

                        fail("rulesDisabled=" + rulesDisabled
                                + "; namingConventionsEnabled=" + namingConventionsEnabled);
                    }
                }
                """);

        return projectDir;
    }

    private Path createProjectWithMisplacedPortInterfaces(String projectName) throws IOException {
        Path projectDir = tempDir.resolve(projectName);
        Files.createDirectories(projectDir);

        write(projectDir.resolve("settings.gradle"), """
                pluginManagement {
                    repositories {
                        gradlePluginPortal()
                    }
                }

                rootProject.name = '%s'
                """.formatted(projectName));

        write(projectDir.resolve("build.gradle"), """
                plugins {
                    id 'java'
                    id 'com.arc-e-tect.architecture-validator'
                }

                group = 'com.example.archtest'
                version = '0.0.1'

                repositories {
                    mavenCentral()
                }

                architectureValidator {
                    basePackage = 'com.example.archtest'
                    useBuiltInHexagonalRulePack = true
                    ignoreFailures = false
                }
                """);

        write(projectDir.resolve("src/main/java/com/example/archtest/application/CreateOrderUseCase.java"), """
                package com.example.archtest.application;

                public interface CreateOrderUseCase {
                    void execute();
                }
                """);

        write(projectDir.resolve("src/main/java/com/example/archtest/application/OrderRepository.java"), """
                package com.example.archtest.application;

                public interface OrderRepository {
                    void save(String orderId);
                }
                """);

        return projectDir;
    }

    private Path createProjectWithAdapterUsingDomainObjectsThroughPort(String projectName) throws IOException {
        Path projectDir = tempDir.resolve(projectName);
        Files.createDirectories(projectDir);

        write(projectDir.resolve("settings.gradle"), """
                pluginManagement {
                    repositories {
                        gradlePluginPortal()
                    }
                }

                rootProject.name = '%s'
                """.formatted(projectName));

        write(projectDir.resolve("build.gradle"), """
                plugins {
                    id 'java'
                    id 'com.arc-e-tect.architecture-validator'
                }

                group = 'com.example.archtest'
                version = '0.0.1'

                repositories {
                    mavenCentral()
                }

                architectureValidator {
                    basePackage = 'com.example.archtest'
                    useBuiltInHexagonalRulePack = true
                    ignoreFailures = false
                }
                """);

        write(projectDir.resolve("src/main/java/com/example/archtest/application/domain/model/Order.java"), """
                package com.example.archtest.application.domain.model;

                public record Order(String id) {
                }
                """);

        write(projectDir.resolve("src/main/java/com/example/archtest/application/port/outbound/OrderRepository.java"), """
            package com.example.archtest.application.port.outbound;

                import com.example.archtest.application.domain.model.Order;

                public interface OrderRepository {
                    void save(Order order);
                }
                """);

        write(projectDir.resolve("src/main/java/com/example/archtest/application/service/OrderApplicationService.java"), """
                package com.example.archtest.application.service;

                import com.example.archtest.application.domain.model.Order;
                import com.example.archtest.application.port.outbound.OrderRepository;

                public class OrderApplicationService {
                    private final OrderRepository orderRepository;

                    public OrderApplicationService(OrderRepository orderRepository) {
                        this.orderRepository = orderRepository;
                    }

                    public void createOrder(String id) {
                        orderRepository.save(new Order(id));
                    }
                }
                """);

        write(projectDir.resolve("src/main/java/com/example/archtest/adapter/outbound/persistence/DatabaseAdapter.java"), """
                package com.example.archtest.adapter.outbound.persistence;

                import com.example.archtest.application.domain.model.Order;
                import com.example.archtest.application.port.outbound.OrderRepository;

                public class DatabaseAdapter implements OrderRepository {
                    @Override
                    public void save(Order order) {
                        // adapter-specific persistence mapping
                    }
                }
                """);

        return projectDir;
    }

    private Path createProjectWithAdapterDependingOnServiceImplementation(String projectName) throws IOException {
        Path projectDir = tempDir.resolve(projectName);
        Files.createDirectories(projectDir);

        write(projectDir.resolve("settings.gradle"), """
                pluginManagement {
                    repositories {
                        gradlePluginPortal()
                    }
                }

                rootProject.name = '%s'
                """.formatted(projectName));

        write(projectDir.resolve("build.gradle"), """
                plugins {
                    id 'java'
                    id 'com.arc-e-tect.architecture-validator'
                }

                group = 'com.example.archtest'
                version = '0.0.1'

                repositories {
                    mavenCentral()
                }

                architectureValidator {
                    basePackage = 'com.example.archtest'
                    useBuiltInHexagonalRulePack = true
                    ignoreFailures = false
                    hexagonalArchitecture {
                        inboundAdapters = ['..adapter.inbound..']
                    }
                }
                """);

        write(projectDir.resolve("src/main/java/com/example/archtest/application/domain/service/OrderDomainService.java"), """
                package com.example.archtest.application.domain.service;

                public class OrderDomainService {
                    public String loadOrder(String id) {
                        return id;
                    }
                }
                """);

        write(projectDir.resolve("src/main/java/com/example/archtest/adapter/inbound/web/OrderController.java"), """
                package com.example.archtest.adapter.inbound.web;

                import com.example.archtest.application.domain.service.OrderDomainService;

                public class OrderController {
                    private final OrderDomainService orderDomainService = new OrderDomainService();

                    public String getOrder(String id) {
                        return orderDomainService.loadOrder(id);
                    }
                }
                """);

        return projectDir;
    }

    private GradleRunner createRunner(Path projectDir) {
        return GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .forwardOutput()
                .withArguments("--no-daemon", "--max-workers=1");
    }

    private String readAllXml(Path xmlReportDir) throws IOException {
        try (Stream<Path> files = Files.walk(xmlReportDir)) {
            return files
                    .filter(path -> path.getFileName().toString().endsWith(".xml"))
                    .map(path -> {
                        try {
                            return Files.readString(path, StandardCharsets.UTF_8);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .reduce("", (left, right) -> left + "\n" + right);
        }
    }

    private void write(Path file, String contents) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, contents, StandardCharsets.UTF_8);
    }
}