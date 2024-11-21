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
package org.apache.maven.cling.invoker.mvn.resident;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Arrays;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.apache.maven.api.cli.Invoker;
import org.apache.maven.api.cli.Parser;
import org.apache.maven.cling.invoker.ProtoLookup;
import org.apache.maven.cling.invoker.mvn.MavenInvokerTestSupport;
import org.apache.maven.cling.invoker.mvn.MavenParser;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

/**
 * Resident UT.
 */
public class DefaultResidentMavenInvokerTest extends MavenInvokerTestSupport {

    @Override
    protected Invoker createInvoker() {
        return new ResidentMavenInvoker(ProtoLookup.builder()
                .addMapping(ClassWorld.class, new ClassWorld("plexus.core", ClassLoader.getSystemClassLoader()))
                .build());
    }

    @Override
    protected Parser createParser() {
        return new MavenParser();
    }

    @Test
    void defaultFs(@TempDir(cleanup = CleanupMode.ON_SUCCESS) Path tempDir) throws Exception {
        invoke(tempDir, Arrays.asList("clean", "verify"));
    }

    @Disabled("Until we move off fully from File")
    @Test
    void jimFs() throws Exception {
        try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
            invoke(fs.getPath("/"), Arrays.asList("clean", "verify"));
        }
    }
}
