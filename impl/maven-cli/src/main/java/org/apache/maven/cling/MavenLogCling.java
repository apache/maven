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
package org.apache.maven.cling;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.cli.Invoker;
import org.apache.maven.api.cli.Parser;
import org.apache.maven.api.cli.ParserRequest;
import org.apache.maven.cling.invoker.ProtoLookup;
import org.apache.maven.cling.invoker.mvnlog.LogInvoker;
import org.apache.maven.cling.invoker.mvnlog.LogParser;
import org.codehaus.plexus.classworlds.ClassWorld;

/**
 * Maven build report / log viewer CLI.
 */
public class MavenLogCling extends ClingSupport {
    /**
     * Java entry point.
     */
    public static void main(String[] args) throws IOException {
        int exitCode = new MavenLogCling().run(args, null, null, null, false);
        System.exit(exitCode);
    }

    /**
     * ClassWorld Launcher "enhanced" entry point.
     */
    public static int main(String[] args, ClassWorld world) throws IOException {
        return new MavenLogCling(world).run(args, null, null, null, false);
    }

    /**
     * ClassWorld Launcher "embedded" entry point.
     */
    public static int main(
            String[] args,
            ClassWorld world,
            @Nullable InputStream stdIn,
            @Nullable OutputStream stdOut,
            @Nullable OutputStream stdErr)
            throws IOException {
        return new MavenLogCling(world).run(args, stdIn, stdOut, stdErr, true);
    }

    public MavenLogCling() {
        super();
    }

    public MavenLogCling(ClassWorld classWorld) {
        super(classWorld);
    }

    @Override
    protected Invoker createInvoker() {
        return new LogInvoker(
                ProtoLookup.builder().addMapping(ClassWorld.class, classWorld).build(), null);
    }

    @Override
    protected Parser createParser() {
        return new LogParser();
    }

    @Override
    protected ParserRequest.Builder createParserRequestBuilder(String[] args) {
        return ParserRequest.mvnlog(args, createMessageBuilderFactory());
    }
}
