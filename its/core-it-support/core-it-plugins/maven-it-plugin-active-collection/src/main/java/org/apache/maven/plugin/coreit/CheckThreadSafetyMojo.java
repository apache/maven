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
package org.apache.maven.plugin.coreit;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Checks the thread-safe retrieval of components from active component collections.
 *
 * @author Benjamin Bentmann
 */
@Mojo(name = "check-thread-safety", defaultPhase = LifecyclePhase.VALIDATE)
public class CheckThreadSafetyMojo extends AbstractMojo {

    /**
     * Project base directory used for manual path alignment.
     */
    @Parameter(defaultValue = "${basedir}", readonly = true)
    private File basedir;

    /**
     * The available components, as a map.
     */
    @Component
    private Map<String, TestComponent> componentMap;

    /**
     * The available components, as a list.
     */
    @Component
    private List<TestComponent> componentList;

    /**
     * The path to the properties file to create.
     */
    @Parameter(property = "collections.outputFile")
    private File outputFile;

    /**
     * Runs this mojo.
     *
     * @throws MojoExecutionException If the output file could not be created.
     */
    public void execute() throws MojoExecutionException {
        Properties componentProperties = new Properties();

        getLog().info("[MAVEN-CORE-IT-LOG] Testing concurrent component access");

        final List<Exception> exceptions = new Vector<>();
        final CountDownLatch startLatch = new CountDownLatch(1);

        Thread[] threads = new Thread[2];
        for (int i = 0; i < threads.length; i++) {
            // NOTE: The threads need to use different realms to trigger changes of the collections
            threads[i] = new CheckThreadSafetyThread((i % 2) == 0 ? getClass().getClassLoader() : MojoExecutionException.class.getClassLoader(), startLatch, componentMap, componentList, exceptions);
            threads[i].start();
        }

        startLatch.countDown(); // Signal all threads to start

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                getLog().warn("[MAVEN-CORE-IT-LOG] Interrupted while joining " + thread);
            }
        }

        componentProperties.setProperty("components", Integer.toString(componentList.size()));
        componentProperties.setProperty("exceptions", Integer.toString(exceptions.size()));

        if (!outputFile.isAbsolute()) {
            outputFile = new File(basedir, outputFile.getPath());
        }

        getLog().info("[MAVEN-CORE-IT-LOG] Creating output file " + outputFile);

        OutputStream out = null;
        try {
            outputFile.getParentFile().mkdirs();
            out = new FileOutputStream(outputFile);
            componentProperties.store(out, "MAVEN-CORE-IT-LOG");
        } catch (IOException e) {
            throw new MojoExecutionException("Output file could not be created: " + outputFile, e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ignored) {
                }
            }
        }

        getLog().info("[MAVEN-CORE-IT-LOG] Created output file " + outputFile);
    }

    class CheckThreadSafetyThread extends Thread {
        private final ClassLoader tccl;
        private final CountDownLatch startLatch;
        private final Map<?, ?> map;
        private final List<?> list;
        private final List<Exception> exceptions;

        CheckThreadSafetyThread(ClassLoader cl, CountDownLatch startLatch, Map<?, ?> map, List<?> list, List<Exception> exceptions) {
            this.tccl = cl;
            this.startLatch = startLatch;
            this.map = map;
            this.list = list;
            this.exceptions = exceptions;
        }

        @Override
        public void run() {
            getLog().info("[MAVEN-CORE-IT-LOG] Thread " + this + " uses " + tccl);
            Thread.currentThread().setContextClassLoader(tccl);
            try {
                startLatch.await(); // Wait for the start signal
                checkThreadSafety();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                getLog().warn("[MAVEN-CORE-IT-LOG] Thread " + this + " was interrupted while waiting");
            }
        }

        private void checkThreadSafety() {
            for (int j = 0; j < 10 * 1000; j++) {
                try {
                    for (Object o : map.values()) {
                        o.toString();
                    }
                    for (Object aList : list) {
                        aList.toString();
                    }
                } catch (Exception e) {
                    getLog().warn("[MAVEN-CORE-IT-LOG] Thread " + this + " encountered concurrency issue", e);
                    exceptions.add(e);
                }
            }
        }
    }
}