package org.slf4j.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

class MavenSimpleLoggerTest {

    @Test
    void includesCauseAndSuppressedExceptionsWhenWritingThrowables(TestInfo testInfo) throws Exception {
        Exception causeOfSuppressed = new NoSuchElementException("cause of suppressed");
        Exception suppressed = new IllegalStateException("suppressed", causeOfSuppressed);
        suppressed.addSuppressed(new IllegalArgumentException("suppressed suppressed", new ArrayIndexOutOfBoundsException("suppressed suppressed cause")));
        Exception cause = new IllegalArgumentException("cause");
        cause.addSuppressed(suppressed);
        Exception throwable = new RuntimeException("top-level", cause);

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        new MavenSimpleLogger("logger").writeThrowable(throwable, new PrintStream(output));

        String actual = output.toString(UTF_8.name());
        List<String> actualLines = Arrays.asList(actual.split(System.lineSeparator()));

        Class<?> testClass = testInfo.getTestClass().get();
        String testMethodName = testInfo.getTestMethod().get().getName();
        String testClassStackTraceLinePattern = "at " + testClass.getName() + "." + testMethodName + " \\(" + testClass.getSimpleName() + ".java:\\d+\\)";
        List<String> expectedLines = Arrays.asList(
                "java.lang.RuntimeException: top-level",
                "    " + testClassStackTraceLinePattern,
                ">> stacktrace >>",
                "Caused by: java.lang.IllegalArgumentException: cause",
                "    " + testClassStackTraceLinePattern,
                ">> stacktrace >>",
                "    Suppressed: java.lang.IllegalStateException: suppressed",
                "        " + testClassStackTraceLinePattern,
                ">> stacktrace >>",
                "        Suppressed: java.lang.IllegalArgumentException: suppressed suppressed",
                "            " + testClassStackTraceLinePattern,
                ">> stacktrace >>",
                "        Caused by: java.lang.ArrayIndexOutOfBoundsException: suppressed suppressed cause",
                "            " + testClassStackTraceLinePattern,
                ">> stacktrace >>",
                "    Caused by: java.util.NoSuchElementException: cause of suppressed",
                "        " + testClassStackTraceLinePattern,
                ">> stacktrace >>"
        );

        assertLinesMatch(expectedLines, actualLines);
    }
}
