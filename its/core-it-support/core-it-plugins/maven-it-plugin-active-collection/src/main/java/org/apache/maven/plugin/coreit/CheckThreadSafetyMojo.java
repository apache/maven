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
import org.apache.maven.plugin.logging.Log;

/**
 * Checks the thread-safe retrieval of components from active component collections.
 *
 * @author Benjamin Bentmann
 */
@Mojo(name = "check-thread-safety", defaultPhase = LifecyclePhase.VALIDATE)
public class CheckThreadSafetyMojo extends AbstractMojo {

    private static final String MAVEN_CORE_IT_LOG = "[MAVEN-CORE-IT-LOG] ";
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
     *
     * @implNote threads need to use different realms to trigger changes of the collections.
     */
    public void execute() throws MojoExecutionException {
        getLog().info(MAVEN_CORE_IT_LOG + "Testing concurrent component access");
        final Thread[] threads = new Thread[2];
        final List<Exception> exceptions = new Vector<>();
        final CountDownLatch startLatch = new CountDownLatch(1);
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(new CheckThreadSafetyTask(
                    (i % 2) == 0 ? getClass().getClassLoader() : MojoExecutionException.class.getClassLoader(),
                    startLatch,
                    componentMap,
                    componentList,
                    exceptions,
                    getLog()
            ));
            threads[i].start();
        }

        startLatch.countDown(); // signal all threads to start
        joinOrInterrupt(threads);
        storeComponentProperties(exceptions);
        getLog().info(MAVEN_CORE_IT_LOG + "Created output file " + outputFile);
    }

    private void joinOrInterrupt(Thread[] threads) {
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                getLog().warn(MAVEN_CORE_IT_LOG + "Interrupted while joining " + thread);
            }
        }
    }

    private void storeComponentProperties( List<Exception> exceptions) throws MojoExecutionException {
        final Properties componentProperties= new Properties();
        componentProperties.setProperty("components", Integer.toString(componentList.size()));
        componentProperties.setProperty("exceptions", Integer.toString(exceptions.size()));

        if (!outputFile.isAbsolute()) {
            outputFile = new File(basedir, outputFile.getPath());
        }

        getLog().info(MAVEN_CORE_IT_LOG + "Creating output file " + outputFile);
        try (OutputStream out = new FileOutputStream(outputFile)) {
            outputFile.getParentFile().mkdirs();
            componentProperties.store(out, "MAVEN-CORE-IT-LOG");
        } catch (IOException e) {
            throw new MojoExecutionException("Output file could not be created: " + outputFile, e);
        }
    }

    private record CheckThreadSafetyTask(
            ClassLoader tccl,
            CountDownLatch startLatch,
            Map<String, TestComponent> map,
            List<TestComponent> list,
            List<Exception> exceptions,
            Log log
    ) implements Runnable {

        @Override
        public void run() {
            log.info(MAVEN_CORE_IT_LOG + "Thread " + Thread.currentThread() + " uses " + tccl);
            Thread.currentThread().setContextClassLoader(tccl);
            try {
                startLatch.await(); // wait for the start signal
                checkThreadSafety();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn(MAVEN_CORE_IT_LOG + "Thread " + Thread.currentThread() + " was interrupted while waiting");
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
                    log.warn(MAVEN_CORE_IT_LOG + "Thread " + Thread.currentThread() + " encountered concurrency issue", e);
                    exceptions.add(e);
                }
            }
        }
    }
}