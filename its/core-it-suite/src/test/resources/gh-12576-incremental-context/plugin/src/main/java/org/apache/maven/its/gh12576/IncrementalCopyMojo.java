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
package org.apache.maven.its.gh12576;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

import org.apache.maven.api.Project;
import org.apache.maven.api.build.incremental.IncrementalContext;
import org.apache.maven.api.build.incremental.Input;
import org.apache.maven.api.build.incremental.Output;
import org.apache.maven.api.build.incremental.Status;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.plugin.Log;
import org.apache.maven.api.plugin.annotations.Mojo;
import org.apache.maven.api.plugin.annotations.Parameter;

/**
 * A simple incremental file-copy mojo that uses the IncrementalContext API.
 * <p>
 * On initial build, all source files are NEW and get copied.
 * On subsequent builds with no changes, all files are UNMODIFIED and skipped.
 * When a source file is modified, only that file is re-copied.
 */
@Mojo(name = "copy", defaultPhase = "process-sources")
public class IncrementalCopyMojo implements org.apache.maven.api.plugin.Mojo {

    @Inject
    private IncrementalContext buildContext;

    @Inject
    private Log log;

    @Inject
    private Project project;

    @Parameter(defaultValue = "src/main/data")
    private String sourceDirectory;

    @Parameter(defaultValue = "${project.build.directory}/data")
    private String outputDirectory;

    @Override
    public void execute() {
        Path srcDir = project.getBasedir().resolve(sourceDirectory);
        Path outDir = Paths.get(outputDirectory);

        if (!Files.isDirectory(srcDir)) {
            log.info("[incremental] Source directory does not exist: " + srcDir);
            return;
        }

        Collection<? extends Input> inputs = buildContext.registerAndProcessInputs(srcDir, null, null);

        int processed = 0;
        int skipped = 0;
        for (Input input : inputs) {
            Path relativePath = srcDir.relativize(input.getPath());
            if (input.getStatus() == Status.NEW || input.getStatus() == Status.MODIFIED) {
                Path outputPath = outDir.resolve(relativePath);
                Output output = input.associateOutput(outputPath);
                try (OutputStream os = output.newOutputStream()) {
                    Files.copy(input.getPath(), os);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to copy " + input.getPath(), e);
                }
                log.info("[incremental] Processed " + relativePath + " (" + input.getStatus() + ")");
                processed++;
            } else {
                skipped++;
            }
        }

        log.info("[incremental] Summary: " + processed + " processed, " + skipped + " skipped");
    }
}
