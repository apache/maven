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
package org.apache.maven.impl.model;

import java.nio.file.Path;

import org.apache.maven.api.model.Build;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Resource;
import org.apache.maven.api.model.Source;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultModelPathTranslatorTest {

    private DefaultModelPathTranslator translator;
    private Path basedir;

    @BeforeEach
    void setUp() {
        translator = new DefaultModelPathTranslator(new DefaultPathTranslator());
        basedir = Path.of("/home/user/myproject").toAbsolutePath();
    }

    @Test
    void sourceDirectoryIsAlignedToBasedir() {
        Source source = Source.newBuilder().directory("src/main/java").build();
        Model model = Model.newBuilder()
                .build(Build.newBuilder().sources(java.util.List.of(source)).build())
                .build();

        Model result = translator.alignToBaseDirectory(model, basedir, null);

        String dir = result.getBuild().getSources().get(0).getDirectory();
        assertTrue(Path.of(dir).isAbsolute(), "directory should be absolute after alignment");
        assertTrue(Path.of(dir).endsWith(Path.of("src/main/java")), "directory should end with original path");
    }

    @Test
    void sourceTargetPathIsNotAlignedToBasedir() {
        Source source = Source.newBuilder()
                .directory("src/main/resources")
                .targetPath("META-INF/resources")
                .build();
        Model model = Model.newBuilder()
                .build(Build.newBuilder().sources(java.util.List.of(source)).build())
                .build();

        Model result = translator.alignToBaseDirectory(model, basedir, null);

        String targetPath = result.getBuild().getSources().get(0).getTargetPath();
        assertEquals("META-INF/resources", targetPath, "targetPath should remain relative");
    }

    @Test
    void sourceTargetPathDotPrefixRemainsRelative() {
        Source source = Source.newBuilder()
                .directory("src/main/resources")
                .targetPath(".grammar")
                .build();
        Model model = Model.newBuilder()
                .build(Build.newBuilder().sources(java.util.List.of(source)).build())
                .build();

        Model result = translator.alignToBaseDirectory(model, basedir, null);

        String targetPath = result.getBuild().getSources().get(0).getTargetPath();
        assertEquals(".grammar", targetPath, "dot-prefixed targetPath should remain relative");
    }

    @Test
    void resourceDirectoryIsAlignedButTargetPathIsNot() {
        Resource resource = Resource.newBuilder()
                .directory("src/main/resources")
                .targetPath("custom-output")
                .build();
        Model model = Model.newBuilder()
                .build(Build.newBuilder().resources(java.util.List.of(resource)).build())
                .build();

        Model result = translator.alignToBaseDirectory(model, basedir, null);

        Resource aligned = result.getBuild().getResources().get(0);
        assertTrue(
                Path.of(aligned.getDirectory()).isAbsolute(), "resource directory should be absolute after alignment");
        assertEquals("custom-output", aligned.getTargetPath(), "resource targetPath should remain relative");
    }
}
