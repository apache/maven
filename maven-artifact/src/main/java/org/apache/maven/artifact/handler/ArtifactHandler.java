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
package org.apache.maven.artifact.handler;

/**
 * An artifact handler provides metadata for an artifact derived from the dependency element that references the artifact:<ul>
 * <li>extension and classifier, to be able to download the file,</li>
 * <li>information on how to use the artifact: whether to add it to the classpath, or to take into account its
 * dependencies.</li>
 * </ul>
 *
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 */
public interface ArtifactHandler {
    @Deprecated
    String ROLE = ArtifactHandler.class.getName();

    /**
     * Returns the file name extension used for dependencies of that type;
     * e.g. "jar", "pom", "xml", etc.
     *
     * @return the file extension
     */
    String getExtension();

    String getDirectory();

    /**
     * Returns the classifier of the dependency.
     *
     * @return the classifier
     */
    String getClassifier();

    String getPackaging();

    boolean isIncludesDependencies();

    String getLanguage();

    boolean isAddedToClasspath();
}
