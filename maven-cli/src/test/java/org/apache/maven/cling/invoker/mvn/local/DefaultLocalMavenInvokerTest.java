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
package org.apache.maven.cling.invoker.mvn.local;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Arrays;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.apache.maven.api.cli.Invoker;
import org.apache.maven.api.cli.Parser;
import org.apache.maven.api.cli.mvn.MavenInvokerRequest;
import org.apache.maven.api.cli.mvn.MavenOptions;
import org.apache.maven.cling.invoker.ProtoLookup;
import org.apache.maven.cling.invoker.mvn.MavenInvokerTestSupport;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Local UT.
 */
public class DefaultLocalMavenInvokerTest
        extends MavenInvokerTestSupport<MavenOptions, MavenInvokerRequest<MavenOptions>> {
    @Override
    protected Invoker<MavenInvokerRequest<MavenOptions>> createInvoker() {
        return new DefaultLocalMavenInvoker(ProtoLookup.builder()
                .addMapping(ClassWorld.class, new ClassWorld("plexus.core", ClassLoader.getSystemClassLoader()))
                .build());
    }

    @Override
    protected Parser<MavenInvokerRequest<MavenOptions>> createParser() {
        return new DefaultLocalMavenParser();
    }

    @Test
    void defaultFs(@TempDir Path tempDir) throws Exception {
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
