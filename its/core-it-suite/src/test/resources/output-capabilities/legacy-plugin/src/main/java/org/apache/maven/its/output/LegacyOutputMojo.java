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
package org.apache.maven.its.output;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/** This fixture must compile and run using only Maven 3.6.3 APIs. */
@Mojo(name = "report", threadSafe = true)
public class LegacyOutputMojo extends AbstractMojo {
    @Parameter(defaultValue = "${session}", readonly = true)
    private MavenSession session;

    @Parameter(defaultValue = "${project.build.directory}/legacy-output-capabilities.properties", readonly = true)
    private File output;

    @Parameter(property = "capabilities.available", defaultValue = "true")
    private boolean expectedAvailable;

    @Override
    public void execute() throws MojoExecutionException {
        Object value = session.getRequest().getData().get("maven.logging.outputCapabilities");
        boolean available = value instanceof Map;
        if (expectedAvailable != available) {
            throw new MojoExecutionException("Unexpected compatibility service availability: " + available);
        }
        Properties properties = new Properties();
        properties.setProperty("available", Boolean.toString(available));
        properties.setProperty("destination", "UNKNOWN");
        properties.setProperty("encoding", "unknown");
        properties.setProperty("defaultEncoding", Charset.defaultCharset().name());
        if (available) {
            Map<?, ?> snapshot = new HashMap<>((Map<?, ?>) value);
            Object destination = snapshot.get("destination");
            if (Arrays.asList("CONSOLE", "FILE", "REDIRECTED", "UNKNOWN").contains(destination)) {
                properties.setProperty("destination", (String) destination);
            }
            Object encoding = snapshot.get("encoding");
            if (encoding instanceof String) {
                properties.setProperty("encoding", (String) encoding);
            }
        }
        try {
            Files.createDirectories(output.toPath().getParent());
            try (OutputStream stream = Files.newOutputStream(output.toPath())) {
                properties.store(stream, "Maven logging compatibility information");
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to write capabilities", e);
        }
        getLog().info("legacy-output-capabilities-marker caf\u00e9 \u251c\u2500");
    }
}
