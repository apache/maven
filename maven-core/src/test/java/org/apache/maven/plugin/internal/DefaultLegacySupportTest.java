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
package org.apache.maven.plugin.internal;

import java.util.concurrent.CountDownLatch;

import junit.framework.TestCase;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;

/**
 * @author Kristian Rosenvold
 */
public class DefaultLegacySupportTest extends TestCase {
    final CountDownLatch latch = new CountDownLatch(1);
    final DefaultLegacySupport defaultLegacySupport = new DefaultLegacySupport();

    public void testSetSession() throws Exception {

        MavenExecutionRequest mavenExecutionRequest = new DefaultMavenExecutionRequest();
        MavenSession m1 = new MavenSession(null, null, mavenExecutionRequest, null);
        defaultLegacySupport.setSession(m1);

        MyRunnable myRunnable = new MyRunnable();
        Thread thread = new Thread(myRunnable);
        thread.start();

        MavenSession m2 = new MavenSession(null, null, mavenExecutionRequest, null);
        defaultLegacySupport.setSession(m2);
        latch.countDown();
        thread.join();
        assertNull(myRunnable.getSession());
    }

    class MyRunnable implements Runnable {

        private volatile MavenSession session;

        public void run() {
            try {
                latch.await();
            } catch (InterruptedException ignore) {
                // Test may fail if we get interrupted
            }
            session = defaultLegacySupport.getSession();
        }

        public MavenSession getSession() {
            return session;
        }
    }
}
