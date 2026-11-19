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
package org.apache.maven.logging.impl;

import java.util.concurrent.TimeUnit;

import org.apache.maven.api.logging.LogEvent;
import org.apache.maven.api.logging.LogLevel;
import org.apache.maven.api.logging.LogSink;
import org.apache.maven.api.logging.Logger;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * JMH benchmark for the Maven unified logging hot path.
 * <p>
 * Measures throughput and allocation pressure of:
 * <ul>
 *   <li>Logging at an enabled level (INFO) with 0, 1, 2 args</li>
 *   <li>Logging at a disabled level (TRACE) — should be near-zero cost</li>
 *   <li>Fluent builder API vs legacy Log API</li>
 *   <li>formatLine rendering</li>
 * </ul>
 *
 * Run from CLI:
 * {@code mvn -pl impl/maven-logging test-compile exec:java -Dexec.classpathScope=test
 *   -Dexec.mainClass=org.apache.maven.logging.impl.LoggingBenchmark}
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(0)
@State(Scope.Benchmark)
public class LoggingBenchmark {

    private DefaultLoggerManager manager;
    private Logger logger;
    private CountingSink sink;

    @Setup(Level.Trial)
    public void setup() {
        manager = new DefaultLoggerManager();
        sink = new CountingSink();
        manager.setLogSink(sink);
        manager.install();
        logger = manager.getLogger("org.apache.maven.benchmark.TestLogger");
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        manager.uninstall();
    }

    // ---- Enabled level (INFO) ----

    @Benchmark
    public void infoNoArgs(Blackhole bh) {
        logger.atInfo().log("Build started");
        bh.consume(sink.count);
    }

    @Benchmark
    public void infoOneArg(Blackhole bh) {
        logger.atInfo().log("Compiling {0} source files", 42);
        bh.consume(sink.count);
    }

    @Benchmark
    public void infoTwoArgs(Blackhole bh) {
        logger.atInfo().log("Resolved {0} artifacts in {1}ms", 128, 350);
        bh.consume(sink.count);
    }

    @Benchmark
    public void infoThreeArgs(Blackhole bh) {
        logger.atInfo().log("Plugin {0}:{1}:{2}", "maven-compiler-plugin", "compile", "3.13.0");
        bh.consume(sink.count);
    }

    // ---- Disabled level (TRACE) — should be near-zero cost ----

    @Benchmark
    public void traceDisabled(Blackhole bh) {
        logger.atTrace().log("Should not appear: {0}", "value");
        bh.consume(sink.count);
    }

    // ---- Legacy Log API (deprecated methods) ----

    @Benchmark
    @SuppressWarnings("deprecation")
    public void legacyInfoCharSequence(Blackhole bh) {
        ((org.apache.maven.api.plugin.Log) logger).info("Build started");
        bh.consume(sink.count);
    }

    // ---- Level check only ----

    @Benchmark
    public void isInfoEnabled(Blackhole bh) {
        bh.consume(logger.isEnabled(LogLevel.INFO));
    }

    @Benchmark
    public void isTraceEnabled(Blackhole bh) {
        bh.consume(logger.isEnabled(LogLevel.TRACE));
    }

    // ---- formatLine rendering ----

    @Benchmark
    public void formatLine(Blackhole bh) {
        DefaultLogEvent event = new DefaultLogEvent(null, LogLevel.INFO, "Build started", "org.example.Foo", null);
        bh.consume(manager.formatLine(event));
    }

    static class CountingSink implements LogSink {
        volatile long count;

        @Override
        public void accept(LogEvent event) {
            count++;
        }
    }

    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(LoggingBenchmark.class.getSimpleName())
                .forks(0)
                .addProfiler("gc")
                .build();
        new Runner(opt).run();
    }
}
