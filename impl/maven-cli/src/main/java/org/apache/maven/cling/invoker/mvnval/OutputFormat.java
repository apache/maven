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
package org.apache.maven.cling.invoker.mvnval;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Output formats the validation tool understands.
 */
enum OutputFormat {
    TEXT(new TextReporter()),
    JSON(new JsonReporter());

    private final Reporter reporter;

    OutputFormat(Reporter reporter) {
        this.reporter = reporter;
    }

    void report(List<Report> reports, Consumer<String> out) {
        reporter.report(reports, out);
    }

    static OutputFormat parse(String name) {
        return Arrays.stream(values())
                .filter(format -> format.name().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown format '" + name + "', expected one of: "
                        + Arrays.stream(values())
                                .map(format -> format.name().toLowerCase(Locale.ROOT))
                                .collect(Collectors.joining(", "))));
    }
}
