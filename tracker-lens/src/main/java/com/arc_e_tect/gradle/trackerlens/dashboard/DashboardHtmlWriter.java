package com.arc_e_tect.gradle.trackerlens.dashboard;

import com.arc_e_tect.gradle.trackerlens.lens.LensNaming;
import com.arc_e_tect.gradle.trackerlens.lens.ResolvedLens;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheException;
import org.gradle.api.GradleException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

/**
 * Renders {@link DashboardView} to {@code dashboard.html} plus one CSS file per discovered lens, via
 * a Mustache template - this plugin's own bundled default, or a user-supplied override.
 *
 * <p>Every element governed by {@code ContractRule} is emitted exactly as that contract requires by
 * the bundled default template; everything else - headings, captions, disclaimer wording - is free
 * text and may be reworded at will, including by simply editing a copy of it (see {@code template}
 * in the extension DSL). Whichever template rendered it, this writer's output is always run back
 * through {@code com.arc_e_tect.gradle.trackerlens.contract.LensContractValidator} by
 * {@code GenerateTrackerLensTask} as the unconditional last step of generation - a user template
 * that has drifted from the contract is caught exactly the same way the bundled one would be.</p>
 */
public class DashboardHtmlWriter {

    private static final String DEFAULT_TEMPLATE_RESOURCE = "templates/dashboard.html.mustache";

    /** Creates a new {@code DashboardHtmlWriter}. */
    public DashboardHtmlWriter() {}

    /**
     * Writes {@code dashboard.html} and one CSS file per lens in {@code view.lenses()} to
     * {@code outputDir}, rendering {@code dashboard.html} from {@code customTemplate} when given,
     * otherwise from this plugin's own bundled default template.
     *
     * @param outputDir      the directory to write into; created if missing
     * @param view           the data to render
     * @param customTemplate a user-supplied Mustache template file, or {@code null} to use the
     *                       bundled default
     * @return the written {@code dashboard.html} file
     */
    public File write(File outputDir, DashboardView view, File customTemplate) {
        try {
            Files.createDirectories(outputDir.toPath());
            for (ResolvedLens lens : view.lenses()) {
                Files.write(outputDir.toPath().resolve(LensNaming.cssFileName(lens.id())), lens.content());
            }
            File dashboardFile = new File(outputDir, "dashboard.html");
            Files.writeString(dashboardFile.toPath(), render(view, customTemplate), StandardCharsets.UTF_8);
            return dashboardFile;
        } catch (IOException e) {
            throw new GradleException("trackerLens: failed to write dashboard to " + outputDir, e);
        }
    }

    private String render(DashboardView view, File customTemplate) {
        Map<String, Object> context = DashboardTemplateContext.build(view);
        String templateName = customTemplate != null ? customTemplate.getName() : DEFAULT_TEMPLATE_RESOURCE;

        try (Reader templateReader = openTemplate(customTemplate); StringWriter writer = new StringWriter()) {
            Mustache mustache = new DefaultMustacheFactory().compile(templateReader, templateName);
            mustache.execute(writer, context).flush();
            return writer.toString();
        } catch (IOException e) {
            throw new GradleException("trackerLens: failed to render dashboard template '" + templateName + "': "
                    + e.getMessage(), e);
        } catch (MustacheException e) {
            throw new GradleException("trackerLens: failed to render dashboard template '" + templateName + "': "
                    + e.getMessage(), e);
        }
    }

    private Reader openTemplate(File customTemplate) throws IOException {
        if (customTemplate != null) {
            return new FileReader(customTemplate, StandardCharsets.UTF_8);
        }
        InputStream stream = getClass().getClassLoader().getResourceAsStream(DEFAULT_TEMPLATE_RESOURCE);
        if (stream == null) {
            throw new GradleException("trackerLens: missing bundled dashboard template: " + DEFAULT_TEMPLATE_RESOURCE);
        }
        return new InputStreamReader(stream, StandardCharsets.UTF_8);
    }
}
