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

import org.apache.maven.cling.invoker.InvokerException;
import org.apache.maven.cling.invoker.ParserException;
import org.apache.maven.cling.invoker.mvn.local.LocalInvoker;
import org.apache.maven.cling.invoker.mvn.local.LocalParser;
import org.apache.maven.jline.MessageUtils;
import org.codehaus.plexus.classworlds.ClassWorld;

import static java.util.Objects.requireNonNull;

/**
 * Maven CLI "new-gen".
 */
public class MavenCling {
    static final String CORE_CLASS_REALM_ID = "plexus.core";

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

    private final ClassWorld classWorld;
    private final boolean classWorldManaged;

    /**
     * Ctor that creates "managed" ClassWorld. This constructor is not used in "normal" circumstances.
     */
    public MavenCling() {
        this.classWorld =
                new ClassWorld(CORE_CLASS_REALM_ID, Thread.currentThread().getContextClassLoader());
        this.classWorldManaged = true;
    }

    /**
     * Ctor used when running in ClassWorlds Launcher.
     */
    public MavenCling(ClassWorld classWorld) {
        this.classWorld = requireNonNull(classWorld);
        this.classWorldManaged = false;
    }

    /**
     * The main entry point.
     */
    public int run(String[] args) throws IOException {
        MessageUtils.systemInstall();
        MessageUtils.registerShutdownHook();
        try {
            return new LocalInvoker(classWorld).invoke(new LocalParser().parse(args));
        } catch (ParserException e) {
            System.err.println(e.getMessage());
            return 1;
        } catch (InvokerException e) {
            return 1;
        } finally {
            if (classWorldManaged) {
                classWorld.close();
            }
            MessageUtils.systemUninstall();
        }
    }
}
