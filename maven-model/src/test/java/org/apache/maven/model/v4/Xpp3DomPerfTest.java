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
package org.apache.maven.model.v4;

import javax.xml.stream.XMLStreamException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.maven.api.model.InputSource;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * <p>Xpp3DomPerfTest class.</p>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3)
@Measurement(time = 10)
public class Xpp3DomPerfTest {
    @State(Scope.Benchmark)
    public static class AdditionState {
        List<Path> poms;

        @Setup(Level.Iteration)
        public void setUp() throws IOException {
            Path userHome = Paths.get(System.getProperty("user.home"));
            poms = Files.walk(userHome.resolve(".m2/repository/org/apache/maven"))
                    .filter(p -> p.getFileName().toString().endsWith(".pom"))
                    .collect(Collectors.toList());
        }
    }

    @Benchmark
    public int readWithStax(AdditionState state) throws IOException, XMLStreamException {
        int i = 0;
        for (Path pom : state.poms) {
            try (InputStream is = Files.newInputStream(pom)) {
                MavenStaxReader reader = new MavenStaxReader();
                reader.setAddLocationInformation(false);
                reader.read(is, true, new InputSource("id", pom.toString()));
                i++;
            } catch (XMLStreamException e) {
                throw new RuntimeException("Error parsing: " + pom, e);
            }
        }
        return i;
    }

    /**
     * <p>main.</p>
     *
     * @param args a {@link String} object.
     * @throws org.openjdk.jmh.runner.RunnerException if any.
     */
    public static void main(String... args) throws RunnerException {
        Options opts = new OptionsBuilder().forks(1).build();
        new Runner(opts).run();
    }
}
