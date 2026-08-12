package com.arc_e_tect.gradle.doppelganger.scan;

import com.arc_e_tect.gradle.detector.core.model.Endpoint;
import com.arc_e_tect.gradle.detector.core.model.HttpVerb;
import com.arc_e_tect.gradle.detector.core.model.PathTemplates;
import com.arc_e_tect.gradle.doppelganger.detect.ContractVerificationSource;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@link ContractVerificationSource} for Spring Cloud Contract: declarative contract DSL files
 * under {@code src/testContract/resources/contracts/**&#47;*.groovy} (and {@code .yml}), read
 * structurally for {@code method '...'} and {@code url '...'} / {@code urlPath '...'} entries.
 *
 * <p>Contract files are data, not code - this is a plain line/regex scan, deliberately without an
 * AST or a Groovy/YAML parser on the classpath.</p>
 */
public class SpringCloudContractScanner implements ContractVerificationSource {

    private static final Pattern GROOVY_METHOD =
            Pattern.compile("method\\s*\\(?\\s*['\"]([A-Za-z]+)['\"]");
    private static final Pattern GROOVY_URL =
            Pattern.compile("url(?:Path)?\\s*\\(?\\s*['\"]([^'\"]+)['\"]");
    private static final Pattern YAML_METHOD =
            Pattern.compile("(?i)^\\s*method\\s*:\\s*['\"]?([A-Za-z]+)['\"]?\\s*$");
    private static final Pattern YAML_URL =
            Pattern.compile("(?i)^\\s*url(?:Path)?\\s*:\\s*['\"]?([^'\"]+?)['\"]?\\s*$");

    /** Creates a new {@code SpringCloudContractScanner}. */
    public SpringCloudContractScanner() {}

    /** {@inheritDoc} */
    @Override
    public List<Endpoint> scan(File rootDir) throws IOException {
        List<Endpoint> endpoints = new ArrayList<>();
        for (File contractFile : collectContractFiles(rootDir)) {
            Endpoint endpoint = scanFile(contractFile, rootDir);
            if (endpoint != null) {
                endpoints.add(endpoint);
            }
        }
        return endpoints;
    }

    private Endpoint scanFile(File file, File rootDir) throws IOException {
        boolean yaml = file.getName().endsWith(".yml");
        Pattern methodPattern = yaml ? YAML_METHOD : GROOVY_METHOD;
        Pattern urlPattern = yaml ? YAML_URL : GROOVY_URL;

        List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);

        HttpVerb verb = null;
        String path = null;
        int verbLine = 0;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (verb == null) {
                Matcher matcher = methodPattern.matcher(line);
                if (matcher.find()) {
                    verb = parseVerb(matcher.group(1));
                    verbLine = i + 1;
                }
            }
            if (path == null) {
                Matcher matcher = urlPattern.matcher(line);
                if (matcher.find()) {
                    path = PathTemplates.normalize(matcher.group(1));
                }
            }
        }

        if (verb == null || path == null) {
            return null;
        }

        String declaringClass = relativeParentPath(rootDir, file);
        String methodSignature = stripExtension(file.getName());
        return new Endpoint(verb, path, declaringClass, methodSignature, file.getName(), verbLine);
    }

    private HttpVerb parseVerb(String raw) {
        try {
            return HttpVerb.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String relativeParentPath(File rootDir, File file) {
        File parent = file.getParentFile();
        String rootPath = rootDir.getAbsoluteFile().getPath();
        String parentPath = parent.getAbsoluteFile().getPath();
        if (parentPath.equals(rootPath)) {
            return "(contracts)";
        }
        String relative = parentPath.substring(rootPath.length() + 1);
        return relative.replace(File.separatorChar, '.');
    }

    private String stripExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot >= 0 ? fileName.substring(0, lastDot) : fileName;
    }

    private List<File> collectContractFiles(File dir) {
        List<File> files = new ArrayList<>();
        collectContractFiles(dir, files);
        return files;
    }

    private void collectContractFiles(File dir, List<File> files) {
        if (!dir.isDirectory()) {
            return;
        }
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child.isFile() && (child.getName().endsWith(".groovy") || child.getName().endsWith(".yml"))) {
                files.add(child);
            } else if (child.isDirectory()) {
                collectContractFiles(child, files);
            }
        }
    }
}
