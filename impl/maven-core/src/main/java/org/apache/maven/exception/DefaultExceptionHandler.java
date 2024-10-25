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
package org.apache.maven.exception;

import javax.inject.Named;
import javax.inject.Singleton;

import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import org.apache.maven.api.plugin.MojoException;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.model.building.ModelProblem;
import org.apache.maven.model.building.ModelProblemUtils;
import org.apache.maven.plugin.AbstractMojoExecutionException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.PluginContainerException;
import org.apache.maven.plugin.PluginExecutionException;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingResult;

/*

- test projects for each of these
- how to categorize the problems so that the id of the problem can be match to a page with descriptive help and the test
  project
- nice little sample projects that could be run in the core as well as integration tests

All Possible Errors
- invalid lifecycle phase (maybe same as bad CLI param, though you were talking about embedder too)
- <module> specified is not found
- malformed settings
- malformed POM
- local repository not writable
- remote repositories not available
- artifact metadata missing
- extension metadata missing
- extension artifact missing
- artifact metadata retrieval problem
- version range violation
- circular dependency
- artifact missing
- artifact retrieval exception
- md5 checksum doesn't match for local artifact, need to redownload this
- POM doesn't exist for a goal that requires one
- parent POM missing (in both the repository + relative path)
- component not found

Plugins:
- plugin metadata missing
- plugin metadata retrieval problem
- plugin artifact missing
- plugin artifact retrieval problem
- plugin dependency metadata missing
- plugin dependency metadata retrieval problem
- plugin configuration problem
- plugin execution failure due to something that is know to possibly go wrong (like compilation failure)
- plugin execution error due to something that is not expected to go wrong (the compiler executable missing)
- asking to use a plugin for which you do not have a version defined - tools to easily select versions
- goal not found in a plugin (probably could list the ones that are)

 */

// PluginNotFoundException, PluginResolutionException, PluginDescriptorParsingException,
// CycleDetectedInPluginGraphException;

/**
 * Transform an exception into useful end-user message.
 */
@Named
@Singleton
public class DefaultExceptionHandler implements ExceptionHandler {
    @Override
    public ExceptionSummary handleException(Throwable exception) {
        return handle("", exception);
    }

    private ExceptionSummary handle(String message, Throwable exception) {
        String reference = getReference(Collections.newSetFromMap(new IdentityHashMap<>()), exception);

        List<ExceptionSummary> children = null;

        if (exception instanceof ProjectBuildingException) {
            List<ProjectBuildingResult> results = ((ProjectBuildingException) exception).getResults();

            children = new ArrayList<>();

            for (ProjectBuildingResult result : results) {
                ExceptionSummary child = handle(result);
                if (child != null) {
                    children.add(child);
                }
            }

            message = "The build could not read " + children.size() + " project" + (children.size() == 1 ? "" : "s");
        } else {
            message = getMessage(message, exception);
        }

        return new ExceptionSummary(exception, message, reference, children);
    }

    private ExceptionSummary handle(ProjectBuildingResult result) {
        List<ExceptionSummary> children = new ArrayList<>();

        for (ModelProblem problem : result.getProblems()) {
            ExceptionSummary child = handle(problem, result.getProjectId());
            if (child != null) {
                children.add(child);
            }
        }

        if (children.isEmpty()) {
            return null;
        }

        String message = System.lineSeparator()
                + "The project " + (result.getProjectId().isEmpty() ? "" : result.getProjectId() + " ")
                + "(" + result.getPomFile() + ") has "
                + children.size() + " error" + (children.size() == 1 ? "" : "s");

        return new ExceptionSummary(null, message, null, children);
    }

    private ExceptionSummary handle(ModelProblem problem, String projectId) {
        if (ModelProblem.Severity.ERROR.compareTo(problem.getSeverity()) >= 0) {
            String message = problem.getMessage();

            String location = ModelProblemUtils.formatLocation(problem, projectId);

            if (!location.isEmpty()) {
                message += " @ " + location;
            }

            return handle(message, problem.getException());
        } else {
            return null;
        }
    }

    private String getReference(Set<Throwable> dejaVu, Throwable exception) {
        String reference = "";
        if (!dejaVu.add(exception)) {
            return reference;
        }

        if (exception != null) {
            if (exception instanceof MojoExecutionException) {
                reference = MojoExecutionException.class.getSimpleName();

                Throwable cause = exception.getCause();
                if (cause instanceof IOException) {
                    cause = cause.getCause();
                    if (cause instanceof ConnectException) {
                        reference = ConnectException.class.getSimpleName();
                    }
                }
            } else if (exception instanceof LinkageError) {
                reference = LinkageError.class.getSimpleName();
            } else if (exception instanceof PluginExecutionException) {
                Throwable cause = exception.getCause();

                if (cause instanceof PluginContainerException) {
                    Throwable cause2 = cause.getCause();

                    if (cause2 instanceof NoClassDefFoundError) {
                        String message = cause2.getMessage();
                        if (message != null && message.contains("org/sonatype/aether/")) {
                            reference = "AetherClassNotFound";
                        }
                    }
                }

                if (reference == null || reference.isEmpty()) {
                    reference = getReference(dejaVu, cause);
                }

                if (reference == null || reference.isEmpty()) {
                    reference = exception.getClass().getSimpleName();
                }
            } else if (exception instanceof LifecycleExecutionException) {
                reference = getReference(dejaVu, exception.getCause());
            } else if (isNoteworthyException(exception)) {
                reference = exception.getClass().getSimpleName();
            }
        }

        if ((reference != null && !reference.isEmpty()) && !reference.startsWith("http:")) {
            reference = "http://cwiki.apache.org/confluence/display/MAVEN/" + reference;
        }

        return reference;
    }

    private boolean isNoteworthyException(Throwable exception) {
        if (exception == null) {
            return false;
        } else if (exception instanceof Error) {
            return true;
        } else if (exception instanceof RuntimeException) {
            return false;
        } else {
            return !exception.getClass().getName().startsWith("java");
        }
    }

    private String getMessage(String message, Throwable exception) {
        String fullMessage = (message != null) ? message : "";

        // To break out of possible endless loop when getCause returns "this", or dejaVu for n-level recursion (n>1)
        Set<Throwable> dejaVu = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable t = exception; t != null && t != t.getCause(); t = t.getCause()) {
            String exceptionMessage = t.getMessage();
            String longMessage = null;

            if (t instanceof AbstractMojoExecutionException) {
                longMessage = ((AbstractMojoExecutionException) t).getLongMessage();
            } else if (t instanceof MojoException) {
                longMessage = ((MojoException) t).getLongMessage();
            }

            if (longMessage != null && !longMessage.isEmpty()) {
                if ((exceptionMessage == null || exceptionMessage.isEmpty())
                        || longMessage.contains(exceptionMessage)) {
                    exceptionMessage = longMessage;
                } else if (!exceptionMessage.contains(longMessage)) {
                    exceptionMessage = join(exceptionMessage, System.lineSeparator() + longMessage);
                }
            }

            if (exceptionMessage == null || exceptionMessage.isEmpty()) {
                exceptionMessage = t.getClass().getSimpleName();
            }

            if (t instanceof UnknownHostException && !fullMessage.contains("host")) {
                fullMessage = join(fullMessage, "Unknown host " + exceptionMessage);
            } else if (!fullMessage.contains(exceptionMessage)) {
                fullMessage = join(fullMessage, exceptionMessage);
            }

            if (!dejaVu.add(t)) {
                fullMessage = join(fullMessage, "[CIRCULAR REFERENCE]");
                break;
            }
        }

        return fullMessage.trim();
    }

    private String join(String message1, String message2) {
        String message = "";

        if (message1 != null && !message1.isEmpty()) {
            message = message1.trim();
        }

        if (message2 != null && !message2.isEmpty()) {
            if (message != null && !message.isEmpty()) {
                if (message.endsWith(".") || message.endsWith("!") || message.endsWith(":")) {
                    message += " ";
                } else {
                    message += ": ";
                }
            }

            message += message2;
        }

        return message;
    }
}
