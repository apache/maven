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
import org.apache.maven.api.cli.ParserException;
import org.apache.maven.api.cli.ParserRequest;
import org.apache.maven.cling.invoker.ProtoLogger;
import org.apache.maven.cling.invoker.ProtoLookup;
import org.apache.maven.cling.invoker.mvnsh.ShellInvoker;
import org.apache.maven.cling.invoker.mvnsh.ShellParser;
import org.apache.maven.jline.JLineMessageBuilderFactory;
import org.codehaus.plexus.classworlds.ClassWorld;

/**
 * Maven shell.
 */
public class MavenShellCling extends ClingSupport {
    /**
     * "Normal" Java entry point. Note: Maven uses ClassWorld Launcher and this entry point is NOT used under normal
     * circumstances.
     */
    public static void main(String[] args) throws IOException {
        int exitCode = new MavenShellCling().run(args);
        System.exit(exitCode);
    }

    /**
     * ClassWorld Launcher "enhanced" entry point: returning exitCode and accepts Class World.
     */
    public static int main(String[] args, ClassWorld world) throws IOException {
        return main(args, world, null, null, null);
    }

    /**
     * Embedded entry point.
     */
    public static int main(
            String[] args,
            ClassWorld world,
            @Nullable InputStream in,
            @Nullable OutputStream out,
            @Nullable OutputStream err)
            throws IOException {
        return new MavenShellCling(world).run(args, in, out, err);
    }

    public MavenShellCling() {
        super();
    }

    public MavenShellCling(ClassWorld classWorld) {
        super(classWorld);
    }

    @Override
    protected Invoker createInvoker() {
        return new ShellInvoker(
                ProtoLookup.builder().addMapping(ClassWorld.class, classWorld).build());
    }

    @Override
    protected Parser createParser() {
        return new ShellParser();
    }

    @Override
    protected ParserRequest.Builder parserRequestBuilder(String[] args) throws ParserException {
        return ParserRequest.mvnsh(args, new ProtoLogger(), new JLineMessageBuilderFactory());
    }
}
