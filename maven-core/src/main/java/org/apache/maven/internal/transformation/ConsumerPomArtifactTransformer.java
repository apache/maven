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
package org.apache.maven.internal.transformation;

import javax.annotation.PreDestroy;
import javax.inject.Named;
import javax.inject.Singleton;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.BiConsumer;

import org.apache.maven.feature.Features;
import org.apache.maven.model.building.DefaultBuildPomXMLFilterFactory;
import org.apache.maven.model.building.TransformerContext;
import org.apache.maven.model.transform.RawToConsumerPomXMLFilterFactory;
import org.apache.maven.model.transform.pull.XmlUtils;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.ProjectArtifact;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.pull.EntityReplacementMap;
import org.codehaus.plexus.util.xml.pull.MXParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.installation.InstallRequest;

/**
 * Consumer POM transformer.
 *
 * @since TBD
 */
@Singleton
@Named("consumer-pom")
public final class ConsumerPomArtifactTransformer {

    private static final String CONSUMER_POM_CLASSIFIER = "consumer";

    private final Set<String> toDelete = new CopyOnWriteArraySet<>();

    public void injectTransformedArtifacts(MavenProject project, RepositorySystemSession session) throws IOException {
        if (project.getFile() == null) {
            // If there is no build POM there is no reason to inject artifacts for the consumer POM.
            return;
        }
        if (isActive(session)) {
            Path generatedFile;
            String buildDirectory =
                    project.getBuild() != null ? project.getBuild().getDirectory() : null;
            if (buildDirectory == null) {
                generatedFile = Files.createTempFile(CONSUMER_POM_CLASSIFIER, "pom");
            } else {
                Path buildDir = Paths.get(buildDirectory);
                Files.createDirectories(buildDir);
                generatedFile = Files.createTempFile(buildDir, CONSUMER_POM_CLASSIFIER, "pom");
            }
            deferDeleteFile(generatedFile);
            project.addAttachedArtifact(new ConsumerPomArtifact(project, generatedFile, session));
        } else if (project.getModel().isRoot()) {
            throw new IllegalStateException(
                    "The use of the root attribute on the model requires the buildconsumer feature to be active");
        }
    }

    private void deferDeleteFile(Path generatedFile) {
        toDelete.add(generatedFile.toAbsolutePath().toString());
    }

    @PreDestroy
    private void doDeleteFiles() {
        for (String file : toDelete) {
            try {
                Files.delete(Paths.get(file));
            } catch (IOException e) {
                // ignore, we did our best...
            }
        }
    }

    public InstallRequest remapInstallArtifacts(RepositorySystemSession session, InstallRequest request) {
        if (isActive(session) && consumerPomPresent(request.getArtifacts())) {
            request.setArtifacts(replacePom(request.getArtifacts()));
        }
        return request;
    }

    public DeployRequest remapDeployArtifacts(RepositorySystemSession session, DeployRequest request) {
        if (isActive(session) && consumerPomPresent(request.getArtifacts())) {
            request.setArtifacts(replacePom(request.getArtifacts()));
        }
        return request;
    }

    private boolean isActive(RepositorySystemSession session) {
        return Features.buildConsumer(session.getUserProperties()).isActive();
    }

    private boolean consumerPomPresent(Collection<Artifact> artifacts) {
        return artifacts.stream().anyMatch(a -> CONSUMER_POM_CLASSIFIER.equals(a.getClassifier()));
    }

    private Collection<Artifact> replacePom(Collection<Artifact> artifacts) {
        ArrayList<Artifact> result = new ArrayList<>(artifacts.size());
        for (Artifact artifact : artifacts) {
            if (CONSUMER_POM_CLASSIFIER.equals(artifact.getClassifier())) {
                // if under CONSUMER_POM_CLASSIFIER, move it to "" classifier
                DefaultArtifact remapped = new DefaultArtifact(
                        artifact.getGroupId(),
                        artifact.getArtifactId(),
                        "",
                        artifact.getExtension(),
                        artifact.getVersion(),
                        artifact.getProperties(),
                        artifact.getFile());
                result.add(remapped);
            } else if ("".equals(artifact.getClassifier())
                            && (artifact.getExtension().equals("pom"))
                    || artifact.getExtension().startsWith("pom.")) {
                // skip POM and POM subordinates
                continue;
            } else {
                // everything else: add as is
                result.add(artifact);
            }
        }
        return result;
    }

    /**
     * Consumer POM is transformed from original POM.
     */
    private static class ConsumerPomArtifact extends TransformedArtifact {

        private ConsumerPomArtifact(MavenProject mavenProject, Path target, RepositorySystemSession session) {
            super(
                    new ProjectArtifact(mavenProject),
                    () -> mavenProject.getFile().toPath(),
                    CONSUMER_POM_CLASSIFIER,
                    "pom",
                    target,
                    transformer(session));
        }

        private static BiConsumer<Path, Path> transformer(RepositorySystemSession session) {
            TransformerContext context = (TransformerContext) session.getData().get(TransformerContext.KEY);
            return (src, dest) -> {
                try (InputStream inputStream = transform(src, context)) {
                    Files.createDirectories(dest.getParent());
                    Files.copy(inputStream, dest, StandardCopyOption.REPLACE_EXISTING);
                } catch (XmlPullParserException | IOException e) {
                    throw new RuntimeException(e);
                }
            };
        }
    }

    /**
     * The actual transformation: visible for testing.
     */
    static InputStream transform(Path pomFile, TransformerContext context) throws IOException, XmlPullParserException {
        XmlStreamReader reader = ReaderFactory.newXmlReader(Files.newInputStream(pomFile));
        XmlPullParser parser = new MXParser(EntityReplacementMap.defaultEntityReplacementMap);
        parser.setInput(reader);
        parser = new RawToConsumerPomXMLFilterFactory(new DefaultBuildPomXMLFilterFactory(context, true))
                .get(parser, pomFile);

        return XmlUtils.writeDocument(reader, parser);
    }
}
