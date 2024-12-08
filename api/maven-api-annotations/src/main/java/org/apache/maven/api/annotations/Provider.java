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
package org.apache.maven.api.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A type implemented by, or extended by maven itself.
 * Maven provides implementations of those types and may inject them in plugins.
 * <p>
 * A type can be marked {@link Consumer} or {@link Provider} but not both. A type is assumed to be
 * {@link Consumer} if it is not marked either {@link Consumer} or {@link Provider}.
 * <p>
 * A package can be marked {@link Provider}. In this case, all types in the package are considered
 * to be a provider type regardless of whether they are marked {@link Consumer} or {@link Provider}.
 *
 * @see Consumer
 * @since 4.0.0
 */
@Experimental
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.PACKAGE})
public @interface Provider {}
