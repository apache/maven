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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * Keeps transformed file up-to-date relative to its source file. It manages state (i.e. hashing the content) using
 * passed in stateFunction, and transforms when needed using passed in transformer bi-consumer.
 * <p>
 * Covered cases:
 * <ul>
 *     <li>when source supplier returns {@code null}, this class will return {@code null}.</li>
 *     <li>when source supplier returns non existing path, this class will return non existing path.</li>
 *     <li>when source supplier returns existing path, this class will ensure transformation is in sync.</li>
 * </ul>
 *
 * @since TBD
 */
final class OnChangeTransformer implements TransformedArtifact.Transformer {

    interface StateFunction {
        String state(Path path) throws Exception;
    }

    interface TransformerFunction {
        void transform(Path source, Path target) throws Exception;
    }

    private final Supplier<Path> source;

    private final Path target;

    private final StateFunction stateFunction;

    private final TransformerFunction transformerConsumer;

    private final AtomicReference<String> sourceState;

    OnChangeTransformer(
            Supplier<Path> source, Path target, StateFunction stateFunction, TransformerFunction transformerConsumer) {
        this.source = requireNonNull(source);
        this.target = requireNonNull(target);
        this.stateFunction = requireNonNull(stateFunction);
        this.transformerConsumer = requireNonNull(transformerConsumer);
        this.sourceState = new AtomicReference<>(null);
    }

    @Override
    public synchronized Path transform() throws Exception {
        String state = mayUpdate();
        if (state == null) {
            return null;
        }
        return target;
    }

    private String mayUpdate() throws Exception {
        String result;
        Path src = source.get();
        if (src == null) {
            Files.deleteIfExists(target);
            result = null;
        } else if (!Files.exists(src)) {
            Files.deleteIfExists(target);
            result = "";
        } else {
            String current = stateFunction.state(src);
            String existing = sourceState.get();
            if (!Objects.equals(current, existing)) {
                transformerConsumer.transform(src, target);
                Files.setLastModifiedTime(target, Files.getLastModifiedTime(src));
            }
            result = current;
        }
        sourceState.set(result);
        return result;
    }
}
