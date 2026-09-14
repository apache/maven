/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.api.Constants;
import org.apache.maven.api.Session;
import org.apache.maven.api.services.BuilderProblem;
import org.apache.maven.api.services.Interpolator;
import org.apache.maven.api.services.SettingsBuilderException;
import org.apache.maven.api.services.SettingsBuilderRequest;
import org.apache.maven.api.services.SettingsBuilderResult;
import org.apache.maven.api.services.Source;
import org.apache.maven.api.services.Sources;
import org.apache.maven.api.services.xml.SettingsXmlFactory;
import org.apache.maven.api.settings.Server;
import org.apache.maven.api.settings.Settings;
import org.apache.maven.api.spi.SettingsParser;
import org.apache.maven.api.spi.SettingsParserException;
import org.apache.maven.di.Injector;
import org.apache.maven.impl.model.DefaultInterpolator;
import org.codehaus.plexus.components.secdispatcher.Dispatcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultSettingsParserTest {
    @TempDir
    Path directory;

    @Test
    void customSettings() throws Exception {
        Path source = directory.resolve("settings.properties");
        Files.writeString(source, propertiesSettings() + "\n");
        var result = build(Sources.fromPath(source), Map.of("properties", new PropertiesSettingsParser()));
        assertEquals(
                directory.resolve("repository").toString(),
                result.getEffectiveSettings().getLocalRepository());
    }

    @Test
    void sourceWithoutBackingFile() throws Exception {
        var result = build(
                source("memory.properties", propertiesSettings()),
                Map.of("properties", new PropertiesSettingsParser()));
        assertEquals(
                directory.resolve("repository").toString(),
                result.getEffectiveSettings().getLocalRepository());
    }

    @Test
    void xmlFallbackDoesNotRequireXmlExtension() throws Exception {
        var result = build(
                source("settings.conf", "<settings><offline>true</offline></settings>"),
                Map.of("properties", new PropertiesSettingsParser()));
        assertTrue(result.getEffectiveSettings().isOffline());
    }

    @Test
    void xmlStrictFailureBecomesWarningAfterLenientParsing() throws Exception {
        var result = build(source("settings.xml", "<settings>\n<unknown/>\n</settings>"), Map.of());
        var problems =
                result.getProblems().problems(BuilderProblem.Severity.WARNING).toList();
        assertEquals(1, problems.size());
        assertEquals(2, problems.get(0).getLineNumber());
        assertTrue(problems.get(0).getColumnNumber() > 0);
    }

    @Test
    void malformedXmlIsFatal() throws Exception {
        var error = assertThrows(
                SettingsBuilderException.class, () -> build(source("settings.xml", "<settings>\n<offline>"), Map.of()));
        assertTrue(error.getMessage().contains("Non-parseable settings settings.xml"));
        assertTrue(error.getProblemCollector()
                .problems(BuilderProblem.Severity.FATAL)
                .allMatch(problem -> problem.getLineNumber() > 0));
    }

    @Test
    void customStrictFailureRetriesSameParser() throws Exception {
        var calls = new ArrayList<Boolean>();
        SettingsParser parser = new PropertiesSettingsParser() {
            @Override
            public Settings parse(Source source, Map<String, ?> options) {
                calls.add((Boolean) options.get(STRICT));
                if (!Boolean.FALSE.equals(options.get(STRICT))) {
                    throw new SettingsParserException("Unknown setting", 3, 7, null);
                }
                return Settings.newInstance().withOffline(true);
            }
        };
        var result = build(source("settings.properties", "unknown=value"), Map.of("properties", parser));
        assertEquals(List.of(true, false), calls);
        assertTrue(result.getEffectiveSettings().isOffline());
        var problem = result.getProblems()
                .problems(BuilderProblem.Severity.WARNING)
                .findFirst()
                .orElseThrow();
        assertEquals("Unknown setting", problem.getMessage());
        assertEquals(3, problem.getLineNumber());
        assertEquals(7, problem.getColumnNumber());
    }

    @Test
    void selectedParserFailureDoesNotFallBackToXml() throws Exception {
        SettingsParser parser = mock(SettingsParser.class);
        when(parser.supports(any())).thenReturn(true);
        when(parser.parse(any(), any())).thenThrow(new SettingsParserException("Invalid custom settings", 4, 2, null));
        var error = assertThrows(
                SettingsBuilderException.class,
                () -> build(source("settings.properties", "<settings/>"), Map.of("properties", parser)));
        assertTrue(error.getMessage().contains("Invalid custom settings"));
        var problem = error.getProblemCollector()
                .problems(BuilderProblem.Severity.FATAL)
                .findFirst()
                .orElseThrow();
        assertEquals(4, problem.getLineNumber());
        assertEquals(2, problem.getColumnNumber());
        verify(parser, times(2)).parse(any(), any());
    }

    @Test
    void conflictingParsersAreReportedBeforeParsing() throws Exception {
        SettingsParser first = mock(SettingsParser.class);
        SettingsParser second = mock(SettingsParser.class);
        when(first.supports(any())).thenReturn(true);
        when(second.supports(any())).thenReturn(true);
        var error = assertThrows(
                SettingsBuilderException.class,
                () -> build(source("settings.properties", "<settings/>"), Map.of("second", second, "first", first)));
        assertTrue(error.getMessage().contains("Multiple settings parsers support this source: first, second"));
        verify(first, never()).parse(any(), any());
        verify(second, never()).parse(any(), any());
    }

    @Test
    void unreadableXmlSourceIsNotRetried() throws Exception {
        Source source = mock(Source.class);
        when(source.getLocation()).thenReturn("unreadable.xml");
        when(source.openStream()).thenThrow(new IOException("Read failed"));
        var error = assertThrows(SettingsBuilderException.class, () -> build(source, Map.of()));
        assertTrue(error.getMessage().contains("Non-readable settings unreadable.xml"));
        verify(source).openStream();
    }

    @Test
    void customIoFailureIsNotRetried() throws Exception {
        SettingsParser parser = mock(SettingsParser.class);
        when(parser.supports(any())).thenReturn(true);
        when(parser.parse(any(), any())).thenThrow(new IOException("Read failed"));
        var error = assertThrows(
                SettingsBuilderException.class,
                () -> build(source("settings.properties", ""), Map.of("properties", parser)));
        assertTrue(error.getMessage().contains("Non-readable settings settings.properties"));
        verify(parser).parse(any(), any());
    }

    @Test
    void xmlStreamsAreClosedAfterBothAttempts() throws Exception {
        Source source = mock(Source.class);
        when(source.getLocation()).thenReturn("settings.xml");
        var streams = new ArrayList<InputStream>();
        when(source.openStream()).thenAnswer(invocation -> {
            InputStream stream = mock(InputStream.class);
            when(stream.read(any(byte[].class), anyInt(), anyInt())).thenThrow(new IOException("Read failed"));
            streams.add(stream);
            return stream;
        });
        assertThrows(SettingsBuilderException.class, () -> build(source, Map.of()));
        assertFalse(streams.isEmpty());
        for (InputStream stream : streams) {
            verify(stream).close();
        }
    }

    @Test
    void formatsShareInterpolationAndMergePrecedence() throws Exception {
        SettingsParser parser = mock(SettingsParser.class);
        when(parser.supports(any()))
                .thenAnswer(call -> ((Source) call.getArgument(0)).getLocation().endsWith(".custom"));
        when(parser.parse(any(), any()))
                .thenReturn(Settings.newBuilder()
                        .servers(List.of(Server.newBuilder()
                                .id("repository")
                                .username("${account}")
                                .build()))
                        .build());
        var result = builder(Map.of("custom", parser))
                .build(SettingsBuilderRequest.builder()
                        .session(mock(Session.class))
                        .installationSettingsSource(source(
                                "installation.xml",
                                "<settings><servers><server><id>repository</id>"
                                        + "<username>installation</username></server></servers></settings>"))
                        .projectSettingsSource(source(
                                "project.xml",
                                "<settings><servers><server><id>repository</id>"
                                        + "<username>project</username></server></servers></settings>"))
                        .userSettingsSource(source("user.custom", ""))
                        .interpolationSource(key -> "account".equals(key) ? "user<&>" : null)
                        .build());
        assertEquals(
                "user<&>", result.getEffectiveSettings().getServers().get(0).getUsername());
    }

    @Test
    void projectSettingsRestrictionsApplyToCustomParser() throws Exception {
        SettingsParser parser = mock(SettingsParser.class);
        when(parser.supports(any())).thenReturn(true);
        when(parser.parse(any(), any())).thenReturn(Settings.newInstance().withOffline(true));
        var result = builder(Map.of("custom", parser))
                .build(SettingsBuilderRequest.builder()
                        .session(mock(Session.class))
                        .projectSettingsSource(source("project.custom", ""))
                        .build());
        assertFalse(result.getEffectiveSettings().isOffline());
        assertTrue(result.getProblems().hasWarningProblems());
    }

    @Test
    void customSettingsDecryptionFailureDoesNotExposeCredentials() throws Exception {
        String encrypted = "{L6L/HbmrY+cH+sNkphn-corrupted-q3fguYepTpM04WlIXb8nB1pk=}";
        SettingsParser parser = mock(SettingsParser.class);
        when(parser.supports(any())).thenReturn(true);
        when(parser.parse(any(), any()))
                .thenReturn(Settings.newBuilder()
                        .servers(List.of(Server.newBuilder()
                                .id("private-repository")
                                .password(encrypted)
                                .build()))
                        .build());
        Session session = mock(Session.class);
        when(session.getEffectiveProperties())
                .thenReturn(Map.of(
                        Constants.MAVEN_SETTINGS_SECURITY,
                        Path.of("src/test/resources/settings/settings-security-decrypt.xml")
                                .toAbsolutePath()
                                .toString()));
        var builder = new DefaultSettingsBuilder(
                new DefaultSettingsXmlFactory(),
                new DefaultInterpolator(),
                Map.of("test", mock(Dispatcher.class)),
                Map.of("custom", parser));
        var request = SettingsBuilderRequest.builder()
                .session(session)
                .userSettingsSource(source("settings.custom", ""))
                .build();
        var error = assertThrows(SettingsBuilderException.class, () -> builder.build(request));
        assertTrue(error.getMessage().contains("Could not decrypt password"));
        assertTrue(error.getMessage().contains("for server private-repository"));
        assertFalse(error.getMessage().contains(encrypted));
    }

    @Test
    void nativeContainerWithoutCustomParsersUsesXml() throws Exception {
        Injector injector = Injector.create()
                .bindInstance(SettingsXmlFactory.class, new DefaultSettingsXmlFactory())
                .bindInstance(Interpolator.class, new DefaultInterpolator())
                .bindInstance(Dispatcher.class, mock(Dispatcher.class));
        injector.bindImplicit(DefaultSettingsBuilder.class);
        Session session = mock(Session.class);
        when(session.getEffectiveProperties()).thenReturn(Map.of("user.home", directory.toString()));
        var result = injector.getInstance(DefaultSettingsBuilder.class)
                .build(SettingsBuilderRequest.builder()
                        .session(session)
                        .userSettingsSource(source("settings.xml", "<settings><offline>true</offline></settings>"))
                        .build());
        assertTrue(result.getEffectiveSettings().isOffline());
    }

    @Test
    void unnamedParserWorksInNativeContainer() throws Exception {
        Injector injector = Injector.create()
                .bindInstance(SettingsXmlFactory.class, new DefaultSettingsXmlFactory())
                .bindInstance(Interpolator.class, new DefaultInterpolator())
                .bindInstance(PropertiesSettingsParser.class, new PropertiesSettingsParser());
        injector.bindImplicit(Dispatcher.class);
        injector.bindImplicit(DefaultSettingsBuilder.class);
        var result = injector.getInstance(DefaultSettingsBuilder.class)
                .build(SettingsBuilderRequest.builder()
                        .session(mock(Session.class))
                        .userSettingsSource(source("memory.properties", propertiesSettings()))
                        .build());
        assertEquals(
                directory.resolve("repository").toString(),
                result.getEffectiveSettings().getLocalRepository());
    }

    @Test
    void unnamedParserConflictIsReported() throws Exception {
        var parsers = new HashMap<String, SettingsParser>();
        parsers.put(null, new PropertiesSettingsParser());
        parsers.put("properties", new PropertiesSettingsParser());
        var error =
                assertThrows(SettingsBuilderException.class, () -> build(source("settings.properties", ""), parsers));
        assertTrue(error.getMessage().contains("Multiple settings parsers support this source: <unnamed>, properties"));
    }

    private String propertiesSettings() throws IOException {
        Properties properties = new Properties();
        properties.setProperty(
                "localRepository", directory.resolve("repository").toString());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        properties.store(output, null);
        return output.toString(StandardCharsets.ISO_8859_1);
    }

    private DefaultSettingsBuilder builder(Map<String, SettingsParser> parsers) {
        return new DefaultSettingsBuilder(
                new DefaultSettingsXmlFactory(), new DefaultInterpolator(), Map.of(), parsers);
    }

    private SettingsBuilderResult build(Source source, Map<String, SettingsParser> parsers) {
        return builder(parsers)
                .build(SettingsBuilderRequest.builder()
                        .session(mock(Session.class))
                        .userSettingsSource(source)
                        .build());
    }

    private Source source(String location, String content) throws IOException {
        Source source = mock(Source.class);
        when(source.getLocation()).thenReturn(location);
        when(source.openStream())
                .thenAnswer(invocation -> new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
        return source;
    }

    private static class PropertiesSettingsParser implements SettingsParser {
        @Override
        public boolean supports(Source source) {
            return source.getLocation().endsWith(".properties");
        }

        @Override
        public Settings parse(Source source, Map<String, ?> options) throws IOException {
            Properties properties = new Properties();
            try (InputStream stream = source.openStream()) {
                properties.load(stream);
            }
            return Settings.newInstance().withLocalRepository(properties.getProperty("localRepository"));
        }
    }
}
