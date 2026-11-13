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
package org.apache.maven.internal.build.incremental.impl;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.apache.maven.api.build.incremental.IncrementalContextException;
import org.apache.maven.api.build.incremental.Input;
import org.apache.maven.api.build.incremental.Metadata;
import org.apache.maven.api.build.incremental.Status;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.apache.maven.api.build.incremental.Status.MODIFIED;
import static org.apache.maven.api.build.incremental.Status.NEW;
import static org.apache.maven.api.build.incremental.Status.UNMODIFIED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultIncrementalContextTest extends AbstractIncrementalContextTest {

    private static void assertIncludedPaths(Collection<Path> expected, Collection<Path> actual) throws IOException {
        assertEquals(toString(expected), toString(actual));
    }

    private static String toString(Collection<Path> files) throws IOException {
        return files.stream()
                .map(DefaultIncrementalContext::normalize)
                .map(Path::toString)
                .sorted()
                .collect(Collectors.joining("\n", "", "\n"));
    }

    @Test
    void testRegisterInputInputFileDoesNotExist() throws Exception {
        Path file = Paths.get("target/does_not_exist");
        assertTrue(!Files.exists(file) && !Files.isReadable(file));
        assertThrows(
                IllegalArgumentException.class, () -> newIncrementalContext().registerInput(file));
    }

    @Test
    void testRegisterInput() throws Exception {
        // this is NOT part of API but rather currently implemented behaviour
        // API allows #registerInput return different instances

        Path file = Paths.get("src/test/resources/simplelogger.properties");
        assertTrue(Files.exists(file) && Files.isReadable(file));
        TestIncrementalContext context = newIncrementalContext();
        assertNotNull(context.registerInput(file));
        assertNotNull(context.registerInput(file));
    }

    @Test
    void testOutputWithoutInputs() throws Exception {
        TestIncrementalContext context = newIncrementalContext();

        Path outputFile = Files.createFile(temp.resolve("output_without_inputs"));
        context.processOutput(outputFile);

        // is not deleted by commit
        context.commit();
        assertTrue(Files.isReadable(outputFile));

        // is not deleted after rebuild with re-registration
        context = newIncrementalContext();
        context.processOutput(outputFile);
        context.commit();
        assertTrue(Files.isReadable(outputFile));

        // deleted after rebuild without re-registration
        context = newIncrementalContext();
        context.commit();
        assertFalse(Files.isReadable(outputFile));
    }

    @Test
    void testGetInputStatus() throws Exception {
        Path inputFile = Files.createFile(temp.resolve("inputFile"));

        // initial build
        TestIncrementalContext context = newIncrementalContext();
        // first time invocation returns Input for processing
        assertEquals(NEW, context.registerInput(inputFile).getStatus());
        // second invocation still returns NEW
        assertEquals(NEW, context.registerInput(inputFile).getStatus());
        context.commit();

        // new build
        context = newIncrementalContext();
        // input file was not modified since last build
        assertEquals(UNMODIFIED, context.registerInput(inputFile).getStatus());
        context.commit();

        // new build
        Files.write(inputFile, Collections.singletonList("test"), StandardOpenOption.APPEND);
        context = newIncrementalContext();
        // input file was modified since last build
        assertEquals(MODIFIED, context.registerInput(inputFile).getStatus());
    }

    @Test
    void testInputModifiedAfterRegistration() throws Exception {
        Path inputFile = Files.createFile(temp.resolve("inputFile"));
        Path outputFile = Files.createFile(temp.resolve("outputFile"));

        TestIncrementalContext context = newIncrementalContext();
        DefaultInput input = context.registerInput(inputFile).process();
        input.associateOutput(outputFile);
        context.commit();

        TestIncrementalContext context2 = newIncrementalContext();
        context2.registerInput(inputFile);
        // this is incorrect use of build-avoidance API
        // input has changed after it was registered for processing
        // IllegalStateException is raised to prevent unexpected process/not-process flip-flop
        Files.write(inputFile, Collections.singletonList("test"), StandardOpenOption.APPEND);
        assertThrows(IncrementalContextException.class, context2::commit);
        assertTrue(Files.isReadable(outputFile));
    }

    @Test
    void testCommitOrphanedOutputsCleanup() throws Exception {
        Path inputFile = Files.createFile(temp.resolve("inputFile"));
        Path outputFile = Files.createFile(temp.resolve("outputFile"));

        TestIncrementalContext context = newIncrementalContext();
        DefaultInput input = context.registerInput(inputFile).process();
        input.associateOutput(outputFile);
        context.commit();

        // input is not part of input set any more
        // associated output must be cleaned up
        context = newIncrementalContext();
        context.commit();
        assertFalse(Files.isReadable(outputFile));
    }

    @Test
    void testCommitStaleOutputCleanup() throws Exception {
        Path inputFile = Files.createFile(temp.resolve("inputFile"));
        Path outputFile1 = Files.createFile(temp.resolve("outputFile1"));
        Path outputFile2 = Files.createFile(temp.resolve("outputFile2"));

        TestIncrementalContext context = newIncrementalContext();
        DefaultInput input = context.registerInput(inputFile).process();
        input.associateOutput(outputFile1);
        input.associateOutput(outputFile2);
        context.commit();

        context = newIncrementalContext();
        input = context.registerInput(inputFile).process();
        input.associateOutput(outputFile1);
        context.commit();
        assertFalse(Files.isReadable(outputFile2));

        context = newIncrementalContext();
        DefaultInputMetadata metadata = context.registerInput(inputFile);
        assertEquals(1, toList(context.getAssociatedOutputs(metadata)).size());
        context.commit();
    }

    @Test
    void testCreateStateParentDirectory() throws Exception {
        Path stateFile = temp.resolve("sub/dir/buildstate.ctx");
        TestIncrementalContext context = new TestIncrementalContext(stateFile, Collections.emptyMap());
        context.commit();
        assertTrue(Files.isReadable(stateFile));
    }

    @Test
    void testRegisterInputNullInput() throws Exception {
        assertThrows(
                IllegalArgumentException.class, () -> newIncrementalContext().registerInput((Path) null));
    }

    @ParameterizedTest
    @MethodSource("fileSystems")
    void testRegisterAndProcessInputs(String type, Supplier<FileSystem> fs) throws Exception {
        Path target = fs.get().getPath("target");
        Files.createDirectories(target);
        temp = Files.createTempDirectory(target, "tmp");

        Path inputFile = Files.createFile(temp.resolve("inputFile"));
        Path outputFile = Files.createFile(temp.resolve("outputFile"));
        List<String> includes = Collections.singletonList("**/" + inputFile.getFileName());

        TestIncrementalContext context = newIncrementalContext();
        List<? extends DefaultInput> inputs = toList(context.registerAndProcessInputs(temp, includes, null));
        assertEquals(1, inputs.size());
        assertEquals(NEW, inputs.get(0).getStatus());
        inputs.get(0).associateOutput(outputFile);
        context.commit();

        // no change rebuild
        context = newIncrementalContext();
        inputs = toList(context.registerAndProcessInputs(temp, includes, null));
        assertEquals(1, inputs.size());
        assertEquals(UNMODIFIED, inputs.get(0).getStatus());
        context.commit();
    }

    @Test
    void testInputDeleted() throws Exception {
        Path inputFile = Files.createFile(temp.resolve("inputFile"));
        Path outputFile = Files.createFile(temp.resolve("outputFile"));

        TestIncrementalContext context = newIncrementalContext();
        context.registerInput(inputFile).process().associateOutput(outputFile);
        context.commit();

        //
        Files.delete(inputFile);
        context = newIncrementalContext();
        context.commit();
        assertFalse(Files.exists(outputFile));
    }

    @Test
    void testInputInListDeleted() throws Exception {
        Path input = temp.resolve("input");
        Path output = temp.resolve("output");
        Files.createDirectories(input);
        Files.createDirectories(output);

        Path inputFile = Files.createFile(input.resolve("file"));
        Path outputFile = Files.createFile(output.resolve("file"));

        TestIncrementalContext context = newIncrementalContext();
        Collection<? extends Input> inputs = context.registerAndProcessInputs(input, null, null);
        assertNotNull(inputs);
        assertFalse(inputs.isEmpty());
        Input i = inputs.iterator().next();
        i.associateOutput(outputFile);
        context.commit();

        //
        Files.delete(inputFile);
        context = newIncrementalContext();
        context.commit();
        assertFalse(Files.exists(outputFile));
    }

    @Test
    void testGetAssociatedOutputs() throws Exception {
        Path inputFile = Files.createFile(temp.resolve("inputFile"));
        Path outputFile = Files.createFile(temp.resolve("outputFile"));

        TestIncrementalContext context = newIncrementalContext();
        context.registerInput(inputFile).process().associateOutput(outputFile);
        context.commit();

        //
        context = newIncrementalContext();
        DefaultInputMetadata metadata = context.registerInput(inputFile);
        List<? extends Metadata> outputs = toList(context.getAssociatedOutputs(metadata));
        assertEquals(1, outputs.size());
        assertEquals(Status.UNMODIFIED, outputs.get(0).getStatus());
        context.commit();

        //
        Files.write(outputFile, Collections.singletonList("test"), StandardOpenOption.APPEND);
        context = newIncrementalContext();
        metadata = context.registerInput(inputFile);
        outputs = toList(context.getAssociatedOutputs(metadata));
        assertEquals(1, outputs.size());
        assertEquals(Status.MODIFIED, outputs.get(0).getStatus());
        context.commit();

        //
        Files.delete(outputFile);
        context = newIncrementalContext();
        metadata = context.registerInput(inputFile);
        outputs = toList(context.getAssociatedOutputs(metadata));
        assertEquals(1, outputs.size());
        assertEquals(Status.REMOVED, outputs.get(0).getStatus());
        context.commit();
    }

    @Test
    void testGetRegisteredInputs() throws Exception {
        Path inputFile1 = Files.createFile(temp.resolve("inputFile1"));
        Path inputFile2 = Files.createFile(temp.resolve("inputFile2"));
        Path inputFile3 = Files.createFile(temp.resolve("inputFile3"));
        Path inputFile4 = Files.createFile(temp.resolve("inputFile4"));

        TestIncrementalContext context = newIncrementalContext();
        inputFile1 = context.registerInput(inputFile1).getPath();
        inputFile2 = context.registerInput(inputFile2).getPath();
        inputFile3 = context.registerInput(inputFile3).getPath();
        context.commit();

        Files.write(inputFile3, Collections.singletonList("test"), StandardOpenOption.APPEND);

        context = newIncrementalContext();

        // context.registerInput(inputFile1); DELETED
        context.registerInput(inputFile2); // UNMODIFIED
        context.registerInput(inputFile3); // MODIFIED
        inputFile4 = context.registerInput(inputFile4).getPath(); // NEW

        Map<Path, Metadata> inputs = new TreeMap<>();
        for (Metadata input : context.getRegisteredInputs()) {
            inputs.put(input.getPath(), input);
        }

        assertEquals(4, inputs.size());
        // assertEquals(ResourceStatus.REMOVED, inputs.get(inputFile1).getStatus());
        assertEquals(Status.UNMODIFIED, inputs.get(inputFile2).getStatus());
        assertEquals(Status.MODIFIED, inputs.get(inputFile3).getStatus());
        assertEquals(Status.NEW, inputs.get(inputFile4).getStatus());
    }

    @Test
    void testInputAttributes() throws Exception {
        Path inputFile = Files.createFile(temp.resolve("inputFile"));

        TestIncrementalContext context = newIncrementalContext();
        DefaultInputMetadata metadata = context.registerInput(inputFile);
        assertNull(context.getAttribute(metadata, "key", String.class));
        DefaultInput input = metadata.process();
        assertNull(context.setAttribute(input, "key", "value"));
        context.commit();

        context = newIncrementalContext();
        metadata = context.registerInput(inputFile);
        assertEquals("value", context.getAttribute(metadata, "key", String.class));
        context.commit();

        context = newIncrementalContext();
        metadata = context.registerInput(inputFile);
        assertEquals("value", context.getAttribute(metadata, "key", String.class));
        input = metadata.process();
        assertNull(context.getAttribute(input, "key", String.class));
        assertEquals("value", context.setAttribute(input, "key", "newValue"));
        assertEquals("value", context.setAttribute(input, "key", "newValue"));
        assertEquals("newValue", context.getAttribute(input, "key", String.class));
        context.commit();

        context = newIncrementalContext();
        metadata = context.registerInput(inputFile);
        assertEquals("newValue", context.getAttribute(metadata, "key", String.class));
        context.commit();
    }

    @Test
    void testOutputStatus() throws Exception {
        Path inputFile = Files.createFile(temp.resolve("inputFile"));
        Path outputFile = temp.resolve("outputFile");

        assertFalse(Files.isReadable(outputFile));

        TestIncrementalContext context = newIncrementalContext();
        DefaultOutput output = context.registerInput(inputFile).process().associateOutput(outputFile);
        assertEquals(Status.NEW, output.getStatus());
        output.newOutputStream().close();
        context.commit();

        // no-change rebuild
        context = newIncrementalContext();
        output = context.registerInput(inputFile).process().associateOutput(outputFile);
        assertEquals(Status.UNMODIFIED, output.getStatus());
        context.commit();

        // modified output
        Files.write(outputFile, Collections.singletonList("test"));
        context = newIncrementalContext();
        output = context.registerInput(inputFile).process().associateOutput(outputFile);
        assertEquals(Status.MODIFIED, output.getStatus());
        context.commit();

        // no-change rebuild
        context = newIncrementalContext();
        output = context.registerInput(inputFile).process().associateOutput(outputFile);
        assertEquals(Status.UNMODIFIED, output.getStatus());
        context.commit();

        // deleted output
        Files.delete(outputFile);
        context = newIncrementalContext();
        output = context.registerInput(inputFile).process().associateOutput(outputFile);
        assertEquals(Status.REMOVED, output.getStatus());
        output.newOutputStream().close(); // processed outputs must exit or commit fails
        context.commit();
    }

    @Test
    void testStateSerializationUseTCCL() throws Exception {
        Path inputFile = Files.createFile(temp.resolve("inputFile"));

        TestIncrementalContext context = newIncrementalContext();

        URL dummyJar = new File("src/test/projects/dummy/dummy-1.0.jar").toURI().toURL();
        ClassLoader tccl = new URLClassLoader(new URL[] {dummyJar});
        ClassLoader origTCCL = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(tccl);

            Object dummy = tccl.loadClass("dummy.Dummy").newInstance();

            DefaultResource input = context.registerInput(inputFile).process();
            context.setAttribute(input, "dummy", (Serializable) dummy);
            context.commit();

            context = newIncrementalContext();
            assertFalse(context.isEscalated());
            assertNotNull(context.getAttribute(context.registerInput(inputFile), "dummy", Serializable.class));
            // no commit
        } finally {
            Thread.currentThread().setContextClassLoader(origTCCL);
        }

        // sanity check, make sure empty state is loaded without proper TCCL
        context = newIncrementalContext();
        assertTrue(context.isEscalated());
    }

    @Test
    void testConfigurationChange() throws Exception {
        Path inputFile = Files.createFile(temp.resolve("input"));
        Path outputFile = Files.createFile(temp.resolve("output"));
        Path looseOutputFile = Files.createFile(temp.resolve("looseOutputFile"));

        TestIncrementalContext context = newIncrementalContext();
        context.registerInput(inputFile).process().associateOutput(outputFile);
        context.processOutput(looseOutputFile);
        context.commit();

        context = newIncrementalContext(Collections.singletonMap("config", "parameter"));
        DefaultInputMetadata metadata = context.registerInput(inputFile);
        assertEquals(Status.MODIFIED, metadata.getStatus());
        DefaultInput input = metadata.process();
        assertEquals(Status.MODIFIED, input.getStatus());
        DefaultOutput output = input.associateOutput(outputFile);
        assertEquals(Status.MODIFIED, output.getStatus());
        DefaultOutput looseOutput = context.processOutput(looseOutputFile);
        assertEquals(Status.MODIFIED, looseOutput.getStatus());
    }

    @Test
    void testRegisterInputsIncludesExcludes() throws Exception {
        Files.createDirectory(temp.resolve("folder"));
        Path f1 = Files.createFile(temp.resolve("input1.txt"));
        Path f2 = Files.createFile(temp.resolve("folder/input2.txt"));
        Path f3 = Files.createFile(temp.resolve("folder/input3.log"));

        TestIncrementalContext context = newIncrementalContext();
        List<Path> actual;

        actual = toFileList(context.registerInputs(temp, null, Collections.singletonList("**")));
        assertIncludedPaths(Collections.emptyList(), actual);

        actual = toFileList(context.registerInputs(temp, null, null));
        assertIncludedPaths(Arrays.asList(f1, f2, f3), actual);

        actual = toFileList(context.registerInputs(temp, Collections.singletonList("**/*.txt"), null));
        assertIncludedPaths(Arrays.asList(f1, f2), actual);

        actual = toFileList(
                context.registerInputs(temp, Collections.singletonList("**"), Collections.singletonList("**/*.log")));
        assertIncludedPaths(Arrays.asList(f1, f2), actual);
    }

    @Test
    void testRegisterInputsDirectoryMatching() throws Exception {
        Files.createDirectory(temp.resolve("folder"));
        Files.createDirectory(temp.resolve("folder/subfolder"));
        Path f1 = Files.createFile(temp.resolve("input1.txt"));
        Path f2 = Files.createFile(temp.resolve("folder/input2.txt"));
        Path f3 = Files.createFile(temp.resolve("folder/subfolder/input3.txt"));

        TestIncrementalContext context = newIncrementalContext();
        List<Path> actual;

        // from http://ant.apache.org/manual/dirtasks.html#patterns
        // When ** is used as the name of a directory in the pattern, it matches zero or more
        // directories.

        actual = toFileList(context.registerInputs(temp, Collections.singletonList("**/*.txt"), null));
        assertIncludedPaths(Arrays.asList(f1, f2, f3), actual);

        actual = toFileList(context.registerInputs(temp, Collections.singletonList("folder/**/*.txt"), null));
        assertIncludedPaths(Arrays.asList(f2, f3), actual);

        actual = toFileList(context.registerInputs(temp, Collections.singletonList("folder/*.txt"), null));
        assertIncludedPaths(Collections.singletonList(f2), actual);

        // / is a shortcut for /**
        actual = toFileList(context.registerInputs(temp, Collections.singletonList("/"), null));
        assertIncludedPaths(Arrays.asList(f1, f2, f3), actual);
        actual = toFileList(context.registerInputs(temp, Collections.singletonList("folder/"), null));
        assertIncludedPaths(Arrays.asList(f2, f3), actual);

        // leading / does not matter
        actual = toFileList(context.registerInputs(temp, Collections.singletonList("/folder/"), null));
        assertIncludedPaths(Arrays.asList(f2, f3), actual);
    }

    private List<Path> toFileList(Iterable<? extends DefaultMetadata> inputs) {
        List<Path> files = new ArrayList<>();
        for (DefaultMetadata input : inputs) {
            files.add(input.getPath());
        }
        return files;
    }

    @Test
    void testClosedContext() throws Exception {
        TestIncrementalContext context = newIncrementalContext();

        context.commit();
        assertThrows(IllegalStateException.class, () -> context.registerInput(Files.createTempFile(temp, "", "")));
        assertThrows(IllegalStateException.class, () -> context.processOutput(Files.createTempFile(temp, "", "")));
    }

    @Test
    void testSkipExecution() throws Exception {
        Path inputFile = Files.createFile(temp.resolve("inputFile"));
        Path outputFile = Files.createFile(temp.resolve("outputFile"));

        TestIncrementalContext context = newIncrementalContext();
        DefaultInput input = context.registerInput(inputFile).process();
        input.associateOutput(outputFile);
        context.commit();

        // make a change
        Files.write(inputFile, Collections.singletonList("test"), StandardOpenOption.APPEND);

        // skip execution
        context = newIncrementalContext();
        context.markSkipExecution();
        context.commit();
        assertTrue(Files.isReadable(outputFile));

        //
        context = newIncrementalContext();
        DefaultInputMetadata inputMetadata = context.registerInput(inputFile);
        assertEquals(Status.MODIFIED, inputMetadata.getStatus());
        inputMetadata.process();
        context.commit();
        assertFalse(Files.isReadable(outputFile));
    }

    @Test
    void testSkipExecutionModifiedContext() throws Exception {
        Path inputFile = Files.createFile(temp.resolve("inputFile"));
        Path outputFile = Files.createFile(temp.resolve("outputFile"));

        TestIncrementalContext context = newIncrementalContext();
        DefaultInput input = context.registerInput(inputFile).process();
        input.associateOutput(outputFile);

        assertThrows(IllegalStateException.class, context::markSkipExecution);
    }

    static Stream<Arguments> fileSystems() {
        return Stream.of(
                Arguments.of("Windows", (Supplier<FileSystem>) () -> Jimfs.newFileSystem(Configuration.windows())),
                Arguments.of("Unix", (Supplier<FileSystem>) () -> Jimfs.newFileSystem(Configuration.unix())),
                Arguments.of("MacOS", (Supplier<FileSystem>) () -> Jimfs.newFileSystem(Configuration.osX())),
                Arguments.of("Native", (Supplier<FileSystem>) FileSystems::getDefault));
    }
}
