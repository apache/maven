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

import org.apache.maven.api.cli.Invoker;
import org.apache.maven.api.cli.Parser;
import org.apache.maven.api.cli.mvn.MavenInvokerRequest;
import org.apache.maven.api.cli.mvn.MavenOptions;
import org.apache.maven.cling.invoker.ProtoLookup;
import org.apache.maven.cling.invoker.mvn.local.DefaultLocalMavenInvoker;
import org.apache.maven.cling.invoker.mvn.local.DefaultLocalMavenParser;
import org.codehaus.plexus.classworlds.ClassWorld;

/**
 * Maven CLI "new-gen".
 */
public class MavenCling extends ClingSupport<MavenOptions, MavenInvokerRequest<MavenOptions>> {
    public static final String NAME = "mvn";

    /**
     * "Normal" Java entry point. Note: Maven uses ClassWorld Launcher and this entry point is NOT used under normal
     * circumstances.
     */
    public static void main(String[] args) throws IOException {
        int exitCode = new MavenCling(NAME).run(args);
        System.exit(exitCode);
    }

    /**
     * ClassWorld Launcher "enhanced" entry point: returning exitCode and accepts Class World.
     */
    public static int main(String[] args, ClassWorld world) throws IOException {
        return new MavenCling(NAME, world).run(args);
    }

    public MavenCling(String command) {
        super(command);
    }

    public MavenCling(String command, ClassWorld classWorld) {
        super(command, classWorld);
    }

    @Override
    protected Invoker<MavenInvokerRequest<MavenOptions>> createInvoker() {
        return new DefaultLocalMavenInvoker(
                ProtoLookup.builder().addMapping(ClassWorld.class, classWorld).build());
    }

    @Override
    protected Parser<MavenInvokerRequest<MavenOptions>> createParser() {
        return new DefaultLocalMavenParser();
    }
}
