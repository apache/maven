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
package org.apache.maven.model.locator;

import javax.inject.Named;
import javax.inject.Singleton;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Locates a POM file within a project base directory.
 *
 */
@Named
@Singleton
public class DefaultModelLocator implements ModelLocator {

    @Deprecated
    @Override
    public File locatePom(File projectDirectory) {
        Path path = locatePom(projectDirectory != null ? projectDirectory.toPath() : null);
        return path != null ? path.toFile() : null;
    }

    @Override
    public Path locatePom(Path projectDirectory) {
        return projectDirectory != null ? projectDirectory : Paths.get(System.getProperty("user.dir"));
    }

    @Deprecated
    @Override
    public File locateExistingPom(File project) {
        Path path = locateExistingPom(project != null ? project.toPath() : null);
        return path != null ? path.toFile() : null;
    }

    @Override
    public Path locateExistingPom(Path project) {
        if (project == null || Files.isDirectory(project)) {
            project = locatePom(project);
        }
        if (Files.isDirectory(project)) {
            Path pom = project.resolve("pom.xml");
            return Files.isRegularFile(pom) ? pom : null;
        } else if (Files.isRegularFile(project)) {
            return project;
        } else {
            return null;
        }
    }
}
