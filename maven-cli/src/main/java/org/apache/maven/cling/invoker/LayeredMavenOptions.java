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
package org.apache.maven.cling.invoker;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/**
 * Options that are "layered" by precedence order.
 */
public class LayeredMavenOptions implements MavenOptions {
    public static MavenOptions layer(MavenOptions... options) {
        return layer(Arrays.asList(options));
    }

    public static MavenOptions layer(Collection<MavenOptions> options) {
        List<MavenOptions> o = options.stream().filter(Objects::nonNull).toList();
        if (o.isEmpty()) {
            throw new IllegalArgumentException("No options specified (or all were null)");
        } else if (o.size() == 1) {
            return o.get(0);
        } else {
            return new LayeredMavenOptions(o);
        }
    }

    private final List<MavenOptions> options;

    private LayeredMavenOptions(List<MavenOptions> options) {
        this.options = requireNonNull(options);
    }

    @Override
    public Optional<Map<String, String>> userProperties() {
        return collectMapIfPresentOrEmpty(MavenOptions::userProperties);
    }

    @Override
    public Optional<String> alternatePomFile() {
        return returnFirstPresentOrEmpty(MavenOptions::alternatePomFile);
    }

    @Override
    public Optional<Boolean> offline() {
        return returnFirstPresentOrEmpty(MavenOptions::offline);
    }

    @Override
    public Optional<Boolean> showVersionAndExit() {
        return returnFirstPresentOrEmpty(MavenOptions::showVersionAndExit);
    }

    @Override
    public Optional<Boolean> showVersion() {
        return returnFirstPresentOrEmpty(MavenOptions::showVersion);
    }

    @Override
    public Optional<Boolean> quiet() {
        return returnFirstPresentOrEmpty(MavenOptions::quiet);
    }

    @Override
    public Optional<Boolean> verbose() {
        return returnFirstPresentOrEmpty(MavenOptions::verbose);
    }

    @Override
    public Optional<Boolean> showErrors() {
        return returnFirstPresentOrEmpty(MavenOptions::showErrors);
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
    public Optional<Boolean> nonInteractive() {
        return returnFirstPresentOrEmpty(MavenOptions::nonInteractive);
    }

    @Override
    public Optional<Boolean> forceInteractive() {
        return returnFirstPresentOrEmpty(MavenOptions::forceInteractive);
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
    public Optional<String> altUserSettings() {
        return returnFirstPresentOrEmpty(MavenOptions::altUserSettings);
    }

    @Override
    public Optional<String> altProjectSettings() {
        return returnFirstPresentOrEmpty(MavenOptions::altProjectSettings);
    }

    @Override
    public Optional<String> altInstallationSettings() {
        return returnFirstPresentOrEmpty(MavenOptions::altInstallationSettings);
    }

    @Override
    public Optional<String> altUserToolchains() {
        return returnFirstPresentOrEmpty(MavenOptions::altUserToolchains);
    }

    @Override
    public Optional<String> altInstallationToolchains() {
        return returnFirstPresentOrEmpty(MavenOptions::altInstallationToolchains);
    }

    @Override
    public Optional<String> failOnSeverity() {
        return returnFirstPresentOrEmpty(MavenOptions::failOnSeverity);
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
    public Optional<String> logFile() {
        return returnFirstPresentOrEmpty(MavenOptions::logFile);
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
    public Optional<String> color() {
        return returnFirstPresentOrEmpty(MavenOptions::color);
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
    public Optional<Boolean> help() {
        return returnFirstPresentOrEmpty(MavenOptions::help);
    }

    @Override
    public Optional<List<String>> goals() {
        return collectListIfPresentOrEmpty(MavenOptions::goals);
    }

    @Override
    public MavenOptions interpolate(Collection<Map<String, String>> properties) {
        ArrayList<MavenOptions> interpolatedOptions = new ArrayList<>(options.size());
        for (MavenOptions o : options) {
            interpolatedOptions.add(o.interpolate(properties));
        }
        return layer(interpolatedOptions);
    }

    @Override
    public void displayHelp(PrintStream printStream) {
        options.get(0).displayHelp(printStream);
    }

    private <T> Optional<T> returnFirstPresentOrEmpty(Function<MavenOptions, Optional<T>> getter) {
        for (MavenOptions option : options) {
            Optional<T> o = getter.apply(option);
            if (o.isPresent()) {
                return o;
            }
        }
        return Optional.empty();
    }

    private Optional<List<String>> collectListIfPresentOrEmpty(Function<MavenOptions, Optional<List<String>>> getter) {
        int had = 0;
        ArrayList<String> items = new ArrayList<>();
        for (MavenOptions option : options) {
            Optional<List<String>> o = getter.apply(option);
            if (o.isPresent()) {
                had++;
                items.addAll(o.get());
            }
        }
        return had == 0 ? Optional.empty() : Optional.of(List.copyOf(items));
    }

    private Optional<Map<String, String>> collectMapIfPresentOrEmpty(
            Function<MavenOptions, Optional<Map<String, String>>> getter) {
        int had = 0;
        HashMap<String, String> items = new HashMap<>();
        for (MavenOptions option : options) {
            Optional<Map<String, String>> up = getter.apply(option);
            if (up.isPresent()) {
                had++;
                items.putAll(up.get());
            }
        }
        return had == 0 ? Optional.empty() : Optional.of(Map.copyOf(items));
    }
}
