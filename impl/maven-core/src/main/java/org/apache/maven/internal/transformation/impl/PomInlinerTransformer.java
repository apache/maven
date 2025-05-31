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
package org.apache.maven.internal.transformation.impl;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.xml.stream.XMLStreamException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.api.feature.Features;
import org.apache.maven.api.model.Model;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystemSession;

/**
 * Inliner POM transformer.
 *
 * @since TBD
 */
@Singleton
@Named
class PomInlinerTransformer extends TransformerSupport {
    protected static final Pattern PLACEHOLDER_EXPRESSION = Pattern.compile("\\$\\{(.+?)}");

    @Override
    public void injectTransformedArtifacts(RepositorySystemSession session, MavenProject project) throws IOException {
        if (!Features.consumerPom(session.getConfigProperties())) {
            try {
                Model model = read(project.getFile().toPath());
                String version = model.getVersion();
                String newVersion = version;
                int lastEnd = -1;
                if (version != null) {
                    Matcher m = PLACEHOLDER_EXPRESSION.matcher(version.trim());
                    while (m.find()) {
                        String property = m.group(1);
                        if (!session.getConfigProperties().containsKey(property)) {
                            throw new IllegalArgumentException("Cannot inline property " + property);
                        }
                        String propertyValue =
                                (String) session.getConfigProperties().get(property);
                        if (lastEnd < 0) {
                            newVersion = version.substring(0, m.start());
                        } else {
                            newVersion += version.substring(lastEnd, m.start());
                        }
                        lastEnd = m.end();
                        newVersion += propertyValue;
                    }
                    if (!Objects.equals(version, newVersion)) {
                        model = model.withVersion(newVersion);
                        Path tmpPom = Files.createTempFile(
                                project.getArtifactId() + "-" + project.getVersion() + "-", ".xml");
                        write(model, tmpPom);
                        project.setFile(tmpPom.toFile());
                    }
                }
            } catch (XMLStreamException e) {
                throw new IOException("Problem during inlining POM", e);
            }
        }
    }
}
