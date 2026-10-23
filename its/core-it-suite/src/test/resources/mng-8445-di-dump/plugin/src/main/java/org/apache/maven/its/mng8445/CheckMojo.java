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
package org.apache.maven.its.mng8445;

import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.api.Project;
import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Provides;
import org.apache.maven.api.plugin.Log;
import org.apache.maven.api.plugin.Mojo;

@org.apache.maven.api.plugin.annotations.Mojo(name = "check")
public class CheckMojo implements Mojo {
    @Inject
    Session session;

    @Inject
    Project project;

    @Inject
    Log log;

    @Override
    public void execute() throws Exception {
        if (session == null || project == null || log == null) {
            throw new IllegalStateException("Missing Mojo context");
        }
        Path result = project.basedir().resolve("target/di-check.txt");
        Files.createDirectories(result.getParent());
        Files.writeString(result, Long.toString(Thread.currentThread().getId()));
        log.info("DI-CHECK " + project.artifactId() + " "
                + Thread.currentThread().getName());
    }

    @Named
    public static class Unused {
        @Inject
        @Nullable
        Runnable optional;

        public Unused() {
            throw new IllegalStateException("Diagnostics provisioned an unused component");
        }

        @Provides
        @Named("never-created")
        String provide() {
            throw new IllegalStateException("Diagnostics invoked an unused provider");
        }

        @Override
        public String toString() {
            throw new IllegalStateException("Diagnostics invoked component toString");
        }
    }
}
