package org.apache.maven.settings.building;

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

/**
 * Collects problems that are encountered during settings building.
 *
 * @author Benjamin Bentmann
 */
public interface SettingsProblemCollector
{

    /**
     * Adds the specified problem.
     *
     * @param severity The severity of the problem, must not be {@code null}.
     * @param message The detail message of the problem, may be {@code null}.
     * @param line The one-based index of the line containing the problem or {@code -1} if unknown.
     * @param column The one-based index of the column containing the problem or {@code -1} if unknown.
     * @param cause The cause of the problem, may be {@code null}.
     */
    void add( SettingsProblem.Severity severity, String message, int line, int column, Exception cause );

}
