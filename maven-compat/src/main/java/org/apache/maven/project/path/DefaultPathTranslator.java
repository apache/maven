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
package org.apache.maven.project.path;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Reporting;
import org.apache.maven.model.Resource;
import org.codehaus.plexus.component.annotations.Component;

/**
 * DefaultPathTranslator
 */
@Deprecated
@Component(role = PathTranslator.class)
public class DefaultPathTranslator implements PathTranslator {
    private static final String[] BASEDIR_EXPRESSIONS = {"${basedir}", "${pom.basedir}", "${project.basedir}"};

    public void alignToBaseDirectory(Model model, File basedir) {
        if (basedir == null) {
            return;
        }

        Build build = model.getBuild();

        if (build != null) {
            build.setDirectory(alignToBaseDirectory(build.getDirectory(), basedir));

            build.setSourceDirectory(alignToBaseDirectory(build.getSourceDirectory(), basedir));

            build.setTestSourceDirectory(alignToBaseDirectory(build.getTestSourceDirectory(), basedir));

            for (Resource resource : build.getResources()) {
                resource.setDirectory(alignToBaseDirectory(resource.getDirectory(), basedir));
            }

            for (Resource resource : build.getTestResources()) {
                resource.setDirectory(alignToBaseDirectory(resource.getDirectory(), basedir));
            }

            if (build.getFilters() != null) {
                List<String> filters = new ArrayList<>();
                for (String filter : build.getFilters()) {
                    filters.add(alignToBaseDirectory(filter, basedir));
                }
                build.setFilters(filters);
            }

            build.setOutputDirectory(alignToBaseDirectory(build.getOutputDirectory(), basedir));

            build.setTestOutputDirectory(alignToBaseDirectory(build.getTestOutputDirectory(), basedir));
        }

        Reporting reporting = model.getReporting();

        if (reporting != null) {
            reporting.setOutputDirectory(alignToBaseDirectory(reporting.getOutputDirectory(), basedir));
        }
    }

    public String alignToBaseDirectory(String path, File basedir) {
        if (basedir == null) {
            return path;
        }

        if (path == null) {
            return null;
        }

        String s = stripBasedirToken(path);

        File file = new File(s);
        if (file.isAbsolute()) {
            // path was already absolute, just normalize file separator and we're done
            s = file.getPath();
        } else if (file.getPath().startsWith(File.separator)) {
            // drive-relative Windows path, don't align with project directory but with drive root
            s = file.getAbsolutePath();
        } else {
            // an ordinary relative path, align with project directory
            s = new File(new File(basedir, s).toURI().normalize()).getAbsolutePath();
        }

        return s;
    }

    private String stripBasedirToken(String s) {
        if (s != null) {
            String basedirExpr = null;
            for (String expression : BASEDIR_EXPRESSIONS) {
                if (s.startsWith(expression)) {
                    basedirExpr = expression;
                    break;
                }
            }

            if (basedirExpr != null) {
                if (s.length() > basedirExpr.length()) {
                    // Take out basedir expression and the leading slash
                    s = chopLeadingFileSeparator(s.substring(basedirExpr.length()));
                } else {
                    s = ".";
                }
            }
        }

        return s;
    }

    /**
     * Removes the leading directory separator from the specified filesystem path (if any). For platform-independent
     * behavior, this method accepts both the forward slash and the backward slash as separator.
     *
     * @param path The filesystem path, may be <code>null</code>.
     * @return The altered filesystem path or <code>null</code> if the input path was <code>null</code>.
     */
    private String chopLeadingFileSeparator(String path) {
        if (path != null) {
            if (path.startsWith("/") || path.startsWith("\\")) {
                path = path.substring(1);
            }
        }
        return path;
    }

    public void unalignFromBaseDirectory(Model model, File basedir) {
        if (basedir == null) {
            return;
        }

        Build build = model.getBuild();

        if (build != null) {
            build.setDirectory(unalignFromBaseDirectory(build.getDirectory(), basedir));

            build.setSourceDirectory(unalignFromBaseDirectory(build.getSourceDirectory(), basedir));

            build.setTestSourceDirectory(unalignFromBaseDirectory(build.getTestSourceDirectory(), basedir));

            for (Resource resource : build.getResources()) {
                resource.setDirectory(unalignFromBaseDirectory(resource.getDirectory(), basedir));
            }

            for (Resource resource : build.getTestResources()) {
                resource.setDirectory(unalignFromBaseDirectory(resource.getDirectory(), basedir));
            }

            if (build.getFilters() != null) {
                List<String> filters = new ArrayList<>();
                for (String filter : build.getFilters()) {
                    filters.add(unalignFromBaseDirectory(filter, basedir));
                }
                build.setFilters(filters);
            }

            build.setOutputDirectory(unalignFromBaseDirectory(build.getOutputDirectory(), basedir));

            build.setTestOutputDirectory(unalignFromBaseDirectory(build.getTestOutputDirectory(), basedir));
        }

        Reporting reporting = model.getReporting();

        if (reporting != null) {
            reporting.setOutputDirectory(unalignFromBaseDirectory(reporting.getOutputDirectory(), basedir));
        }
    }

    public String unalignFromBaseDirectory(String path, File basedir) {
        if (basedir == null) {
            return path;
        }

        if (path == null) {
            return null;
        }

        path = path.trim();

        String base = basedir.getAbsolutePath();
        if (path.startsWith(base)) {
            path = chopLeadingFileSeparator(path.substring(base.length()));
        }

        if (path.length() <= 0) {
            path = ".";
        }

        if (!new File(path).isAbsolute()) {
            path = path.replace('\\', '/');
        }

        return path;
    }
}
