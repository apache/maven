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
package org.apache.maven.cling.executor.impl;

import org.apache.maven.cling.executor.ExecutorHelper;
import org.apache.maven.cling.executor.internal.HelperImpl;
import org.junit.jupiter.api.Test;

import static org.apache.maven.cling.executor.MavenExecutorTestSupport.mvn3ExecutorRequestBuilder;
import static org.apache.maven.cling.executor.MavenExecutorTestSupport.mvn4ExecutorRequestBuilder;

public class HelperImplTest {
    @Test
    void smoke3() {
        try (ExecutorHelper mvn3 =
                new HelperImpl(mvn3ExecutorRequestBuilder().build().installationDirectory())) {
            String localRepository = mvn3.localRepository(mvn3.executorRequest());
            System.out.println(localRepository);
        }
    }

    @Test
    void smoke4() {
        try (ExecutorHelper mvn4 =
                new HelperImpl(mvn4ExecutorRequestBuilder().build().installationDirectory())) {
            String localRepository = mvn4.localRepository(mvn4.executorRequest());
            System.out.println(localRepository);
        }
    }
}
