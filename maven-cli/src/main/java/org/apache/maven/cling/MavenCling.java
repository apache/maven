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
import org.apache.maven.api.cli.ParserException;
import org.apache.maven.api.cli.mvn.MavenInvokerRequest;
import org.apache.maven.api.cli.mvn.MavenOptions;
import org.apache.maven.cling.invoker.ProtoLogger;
import org.apache.maven.cling.invoker.ProtoLookup;
import org.apache.maven.cling.invoker.mvn.DefaultMavenParser;
import org.apache.maven.cling.invoker.mvn.local.DefaultLocalMavenInvoker;
import org.apache.maven.jline.JLineMessageBuilderFactory;
import org.codehaus.plexus.classworlds.ClassWorld;

/**
 * Maven CLI "new-gen".
 */
public class MavenCling extends ClingSupport<MavenOptions, MavenInvokerRequest<MavenOptions>> {
    /**
     * "Normal" Java entry point. Note: Maven uses ClassWorld Launcher and this entry point is NOT used under normal
     * circumstances.
     */
    public static void main(String[] args) throws IOException {
        int exitCode = new MavenCling().run(args);
        System.exit(exitCode);
    }

    /**
     * ClassWorld Launcher "enhanced" entry point: returning exitCode and accepts Class World.
     */
    public static int main(String[] args, ClassWorld world) throws IOException {
        return new MavenCling(world).run(args);
    }

    public MavenCling() {
        super();
    }

    public MavenCling(ClassWorld classWorld) {
        super(classWorld);
    }

    @Override
    protected Invoker<MavenInvokerRequest<MavenOptions>> createInvoker() {
        return new DefaultLocalMavenInvoker(
                ProtoLookup.builder().addMapping(ClassWorld.class, classWorld).build());
    }

    @Override
    protected MavenInvokerRequest<MavenOptions> parseArguments(String[] args) throws ParserException, IOException {
        return new DefaultMavenParser().mvn(args, new ProtoLogger(), new JLineMessageBuilderFactory());
    }
}
