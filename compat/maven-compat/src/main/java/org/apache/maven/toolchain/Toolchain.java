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
package org.apache.maven.toolchain;

/**
 * Toolchain interface.
 *
 * @since 2.0.9
 */
public interface Toolchain {

    /**
     * get the type of toolchain.
     *
     * @return the toolchain type
     */
    String getType();

    /**
     * Gets the platform tool executable.
     *
     * @param toolName the tool platform independent tool name.
     * @return file representing the tool executable, or null if the tool can not be found
     */
    String findTool(String toolName);
}
