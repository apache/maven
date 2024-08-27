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

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.apache.maven.api.cli.Options;

/**
 * Options that are "layered" by precedence order.
 *
 * @param <O> The type of options.
 */
public abstract class LayeredOptions<O extends Options> implements Options {
    protected final List<O> options;

    protected LayeredOptions(List<O> options) {
        this.options = new ArrayList<>(options);
    }

    @Override
    public Optional<Map<String, String>> userProperties() {
        return collectMapIfPresentOrEmpty(Options::userProperties);
    }

    @Override
    public String source() {
        return String.format(
                "layered(%s)", options.stream().map(Options::source).toList());
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
    public Optional<String> failOnSeverity() {
        return returnFirstPresentOrEmpty(Options::failOnSeverity);
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
    public Optional<String> logFile() {
        return returnFirstPresentOrEmpty(Options::logFile);
    }

    @Override
    public Optional<String> color() {
        return returnFirstPresentOrEmpty(Options::color);
    }

    @Override
    public Optional<Boolean> help() {
        return returnFirstPresentOrEmpty(Options::help);
    }

    @Override
    public void warnAboutDeprecatedOptions(PrintWriter printWriter) {}

    @Override
    public void displayHelp(String command, PrintWriter printWriter) {
        options.get(0).displayHelp(command, printWriter);
    }

    protected <T> Optional<T> returnFirstPresentOrEmpty(Function<O, Optional<T>> getter) {
        for (O option : options) {
            Optional<T> o = getter.apply(option);
            if (o.isPresent()) {
                return o;
            }
        }
        return Optional.empty();
    }

    protected Optional<List<String>> collectListIfPresentOrEmpty(Function<O, Optional<List<String>>> getter) {
        int had = 0;
        ArrayList<String> items = new ArrayList<>();
        for (O option : options) {
            Optional<List<String>> o = getter.apply(option);
            if (o.isPresent()) {
                had++;
                items.addAll(o.get());
            }
        }
        return had == 0 ? Optional.empty() : Optional.of(List.copyOf(items));
    }

    protected Optional<Map<String, String>> collectMapIfPresentOrEmpty(
            Function<O, Optional<Map<String, String>>> getter) {
        int had = 0;
        HashMap<String, String> items = new HashMap<>();
        for (O option : options) {
            Optional<Map<String, String>> up = getter.apply(option);
            if (up.isPresent()) {
                had++;
                items.putAll(up.get());
            }
        }
        return had == 0 ? Optional.empty() : Optional.of(Map.copyOf(items));
    }
}
