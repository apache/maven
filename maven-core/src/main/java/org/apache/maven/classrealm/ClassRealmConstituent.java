package org.apache.maven.classrealm;

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

import java.io.File;

/**
 * Describes a constituent of a class realm.
 *
 * @author Benjamin Bentmann
 */
public interface ClassRealmConstituent
{

    /**
     * Gets the group id of the constituent's artifact.
     *
     * @return The group id, never {@code null}.
     */
    String getGroupId();

    /**
     * Gets the artifact id of the constituent's artifact.
     *
     * @return The artifact id, never {@code null}.
     */
    String getArtifactId();

    /**
     * Gets the type of the constituent's artifact.
     *
     * @return The type, never {@code null}.
     */
    String getType();

    /**
     * Gets the classifier of the constituent's artifact.
     *
     * @return The classifier or an empty string, never {@code null}.
     */
    String getClassifier();

    /**
     * Gets the version of the constituent's artifact.
     *
     * @return The version, never {@code null}.
     */
    String getVersion();

    /**
     * Gets the file of the constituent's artifact.
     *
     * @return The file, never {@code null}.
     */
    File getFile();

}
