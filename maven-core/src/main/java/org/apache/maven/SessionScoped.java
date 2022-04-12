package org.apache.maven;

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

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.google.inject.ScopeAnnotation;

/**
 * Indicates that annotated component should be instantiated before session execution starts
 * and discarded after session execution completes.
 *
 * Note that components will be cached in the session scope and be injected with the root session.
 * Derived sessions will reuse the same components than their root sessions, thus components
 * should not rely on {@link org.apache.maven.execution.MavenSession#getCurrentProject()} which
 * will always return the root project.
 *
 * @author Jason van Zyl
 * @since 3.2.0
 */
@Target( { TYPE } )
@Retention( RUNTIME )
@ScopeAnnotation
public @interface SessionScoped
{
}
