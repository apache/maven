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
package org.apache.maven.api.di;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Provides a unique identifier for dependencies when multiple implementations
 * of the same type are available.
 * <p>
 * This annotation can be used in conjunction with {@link Inject} to specify
 * which implementation should be injected when multiple candidates exist.
 * The value represents a unique identifier for the dependency.
 * <p>
 * Example usage:
 * <pre>
 * {@literal @}Inject
 * {@literal @}Named("mysql")
 * private Repository mysqlRepository;
 * </pre>
 *
 * @see Inject
 * @see Qualifier
 * @since 4.0.0
 */
@Qualifier
@Retention(RUNTIME)
@Documented
public @interface Named {
    /**
     * The name identifier for the annotated element.
     * <p>
     * If no value is specified, the default empty string will be used.
     * When used as a qualifier, this value helps distinguish between different
     * implementations or instances of the same type.
     *
     * @return the name that identifies this component
     */
    String value() default "";
}
