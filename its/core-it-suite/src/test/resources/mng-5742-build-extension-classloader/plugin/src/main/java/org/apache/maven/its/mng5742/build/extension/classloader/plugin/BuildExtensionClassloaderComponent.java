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
package org.apache.maven.its.mng5742.build.extension.classloader.plugin;

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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;

@Named
@Singleton
public class BuildExtensionClassloaderComponent extends AbstractMavenLifecycleParticipant {
    public static final String FILE_PATH = "target/execution-success.txt";

    @Inject
    public BuildExtensionClassloaderComponent() {}

    private final AtomicInteger pluginExecutionCount = new AtomicInteger();

    private final AtomicInteger extensionExecutionCount = new AtomicInteger();

    public void invokedFromMojo() {
        pluginExecutionCount.incrementAndGet();
    }

    @Override
    public void afterSessionEnd(MavenSession session) throws MavenExecutionException {
        if (extensionExecutionCount.incrementAndGet() != 1) {
            throw new IllegalStateException();
        }

        if (pluginExecutionCount.intValue() != 1) {
            throw new IllegalStateException();
        }

        try {
            File file = new File(session.getTopLevelProject().getBasedir(), FILE_PATH);
            file.getParentFile().mkdirs();
            Writer w = new OutputStreamWriter(new FileOutputStream(file, true), "UTF-8");
            try {
                w.write("executed");
            } finally {
                w.close();
            }
        } catch (IOException e) {
            throw new MavenExecutionException(e.getMessage(), e);
        }
    }
}
