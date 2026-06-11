package com.arc_e_tect.gradle.architecture;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public abstract class GenerateArchitectureTestsTask extends DefaultTask {

    private static final String GENERATED_PACKAGE = "com.arc_e_tect.gradle.architecture.generated";

    @Input
    public abstract Property<String> getBasePackage();

    @Input
    public abstract Property<String> getBuiltInTemplate();

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
    public abstract ListProperty<String> getPresentation();

    @Input
    public abstract ListProperty<String> getLayeredApplication();

    @Input
    public abstract ListProperty<String> getLayeredDomain();

    @Input
    public abstract ListProperty<String> getInfrastructure();

    @TaskAction
    public void generate() throws Exception {
        TemplateSpec templateSpec = selectTemplate();
        String template = Files.readString(Path.of(templateSpec.templatePath()), StandardCharsets.UTF_8);

        Map<String, String> replacements = switch (getBuiltInTemplate().get()) {
            case "hexagonal" -> hexagonalReplacements();
            case "layered" -> layeredReplacements();
            default -> throw new GradleException("Unsupported built-in template: " + getBuiltInTemplate().get());
        };

        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            template = template.replace(entry.getKey(), entry.getValue());
        }

        Path generatedFile = Path.of("build/generated/testArchitecture/java/com/arc_e_tect/gradle/architecture/generated/"
                + templateSpec.outputClassName() + ".java");
        Files.createDirectories(generatedFile.getParent());
        Files.writeString(generatedFile, template, StandardCharsets.UTF_8);
    }

    private TemplateSpec selectTemplate() {
        return switch (getBuiltInTemplate().get()) {
            case "hexagonal" -> new TemplateSpec(
                    "src/main/resources/templates/HexagonalArchitectureTest.java.template",
                    "HexagonalArchitectureTest");
            case "layered" -> new TemplateSpec(
                    "src/main/resources/templates/LayeredArchitectureTest.java.template",
                    "LayeredArchitectureTest");
            default -> throw new GradleException("Unsupported built-in template: " + getBuiltInTemplate().get());
        };
    }

    private Map<String, String> hexagonalReplacements() {
        return Map.of(
                "${generatedPackage}", GENERATED_PACKAGE,
                "${basePackage}", escapeJava(getBasePackage().get()),
                "${inPorts}", javaArrayLiteral(getInPorts().get()),
                "${outPorts}", javaArrayLiteral(getOutPorts().get()),
                "${domainModel}", javaArrayLiteral(getDomainModel().get()),
                "${adapters}", javaArrayLiteral(getAdapters().get()),
                "${applicationServices}", javaArrayLiteral(getApplicationServices().get()),
                "${commonPackages}", javaArrayLiteral(getCommonPackages().get())
        );
    }

    private Map<String, String> layeredReplacements() {
        return Map.of(
                "${generatedPackage}", GENERATED_PACKAGE,
                "${basePackage}", escapeJava(getBasePackage().get()),
                "${presentation}", javaArrayLiteral(getPresentation().get()),
                "${application}", javaArrayLiteral(getLayeredApplication().get()),
                "${domain}", javaArrayLiteral(getLayeredDomain().get()),
                "${infrastructure}", javaArrayLiteral(getInfrastructure().get())
        );
    }

    private String javaArrayLiteral(Iterable<String> values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (!builder.isEmpty()) {
                builder.append(",\n                        ");
            }
            builder.append('"').append(escapeJava(value)).append('"');
        }
        return builder.toString();
    }

    private String escapeJava(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private record TemplateSpec(String templatePath, String outputClassName) {
    }
}