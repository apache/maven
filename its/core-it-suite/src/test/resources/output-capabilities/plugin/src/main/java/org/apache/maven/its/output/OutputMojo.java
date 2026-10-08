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
import java.util.Properties;

import org.apache.maven.api.Session;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.plugin.Log;
import org.apache.maven.api.plugin.MojoException;
import org.apache.maven.api.plugin.annotations.Mojo;
import org.apache.maven.api.plugin.annotations.Parameter;
import org.apache.maven.api.services.OutputCapabilities;

@Mojo(name = "report")
public class OutputMojo implements org.apache.maven.api.plugin.Mojo {
    @Inject
    private OutputCapabilities capabilities;

    @Inject
    private Session session;

    @Inject
    private Log log;

    @Parameter(defaultValue = "${project.build.directory}/output-capabilities.properties", readonly = true)
    private File output;

    @Override
    public void execute() throws MojoException {
        if (session.getService(OutputCapabilities.class) != capabilities) {
            OutputCapabilities service = session.getService(OutputCapabilities.class);
            throw new MojoException("Service " + service.getClass().getName() + " " + service.getDestination()
                    + "/" + service.getFormat() + " differs from injected " + capabilities.getClass().getName()
                    + " " + capabilities.getDestination() + "/" + capabilities.getFormat());
        }
        Properties properties = new Properties();
        capabilities.getDestination().ifPresent(value -> properties.setProperty("destination", value.name()));
        capabilities.getFormat().ifPresent(value -> properties.setProperty("format", value.name()));
        capabilities.getEncoding().ifPresent(value -> properties.setProperty("encoding", value.name()));
        properties.setProperty("defaultEncoding", Charset.defaultCharset().name());
        try {
            Files.createDirectories(output.toPath().getParent());
            try (OutputStream stream = Files.newOutputStream(output.toPath())) {
                properties.store(stream, "Maven logging capabilities");
            }
        } catch (IOException e) {
            throw new MojoException("Unable to write capabilities", e);
        }
        log.info("output-capabilities-marker caf\u00e9 \u251c\u2500");
    }
}
