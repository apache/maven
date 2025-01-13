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
package org.apache.maven.cling.invoker.mvn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import org.apache.maven.api.cli.mvn.MavenOptions;
import org.apache.maven.cling.invoker.LayeredOptions;

/**
 * Options that are "layered" by precedence order.
 *
 * @param <O> the specific type of Maven Options that are layered
 */
public class LayeredMavenOptions<O extends MavenOptions> extends LayeredOptions<O> implements MavenOptions {
    public static MavenOptions layerMavenOptions(Collection<MavenOptions> options) {
        List<MavenOptions> o = options.stream().filter(Objects::nonNull).toList();
        if (o.isEmpty()) {
            throw new IllegalArgumentException("No options specified (or all were null)");
        } else if (o.size() == 1) {
            return o.get(0);
        } else {
            return new LayeredMavenOptions<>(o);
        }
    }

    protected LayeredMavenOptions(List<O> options) {
        super(options);
    }

    @Override
    public Optional<String> alternatePomFile() {
        return returnFirstPresentOrEmpty(MavenOptions::alternatePomFile);
    }

    @Override
    public Optional<Boolean> nonRecursive() {
        return returnFirstPresentOrEmpty(MavenOptions::nonRecursive);
    }

    @Override
    public Optional<Boolean> updateSnapshots() {
        return returnFirstPresentOrEmpty(MavenOptions::updateSnapshots);
    }

    @Override
    public Optional<List<String>> activatedProfiles() {
        return collectListIfPresentOrEmpty(MavenOptions::activatedProfiles);
    }

    @Override
    public Optional<Boolean> suppressSnapshotUpdates() {
        return returnFirstPresentOrEmpty(MavenOptions::suppressSnapshotUpdates);
    }

    @Override
    public Optional<Boolean> strictChecksums() {
        return returnFirstPresentOrEmpty(MavenOptions::strictChecksums);
    }

    @Override
    public Optional<Boolean> relaxedChecksums() {
        return returnFirstPresentOrEmpty(MavenOptions::relaxedChecksums);
    }

    @Override
    public Optional<Boolean> failFast() {
        return returnFirstPresentOrEmpty(MavenOptions::failFast);
    }

    @Override
    public Optional<Boolean> failAtEnd() {
        return returnFirstPresentOrEmpty(MavenOptions::failAtEnd);
    }

    @Override
    public Optional<Boolean> failNever() {
        return returnFirstPresentOrEmpty(MavenOptions::failNever);
    }

    @Override
    public Optional<Boolean> resume() {
        return returnFirstPresentOrEmpty(MavenOptions::resume);
    }

    @Override
    public Optional<String> resumeFrom() {
        return returnFirstPresentOrEmpty(MavenOptions::resumeFrom);
    }

    @Override
    public Optional<List<String>> projects() {
        return collectListIfPresentOrEmpty(MavenOptions::projects);
    }

    @Override
    public Optional<Boolean> alsoMake() {
        return returnFirstPresentOrEmpty(MavenOptions::alsoMake);
    }

    @Override
    public Optional<Boolean> alsoMakeDependents() {
        return returnFirstPresentOrEmpty(MavenOptions::alsoMakeDependents);
    }

    @Override
    public Optional<String> threads() {
        return returnFirstPresentOrEmpty(MavenOptions::threads);
    }

    @Override
    public Optional<String> builder() {
        return returnFirstPresentOrEmpty(MavenOptions::builder);
    }

    @Override
    public Optional<Boolean> noTransferProgress() {
        return returnFirstPresentOrEmpty(MavenOptions::noTransferProgress);
    }

    @Override
    public Optional<Boolean> cacheArtifactNotFound() {
        return returnFirstPresentOrEmpty(MavenOptions::cacheArtifactNotFound);
    }

    @Override
    public Optional<Boolean> strictArtifactDescriptorPolicy() {
        return returnFirstPresentOrEmpty(MavenOptions::strictArtifactDescriptorPolicy);
    }

    @Override
    public Optional<Boolean> ignoreTransitiveRepositories() {
        return returnFirstPresentOrEmpty(MavenOptions::ignoreTransitiveRepositories);
    }

    @Override
    public Optional<List<String>> goals() {
        return collectListIfPresentOrEmpty(MavenOptions::goals);
    }

    @Override
    public MavenOptions interpolate(Function<String, String> callback) {
        ArrayList<MavenOptions> interpolatedOptions = new ArrayList<>(options.size());
        for (MavenOptions o : options) {
            interpolatedOptions.add(o.interpolate(callback));
        }
        return layerMavenOptions(interpolatedOptions);
    }
}
