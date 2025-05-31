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
package org.apache.maven.cling.invoker.mvnup.jdom;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.aether.artifact.Artifact;
import org.jdom2.Document;

import static java.util.Objects.requireNonNull;

/**
 * Construction to accept collection of artifacts, and applies it to some extensions.xml based on provided transformations.
 */
public final class JDomExtensionsTransformer {

    public static Function<Artifact, Consumer<JDomTransformationContext.JdomExtensionsTransformationContext>>
            updateExtension(boolean upsert) {
        return a -> context ->
                JDomExtensionsEditor.updateExtension(context.getDocument().getRootElement(), a, upsert);
    }

    public static Function<Artifact, Consumer<JDomTransformationContext.JdomExtensionsTransformationContext>>
            deleteExtension() {
        return a -> context -> {
            JDomExtensionsEditor.deleteExtension(context.getDocument().getRootElement(), a);
        };
    }

    private final Path extensions;

    public JDomExtensionsTransformer(Path extensions) {
        this.extensions = requireNonNull(extensions);
    }

    public void apply(List<Consumer<JDomTransformationContext.JdomExtensionsTransformationContext>> transformations)
            throws IOException {
        requireNonNull(transformations, "transformations");
        if (!transformations.isEmpty()) {
            try (JDomDocumentIO domDocumentIO = new JDomDocumentIO(extensions)) {
                ArrayList<Consumer<JDomTransformationContext.JdomExtensionsTransformationContext>> postProcessors =
                        new ArrayList<>();
                JDomTransformationContext.JdomExtensionsTransformationContext context =
                        new JDomTransformationContext.JdomExtensionsTransformationContext() {
                            @Override
                            public Document getDocument() {
                                return domDocumentIO.getDocument();
                            }

                            @Override
                            public void registerPostTransformation(
                                    Consumer<JdomExtensionsTransformationContext> transformation) {
                                postProcessors.add(transformation);
                            }

                            @Override
                            public Path extensions() {
                                return extensions;
                            }
                        };
                for (Consumer<JDomTransformationContext.JdomExtensionsTransformationContext> transformation :
                        transformations) {
                    transformation.accept(context);
                }
                for (Consumer<JDomTransformationContext.JdomExtensionsTransformationContext> transformation :
                        postProcessors) {
                    transformation.accept(context);
                }
            }
        }
    }
}
