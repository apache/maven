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

import javax.lang.model.SourceVersion;
import javax.tools.Tool;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import org.apache.maven.cling.invoker.InvokerException;
import org.apache.maven.cling.invoker.ParserException;
import org.apache.maven.cling.invoker.ParserRequest;
import org.apache.maven.cling.invoker.local.LocalInvoker;
import org.apache.maven.cling.invoker.local.LocalParser;
import org.apache.maven.jline.MessageUtils;
import org.codehaus.plexus.classworlds.ClassWorld;

/**
 * Maven Encryption Tool.
 */
public class MavenEncryptTool implements Tool {
    public MavenEncryptTool() {}

    @Override
    public String name() {
        return "mvnenc";
    }

    @Override
    public Set<SourceVersion> getSourceVersions() {
        // basically "all"?
        return Collections.unmodifiableSet(EnumSet.range(SourceVersion.RELEASE_3, SourceVersion.latest()));
    }

    @Override
    public int run(InputStream in, OutputStream out, OutputStream err, String... arguments) {
        PrintStream stderr = toPsOrDef(err, System.err);
        try (ClassWorld classWorld = new ClassWorld(
                MavenCling.CORE_CLASS_REALM_ID, Thread.currentThread().getContextClassLoader())) {
            MessageUtils.systemInstall();
            MessageUtils.registerShutdownHook();
            try {
                return new LocalInvoker(classWorld)
                        .invoke(new LocalParser()
                                .parse(ParserRequest.builder(arguments)
                                        .in(in)
                                        .out(toPs(out))
                                        .err(toPs(err))
                                        .build()));
            } catch (ParserException e) {
                stderr.println(e.getMessage());
                return 1;
            } catch (InvokerException e) {
                return 1;
            } finally {
                MessageUtils.systemUninstall();
            }
        } catch (IOException e) {
            e.printStackTrace(stderr);
            return 2;
        }
    }

    private PrintStream toPs(OutputStream outputStream) {
        return toPsOrDef(outputStream, null);
    }

    private PrintStream toPsOrDef(OutputStream outputStream, PrintStream def) {
        if (outputStream == null) {
            return def;
        }
        if (outputStream instanceof PrintStream ps) {
            return ps;
        }
        return new PrintStream(outputStream);
    }
}
