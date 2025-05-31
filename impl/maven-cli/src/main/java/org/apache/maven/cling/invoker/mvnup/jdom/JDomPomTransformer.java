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
 * Construction to accept collection of artifacts, and applies it to some POM based on provided transformations.
 */
public final class JDomPomTransformer {

    /**
     * Removes empty remnant tags, like {@code <plugins />}.
     */
    private static final Consumer<JDomTransformationContext.JDomPomTransformationContext> REMOVE_EMPTY_ELEMENTS =
            ctx -> {
                JDomPomCleanupHelper.cleanup(ctx.getDocument().getRootElement());
            };

    private static Consumer<JDomTransformationContext.JDomPomTransformationContext> setProperty(
            String key, String value, boolean upsert) {
        return context -> JDomPomEditor.setProperty(context.getDocument().getRootElement(), key, value, upsert);
    }

    public static Function<Artifact, Consumer<JDomTransformationContext.JDomPomTransformationContext>>
            updateManagedPlugin(boolean upsert) {
        return a -> context ->
                JDomPomEditor.updateManagedPlugin(context.getDocument().getRootElement(), a, upsert);
    }

    public static Function<Artifact, Consumer<JDomTransformationContext.JDomPomTransformationContext>>
            deleteManagedPlugin() {
        return a -> context -> {
            JDomPomEditor.deleteManagedPlugin(context.getDocument().getRootElement(), a);
            context.registerPostTransformation(REMOVE_EMPTY_ELEMENTS);
        };
    }

    public static Function<Artifact, Consumer<JDomTransformationContext.JDomPomTransformationContext>> updatePlugin(
            boolean upsert) {
        return a -> context -> JDomPomEditor.updatePlugin(context.getDocument().getRootElement(), a, upsert);
    }

    public static Function<Artifact, Consumer<JDomTransformationContext.JDomPomTransformationContext>> deletePlugin() {
        return a -> context -> {
            JDomPomEditor.deletePlugin(context.getDocument().getRootElement(), a);
            context.registerPostTransformation(REMOVE_EMPTY_ELEMENTS);
        };
    }

    public static Function<Artifact, Consumer<JDomTransformationContext.JDomPomTransformationContext>>
            deletePluginVersion() {
        return a -> context -> {
            JDomPomEditor.deletePluginVersion(context.getDocument().getRootElement(), a);
        };
    }

    public static Function<Artifact, Consumer<JDomTransformationContext.JDomPomTransformationContext>>
            updateManagedDependency(boolean upsert) {
        return a -> context ->
                JDomPomEditor.updateManagedDependency(context.getDocument().getRootElement(), a, upsert);
    }

    public static Function<Artifact, Consumer<JDomTransformationContext.JDomPomTransformationContext>>
            deleteManagedDependency() {
        return a -> context -> {
            JDomPomEditor.deleteManagedDependency(context.getDocument().getRootElement(), a);
            context.registerPostTransformation(REMOVE_EMPTY_ELEMENTS);
        };
    }

    public static Function<Artifact, Consumer<JDomTransformationContext.JDomPomTransformationContext>> updateDependency(
            boolean upsert) {
        return a ->
                context -> JDomPomEditor.updateDependency(context.getDocument().getRootElement(), a, upsert);
    }

    public static Function<Artifact, Consumer<JDomTransformationContext.JDomPomTransformationContext>>
            deleteDependency() {
        return a -> context -> {
            JDomPomEditor.deleteDependency(context.getDocument().getRootElement(), a);
            context.registerPostTransformation(REMOVE_EMPTY_ELEMENTS);
        };
    }

    public static Function<Artifact, Consumer<JDomTransformationContext.JDomPomTransformationContext>> updateExtension(
            boolean upsert) {
        return a ->
                context -> JDomPomEditor.updateExtension(context.getDocument().getRootElement(), a, upsert);
    }

    public static Function<Artifact, Consumer<JDomTransformationContext.JDomPomTransformationContext>>
            deleteExtension() {
        return a -> context -> {
            JDomPomEditor.deleteExtension(context.getDocument().getRootElement(), a);
            context.registerPostTransformation(REMOVE_EMPTY_ELEMENTS);
        };
    }

    private final Path pom;

    public JDomPomTransformer(Path pom) {
        this.pom = requireNonNull(pom);
    }

    public void apply(List<Consumer<JDomTransformationContext.JDomPomTransformationContext>> transformations)
            throws IOException {
        requireNonNull(transformations, "transformations");
        if (!transformations.isEmpty()) {
            try (JDomDocumentIO domDocumentIO = new JDomDocumentIO(pom)) {
                ArrayList<Consumer<JDomTransformationContext.JDomPomTransformationContext>> postProcessors =
                        new ArrayList<>();
                JDomTransformationContext.JDomPomTransformationContext context =
                        new JDomTransformationContext.JDomPomTransformationContext() {
                            @Override
                            public Document getDocument() {
                                return domDocumentIO.getDocument();
                            }

                            @Override
                            public void registerPostTransformation(
                                    Consumer<JDomTransformationContext.JDomPomTransformationContext> transformation) {
                                postProcessors.add(transformation);
                            }

                            @Override
                            public Path pom() {
                                return pom;
                            }
                        };
                for (Consumer<JDomTransformationContext.JDomPomTransformationContext> transformation :
                        transformations) {
                    transformation.accept(context);
                }
                for (Consumer<JDomTransformationContext.JDomPomTransformationContext> transformation : postProcessors) {
                    transformation.accept(context);
                }
            }
        }
    }
}
