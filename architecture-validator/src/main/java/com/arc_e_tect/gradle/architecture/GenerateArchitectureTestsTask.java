package com.arc_e_tect.gradle.architecture;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePathScanner;
import com.tngtech.archunit.core.domain.PackageMatcher;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import javax.inject.Inject;
import com.arc_e_tect.sedr.utils.jacoco.marker.ExcludeFromJacocoGeneratedCodeCoverage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/**
 * Generates the built-in hexagonal architecture test, and a bridge suite for any external rule
 * pack tests discovered on {@link #getRulePackClasspath()}, under {@link #getOutputDirectory()}.
 */
@DisableCachingByDefault(because = "Generates test sources from extension configuration")
public abstract class GenerateArchitectureTestsTask extends DefaultTask {

    private static final String TEMPLATE_PATH = "templates/HexagonalArchitectureTest.java.template";
    private static final String GENERATED_PACKAGE = "com.arc_e_tect.gradle.architecture.generated";
    private static final String EXTERNAL_SUITE_CLASS_NAME = "ExternalRulePackSuite";
    private static final String SOURCE_DEPENDENCY_TEST_CLASS_NAME = "SourceDependencyValidationTest";
    private static final Pattern PACKAGE_SEGMENT = Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*");

    /** Creates a new task instance. Instantiated by Gradle infrastructure. */
    @Inject
    public GenerateArchitectureTestsTask() {
        setGroup("verification");
        setDescription("Generates the built-in hexagonal architecture tests.");
        getUseBuiltInHexagonalRulePack().convention(true);
        getFallbackBasePackage().convention("");
        getAdapters().convention(List.of("..adapter..", "..adapters.."));
        getInboundAdapters().convention(List.of("..adapter.inbound..", "..adapters.inbound.."));
        getOutboundAdapters().convention(List.of("..adapter.outbound..", "..adapters.outbound.."));
        getConfigurationPackages().convention(List.of("..configuration.."));
        getPortDataTypePackages().convention(List.of("..command..", "..query..", "..result.."));
        getDomainAllowedPackages().convention(List.of("java.lang..", "java.time..", "java.util..", "java.math.."));
        getFrameworkDenylistPackages().convention(List.of());
        getNamingConventionsEnabled().convention(false);
    }

    /**
     * The explicitly configured base package, i.e. {@code architectureValidator.basePackage}.
     * Takes priority over both an inferred package (from {@link #getMainSourceDirectory()}'s own
     * layout) and {@link #getFallbackBasePackage()} when non-blank.
     *
     * @return mutable property for the configured base package
     */
    @Input
    public abstract Property<String> getBasePackage();

    /**
     * Base package used only when {@link #getBasePackage()} is blank and no package could be
     * inferred from {@link #getMainSourceDirectory()}'s own layout - the project's own
     * {@code group}, resolved by the plugin at apply time.
     *
     * @return mutable property for the fallback base package
     */
    @Input
    public abstract Property<String> getFallbackBasePackage();

    /**
     * Inbound port package patterns.
     *
     * @return mutable list property of inbound port package patterns
     */
    @Input
    public abstract ListProperty<String> getInPorts();

    /**
     * Outbound port package patterns.
     *
     * @return mutable list property of outbound port package patterns
     */
    @Input
    public abstract ListProperty<String> getOutPorts();

    /**
     * Domain model package patterns.
     *
     * @return mutable list property of domain model package patterns
     */
    @Input
    public abstract ListProperty<String> getDomainModel();

    /**
     * Domain service package patterns.
     *
     * @return mutable list property of domain service package patterns
     */
    @Input
    public abstract ListProperty<String> getDomainServices();

    /**
     * Adapter package patterns, matching both inbound and outbound adapters not already covered by
     * {@link #getInboundAdapters()}/{@link #getOutboundAdapters()}.
     *
     * @return mutable list property of adapter package patterns
     */
    @Input
    public abstract ListProperty<String> getAdapters();

    /**
     * Inbound adapter package patterns.
     *
     * @return mutable list property of inbound adapter package patterns
     */
    @Input
    public abstract ListProperty<String> getInboundAdapters();

    /**
     * Outbound adapter package patterns.
     *
     * @return mutable list property of outbound adapter package patterns
     */
    @Input
    public abstract ListProperty<String> getOutboundAdapters();

    /**
     * Shared/common package patterns, excluded from layer-boundary rules.
     *
     * @return mutable list property of common package patterns
     */
    @Input
    public abstract ListProperty<String> getCommonPackages();

    /**
     * Package patterns nested inside a port package that hold pure data-transfer types.
     *
     * @return mutable list property of port data-type package patterns
     */
    @Input
    public abstract ListProperty<String> getPortDataTypePackages();

    /**
     * Package patterns holding DI-framework wiring/configuration classes.
     *
     * @return mutable list property of configuration package patterns
     */
    @Input
    public abstract ListProperty<String> getConfigurationPackages();

    /**
     * JDK package patterns the domain model is allowed to depend on, in addition to its own
     * {@link #getDomainModel()} packages.
     *
     * @return mutable list property of JDK package patterns allowed from the domain model
     */
    @Input
    public abstract ListProperty<String> getDomainAllowedPackages();

    /**
     * Framework/library package patterns the domain model, domain services, and ports must never
     * depend on.
     *
     * @return mutable list property of denylisted framework package patterns
     */
    @Input
    public abstract ListProperty<String> getFrameworkDenylistPackages();

    /**
     * Whether bidirectional naming-convention rules are generated in addition to the
     * layer-boundary rules.
     *
     * @return mutable property for the naming-conventions-enabled flag
     */
    @Input
    public abstract Property<Boolean> getNamingConventionsEnabled();

    /**
     * Whether generation fails when duplicate rules are discovered.
     *
     * @return mutable property for the fail-on-duplicate-rules flag
     */
    @Input
    public abstract Property<Boolean> getFailOnDuplicateRules();

    /**
     * Whether the built-in hexagonal architecture test is generated at all.
     *
     * @return mutable property for the use-built-in-hexagonal-rule-pack flag
     */
    @Input
    public abstract Property<Boolean> getUseBuiltInHexagonalRulePack();

    /**
     * Directory holding hand-written architecture tests, scanned only to detect duplicate rules
     * against the generated ones - not itself compiled by this task.
     *
     * @return read-only directory property for the hand-written test directory
     */
    @Internal
    public abstract DirectoryProperty getUserTestsDirectory();

    /**
     * The project's main Java source directory, used to infer a base package from its own
     * directory layout when {@link #getBasePackage()} is blank.
     *
     * @return read-only directory property for the main source directory
     */
    @Internal
    public abstract DirectoryProperty getMainSourceDirectory();

    /**
     * Classpath scanned for external rule pack tests to bridge into a generated JUnit Platform
     * suite.
     *
     * @return read-only file collection for the rule pack classpath
     */
    @Classpath
    public abstract ConfigurableFileCollection getRulePackClasspath();

    /**
     * Directory generated test sources are written to.
     *
     * @return mutable directory property for the output directory
     */
    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    /** Generates the built-in hexagonal architecture test and the external rule pack bridge suite. */
    @TaskAction
    public void generate() {
        Path outputRoot = getOutputDirectory().get().getAsFile().toPath();
        resetOutputDirectory(outputRoot);

        try {
            if (getUseBuiltInHexagonalRulePack().getOrElse(true)) {
                String template = loadTemplate();
                String effectiveBasePackage = resolveBasePackage();
                // Map.of() tops out at 10 key-value pairs; Map.ofEntries() has no such limit.
                Map<String, String> replacements = Map.ofEntries(
                        Map.entry("${generatedPackage}", GENERATED_PACKAGE),
                        Map.entry("${basePackage}", escapeJava(effectiveBasePackage)),
                        Map.entry("${inPorts}", javaArrayLiteral(getInPorts().get())),
                        Map.entry("${outPorts}", javaArrayLiteral(getOutPorts().get())),
                        Map.entry("${allPorts}", javaArrayLiteral(resolveAllPortPatterns())),
                        Map.entry("${domainModel}", javaArrayLiteral(getDomainModel().get())),
                        Map.entry("${domainServices}", javaArrayLiteral(getDomainServices().get())),
                        Map.entry("${allAdapters}", javaArrayLiteral(resolveAllAdapterPatterns())),
                        Map.entry("${inboundAdapters}", javaArrayLiteral(getInboundAdapters().get())),
                        Map.entry("${outboundAdapters}", javaArrayLiteral(getOutboundAdapters().get())),
                        Map.entry("${configurationPackages}", javaArrayLiteral(getConfigurationPackages().get())),
                        Map.entry("${portDataTypePackages}", javaArrayLiteral(getPortDataTypePackages().get())),
                        Map.entry("${commonPackages}", javaArrayLiteral(getCommonPackages().get())),
                        Map.entry("${domainAllowedPackages}", javaArrayLiteral(getDomainAllowedPackages().get())),
                        Map.entry("${frameworkDenylistPackages}", javaArrayLiteral(getFrameworkDenylistPackages().get())),
                        Map.entry("${coreLayerPackages}", javaArrayLiteral(resolveCoreLayerPatterns())),
                        Map.entry("${namingConventionsSection}", renderNamingConventionsSection())
                );

                String rendered = template;
                for (Map.Entry<String, String> entry : replacements.entrySet()) {
                    rendered = rendered.replace(entry.getKey(), entry.getValue());
                }

                Path target = outputRoot.resolve("com/arc_e_tect/gradle/architecture/generated/HexagonalArchitectureTest.java");
                Files.createDirectories(target.getParent());
                Files.writeString(target, rendered, StandardCharsets.UTF_8);
                generateSourceDependencyValidationTest(outputRoot);
            }
            generateExternalRulePackSuite(outputRoot);
        } catch (IOException exception) {
            throw new GradleException("Failed to generate architecture tests", exception);
        }
    }

    private void generateSourceDependencyValidationTest(Path outputRoot) throws IOException {
        List<SourceDependencyViolation> violations = findSourceAnnotationDependencyViolations();
        Path target = outputRoot.resolve(
                "com/arc_e_tect/gradle/architecture/generated/" + SOURCE_DEPENDENCY_TEST_CLASS_NAME + ".java");
        Files.createDirectories(target.getParent());
        Files.writeString(target, renderSourceDependencyValidationTest(violations), StandardCharsets.UTF_8);
    }

    private List<SourceDependencyViolation> findSourceAnnotationDependencyViolations() throws IOException {
        if (!getMainSourceDirectory().isPresent()) {
            return List.of();
        }
        Path sourceRoot = getMainSourceDirectory().get().getAsFile().toPath();
        if (!Files.isDirectory(sourceRoot)) {
            return List.of();
        }

        List<File> sourceFiles;
        try (Stream<Path> paths = Files.walk(sourceRoot)) {
            sourceFiles = paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .map(Path::toFile)
                    .toList();
        }
        if (sourceFiles.isEmpty()) {
            return List.of();
        }

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new GradleException("Architecture source dependency validation requires a JDK, not a JRE");
        }

        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8)) {
            Iterable<? extends JavaFileObject> units = fileManager.getJavaFileObjectsFromFiles(sourceFiles);
            JavacTask task = (JavacTask) compiler.getTask(null, fileManager, null, List.of("-proc:none"), null, units);
            List<SourceDependencyViolation> violations = new ArrayList<>();
            for (CompilationUnitTree unit : task.parse()) {
                String packageName = unit.getPackageName() == null ? "" : unit.getPackageName().toString();
                List<SourceDependencyPolicy> policies = policiesFor(packageName);
                if (policies.isEmpty()) {
                    continue;
                }

                for (String annotationType : annotationTypes(unit, packageName)) {
                    for (SourceDependencyPolicy policy : policies) {
                        if (!residesInAnyPackage(annotationType, policy.allowedPackages())) {
                            violations.add(new SourceDependencyViolation(
                                    policy.ruleName(),
                                    sourceRoot.relativize(Path.of(unit.getSourceFile().toUri())).toString(),
                                    annotationType,
                                    policy.description()));
                        }
                    }
                }
            }
            return violations;
        }
    }

    private List<SourceDependencyPolicy> policiesFor(String packageName) {
        List<SourceDependencyPolicy> policies = new ArrayList<>();
        if (packageMatchesAny(packageName, getDomainModel().get())) {
            policies.add(new SourceDependencyPolicy(
                    "domain_must_only_depend_on_domain_or_jdk_core",
                    concat(getDomainModel().get(), getDomainAllowedPackages().get()),
                    "domain model, or the configured JDK allow-list"));
        }
        if (packageMatchesAny(packageName, getDomainServices().get())) {
            policies.add(new SourceDependencyPolicy(
                    "domain_services_must_only_depend_on_domain_core_and_ports",
                    concat(getDomainModel().get(), getDomainServices().get(), getInPorts().get(), getOutPorts().get(),
                            getDomainAllowedPackages().get()),
                    "domain model, domain services, ports, or the configured JDK allow-list"));
        }
        if (packageMatchesAny(packageName, resolveAllPortPatterns())) {
            policies.add(new SourceDependencyPolicy(
                    "ports_must_only_depend_on_domain_ports_or_jdk_core",
                    concat(getDomainModel().get(), getInPorts().get(), getOutPorts().get(), getDomainAllowedPackages().get()),
                    "domain model, ports, or the configured JDK allow-list"));
        }
        return policies;
    }

    @SafeVarargs
    private static List<String> concat(List<String>... packageLists) {
        return Stream.of(packageLists)
                .flatMap(Collection::stream)
                .distinct()
                .toList();
    }

    private static boolean residesInAnyPackage(String typeName, List<String> packagePatterns) {
        return packageMatchesAny(packageName(typeName), packagePatterns);
    }

    private static boolean packageMatchesAny(String packageName, List<String> packagePatterns) {
        return packagePatterns.stream().anyMatch(pattern -> PackageMatcher.of(pattern).matches(packageName));
    }

    private static String packageName(String typeName) {
        int lastDot = typeName.lastIndexOf('.');
        return lastDot < 0 ? "" : typeName.substring(0, lastDot);
    }

    private static List<String> annotationTypes(CompilationUnitTree unit, String packageName) {
        Map<String, String> explicitImports = new LinkedHashMap<>();
        List<String> wildcardImports = new ArrayList<>();
        for (ImportTree importTree : unit.getImports()) {
            if (importTree.isStatic()) {
                continue;
            }
            String importedType = importTree.getQualifiedIdentifier().toString();
            if (importedType.endsWith(".*")) {
                wildcardImports.add(importedType.substring(0, importedType.length() - 2));
            } else {
                explicitImports.put(importedType.substring(importedType.lastIndexOf('.') + 1), importedType);
            }
        }

        List<String> annotationTypes = new ArrayList<>();
        new TreePathScanner<Void, Void>() {
            @Override
            public Void visitAnnotation(AnnotationTree annotationTree, Void ignored) {
                String annotationType = resolveAnnotationType(
                        annotationTree.getAnnotationType().toString(),
                        explicitImports,
                        wildcardImports);
                if (annotationType != null) {
                    annotationTypes.add(annotationType);
                }
                return super.visitAnnotation(annotationTree, ignored);
            }
        }.scan(unit, null);
        return annotationTypes;
    }

    private static String resolveAnnotationType(
            String annotationType,
            Map<String, String> explicitImports,
            List<String> wildcardImports
    ) {
        String explicitImport = explicitImports.get(annotationType);
        if (explicitImport != null) {
            return explicitImport;
        }
        if (annotationType.contains(".")) {
            String rootType = annotationType.substring(0, annotationType.indexOf('.'));
            String rootImport = explicitImports.get(rootType);
            return rootImport == null ? annotationType : rootImport + annotationType.substring(rootType.length());
        }
        if (!wildcardImports.isEmpty()) {
            return wildcardImports.getFirst() + "." + annotationType;
        }
        return null;
    }

    private static String renderSourceDependencyValidationTest(List<SourceDependencyViolation> violations) {
        StringBuilder source = new StringBuilder("""
                package com.arc_e_tect.gradle.architecture.generated;

                import org.junit.jupiter.api.Test;

                import static org.junit.jupiter.api.Assertions.fail;

                class SourceDependencyValidationTest {

                    @Test
                    void source_annotation_dependencies_must_obey_core_allow_lists() {
                """);
        if (violations.isEmpty()) {
            source.append("    }\n}\n");
            return source.toString();
        }

        source.append("        fail(\"")
                .append(escapeJava(renderViolations(violations)))
                .append("\");\n    }\n}\n");
        return source.toString();
    }

    private static String renderViolations(List<SourceDependencyViolation> violations) {
        return violations.stream()
                .map(violation -> violation.ruleName() + ": " + violation.sourceFile()
                        + " uses source annotation " + violation.annotationType()
                        + ", which is outside the allowed " + violation.allowedDescription())
                .collect(Collectors.joining("\\n"));
    }

    private record SourceDependencyPolicy(String ruleName, List<String> allowedPackages, String description) {
    }

    private record SourceDependencyViolation(
            String ruleName,
            String sourceFile,
            String annotationType,
            String allowedDescription
    ) {
    }

    private List<String> resolveCoreLayerPatterns() {
        LinkedHashSet<String> patterns = new LinkedHashSet<>();
        patterns.addAll(getDomainModel().get());
        patterns.addAll(getDomainServices().get());
        patterns.addAll(getInPorts().get());
        patterns.addAll(getOutPorts().get());
        return new ArrayList<>(patterns);
    }

    private String renderNamingConventionsSection() {
        if (!getNamingConventionsEnabled().getOrElse(false)) {
            return "";
        }
        return "\n"
                + "    @ArchTest\n"
                + "    static final ArchRule inbound_ports_should_have_conventional_suffix =\n"
                + "            classes()\n"
                + "                    .that().resideInAnyPackage(\n"
                + "                            " + javaArrayLiteral(getInPorts().get()) + ")\n"
                + "                    .and().resideOutsideOfPackages(\n"
                + "                            " + javaArrayLiteral(getPortDataTypePackages().get()) + ")\n"
                + "                    .should(INBOUND_PORT_NAMING_CONVENTION)\n"
                + "                    .allowEmptyShould(true)\n"
                + "                    .as(\"Inbound port interfaces must end with 'UseCase' or 'InputPort'\");\n"
                + "\n"
                + "    @ArchTest\n"
                + "    static final ArchRule conventionally_named_inbound_ports_reside_in_inbound_port_package =\n"
                + "            classes()\n"
                + "                    .that().haveSimpleNameEndingWith(\"UseCase\")\n"
                + "                    .or().haveSimpleNameEndingWith(\"InputPort\")\n"
                + "                    .should().resideInAnyPackage(\n"
                + "                            " + javaArrayLiteral(getInPorts().get()) + ")\n"
                + "                    .allowEmptyShould(true)\n"
                + "                    .as(\"Classes named with the 'UseCase' or 'InputPort' suffix must reside in the \"\n"
                + "                            + \"inbound ports package — the naming convention is bidirectional\");\n"
                + "\n"
                + "    @ArchTest\n"
                + "    static final ArchRule domain_services_should_have_conventional_suffix =\n"
                + "            classes()\n"
                + "                    .that().resideInAnyPackage(\n"
                + "                            " + javaArrayLiteral(getDomainServices().get()) + ")\n"
                + "                    .should(DOMAIN_SERVICE_NAMING_CONVENTION)\n"
                + "                    .allowEmptyShould(true)\n"
                + "                    .as(\"Domain service classes must end with 'Service' or 'Policy'\");\n"
                + "\n"
                + "    @ArchTest\n"
                + "    static final ArchRule conventionally_named_domain_services_reside_in_domain_service_package =\n"
                + "            classes()\n"
                + "                    .that().haveSimpleNameEndingWith(\"Service\")\n"
                + "                    .or().haveSimpleNameEndingWith(\"Policy\")\n"
                + "                    .should().resideInAnyPackage(\n"
                + "                            " + javaArrayLiteral(getDomainServices().get()) + ")\n"
                + "                    .allowEmptyShould(true)\n"
                + "                    .as(\"Classes named with the 'Service' or 'Policy' suffix must reside in the \"\n"
                + "                            + \"domain services package — the naming convention is bidirectional\");\n";
    }

    private List<String> resolveAllAdapterPatterns() {
        LinkedHashSet<String> patterns = new LinkedHashSet<>();
        patterns.addAll(getAdapters().get());
        patterns.addAll(getInboundAdapters().get());
        patterns.addAll(getOutboundAdapters().get());
        return new ArrayList<>(patterns);
    }

    private List<String> resolveAllPortPatterns() {
        LinkedHashSet<String> patterns = new LinkedHashSet<>();
        patterns.addAll(getInPorts().get());
        patterns.addAll(getOutPorts().get());
        return new ArrayList<>(patterns);
    }

    private void generateExternalRulePackSuite(Path outputRoot) throws IOException {
        Set<String> discoveredClasses = discoverRulePackTests();
        warnOrFailOnDuplicateRules(discoveredClasses);

        if (discoveredClasses.isEmpty()) {
            return;
        }

        Set<String> discoveredPackages = discoveredClasses.stream()
                .map(GenerateArchitectureTestsTask::packageNameOf)
                .filter(packageName -> !packageName.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (discoveredPackages.isEmpty()) {
            return;
        }

        Path suiteFile = outputRoot.resolve("com/arc_e_tect/gradle/architecture/generated/" + EXTERNAL_SUITE_CLASS_NAME + ".java");
        Files.createDirectories(suiteFile.getParent());
        Files.writeString(suiteFile, externalRulePackSuite(discoveredPackages), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private Set<String> discoverRulePackTests() throws IOException {
        Set<String> classNames = new LinkedHashSet<>();
        for (File classpathEntry : getRulePackClasspath()) {
            if (!classpathEntry.exists()) {
                continue;
            }
            if (classpathEntry.isDirectory()) {
                classNames.addAll(discoverRulePackTestsFromDirectory(classpathEntry.toPath()));
            } else if (classpathEntry.getName().endsWith(".jar")) {
                classNames.addAll(discoverRulePackTestsFromJar(classpathEntry));
            }
        }
        return classNames;
    }

    private Collection<String> discoverRulePackTestsFromDirectory(Path root) throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            return paths.filter(Files::isRegularFile)
                    .map(root::relativize)
                    .map(Path::toString)
                    .filter(name -> name.endsWith("Test.class"))
                    .filter(name -> !name.contains("$"))
                    .map(name -> name.replace(File.separatorChar, '.'))
                    .map(name -> name.substring(0, name.length() - ".class".length()))
                    .filter(GenerateArchitectureTestsTask::isRulePackClass)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
    }

    @ExcludeFromJacocoGeneratedCodeCoverage(
            justification = "JAR-scanning path is only reachable when a real JAR-packaged rule pack is on the compile classpath."
                    + " Unit tests supply directories, not JARs. Covered by the testComponent suite.")
    private Collection<String> discoverRulePackTestsFromJar(File jarFile) throws IOException {
        List<String> classNames = new ArrayList<>();
        try (JarFile archive = new JarFile(jarFile)) {
            archive.stream()
                    .map(JarEntry::getName)
                    .filter(name -> name.endsWith("Test.class"))
                    .filter(name -> !name.contains("$"))
                    .map(name -> name.replace('/', '.'))
                    .map(name -> name.substring(0, name.length() - ".class".length()))
                    .filter(GenerateArchitectureTestsTask::isRulePackClass)
                    .forEach(classNames::add);
        }
        return classNames;
    }

    private void warnOrFailOnDuplicateRules(Set<String> discoveredClasses) {
        Map<String, List<String>> duplicates = new LinkedHashMap<>();
        Set<String> localRuleNames = new LinkedHashSet<>();
        if (getUseBuiltInHexagonalRulePack().getOrElse(true)) {
            localRuleNames.add("HexagonalArchitectureTest");
        }
        localRuleNames.addAll(discoverLocalRuleNames());

        Map<String, List<String>> externalBySimpleName = discoveredClasses.stream()
                .collect(Collectors.groupingBy(GenerateArchitectureTestsTask::simpleNameOf, LinkedHashMap::new, Collectors.toList()));

        externalBySimpleName.forEach((simpleName, classes) -> {
            if (classes.size() > 1 || localRuleNames.contains(simpleName)) {
                duplicates.put(simpleName, classes);
            }
        });

        if (duplicates.isEmpty()) {
            return;
        }

        String duplicateMessage = duplicates.entrySet().stream()
                .map(entry -> entry.getKey() + " -> " + entry.getValue())
                .collect(Collectors.joining("; "));

        if (getFailOnDuplicateRules().getOrElse(false)) {
            throw new GradleException("Duplicate architecture rules discovered: " + duplicateMessage);
        }

        getLogger().warn("Duplicate architecture rules discovered: {}", duplicateMessage);
    }

    private Set<String> discoverLocalRuleNames() {
        File userTestsDir = getUserTestsDirectory().isPresent() ? getUserTestsDirectory().get().getAsFile() : null;
        if (userTestsDir == null || !userTestsDir.exists()) {
            return Set.of();
        }
        try (Stream<Path> files = Files.walk(userTestsDir.toPath())) {
            return files.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith("Test.java"))
                    .map(name -> name.substring(0, name.length() - ".java".length()))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        } catch (IOException exception) {
            throw new GradleException("Failed to inspect local architecture tests in " + userTestsDir, exception);
        }
    }

    private void resetOutputDirectory(Path outputRoot) {
        try {
            if (Files.exists(outputRoot)) {
                try (Stream<Path> paths = Files.walk(outputRoot)) {
                    paths.sorted(Comparator.reverseOrder())
                            .forEach(path -> {
                                try {
                                    Files.delete(path);
                                } catch (IOException exception) {
                                    throw new RuntimeException(exception);
                                }
                            });
                }
            }
            Files.createDirectories(outputRoot);
        } catch (RuntimeException | IOException exception) {
            Throwable cause = exception instanceof RuntimeException && exception.getCause() != null
                    ? exception.getCause()
                    : exception;
            throw new GradleException("Failed to reset generated architecture test directory", cause);
        }
    }

    private String externalRulePackSuite(Set<String> packageNames) {
        String packageList = packageNames.stream()
                .map(GenerateArchitectureTestsTask::quoted)
                .collect(Collectors.joining(",\n        "));
        return "package " + GENERATED_PACKAGE + ";\n\n"
                + "import org.junit.platform.suite.api.IncludeClassNamePatterns;\n"
                + "import org.junit.platform.suite.api.SelectPackages;\n"
                + "import org.junit.platform.suite.api.Suite;\n\n"
                + "@Suite\n"
                + "@SelectPackages({\n        " + packageList + "\n})\n"
                + "@IncludeClassNamePatterns({\".*Test\"})\n"
                + "class " + EXTERNAL_SUITE_CLASS_NAME + " {\n"
                + "}\n";
    }

    private String loadTemplate() {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(TEMPLATE_PATH)) {
            if (stream == null) {
                throw new GradleException("Missing template: " + TEMPLATE_PATH);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new GradleException("Failed to load template: " + TEMPLATE_PATH, exception);
        }
    }

    private String resolveBasePackage() {
        String configuredBasePackage = getBasePackage().getOrElse("").trim();
        if (!configuredBasePackage.isEmpty()) {
            return configuredBasePackage;
        }

        String inferredFromSourceLayout = inferBasePackageFromMainSourceLayout();
        if (!inferredFromSourceLayout.isEmpty()) {
            return inferredFromSourceLayout;
        }

        String fallbackBasePackage = getFallbackBasePackage().getOrElse("").trim();
        if (!fallbackBasePackage.isEmpty() && !"unspecified".equals(fallbackBasePackage)) {
            return fallbackBasePackage;
        }

        throw new GradleException(
                "Unable to resolve architectureValidator.basePackage for generated tests. "
                        + "Set architectureValidator.basePackage explicitly or create package directories under src/main/java.");
    }

    private String inferBasePackageFromMainSourceLayout() {
        if (!getMainSourceDirectory().isPresent()) {
            return "";
        }
        Path mainJavaRoot = getMainSourceDirectory().get().getAsFile().toPath();
        if (!Files.isDirectory(mainJavaRoot)) {
            return "";
        }

        List<List<String>> packagePaths = new ArrayList<>();
        try (Stream<Path> files = Files.walk(mainJavaRoot)) {
            files.filter(Files::isRegularFile)
                    .forEach(file -> {
                        Path relative = mainJavaRoot.relativize(file);
                        Path parent = relative.getParent();
                        if (parent == null) {
                            return;
                        }
                        List<String> segments = new ArrayList<>();
                        for (Path segment : parent) {
                            String value = segment.toString();
                            if (!PACKAGE_SEGMENT.matcher(value).matches()) {
                                return;
                            }
                            segments.add(value);
                        }
                        if (!segments.isEmpty()) {
                            packagePaths.add(segments);
                        }
                    });
        } catch (IOException exception) {
            throw new GradleException("Failed to infer base package from src/main/java", exception);
        }

        if (packagePaths.isEmpty()) {
            return "";
        }

        List<String> prefix = new ArrayList<>(packagePaths.get(0));
        for (int i = 1; i < packagePaths.size(); i++) {
            List<String> candidate = packagePaths.get(i);
            int commonLength = 0;
            while (commonLength < prefix.size()
                    && commonLength < candidate.size()
                    && prefix.get(commonLength).equals(candidate.get(commonLength))) {
                commonLength++;
            }
            prefix = new ArrayList<>(prefix.subList(0, commonLength));
            if (prefix.isEmpty()) {
                return "";
            }
        }

        return String.join(".", prefix);
    }

    private static String javaArrayLiteral(List<String> values) {
        if (values.isEmpty()) {
            return "";
        }
        return values.stream()
                .map(GenerateArchitectureTestsTask::quoted)
                .reduce((left, right) -> left + ",\n                        " + right)
                .orElse("");
    }

    private static String quoted(String value) {
        return '"' + escapeJava(value) + '"';
    }

    private static String simpleNameOf(String fqcn) {
        int separator = fqcn.lastIndexOf('.');
        return separator >= 0 ? fqcn.substring(separator + 1) : fqcn;
    }

    private static boolean isRulePackClass(String fqcn) {
        return !fqcn.startsWith("org.junit.")
                && !fqcn.startsWith("org.gradle.")
                && !fqcn.startsWith("com.tngtech.archunit.")
                && !fqcn.startsWith("org.opentest4j.")
                && !fqcn.startsWith("org.apiguardian.")
                && !fqcn.startsWith("java.")
                && !fqcn.startsWith("javax.");
    }

    private static String packageNameOf(String fqcn) {
        int separator = fqcn.lastIndexOf('.');
        return separator >= 0 ? fqcn.substring(0, separator) : "";
    }

    private static String escapeJava(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}