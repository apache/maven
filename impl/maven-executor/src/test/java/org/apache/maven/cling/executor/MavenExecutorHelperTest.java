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
package org.apache.maven.cling.executor;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.apache.maven.cling.executor.MavenExecutorTestSupport.mvn3ExecutorRequestBuilder;
import static org.apache.maven.cling.executor.MavenExecutorTestSupport.mvn4ExecutorRequestBuilder;

public class MavenExecutorHelperTest {
    @Test
    void smoke3() {
        MavenExecutorHelper mvn3 =
                new MavenExecutorHelper(mvn3ExecutorRequestBuilder().build().installationDirectory());
        Path localRepository = mvn3.localRepository(null);
        System.out.println(localRepository);
    }

    @Test
    void smoke4() {
        MavenExecutorHelper mvn4 =
                new MavenExecutorHelper(mvn4ExecutorRequestBuilder().build().installationDirectory());
        Path localRepository = mvn4.localRepository(null);
        System.out.println(localRepository);
    }
}
