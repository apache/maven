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
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Explicitly specifies the types that should be used for dependency injection.
 * <p>
 * This annotation allows you to limit which types of a bean should be available
 * for injection. It can be used to prevent unintended automatic binding of implemented
 * interfaces or extended classes.
 * <p>
 * Example usage:
 * <pre>
 * {@literal @}Typed(ServiceImpl.class)
 * public class ServiceImpl implements Service {
 *     // Implementation
 * }
 * </pre>
 *
 * @since 4.0.0
 */
@Target({FIELD, METHOD, TYPE})
@Retention(RUNTIME)
@Documented
public @interface Typed {
    /**
     * Specifies the types that should be considered for dependency injection.
     * <p>
     * When specified, only the listed types will be available for injection,
     * even if the class implements or extends other types. If empty, the
     * default behavior is to make all supertypes available for injection.
     * <p>
     * Example:
     * <pre>
     * {@literal @}Typed({Service.class, Monitored.class})
     * public class ServiceImpl implements Service, Monitored, Logging {
     *     // Only Service and Monitored will be available for injection,
     *     // Logging interface will be ignored
     * }
     * </pre>
     *
     * @return an array of classes that should be considered for injection
     */
    Class<?>[] value() default {};
}
