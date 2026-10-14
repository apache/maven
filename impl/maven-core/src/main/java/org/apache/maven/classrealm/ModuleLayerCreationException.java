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
package org.apache.maven.classrealm;

/**
 * Thrown when a modular plugin's {@link ModuleLayer} cannot be created.
 * <p>
 * This is a hard error with no fallback: if a plugin declares {@code <modular>true</modular>}
 * in its descriptor, it <strong>must</strong> be loadable as a JPMS module. Common causes
 * include split packages between dependencies, missing {@code module-info.class} in the
 * plugin JAR, or unresolvable module dependencies.
 * </p>
 *
 * @since 4.1.0
 */
public class ModuleLayerCreationException extends Exception {

    /**
     * Creates a new exception with the specified detail message.
     *
     * @param message the detail message
     */
    public ModuleLayerCreationException(String message) {
        super(message);
    }

    /**
     * Creates a new exception with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public ModuleLayerCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
