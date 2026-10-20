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
package org.apache.maven.its.gh13644;

import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.api.Project;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.plugin.Log;
import org.apache.maven.api.plugin.annotations.Mojo;
import org.apache.maven.api.plugin.annotations.Parameter;

/**
 * A minimal V4 mojo whose descriptor is generated entirely by
 * {@code maven-api-plugin-processor} — no {@code maven-plugin-plugin} involved.
 *
 * @since 1.0-SNAPSHOT
 */
@Mojo(name = "touch", defaultPhase = "compile")
public class TouchMojo implements org.apache.maven.api.plugin.Mojo {

    /**
     * Output file name.
     *
     * @since 1.0-SNAPSHOT
     */
    @Parameter(property = "gh13644.outputFile", defaultValue = "touch.txt")
    private String outputFile;

    @Inject
    private Log log;

    @Inject
    private Project project;

    @Override
    public void execute() throws Exception {
        log.info("[GH-13644] touch goal executed via annotation-processor-generated descriptor");
        Path targetDir = project.getBasedir().resolve("target");
        Files.createDirectories(targetDir);
        Path out = targetDir.resolve(outputFile);
        if (!Files.exists(out)) {
            Files.createFile(out);
        }
    }
}
