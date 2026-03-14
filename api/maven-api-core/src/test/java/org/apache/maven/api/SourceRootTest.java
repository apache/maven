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
package org.apache.maven.api;

import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Collection;
import java.util.Optional;

import org.apache.maven.api.model.Build;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SourceRootTest implements SourceRoot {
    private ProjectScope scope;

    private Language language;

    private String moduleName;

    @Override
    public ProjectScope scope() {
        return (scope != null) ? scope : SourceRoot.super.scope();
    }

    @Override
    public Language language() {
        return (language != null) ? language : SourceRoot.super.language();
    }

    @Override
    public Optional<String> module() {
        return Optional.ofNullable(moduleName);
    }

    @Override
    public PathMatcher matcher(Collection<String> defaultIncludes, boolean useDefaultExcludes) {
        return null; // Not used for this test.
    }

    @Test
    void testDirectory() {
        assertEquals(Path.of("src", "main", "java"), directory());

        scope = ProjectScope.TEST;
        assertEquals(Path.of("src", "test", "java"), directory());

        moduleName = "org.foo";
        assertEquals(Path.of("src", "org.foo", "test", "java"), directory());
    }

    @Test
    void testTargetPath() {
        Build build = mock(Build.class);
        when(build.getDirectory()).thenReturn("target");
        when(build.getOutputDirectory()).thenReturn("target/classes");
        when(build.getTestOutputDirectory()).thenReturn("target/test-classes");

        Project project = mock(Project.class);
        when(project.getBuild()).thenReturn(build);
        when(project.getBasedir()).thenReturn(Path.of("myproject"));
        when(project.getOutputDirectory(any(ProjectScope.class))).thenCallRealMethod();

        assertEquals(Path.of("myproject", "target", "classes"), targetPath(project));

        scope = ProjectScope.TEST;
        assertEquals(Path.of("myproject", "target", "test-classes"), targetPath(project));
    }
}
