package com.arc_e_tect.gradle.gherkin.glue;

import io.cucumber.cucumberexpressions.Expression;
import io.cucumber.cucumberexpressions.ExpressionFactory;
import io.cucumber.cucumberexpressions.ParameterTypeRegistry;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scans a directory of Cucumber-JVM glue code (step definition source files) and
 * extracts every {@code @Given}/{@code @When}/{@code @Then}/{@code @And}/{@code @But}
 * step definition pattern
 * as a compiled {@link Expression}.
 *
 * <p>Step definitions are matched with a regular expression over the raw source text
 * rather than a full Java/Kotlin parser, since only the string literal passed to the
 * annotation is needed. Both Cucumber Expressions and regular-expression step patterns
 * are supported, since {@link ExpressionFactory} auto-detects which kind a given
 * pattern is.</p>
 */
public class GlueCodeScanner {

    private static final Logger LOGGER = Logging.getLogger(GlueCodeScanner.class);

        private static final Pattern STEP_ANNOTATION = Pattern.compile(
            "@(?:Given|When|Then|And|But)\\s*\\(\\s*\"((?:[^\"\\\\]|\\\\.)*)\"\\s*\\)");

    private static final List<String> SOURCE_EXTENSIONS = List.of(".java", ".kt", ".groovy");

    private final ExpressionFactory expressionFactory =
            new ExpressionFactory(new ParameterTypeRegistry(Locale.ENGLISH));

    /** Creates a new {@code GlueCodeScanner}. */
    public GlueCodeScanner() {}

    /**
     * Recursively scans {@code glueCodeDir} for step definition source files and
     * returns every step pattern found as a compiled {@link Expression}. Equivalent to
     * {@link #scan(File, Consumer)} with a callback that does nothing.
     *
     * @param glueCodeDir directory containing the glue code source files
     * @return step definition patterns found across all source files; patterns that
     *         cannot be compiled are skipped and logged as a warning
     */
    public List<Expression> scan(File glueCodeDir) {
        return scan(glueCodeDir, file -> { });
    }

    /**
     * Same as {@link #scan(File)}, additionally invoking {@code onFileScanned} once for every
     * source file it processes - whether or not that file contains any step definitions - so a
     * caller can drive a progress indicator during what would otherwise be a single opaque,
     * potentially long-running call across a whole directory tree.
     *
     * @param glueCodeDir   directory containing the glue code source files
     * @param onFileScanned invoked once per source file found under {@code glueCodeDir}; never
     *                      {@code null}
     * @return step definition patterns found across all source files; patterns that
     *         cannot be compiled are skipped and logged as a warning
     */
    public List<Expression> scan(File glueCodeDir, Consumer<File> onFileScanned) {
        List<Expression> expressions = new ArrayList<>();
        List<File> sourceFiles = new ArrayList<>();
        collectSourceFiles(glueCodeDir, sourceFiles);

        for (File file : sourceFiles) {
            for (String rawPattern : extractPatterns(file)) {
                try {
                    expressions.add(expressionFactory.createExpression(rawPattern));
                } catch (RuntimeException e) {
                    LOGGER.warn("Could not compile step definition pattern '{}' in '{}': {}",
                            rawPattern, file, e.getMessage());
                }
            }
            onFileScanned.accept(file);
        }
        return expressions;
    }

    private List<String> extractPatterns(File file) {
        List<String> patterns = new ArrayList<>();
        try {
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            Matcher matcher = STEP_ANNOTATION.matcher(content);
            while (matcher.find()) {
                patterns.add(unescape(matcher.group(1)));
            }
        } catch (IOException e) {
            LOGGER.warn("Could not read glue code file '{}': {}", file, e.getMessage());
        }
        return patterns;
    }

    private void collectSourceFiles(File dir, List<File> files) {
        if (!dir.isDirectory()) {
            return;
        }
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child.isFile() && hasSourceExtension(child.getName())) {
                files.add(child);
            } else if (child.isDirectory()) {
                collectSourceFiles(child, files);
            }
        }
    }

    private boolean hasSourceExtension(String fileName) {
        return SOURCE_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }

    private String unescape(String literal) {
        return literal.replace("\\\"", "\"").replace("\\\\", "\\");
    }
}
