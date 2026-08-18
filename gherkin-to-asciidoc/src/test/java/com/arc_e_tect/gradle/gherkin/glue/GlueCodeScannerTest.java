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
import java.util.concurrent.atomic.AtomicInteger;

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
        assertThat(expressions.get(0).match("the login page is open")).isPresent();
        assertThat(expressions.get(1).match("the user submits \"alice\" and \"secret\"")).isPresent();
        assertThat(expressions.get(2).match("the dashboard is displayed")).isPresent();
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
        assertThat(expressions.get(0).match("I have 42 cukes")).isPresent();
    }

    @Test
    @DisplayName("extracts step definitions declared with @And and @But")
    void scannerShouldExtractStepDefinitionsWhenGlueUsesAndAndButAnnotations(@TempDir Path tempDir) throws IOException {
        writeFile(tempDir, "AndButSteps.java", """
                public class AndButSteps {
                    @And("the audit trail contains {string}")
                    public void auditTrailContains(String entry) {}

                    @But("the account status remains active")
                    public void statusRemainsActive() {}
                }
                """);

        List<Expression> expressions = scanner.scan(tempDir.toFile());

        assertThat(expressions).hasSize(2);
        assertThat(expressions.get(0).match("the audit trail contains \"login\"")).isPresent();
        assertThat(expressions.get(1).match("the account status remains active")).isPresent();
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
        assertThat(expressions.get(0).match("a nested step")).isPresent();
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

    // --- scan(File, Consumer<File>) callback overload ---

    @Test
    @DisplayName("callback overload invokes the callback exactly once per source file scanned")
    void callbackOverloadInvokesCallbackOncePerSourceFile(@TempDir Path tempDir) throws IOException {
        writeFile(tempDir, "LoginSteps.java", """
                public class LoginSteps {
                    @Given("the login page is open")
                    public void loginPageOpen() {}
                }
                """);
        writeFile(tempDir, "PlainClass.java", "public class PlainClass {}");
        writeFile(tempDir, "notes.txt", "not a source file");
        AtomicInteger callbackCount = new AtomicInteger();

        scanner.scan(tempDir.toFile(), file -> callbackCount.incrementAndGet());

        assertThat(callbackCount.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("callback overload returns the exact same expressions the no-callback overload would")
    void callbackOverloadBehavesExactlyLikeNoCallbackOverload(@TempDir Path tempDir) throws IOException {
        writeFile(tempDir, "LoginSteps.java", """
                public class LoginSteps {
                    @Given("the login page is open")
                    public void loginPageOpen() {}
                }
                """);

        List<Expression> expressions = scanner.scan(tempDir.toFile(), file -> { });

        assertThat(expressions).hasSize(1);
    }

    @Test
    @DisplayName("no-callback overload still behaves exactly as before this overload existed")
    void noCallbackOverloadUnaffectedByCallbackOverloadExisting(@TempDir Path tempDir) throws IOException {
        writeFile(tempDir, "LoginSteps.java", """
                public class LoginSteps {
                    @Given("the login page is open")
                    public void loginPageOpen() {}
                }
                """);

        List<Expression> expressions = scanner.scan(tempDir.toFile());

        assertThat(expressions).hasSize(1);
    }

    private void writeFile(Path dir, String name, String content) throws IOException {
        Files.writeString(dir.resolve(name), content, StandardCharsets.UTF_8);
    }
}
