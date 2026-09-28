package com.arc_e_tect.gradle.mirage.scan;

import com.arc_e_tect.gradle.detector.core.model.Endpoint;
import com.arc_e_tect.gradle.detector.core.model.HttpVerb;
import com.arc_e_tect.gradle.detector.core.model.PathMatcher;
import com.arc_e_tect.gradle.detector.core.model.PathTemplates;
import com.arc_e_tect.gradle.detector.core.openapi.DescribedEndpoint;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
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
 * {@code urlPathPattern}/{@code urlPathTemplate} field found in the file are used.</p>
 *
 * <p>{@code urlPathTemplate} is WireMock's path-template matcher, and already uses the
 * OpenAPI-style {@code "{name}"} placeholders a declared/implemented endpoint's path uses, so it is
 * read as a template as it stands.</p>
 *
 * <p>{@code urlPattern}/{@code urlPathPattern} are WireMock regular expressions, not Spring/
 * OpenAPI path templates - see {@link #normalizePattern(String)} for how a regex path-variable
 * segment (e.g. {@code "[0-9]+"}) is recognised and rewritten into the {@code "{id}"}-style
 * placeholder a declared/implemented endpoint's path actually uses, so this scanner's stub
 * evidence can be recognised as evidence for that endpoint instead of accumulating as a
 * permanently unmatched, orphaned path in its own right.</p>
 *
 * <p>{@code url}/{@code urlPath} are literal WireMock matchers, never regular expressions. When the
 * scanner is given the declared endpoints, a literal path is first matched against them - see
 * {@link #WireMockStubScanner(List, String)} - and recorded as the declared template it is an
 * instance of. Only a literal path that matches no declared template falls back to the heuristic
 * below: a purely numeric segment (e.g. {@code "700336"} in {@code "/persons/700336"}) is, just as
 * heuristically, rewritten into a {@code "{id}"} placeholder too, by
 * {@link #normalizeLiteral(String)}: a hand-written literal stub overwhelmingly picks an arbitrary
 * example value to stand in for a path variable, the same way {@link #normalizePattern(String)}
 * already treats a regular expression's own numeric-id shape. This is a deliberately imprecise
 * heuristic, not a certainty - a genuinely numeric literal path segment that isn't a path
 * variable at all (e.g. a year in a URL) is indistinguishable from an example id by shape alone.
 * Matching the declared templates first records it as its declared literal path; only a path
 * declared nowhere, such as one only a controller implements, is rewritten the same way, which
 * then fails to match that segment's own, genuinely literal, implemented path instead. See the
 * plugin README's "Matching a stub's id segment to its declared/implemented endpoint's path
 * variable" section for the accepted trade-off this represents.</p>
 */
public class WireMockStubScanner {

    private static final Pattern METHOD = Pattern.compile("\"method\"\\s*:\\s*\"([A-Za-z]+)\"");
    private static final Pattern URL =
            Pattern.compile("\"(url(?:Path)?(?:Pattern|Template)?)\"\\s*:\\s*\"([^\"]+)\"");

    /**
     * The {@code operation} field a generated stub's {@code metadata} carries, e.g. the API-Only
     * TranscriberJ's WireMock emitter's: the {@code operationId} the stub was generated from.
     */
    private static final Pattern OPERATION = Pattern.compile("\"operation\"\\s*:\\s*\"([^\"]+)\"");

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

    /**
     * A {@code "/"}-delimited segment recognised as a probable example id value in a literal
     * {@code url}/{@code urlPath} matcher: digits only. See {@link #normalizeLiteral(String)}.
     */
    private static final Pattern NUMERIC_SEGMENT = Pattern.compile("[0-9]+");

    private final List<DescribedEndpoint> declared;
    private final String basePath;

    /**
     * Creates a new {@code WireMockStubScanner} with no declared endpoints to match literal paths
     * against: a literal path's segments are rewritten by the digits-only heuristic alone.
     */
    public WireMockStubScanner() {
        this(List.of(), null);
    }

    /**
     * Creates a new {@code WireMockStubScanner} that matches a literal {@code url}/{@code urlPath}
     * against {@code declared} before falling back to the digits-only heuristic.
     *
     * <p>A literal path is recorded as a declared template of the stub's own method when it is an
     * instance of it, segment by segment, with a declared placeholder accepting any value - so
     * {@code "/v1/users/aaaa"} is recorded as {@code "/v1/users/{username}"}. When it is an
     * instance of several, the one routing would pick wins: at the first segment where two
     * templates differ, a literal segment beats a placeholder, so {@code "/users/me"} is recorded
     * as a declared {@code "/users/me"} rather than {@code "/users/{id}"}. Where the stub's
     * {@code metadata} names an {@code operation}, the declared operation with that
     * {@code operationId} is preferred, as long as the path is an instance of its template.</p>
     *
     * @param declared the declared endpoints, from the OpenAPI documentation; their paths never
     *                 include a base path
     * @param basePath the base path a stub's path may carry in front of a declared path, or
     *                 {@code null} when there is none; a matched path keeps it, so it is stripped
     *                 the same way as every other stub path
     */
    public WireMockStubScanner(List<DescribedEndpoint> declared, String basePath) {
        this.declared = List.copyOf(declared);
        this.basePath = basePath;
    }

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
        String fieldName = null;
        String rawValue = null;
        String operationId = null;
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
            if (fieldName == null) {
                Matcher matcher = URL.matcher(line);
                if (matcher.find()) {
                    fieldName = matcher.group(1);
                    rawValue = matcher.group(2);
                }
            }
            if (operationId == null) {
                Matcher matcher = OPERATION.matcher(line);
                if (matcher.find()) {
                    operationId = matcher.group(1);
                }
            }
        }

        if (verb == null || fieldName == null) {
            return null;
        }

        String path;
        if (fieldName.endsWith("Pattern")) {
            path = normalizePattern(rawValue);
        } else if (fieldName.endsWith("Template")) {
            path = PathTemplates.normalize(rawValue);
        } else {
            path = resolveLiteral(verb, rawValue, operationId);
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
     * <p>A segment that reads as a plain literal (no regex metacharacter) is <em>also</em> rewritten
     * when it's purely numeric, exactly like {@link #normalizeLiteral(String)} treats one - a
     * {@code urlPattern}/{@code urlPathPattern} field is free to contain a plain literal value
     * instead of an actual regular expression (WireMock imposes no such restriction), and a stub
     * author reaches for that field name as often out of habit as out of an actual need for regex
     * matching. Without this, a {@code urlPathPattern} value like
     * {@code "/customers/123/signatories/111"} - fully literal, no regex metacharacter anywhere -
     * would be left completely untouched, the exact orphaned-path failure this method exists to
     * fix in the first place, just for the field name that most looks like it should already be
     * covered.</p>
     *
     * @param rawPattern the raw regular expression - or plain literal value - from a
     *                    {@code urlPattern}/{@code urlPathPattern} field
     * @return the pattern with every non-literal or purely numeric segment replaced by
     *         {@code "{id}"}, normalized
     */
    private String normalizePattern(String rawPattern) {
        return rewriteSegments(rawPattern, segment -> !isLiteralSegment(segment) || isNumericSegment(segment));
    }

    /**
     * Resolves a literal {@code url}/{@code urlPath} matcher to the declared template it is an
     * instance of - see {@link #WireMockStubScanner(List, String)} - or, when it is an instance of
     * none, to {@link #normalizeLiteral(String)}'s rewrite of it.
     *
     * @param verb        the stub's method
     * @param rawValue    the raw literal value from a {@code url}/{@code urlPath} field
     * @param operationId the {@code operation} the stub's {@code metadata} names, or {@code null}
     * @return the matched declared template, carrying the base path when the literal does, or the
     *         heuristic rewrite
     */
    private String resolveLiteral(HttpVerb verb, String rawValue, String operationId) {
        int query = rawValue.indexOf('?');
        String literal = PathTemplates.normalize(query >= 0 ? rawValue.substring(0, query) : rawValue);
        String relative = PathTemplates.stripBasePath(literal, basePath);
        String prefix = relative.equals(literal) ? "" : basePath;

        List<DescribedEndpoint> candidates = declared.stream()
                .filter(d -> verb == HttpVerb.ANY || d.verb() == verb)
                .filter(d -> PathMatcher.matchesConcrete(relative, PathTemplates.normalize(d.path())))
                .toList();
        Optional<String> template = candidates.stream()
                .filter(d -> operationId != null && operationId.equals(d.operationId()))
                .map(d -> PathTemplates.normalize(d.path()))
                .findFirst()
                .or(() -> candidates.stream()
                        .map(d -> PathTemplates.normalize(d.path()))
                        .reduce(WireMockStubScanner::moreSpecific));
        return template
                .map(t -> PathTemplates.join(prefix, t))
                .orElseGet(() -> normalizeLiteral(rawValue));
    }

    /**
     * The more specific of two templates the same literal path is an instance of, the way routing
     * picks one: at the first segment where one is a literal and the other a placeholder, the
     * literal wins. Both have the same number of segments, and where both are literals they are
     * equal, since both match the same path.
     */
    private static String moreSpecific(String a, String b) {
        String[] segmentsA = a.split("/");
        String[] segmentsB = b.split("/");
        for (int i = 0; i < segmentsA.length; i++) {
            boolean aPlaceholder = PathTemplates.isPlaceholder(segmentsA[i]);
            if (aPlaceholder != PathTemplates.isPlaceholder(segmentsB[i])) {
                return aPlaceholder ? b : a;
            }
        }
        return a;
    }

    /**
     * Rewrites a literal {@code url}/{@code urlPath} matcher's own probable example-id segments
     * (digits only, e.g. {@code "700336"}) into a {@code "{id}"} placeholder, the same shape
     * {@link #normalizePattern(String)} rewrites a regular expression's numeric-id shape into -
     * see this class's own javadoc for why, and for the imprecision this deliberately accepts.
     *
     * @param rawValue the raw literal value from a {@code url}/{@code urlPath} field
     * @return the value with every purely numeric segment replaced by {@code "{id}"}, normalized
     */
    private String normalizeLiteral(String rawValue) {
        return rewriteSegments(rawValue, this::isNumericSegment);
    }

    private String rewriteSegments(String rawPath, Predicate<String> isPlaceholderCandidate) {
        String normalized = PathTemplates.normalize(rawPath);
        StringBuilder result = new StringBuilder();
        for (String segment : normalized.split("/")) {
            if (segment.isEmpty()) {
                continue;
            }
            result.append('/').append(isPlaceholderCandidate.test(segment) ? "{id}" : segment);
        }
        return result.length() == 0 ? "/" : result.toString();
    }

    private boolean isLiteralSegment(String segment) {
        return LITERAL_SEGMENT.matcher(segment).matches();
    }

    private boolean isNumericSegment(String segment) {
        return NUMERIC_SEGMENT.matcher(segment).matches();
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
