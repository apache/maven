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
package jar;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

public abstract class AbstractCheckMojo extends AbstractMojo {

    protected static boolean forkHasRun = false;

    /**
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * @parameter default-value="${executedProject}"
     * @required
     * @readonly
     */
    private MavenProject executionProject;

    public void execute() throws MojoExecutionException {
        if (getMainProject().getBasedir() == null) {
            throw new MojoExecutionException("Basedir is null on the main project instance.");
        }

        if (getTestProject().getBasedir() == null) {
            throw new MojoExecutionException(
                    "Basedir is null on the " + getTestProjectLabel() + " instance (during mojo execution).");
        }

        String executionBasedir = getTestProject().getBasedir().getAbsolutePath();

        Map failedPaths = new LinkedHashMap();

        checkListOfPaths(getTestProject().getCompileSourceRoots(), executionBasedir, "compileSourceRoots", failedPaths);
        checkListOfPaths(
                getTestProject().getTestCompileSourceRoots(), executionBasedir, "testCompileSourceRoots", failedPaths);

        // MNG-3741: Don't worry about relative paths in scriptSourceRoots.
        // checkListOfPaths( getTestProject().getScriptSourceRoots(), executionBasedir, "scriptSourceRoots", failedPaths
        // );

        if (!failedPaths.isEmpty()) {
            StringBuffer buffer = new StringBuffer();
            buffer.append("The following paths were relative (should have been absolute):");
            for (Iterator it = failedPaths.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry entry = (Map.Entry) it.next();

                buffer.append("\n-  ")
                        .append(entry.getKey())
                        .append(": '")
                        .append(entry.getValue())
                        .append("'");
            }

            throw new MojoExecutionException(buffer.toString());
        }

        forkHasRun = true;
    }

    protected MavenProject getMainProject() {
        return project;
    }

    protected MavenProject getExecutionProject() {
        return executionProject;
    }

    protected abstract MavenProject getTestProject();

    protected abstract String getTestProjectLabel();

    private void checkListOfPaths(List paths, String base, String label, Map failedPaths) {
        if (paths != null && !paths.isEmpty()) {
            for (int i = 0; i < paths.size(); i++) {
                String root = (String) paths.get(i);
                if (!root.startsWith(base)) {
                    failedPaths.put(label + "[" + i + "]", root);
                }
            }
        }
    }
}
