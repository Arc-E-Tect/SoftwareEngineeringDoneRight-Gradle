package com.arc_e_tect.gradle.gherkin.glue;

import io.cucumber.cucumberexpressions.Expression;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlueCodeScanner")
class GlueCodeScannerTest {

    private final GlueCodeScanner scanner = new GlueCodeScanner();

    @Test
    @DisplayName("extracts Cucumber Expression step definitions from a Java glue file")
    void extractsCucumberExpressionStepDefinitions(@TempDir Path tempDir) throws IOException {
        writeFile(tempDir, "LoginSteps.java", """
                public class LoginSteps {
                    @Given("the login page is open")
                    public void loginPageOpen() {}

                    @When("the user submits {string} and {string}")
                    public void submits(String u, String p) {}

                    @Then("the dashboard is displayed")
                    public void dashboard() {}
                }
                """);

        List<Expression> expressions = scanner.scan(tempDir.toFile());

        assertThat(expressions).hasSize(3);
        assertThat(expressions.get(0).match("the login page is open")).isNotNull();
        assertThat(expressions.get(1).match("the user submits \"alice\" and \"secret\"")).isNotNull();
        assertThat(expressions.get(2).match("the dashboard is displayed")).isNotNull();
    }

    @Test
    @DisplayName("extracts regular-expression step definitions")
    void extractsRegularExpressionStepDefinitions(@TempDir Path tempDir) throws IOException {
        writeFile(tempDir, "RegexSteps.java", """
                public class RegexSteps {
                    @Given("^I have (\\d+) cukes$")
                    public void haveCukes(int count) {}
                }
                """);

        List<Expression> expressions = scanner.scan(tempDir.toFile());

        assertThat(expressions).hasSize(1);
        assertThat(expressions.get(0).match("I have 42 cukes")).isNotNull();
    }

    @Test
    @DisplayName("scans subdirectories recursively")
    void scansSubdirectoriesRecursively(@TempDir Path tempDir) throws IOException {
        Path subDir = tempDir.resolve("nested");
        Files.createDirectories(subDir);
        writeFile(subDir, "NestedSteps.java", """
                public class NestedSteps {
                    @Given("a nested step")
                    public void nested() {}
                }
                """);

        List<Expression> expressions = scanner.scan(tempDir.toFile());

        assertThat(expressions).hasSize(1);
        assertThat(expressions.get(0).match("a nested step")).isNotNull();
    }

    @Test
    @DisplayName("ignores non-source files")
    void ignoresNonSourceFiles(@TempDir Path tempDir) throws IOException {
        writeFile(tempDir, "notes.txt", "@Given(\"should be ignored\")");

        List<Expression> expressions = scanner.scan(tempDir.toFile());

        assertThat(expressions).isEmpty();
    }

    @Test
    @DisplayName("returns an empty list for a non-existent directory")
    void returnsEmptyListForNonExistentDirectory() {
        List<Expression> expressions = scanner.scan(new File("/does/not/exist"));

        assertThat(expressions).isEmpty();
    }

    @Test
    @DisplayName("returns an empty list for a glue code directory with no step definitions")
    void returnsEmptyListWhenNoStepDefinitionsPresent(@TempDir Path tempDir) throws IOException {
        writeFile(tempDir, "PlainClass.java", "public class PlainClass {}");

        List<Expression> expressions = scanner.scan(tempDir.toFile());

        assertThat(expressions).isEmpty();
    }

    private void writeFile(Path dir, String name, String content) throws IOException {
        Files.writeString(dir.resolve(name), content, StandardCharsets.UTF_8);
    }
}
