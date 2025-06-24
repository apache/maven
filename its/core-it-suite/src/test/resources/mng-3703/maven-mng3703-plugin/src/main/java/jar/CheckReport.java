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
import java.util.Locale;
import java.util.Map;

import org.apache.maven.doxia.siterenderer.DefaultSiteRenderer;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;

/**
 * @goal check-report
 * @execute phase="compile"
 */
public class CheckReport extends AbstractMavenReport {

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

    /**
     * @parameter default-value="${project.build.directory}/generated-site/mng3703"
     * @readonly
     */
    private String outputDirectory;

    protected void executeReport(Locale locale) throws MavenReportException {
        if (getMainProject().getBasedir() == null) {
            throw new MavenReportException("Basedir is null on the main project instance.");
        }

        if (executionProject.getBasedir() == null) {
            throw new MavenReportException("Basedir is null on the forked project instance (during report execution).");
        }

        String executionBasedir = executionProject.getBasedir().getAbsolutePath();

        Map failedPaths = new LinkedHashMap();

        checkListOfPaths(executionProject.getCompileSourceRoots(), executionBasedir, "compileSourceRoots", failedPaths);
        checkListOfPaths(
                executionProject.getTestCompileSourceRoots(), executionBasedir, "testCompileSourceRoots", failedPaths);

        // MNG-3741: Don't worry about relative paths in scriptSourceRoots.
        // checkListOfPaths( executionProject.getScriptSourceRoots(), executionBasedir, "scriptSourceRoots", failedPaths
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

            throw new MavenReportException(buffer.toString());
        }
    }

    protected MavenProject getMainProject() {
        return project;
    }

    protected MavenProject getExecutionProject() {
        return executionProject;
    }

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

    protected String getOutputDirectory() {
        return outputDirectory;
    }

    protected MavenProject getProject() {
        return project;
    }

    protected Renderer getSiteRenderer() {
        return new DefaultSiteRenderer();
    }

    public String getDescription(Locale locale) {
        return getOutputName();
    }

    public String getName(Locale locale) {
        return getOutputName();
    }

    public String getOutputName() {
        return "check-report";
    }
}
