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
package org.apache.maven.plugin;

/**
 * Base exception.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public abstract class AbstractMojoExecutionException extends Exception {
    protected Object source;

    protected String longMessage;

    public AbstractMojoExecutionException(String message) {
        super(message);
    }

    public AbstractMojoExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new {@code AbstractMojoExecutionException} exception wrapping an underlying {@code Throwable}.
     *
     * @param cause the cause which is saved for later retrieval by the {@link #getCause()} method.
     *              A {@code null} value is permitted, and indicates that the cause is nonexistent or unknown.
     * @since 3.8.3
     */
    public AbstractMojoExecutionException(Throwable cause) {
        super(cause);
    }

    public String getLongMessage() {
        return longMessage;
    }

    public Object getSource() {
        return source;
    }
}
