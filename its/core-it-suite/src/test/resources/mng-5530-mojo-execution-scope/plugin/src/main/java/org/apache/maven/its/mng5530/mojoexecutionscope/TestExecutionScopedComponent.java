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
package org.apache.maven.its.mng5530.mojoexecutionscope;

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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.MojoExecutionEvent;
import org.apache.maven.execution.scope.MojoExecutionScoped;
import org.apache.maven.execution.scope.WeakMojoExecutionListener;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

@Named
@MojoExecutionScoped
public class TestExecutionScopedComponent implements WeakMojoExecutionListener {
    private final File basedir;

    private MavenSession session;

    private MojoExecution execution;

    @Inject
    public TestExecutionScopedComponent(MavenSession session, MavenProject project, MojoExecution execution) {
        this.session = session;
        this.execution = execution;
        this.basedir = new File(project.getBuild().getDirectory());
    }

    public void recordExecute() {
        touch(new File(basedir, "execution-executed.txt"));
    }

    private void touch(File file) {
        file.getParentFile().mkdirs();
        try {
            new FileOutputStream(file).close();
        } catch (IOException e) {
            // ignore
        }
    }

    public void afterExecutionFailure(MojoExecutionEvent event) {
        touch(new File(basedir, "execution-failure.txt"));
    }

    public void afterMojoExecutionSuccess(MojoExecutionEvent event) throws MojoExecutionException {
        touch(new File(basedir, "execution-success.txt"));
    }

    public void beforeMojoExecution(MojoExecutionEvent event) throws MojoExecutionException {
        touch(new File(basedir, "execution-before.txt"));
    }
}
