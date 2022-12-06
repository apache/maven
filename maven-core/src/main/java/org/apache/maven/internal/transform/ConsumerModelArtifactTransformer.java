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
package org.apache.maven.internal.transform;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Predicate;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.model.building.DefaultBuildPomXMLFilterFactory;
import org.apache.maven.model.building.TransformerContext;
import org.apache.maven.model.transform.RawToConsumerPomXMLFilterFactory;
import org.apache.maven.model.transform.pull.XmlUtils;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.pull.EntityReplacementMap;
import org.codehaus.plexus.util.xml.pull.MXParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.transform.ArtifactTransformer;
import org.eclipse.aether.transform.TransformException;
import org.eclipse.aether.transform.TransformedArtifact;
import org.eclipse.aether.util.FileUtils;

/**
 * Consumer POM transformer: it transforms the same way for install and deploy, and deletes transformed POM temporary
 * files once install/deploy is done.
 *
 * @since TBD
 */
@Singleton
@Named
final class ConsumerModelArtifactTransformer implements ArtifactTransformer, Predicate<Artifact> {
    @Override
    public boolean test(Artifact artifact) {
        return "pom".equals(artifact.getExtension()) && StringUtils.isBlank(artifact.getClassifier());
    }

    @Override
    public TransformedArtifact transformInstallArtifact(RepositorySystemSession session, Artifact artifact)
            throws TransformException, IOException {
        return transformArtifact(session, artifact);
    }

    @Override
    public TransformedArtifact transformDeployArtifact(RepositorySystemSession session, Artifact artifact)
            throws TransformException, IOException {
        return transformArtifact(session, artifact);
    }

    private TransformedArtifact transformArtifact(RepositorySystemSession session, Artifact artifact)
            throws TransformException, IOException {

        try {
            TransformerContext context = (TransformerContext) session.getData().get(TransformerContext.KEY);
            requireNonNull(context, "context is null"); // bug: why are we invoked then?
            try (InputStream inputStream = transform(artifact.getFile().toPath(), context)) {
                FileUtils.TempFile tempFile = FileUtils.newTempFile(); // TODO: where? maybe target?
                Files.copy(inputStream, tempFile.getPath(), StandardCopyOption.REPLACE_EXISTING);
                return new ConsumerPomArtifact(artifact, tempFile);
            }
        } catch (XmlPullParserException e) {
            throw new TransformException(e);
        }
    }

    /**
     * Performs the actual transformation. Visible for testing.
     */
    static InputStream transform(Path pomFile, TransformerContext context) throws IOException, XmlPullParserException {
        XmlStreamReader reader = ReaderFactory.newXmlReader(Files.newInputStream(pomFile));
        XmlPullParser parser = new MXParser(EntityReplacementMap.defaultEntityReplacementMap);
        parser.setInput(reader);
        parser = new RawToConsumerPomXMLFilterFactory(new DefaultBuildPomXMLFilterFactory(context, true))
                .get(parser, pomFile);
        return XmlUtils.writeDocument(reader, parser);
    }

    private static class ConsumerPomArtifact extends TransformedArtifact {
        private final Artifact artifact;

        private final FileUtils.TempFile tempFile;

        private ConsumerPomArtifact(Artifact artifact, FileUtils.TempFile tempFile) {
            this.artifact = artifact.setFile(tempFile.getPath().toFile());
            this.tempFile = tempFile;
        }

        @Override
        public Artifact getTransformedArtifact() {
            return artifact;
        }

        @Override
        public void close() throws IOException {
            tempFile.close();
        }
    }
}
