package com.arc_e_tect.gradle.gherkin.console;

import org.gradle.api.logging.LogLevel;
import org.gradle.api.logging.Logger;
import org.slf4j.Marker;
import org.slf4j.helpers.MessageFormatter;

import java.util.ArrayList;
import java.util.List;

/**
 * Hand-written {@link Logger} test double that records every message passed to
 * {@code lifecycle}/{@code warn}/{@code info} and no-ops everything else - this codebase's tests
 * use hand-written fakes rather than a mocking framework.
 *
 * <p>Public rather than package-private, and reused as-is from other test packages within this
 * module (e.g. the {@code GenerateFeatureDocsTask} component test), instead of duplicating this
 * ~280-line fake per package.</p>
 */
public class RecordingLogger implements Logger {

    private final List<String> lifecycleMessages = new ArrayList<>();
    private final List<String> warnMessages = new ArrayList<>();
    private final List<String> infoMessages = new ArrayList<>();
    private boolean infoEnabled = false;

    /** Creates a new {@code RecordingLogger}, with {@code --info} logging disabled by default. */
    public RecordingLogger() {}

    /**
     * Every message passed to {@code lifecycle(...)}, in call order.
     *
     * @return the recorded lifecycle messages
     */
    public List<String> lifecycleMessages() {
        return lifecycleMessages;
    }

    /**
     * Every message passed to {@code warn(...)}, in call order.
     *
     * @return the recorded warning messages
     */
    public List<String> warnMessages() {
        return warnMessages;
    }

    /**
     * Every message passed to {@code info(...)}, in call order - recorded regardless of
     * {@link #setInfoEnabled(boolean)}, so tests can assert on what would have been logged even
     * when simulating {@code --info} being off.
     *
     * @return the recorded info messages
     */
    public List<String> infoMessages() {
        return infoMessages;
    }

    /**
     * Controls what {@link #isInfoEnabled()} returns, so tests can simulate the build being run
     * with (or without) {@code --info}.
     *
     * @param infoEnabled whether {@link #isInfoEnabled()} should report info logging as enabled
     */
    public void setInfoEnabled(boolean infoEnabled) {
        this.infoEnabled = infoEnabled;
    }

    @Override
    public void lifecycle(String message) {
        lifecycleMessages.add(message);
    }

    @Override
    public void lifecycle(String message, Object... objects) {
        lifecycleMessages.add(message);
    }

    @Override
    public void lifecycle(String message, Throwable throwable) {
        lifecycleMessages.add(message);
    }

    @Override
    public boolean isLifecycleEnabled() {
        return true;
    }

    @Override
    public void quiet(String message) {}

    @Override
    public void quiet(String message, Object... objects) {}

    @Override
    public void quiet(String message, Throwable throwable) {}

    @Override
    public boolean isQuietEnabled() {
        return false;
    }

    @Override
    public boolean isEnabled(LogLevel level) {
        return false;
    }

    @Override
    public void log(LogLevel level, String message) {}

    @Override
    public void log(LogLevel level, String message, Object... objects) {}

    @Override
    public void log(LogLevel level, String message, Throwable throwable) {}

    @Override
    public String getName() {
        return "RecordingLogger";
    }

    @Override
    public boolean isTraceEnabled() {
        return false;
    }

    @Override
    public void trace(String msg) {}

    @Override
    public void trace(String format, Object arg) {}

    @Override
    public void trace(String format, Object arg1, Object arg2) {}

    @Override
    public void trace(String format, Object... arguments) {}

    @Override
    public void trace(String msg, Throwable t) {}

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return false;
    }

    @Override
    public void trace(Marker marker, String msg) {}

    @Override
    public void trace(Marker marker, String format, Object arg) {}

    @Override
    public void trace(Marker marker, String format, Object arg1, Object arg2) {}

    @Override
    public void trace(Marker marker, String format, Object... argArray) {}

    @Override
    public void trace(Marker marker, String msg, Throwable t) {}

    @Override
    public boolean isDebugEnabled() {
        return false;
    }

    @Override
    public void debug(String msg) {}

    @Override
    public void debug(String format, Object arg) {}

    @Override
    public void debug(String format, Object arg1, Object arg2) {}

    @Override
    public void debug(String format, Object... arguments) {}

    @Override
    public void debug(String msg, Throwable t) {}

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return false;
    }

    @Override
    public void debug(Marker marker, String msg) {}

    @Override
    public void debug(Marker marker, String format, Object arg) {}

    @Override
    public void debug(Marker marker, String format, Object arg1, Object arg2) {}

    @Override
    public void debug(Marker marker, String format, Object... arguments) {}

    @Override
    public void debug(Marker marker, String msg, Throwable t) {}

    @Override
    public boolean isInfoEnabled() {
        return infoEnabled;
    }

    @Override
    public void info(String msg) {
        infoMessages.add(msg);
    }

    @Override
    public void info(String format, Object arg) {
        infoMessages.add(MessageFormatter.format(format, arg).getMessage());
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        infoMessages.add(MessageFormatter.format(format, arg1, arg2).getMessage());
    }

    @Override
    public void info(String format, Object... arguments) {
        infoMessages.add(MessageFormatter.arrayFormat(format, arguments).getMessage());
    }

    @Override
    public void info(String msg, Throwable t) {
        infoMessages.add(msg);
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return false;
    }

    @Override
    public void info(Marker marker, String msg) {}

    @Override
    public void info(Marker marker, String format, Object arg) {}

    @Override
    public void info(Marker marker, String format, Object arg1, Object arg2) {}

    @Override
    public void info(Marker marker, String format, Object... arguments) {}

    @Override
    public void info(Marker marker, String msg, Throwable t) {}

    @Override
    public boolean isWarnEnabled() {
        return true;
    }

    @Override
    public void warn(String msg) {
        warnMessages.add(msg);
    }

    @Override
    public void warn(String format, Object arg) {
        warnMessages.add(MessageFormatter.format(format, arg).getMessage());
    }

    @Override
    public void warn(String format, Object... arguments) {
        warnMessages.add(MessageFormatter.arrayFormat(format, arguments).getMessage());
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        warnMessages.add(MessageFormatter.format(format, arg1, arg2).getMessage());
    }

    @Override
    public void warn(String msg, Throwable t) {
        warnMessages.add(msg);
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return false;
    }

    @Override
    public void warn(Marker marker, String msg) {}

    @Override
    public void warn(Marker marker, String format, Object arg) {}

    @Override
    public void warn(Marker marker, String format, Object arg1, Object arg2) {}

    @Override
    public void warn(Marker marker, String format, Object... arguments) {}

    @Override
    public void warn(Marker marker, String msg, Throwable t) {}

    @Override
    public boolean isErrorEnabled() {
        return false;
    }

    @Override
    public void error(String msg) {}

    @Override
    public void error(String format, Object arg) {}

    @Override
    public void error(String format, Object arg1, Object arg2) {}

    @Override
    public void error(String format, Object... arguments) {}

    @Override
    public void error(String msg, Throwable t) {}

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return false;
    }

    @Override
    public void error(Marker marker, String msg) {}

    @Override
    public void error(Marker marker, String format, Object arg) {}

    @Override
    public void error(Marker marker, String format, Object arg1, Object arg2) {}

    @Override
    public void error(Marker marker, String format, Object... arguments) {}

    @Override
    public void error(Marker marker, String msg, Throwable t) {}
}
