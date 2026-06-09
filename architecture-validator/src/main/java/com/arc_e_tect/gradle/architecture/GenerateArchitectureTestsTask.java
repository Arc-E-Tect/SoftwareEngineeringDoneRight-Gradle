package com.arc_e_tect.gradle.architecture;

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
import java.util.stream.Collectors;
import java.util.stream.Stream;

@DisableCachingByDefault(because = "Generates test sources from extension configuration")
public abstract class GenerateArchitectureTestsTask extends DefaultTask {

    private static final String TEMPLATE_PATH = "templates/HexagonalArchitectureTest.java.template";
    private static final String GENERATED_PACKAGE = "com.arc_e_tect.gradle.architecture.generated";
    private static final String EXTERNAL_SUITE_CLASS_NAME = "ExternalRulePackSuite";

    @Inject
    public GenerateArchitectureTestsTask() {
        setGroup("verification");
        setDescription("Generates the built-in hexagonal architecture tests.");
        getUseBuiltInHexagonalRulePack().convention(true);
    }

    @Input
    public abstract Property<String> getBasePackage();

    @Input
    public abstract ListProperty<String> getInPorts();

    @Input
    public abstract ListProperty<String> getOutPorts();

    @Input
    public abstract ListProperty<String> getDomainModel();

    @Input
    public abstract ListProperty<String> getAdapters();

    @Input
    public abstract ListProperty<String> getApplicationServices();

    @Input
    public abstract ListProperty<String> getCommonPackages();

    @Input
    public abstract Property<Boolean> getFailOnDuplicateRules();

    @Input
    public abstract Property<Boolean> getUseBuiltInHexagonalRulePack();

    @Internal
    public abstract DirectoryProperty getUserTestsDirectory();

    @Classpath
    public abstract ConfigurableFileCollection getRulePackClasspath();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    @TaskAction
    public void generate() {
        Path outputRoot = getOutputDirectory().get().getAsFile().toPath();
        resetOutputDirectory(outputRoot);

        try {
            if (getUseBuiltInHexagonalRulePack().getOrElse(true)) {
                String template = loadTemplate();
                Map<String, String> replacements = Map.of(
                        "${generatedPackage}", GENERATED_PACKAGE,
                        "${basePackage}", escapeJava(getBasePackage().getOrElse("")),
                        "${inPorts}", javaArrayLiteral(getInPorts().get()),
                        "${outPorts}", javaArrayLiteral(getOutPorts().get()),
                        "${domainModel}", javaArrayLiteral(getDomainModel().get()),
                        "${adapters}", javaArrayLiteral(getAdapters().get()),
                        "${applicationServices}", javaArrayLiteral(getApplicationServices().get()),
                        "${commonPackages}", javaArrayLiteral(getCommonPackages().get())
                );

                String rendered = template;
                for (Map.Entry<String, String> entry : replacements.entrySet()) {
                    rendered = rendered.replace(entry.getKey(), entry.getValue());
                }

                Path target = outputRoot.resolve("com/arc_e_tect/gradle/architecture/generated/HexagonalArchitectureTest.java");
                Files.createDirectories(target.getParent());
                Files.writeString(target, rendered, StandardCharsets.UTF_8);
            }
            generateExternalRulePackSuite(outputRoot);
        } catch (IOException exception) {
            throw new GradleException("Failed to generate architecture tests", exception);
        }
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