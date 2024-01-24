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
 * An artifact handler contains information explaining how an artifact plugs into the Maven build:<ul>
 * <li>Information needed to find the artifact file in a repository including extension and classifier</li>
 * <li>Information on how to use the artifact as a dependency: whether to add it to the classpath, whether to load its
 * dependencies transitively</li>
 * </ul>
 *
 */
public interface ArtifactHandler {
    @Deprecated
    String ROLE = ArtifactHandler.class.getName();

    /**
     * Returns the file name extension of the artifact;
     * e.g. "jar", "pom", "xml", etc.
     *
     * @return the file extension
     */
    String getExtension();

    String getDirectory();

    /**
     * Returns the default classifier used if a different one is not set in pom.xml.
     *
     * @return the classifier
     */
    String getClassifier();

    String getPackaging();

    boolean isIncludesDependencies();

    String getLanguage();

    /**
     * IMPORTANT: this is WRONGLY NAMED method (and/or remnant for Maven2).
     * Its meaning is "is added to build path", that is used to create classpath/modulepath/etc.
     */
    boolean isAddedToClasspath();
}
