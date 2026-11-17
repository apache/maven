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
package org.apache.maven.plugin.coreit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.api.Project;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.plugin.Log;
import org.apache.maven.api.plugin.MojoException;
import org.apache.maven.api.plugin.annotations.Mojo;
import org.apache.maven.api.plugin.annotations.Parameter;

/**
 * Creates a text file.
 * <p>
 * This mojo uses the Maven 4 API so that it can be loaded from reactor-built
 * classes (target/classes/) without requiring the Maven 3 compat jars.
 *
 * @author Benjamin Bentmann
 */
@Mojo(name = "touch", defaultPhase = "validate")
public class TouchMojo implements org.apache.maven.api.plugin.Mojo {

    @Inject
    private Log log;

    @Inject
    private Project project;

    /**
     * The path to the output file, relative to the project base directory.
     */
    @Parameter(property = "touch.file", defaultValue = "target/touch.txt")
    private String file;

    /**
     * Runs this mojo.
     *
     * @throws MojoException If the output file could not be created.
     */
    @Override
    public void execute() {
        Path path = project.getBasedir().resolve(file);
        log.info("[MAVEN-CORE-IT-LOG] Creating output file: " + path.toAbsolutePath());

        try {
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) {
                Files.createFile(path);
            }
        } catch (IOException e) {
            throw new MojoException("Output file could not be created: " + path, e);
        }
    }
}
