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
package org.apache.maven.model.resolution;

import org.apache.maven.model.Repository;

/**
 * Signals an error when adding a repository to the model resolver.
 *
 * @author Benjamin Bentmann
 */
public class InvalidRepositoryException extends Exception {

    /**
     * The repository that raised this error, can be {@code null}.
     */
    private Repository repository;

    /**
     * Creates a new exception with specified detail message and cause for the given repository.
     *
     * @param message The detail message, may be {@code null}.
     * @param repository The repository that caused the error, may be {@code null}.
     * @param cause The cause, may be {@code null}.
     */
    public InvalidRepositoryException(String message, Repository repository, Throwable cause) {
        super(message, cause);
        this.repository = repository;
    }

    /**
     * Creates a new exception with specified detail message for the given repository.
     *
     * @param message The detail message, may be {@code null}.
     * @param repository The repository that caused the error, may be {@code null}.
     */
    public InvalidRepositoryException(String message, Repository repository) {
        super(message);
        this.repository = repository;
    }

    /**
     * Gets the repository that causes this error (if any).
     *
     * @return The repository that causes this error or {@code null} if not known.
     */
    public Repository getRepository() {
        return repository;
    }
}
