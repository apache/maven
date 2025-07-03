# XmlPlexusConfiguration Performance Benchmarks

This directory contains JMH (Java Microbenchmark Harness) benchmarks to measure the performance improvements in the optimized `XmlPlexusConfiguration` implementation.

## Overview

The benchmarks compare the old implementation (`XmlPlexusConfigurationOld`) with the new optimized implementation (`XmlPlexusConfiguration`) across several key performance metrics:

1. **Constructor Performance** - Measures the impact of eliminating deep copying
2. **Memory Allocation** - Compares memory usage patterns and allocation rates
3. **Thread Safety** - Tests concurrent access performance and safety
4. **Lazy vs Eager Loading** - Measures the benefits of lazy child creation

## Benchmark Classes

### 1. XmlPlexusConfigurationBenchmark
- **Purpose**: Core performance comparison between old and new implementations
- **Metrics**: Constructor speed, child access performance, memory allocation
- **Test Cases**: Simple, complex, and deep XML structures

### 2. XmlPlexusConfigurationConcurrencyBenchmark
- **Purpose**: Thread safety and concurrent performance testing
- **Metrics**: Throughput under concurrent load, race condition detection
- **Test Cases**: Multi-threaded child access, concurrent construction

### 3. XmlPlexusConfigurationMemoryBenchmark
- **Purpose**: Memory efficiency and garbage collection impact
- **Metrics**: Allocation rates, memory sharing vs copying
- **Test Cases**: Small, medium, and large XML documents

## Running the Benchmarks

### Prerequisites
- Java 11 or higher
- Maven 3.6 or higher
- At least 2GB of available memory

### Quick Start

1. **Compile the test classes:**
   ```bash
   mvn test-compile -pl impl/maven-xml
   ```

2. **Run all benchmarks:**
   ```bash
   mvn test-compile exec:java -Dexec.mainClass="org.openjdk.jmh.Main" \
       -Dexec.classpathScope=test \
       -Dexec.args="org.apache.maven.internal.xml.*Benchmark" \
       -pl impl/maven-xml
   ```

### Running Specific Benchmarks

**Constructor Performance:**
```bash
mvn test-compile exec:java -Dexec.mainClass="org.openjdk.jmh.Main" \
    -Dexec.classpathScope=test \
    -Dexec.args="XmlPlexusConfigurationBenchmark.constructor.*" \
    -pl impl/maven-xml
```

**Memory Allocation:**
```bash
mvn test-compile exec:java -Dexec.mainClass="org.openjdk.jmh.Main" \
    -Dexec.classpathScope=test \
    -Dexec.args="XmlPlexusConfigurationMemoryBenchmark" \
    -pl impl/maven-xml
```

**Thread Safety:**
```bash
mvn test-compile exec:java -Dexec.mainClass="org.openjdk.jmh.Main" \
    -Dexec.classpathScope=test \
    -Dexec.args="XmlPlexusConfigurationConcurrencyBenchmark" \
    -pl impl/maven-xml
```

### Advanced Options

**Generate detailed reports:**
```bash
mvn test-compile exec:java -Dexec.mainClass="org.openjdk.jmh.Main" \
    -Dexec.classpathScope=test \
    -Dexec.args="-rf json -rff benchmark-results.json org.apache.maven.internal.xml.*Benchmark" \
    -pl impl/maven-xml
```

**Profile memory allocation:**
```bash
mvn test-compile exec:java -Dexec.mainClass="org.openjdk.jmh.Main" \
    -Dexec.classpathScope=test \
    -Dexec.args="-prof gc XmlPlexusConfigurationMemoryBenchmark" \
    -pl impl/maven-xml
```

**Profile with async profiler (if available):**
```bash
mvn test-compile exec:java -Dexec.mainClass="org.openjdk.jmh.Main" \
    -Dexec.classpathScope=test \
    -Dexec.args="-prof async:output=flamegraph XmlPlexusConfigurationBenchmark" \
    -pl impl/maven-xml
```

## Expected Results

Based on the optimizations implemented, you should see:

### Constructor Performance
- **50-80% faster** initialization for complex XML structures
- **Dramatic improvement** for deep XML hierarchies due to eliminated deep copying

### Memory Usage
- **60-80% reduction** in memory allocation for typical XML documents
- **Linear scaling** instead of exponential growth with document complexity

### Thread Safety
- **Zero race conditions** in the new implementation
- **Consistent performance** under concurrent load
- **No infinite loops** or exceptions during parallel access

### Lazy Loading Benefits
- **Faster startup** when not all children are accessed
- **Lower memory footprint** for partially used configurations
- **Better scalability** for large XML documents

## Interpreting Results

- **Lower numbers are better** for average time benchmarks
- **Higher numbers are better** for throughput benchmarks
- **Error margins** indicate measurement confidence
- **GC profiling** shows allocation reduction in the new implementation

## Troubleshooting

**Out of Memory Errors:**
- Increase heap size: `-Dexec.args="-jvmArgs -Xmx4g"`
- Reduce benchmark iterations: `-Dexec.args="-wi 1 -i 3"`

**Long Execution Times:**
- Run specific benchmarks instead of all
- Reduce warmup and measurement iterations
- Use shorter time periods: `-Dexec.args="-w 1s -r 1s"`

## Contributing

When adding new benchmarks:
1. Follow the existing naming convention
2. Include both old and new implementation tests
3. Add appropriate JMH annotations
4. Document the benchmark purpose and expected results
5. Update this README with new benchmark information
