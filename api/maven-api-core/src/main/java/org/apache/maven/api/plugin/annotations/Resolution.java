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
package org.apache.maven.api.plugin.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.maven.api.annotations.Experimental;

/**
 * Indicates that a given field will be injected with the result of
 * a dependency collection or resolution request. Whether a collection
 * or resolution request is performed is controlled by the {@link #pathScope()}
 * field, the injected field type and the {@link #requestType()}.
 * <p>
 * If the {@code requestType} is not set explicitly, it will be inferred
 * from the {@code pathScope} and the injected field type. If the type
 * is {@link org.apache.maven.api.Node Node} and {@code pathScope == ""},
 * then the dependencies will be <i>collected</i>.
 * If the type is {@link org.apache.maven.api.Node Node} or
 * {@code List<}{@link org.apache.maven.api.Node Node}{@code >},
 * and {@code pathScope != ""}, the dependencies will be <i>flattened</i>.
 * Else the dependencies will be <i>resolved</i> and {@code pathScope} must be non empty,
 * and the field type can be {@link org.apache.maven.api.Node Node},
 * {@code List<}{@link org.apache.maven.api.Node Node}{@code >},
 * {@link org.apache.maven.api.services.DependencyResolverResult DependencyResolverResult},
 * {@code List<}{@link java.nio.file.Path Path}{@code >},
 * {@code Map<}{@link org.apache.maven.api.PathType PathType}{@code , List<}{@link java.nio.file.Path Path}{@code >>},
 * or {@code Map<}{@link org.apache.maven.api.Dependency Dependency}{@code , }{@link java.nio.file.Path Path}{@code >}.
 *
 * @since 4.0.0
 */
@Experimental
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Resolution {

    /**
     * The id of a {@link org.apache.maven.api.PathScope} enum value.
     * If specified, a dependency resolution request will be issued,
     * else a dependency collection request will be done.
     *
     * @return the id of the path scope
     */
    String pathScope() default "";

    /**
     * The request type, in case the default one is not correct.
     * Valid values are {@code collect}, {@code flatten}, or {@code resolve}.
     *
     * @return the request type
     */
    String requestType() default "";
}
