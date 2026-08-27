package com.arc_e_tect.gradle.mirage.scan;

import com.arc_e_tect.gradle.detector.core.model.Endpoint;
import com.arc_e_tect.gradle.detector.core.model.HttpVerb;
import com.arc_e_tect.gradle.detector.core.model.PathTemplates;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scans WireMock stub mapping files ({@code *.json}) for the request each one stubs, treating
 * every stubbed request as an implemented endpoint.
 *
 * <p>Stub files are data, not code - this is a plain line/regex scan, deliberately without a JSON
 * parser or the WireMock library itself on the classpath. One file is expected to describe one
 * stub, the common convention for files stored under a {@code mappings} directory: the first
 * {@code method} field and the first {@code url}/{@code urlPath}/{@code urlPattern}/
 * {@code urlPathPattern} field found in the file are used.</p>
 *
 * <p>{@code urlPattern}/{@code urlPathPattern} are WireMock regular expressions, not Spring/
 * OpenAPI path templates - see {@link #normalizePattern(String)} for how a regex path-variable
 * segment (e.g. {@code "[0-9]+"}) is recognised and rewritten into the {@code "{id}"}-style
 * placeholder a declared/implemented endpoint's path actually uses, so this scanner's stub
 * evidence can be recognised as evidence for that endpoint instead of accumulating as a
 * permanently unmatched, orphaned path in its own right. {@code url}/{@code urlPath} are literal
 * WireMock matchers, never regular expressions, and are used as-is (beyond
 * {@link PathTemplates#normalize(String)}'s own formatting normalisation).</p>
 */
public class WireMockStubScanner {

    private static final Pattern METHOD = Pattern.compile("\"method\"\\s*:\\s*\"([A-Za-z]+)\"");
    private static final Pattern URL =
            Pattern.compile("\"(url(?:Path)?(?:Pattern)?)\"\\s*:\\s*\"([^\"]+)\"");

    /**
     * A {@code "/"}-delimited segment recognised as a plain literal, as opposed to a regular
     * expression fragment: letters, digits, underscore, dot, colon, and hyphen only. Deliberately
     * conservative - matching WireMock's own regex syntax precisely is out of scope for a
     * plain-text scanner that never parses the pattern as an actual regular expression - so a
     * segment containing any character outside this set (e.g. {@code "["}, {@code "+"},
     * {@code "\"}, {@code "."} used as a wildcard) is instead treated as a path-variable
     * placeholder by {@link #normalizePattern(String)}.
     */
    private static final Pattern LITERAL_SEGMENT = Pattern.compile("[A-Za-z0-9_.:-]*");

    /** Creates a new {@code WireMockStubScanner}. */
    public WireMockStubScanner() {}

    /**
     * Scans every {@code .json} file under {@code rootDir}, recursively, for the stubbed method
     * and URL.
     *
     * @param rootDir directory to search; may not exist, in which case an empty list is returned
     * @return one {@link Endpoint} per stub file that has both a recognised method and URL
     * @throws IOException if a stub file cannot be read
     */
    public List<Endpoint> scan(File rootDir) throws IOException {
        List<Endpoint> endpoints = new ArrayList<>();
        for (File stubFile : collectStubFiles(rootDir)) {
            Endpoint endpoint = scanFile(stubFile, rootDir);
            if (endpoint != null) {
                endpoints.add(endpoint);
            }
        }
        return endpoints;
    }

    private Endpoint scanFile(File file, File rootDir) throws IOException {
        List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);

        HttpVerb verb = null;
        String path = null;
        int methodLine = 0;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (verb == null) {
                Matcher matcher = METHOD.matcher(line);
                if (matcher.find()) {
                    verb = parseVerb(matcher.group(1));
                    methodLine = i + 1;
                }
            }
            if (path == null) {
                Matcher matcher = URL.matcher(line);
                if (matcher.find()) {
                    String fieldName = matcher.group(1);
                    String rawValue = matcher.group(2);
                    path = fieldName.endsWith("Pattern")
                            ? normalizePattern(rawValue) : PathTemplates.normalize(rawValue);
                }
            }
        }

        if (verb == null || path == null) {
            return null;
        }

        String declaringGroup = relativeParentPath(rootDir, file);
        String stubName = stripExtension(file.getName());
        return new Endpoint(verb, path, declaringGroup, stubName, file.getName(), methodLine);
    }

    /**
     * Rewrites a WireMock {@code urlPattern}/{@code urlPathPattern} regular expression into an
     * OpenAPI-comparable path template: every {@code "/"}-delimited segment that isn't a plain
     * literal per {@link #LITERAL_SEGMENT} - e.g. {@code "[0-9]+"}, matching WireMock's own
     * idiomatic way to express "any numeric id here" - is replaced with a single {@code "{id}"}
     * placeholder segment, the same shape a Spring-mapped controller method or an
     * OpenAPI-declared path uses for a path variable.
     *
     * <p>Without this, a stub matched by regular expression would never be recognised as evidence
     * for its corresponding {@code "{customerId}"}-style declared/implemented endpoint - both
     * {@link com.arc_e_tect.gradle.detector.core.model.PathMatcher}, used to compare this run's
     * scanned endpoints, and the fingerprint persisted contract history is keyed by, are looking
     * for a placeholder segment, not a literal regular expression fragment. It would instead
     * accumulate as its own, permanently unmatched, orphaned path.</p>
     *
     * @param rawPattern the raw regular expression from a {@code urlPattern}/
     *                    {@code urlPathPattern} field
     * @return the pattern with every non-literal segment replaced by {@code "{id}"}, normalized
     */
    private String normalizePattern(String rawPattern) {
        String normalized = PathTemplates.normalize(rawPattern);
        StringBuilder result = new StringBuilder();
        for (String segment : normalized.split("/")) {
            if (segment.isEmpty()) {
                continue;
            }
            result.append('/').append(isLiteralSegment(segment) ? segment : "{id}");
        }
        return result.length() == 0 ? "/" : result.toString();
    }

    private boolean isLiteralSegment(String segment) {
        return LITERAL_SEGMENT.matcher(segment).matches();
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
            return "(mappings)";
        }
        String relative = parentPath.substring(rootPath.length() + 1);
        return relative.replace(File.separatorChar, '.');
    }

    private String stripExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot >= 0 ? fileName.substring(0, lastDot) : fileName;
    }

    private List<File> collectStubFiles(File dir) {
        List<File> files = new ArrayList<>();
        collectStubFiles(dir, files);
        return files;
    }

    private void collectStubFiles(File dir, List<File> files) {
        if (!dir.isDirectory()) {
            return;
        }
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child.isFile() && child.getName().endsWith(".json")) {
                files.add(child);
            } else if (child.isDirectory()) {
                collectStubFiles(child, files);
            }
        }
    }
}
