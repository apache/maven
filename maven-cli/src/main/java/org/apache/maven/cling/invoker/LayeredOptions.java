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
public class LayeredOptions implements Options {
    public static Options layer(Options... options) {
        return layer(Arrays.asList(options));
    }

    public static Options layer(Collection<Options> options) {
        List<Options> o = options.stream().filter(Objects::nonNull).toList();
        if (o.isEmpty()) {
            throw new IllegalArgumentException("No options specified (or all were null)");
        } else if (o.size() == 1) {
            return o.get(0);
        } else {
            return new LayeredOptions(o);
        }
    }

    private final List<Options> options;

    private LayeredOptions(List<Options> options) {
        this.options = requireNonNull(options);
    }

    @Override
    public Optional<Map<String, String>> userProperties() {
        return collectMapIfPresentOrEmpty(Options::userProperties);
    }

    @Override
    public Optional<String> alternatePomFile() {
        return returnFirstPresentOrEmpty(Options::alternatePomFile);
    }

    @Override
    public Optional<Boolean> offline() {
        return returnFirstPresentOrEmpty(Options::offline);
    }

    @Override
    public Optional<Boolean> showVersionAndExit() {
        return returnFirstPresentOrEmpty(Options::showVersionAndExit);
    }

    @Override
    public Optional<Boolean> showVersion() {
        return returnFirstPresentOrEmpty(Options::showVersion);
    }

    @Override
    public Optional<Boolean> quiet() {
        return returnFirstPresentOrEmpty(Options::quiet);
    }

    @Override
    public Optional<Boolean> verbose() {
        return returnFirstPresentOrEmpty(Options::verbose);
    }

    @Override
    public Optional<Boolean> showErrors() {
        return returnFirstPresentOrEmpty(Options::showErrors);
    }

    @Override
    public Optional<Boolean> nonRecursive() {
        return returnFirstPresentOrEmpty(Options::nonRecursive);
    }

    @Override
    public Optional<Boolean> updateSnapshots() {
        return returnFirstPresentOrEmpty(Options::updateSnapshots);
    }

    @Override
    public Optional<List<String>> activatedProfiles() {
        return collectListIfPresentOrEmpty(Options::activatedProfiles);
    }

    @Override
    public Optional<Boolean> nonInteractive() {
        return returnFirstPresentOrEmpty(Options::nonInteractive);
    }

    @Override
    public Optional<Boolean> forceInteractive() {
        return returnFirstPresentOrEmpty(Options::forceInteractive);
    }

    @Override
    public Optional<Boolean> suppressSnapshotUpdates() {
        return returnFirstPresentOrEmpty(Options::suppressSnapshotUpdates);
    }

    @Override
    public Optional<Boolean> strictChecksums() {
        return returnFirstPresentOrEmpty(Options::strictChecksums);
    }

    @Override
    public Optional<Boolean> relaxedChecksums() {
        return returnFirstPresentOrEmpty(Options::relaxedChecksums);
    }

    @Override
    public Optional<String> altUserSettings() {
        return returnFirstPresentOrEmpty(Options::altUserSettings);
    }

    @Override
    public Optional<String> altProjectSettings() {
        return returnFirstPresentOrEmpty(Options::altProjectSettings);
    }

    @Override
    public Optional<String> altInstallationSettings() {
        return returnFirstPresentOrEmpty(Options::altInstallationSettings);
    }

    @Override
    public Optional<String> altUserToolchains() {
        return returnFirstPresentOrEmpty(Options::altUserToolchains);
    }

    @Override
    public Optional<String> altInstallationToolchains() {
        return returnFirstPresentOrEmpty(Options::altInstallationToolchains);
    }

    @Override
    public Optional<String> failOnSeverity() {
        return returnFirstPresentOrEmpty(Options::failOnSeverity);
    }

    @Override
    public Optional<Boolean> failFast() {
        return returnFirstPresentOrEmpty(Options::failFast);
    }

    @Override
    public Optional<Boolean> failAtEnd() {
        return returnFirstPresentOrEmpty(Options::failAtEnd);
    }

    @Override
    public Optional<Boolean> failNever() {
        return returnFirstPresentOrEmpty(Options::failNever);
    }

    @Override
    public Optional<Boolean> resume() {
        return returnFirstPresentOrEmpty(Options::resume);
    }

    @Override
    public Optional<String> resumeFrom() {
        return returnFirstPresentOrEmpty(Options::resumeFrom);
    }

    @Override
    public Optional<List<String>> projects() {
        return collectListIfPresentOrEmpty(Options::projects);
    }

    @Override
    public Optional<Boolean> alsoMake() {
        return returnFirstPresentOrEmpty(Options::alsoMake);
    }

    @Override
    public Optional<Boolean> alsoMakeDependents() {
        return returnFirstPresentOrEmpty(Options::alsoMakeDependents);
    }

    @Override
    public Optional<String> logFile() {
        return returnFirstPresentOrEmpty(Options::logFile);
    }

    @Override
    public Optional<String> threads() {
        return returnFirstPresentOrEmpty(Options::threads);
    }

    @Override
    public Optional<String> builder() {
        return returnFirstPresentOrEmpty(Options::builder);
    }

    @Override
    public Optional<Boolean> noTransferProgress() {
        return returnFirstPresentOrEmpty(Options::noTransferProgress);
    }

    @Override
    public Optional<String> color() {
        return returnFirstPresentOrEmpty(Options::color);
    }

    @Override
    public Optional<Boolean> cacheArtifactNotFound() {
        return returnFirstPresentOrEmpty(Options::cacheArtifactNotFound);
    }

    @Override
    public Optional<Boolean> strictArtifactDescriptorPolicy() {
        return returnFirstPresentOrEmpty(Options::strictArtifactDescriptorPolicy);
    }

    @Override
    public Optional<Boolean> ignoreTransitiveRepositories() {
        return returnFirstPresentOrEmpty(Options::ignoreTransitiveRepositories);
    }

    @Override
    public Optional<Boolean> help() {
        return returnFirstPresentOrEmpty(Options::help);
    }

    @Override
    public Optional<List<String>> goals() {
        return collectListIfPresentOrEmpty(Options::goals);
    }

    @Override
    public Options interpolate(Collection<Map<String, String>> properties) {
        ArrayList<Options> interpolatedOptions = new ArrayList<>(options.size());
        for (Options o : options) {
            interpolatedOptions.add(o.interpolate(properties));
        }
        return layer(interpolatedOptions);
    }

    @Override
    public void displayHelp(PrintStream printStream) {
        options.get(0).displayHelp(printStream);
    }

    private <T> Optional<T> returnFirstPresentOrEmpty(Function<Options, Optional<T>> getter) {
        for (Options option : options) {
            Optional<T> o = getter.apply(option);
            if (o.isPresent()) {
                return o;
            }
        }
        return Optional.empty();
    }

    private Optional<List<String>> collectListIfPresentOrEmpty(Function<Options, Optional<List<String>>> getter) {
        int had = 0;
        ArrayList<String> items = new ArrayList<>();
        for (Options option : options) {
            Optional<List<String>> o = getter.apply(option);
            if (o.isPresent()) {
                had++;
                items.addAll(o.get());
            }
        }
        return had == 0 ? Optional.empty() : Optional.of(List.copyOf(items));
    }

    private Optional<Map<String, String>> collectMapIfPresentOrEmpty(
            Function<Options, Optional<Map<String, String>>> getter) {
        int had = 0;
        HashMap<String, String> items = new HashMap<>();
        for (Options option : options) {
            Optional<Map<String, String>> up = getter.apply(option);
            if (up.isPresent()) {
                had++;
                items.putAll(up.get());
            }
        }
        return had == 0 ? Optional.empty() : Optional.of(Map.copyOf(items));
    }
}
