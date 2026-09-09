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
package org.apache.maven.impl.resolver.type;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.api.JavaPathType;
import org.apache.maven.api.PathType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultTypeProviderTest {

    private static final Map<String, PathType> PATH_TYPE_NAMES = Map.of(
            "classes", JavaPathType.CLASSES,
            "modules", JavaPathType.MODULES,
            "patch module", JavaPathType.PATCH_MODULE,
            "processor classes", JavaPathType.PROCESSOR_CLASSES,
            "processor modules", JavaPathType.PROCESSOR_MODULES);

    @Test
    void testDocumentationConsistency() throws Exception {
        Map<String, DefaultType> types =
                new DefaultTypeProvider().types().stream().collect(Collectors.toMap(DefaultType::id, t -> t));

        Path doc = Path.of(System.getProperty("basedir", ""), "src/site/markdown/dependency-types.md");
        List<String> lines = Files.readAllLines(doc);

        Set<String> documentedTypes = new LinkedHashSet<>();

        for (String line : lines) {
            if (!line.startsWith("|") || line.startsWith("|type|") || line.startsWith("|:-")) {
                continue; // not a row, or the header row, or the alignment row beneath it
            }

            String[] cols = line.split("\\|");
            String typeId = trimApt(cols[1]);
            if (typeId == null) {
                continue;
            }
            // a row carrying only a label groups the rows beneath it; APT wrote those as a
            // second header row, which a Markdown table cannot express
            if (Arrays.stream(cols).skip(2).allMatch(c -> trimApt(c) == null)) {
                continue;
            }

            documentedTypes.add(typeId);

            String classifier = trimApt(cols[2]);
            String extension = trimApt(cols[3]);
            if ("= type".equals(extension)) {
                extension = typeId;
            }
            String language = trimApt(cols[4]);
            String pathTypesStr = trimApt(cols[5]);
            String includesDependencies = trimApt(cols[6]);

            DefaultType type = types.get(typeId);
            assertNotNull(type, "Type not found in provider: " + typeId);
            assertEquals(extension, type.getExtension(), typeId + " extension");
            assertEquals(classifier, type.getClassifier(), typeId + " classifier");
            assertEquals(language, type.getLanguage().id(), typeId + " language");
            assertEquals(
                    type.isIncludesDependencies() ? "true" : null,
                    includesDependencies,
                    typeId + " includesDependencies");
            assertEquals(parsePathTypes(pathTypesStr), type.getPathTypes(), typeId + " pathTypes");
        }

        Set<String> undocumented = new LinkedHashSet<>(types.keySet());
        undocumented.removeAll(documentedTypes);
        assertTrue(undocumented.isEmpty(), "Types in provider but not documented: " + undocumented);
    }

    private Set<PathType> parsePathTypes(String pathTypesStr) {
        Set<PathType> result = new LinkedHashSet<>();
        if (pathTypesStr != null) {
            for (String name : pathTypesStr.split(",")) {
                name = name.trim();
                PathType pt = PATH_TYPE_NAMES.get(name);
                if (pt != null) {
                    result.add(pt);
                }
            }
        }
        return result;
    }

    /** Strips the Markdown emphasis and code markers the table uses around a cell value. */
    private String trimApt(String content) {
        content = content.replace('`', ' ')
                .replace('_', ' ')
                .replace('<', ' ')
                .replace('>', ' ')
                .replace('*', ' ')
                .trim();
        return content.isEmpty() ? null : content;
    }
}
