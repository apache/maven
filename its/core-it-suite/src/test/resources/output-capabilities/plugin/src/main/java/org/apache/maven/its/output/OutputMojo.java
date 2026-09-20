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

import javax.inject.Inject;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Properties;

import org.apache.maven.logging.OutputCapabilities;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "report", threadSafe = true)
public class OutputMojo extends AbstractMojo {
    @Inject
    private OutputCapabilities capabilities;

    @Parameter(defaultValue = "${project.build.directory}/output-capabilities.properties", readonly = true)
    private File output;

    @Override
    public void execute() throws MojoExecutionException {
        Properties properties = new Properties();
        properties.setProperty("destination", capabilities.getDestination().name());
        properties.setProperty("encoding", capabilities.getEncoding().map(Charset::name).orElse("unknown"));
        properties.setProperty("defaultEncoding", Charset.defaultCharset().name());
        try {
            Files.createDirectories(output.toPath().getParent());
            try (OutputStream stream = Files.newOutputStream(output.toPath())) {
                properties.store(stream, "Maven logging capabilities");
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to write capabilities", e);
        }
        getLog().info("output-capabilities-marker caf\u00e9 \u251c\u2500");
    }
}
