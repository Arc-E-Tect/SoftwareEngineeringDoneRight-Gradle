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
 */
public class WireMockStubScanner {

    private static final Pattern METHOD = Pattern.compile("\"method\"\\s*:\\s*\"([A-Za-z]+)\"");
    private static final Pattern URL =
            Pattern.compile("\"url(?:Path(?:Pattern)?|Pattern)?\"\\s*:\\s*\"([^\"]+)\"");

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
                    path = PathTemplates.normalize(matcher.group(1));
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
