package org.apache.maven.api.plugin.annotations;

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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to configure injection of Plexus components by
 * <a href="/ref/current/maven-core/apidocs/org/apache/maven/plugin/MavenPluginManager.html">
 * <code>MavenPluginManager.getConfiguredMojo(...)</code></a>.
 *
 * @author Olivier Lamy
 * @since 3.0
 */
@Documented
@Retention( RetentionPolicy.CLASS )
@Target( { ElementType.FIELD } )
@Inherited
public @interface Component
{
    /**
     * role of the component to inject.
     * @return the role
     */
    Class<?> role() default Object.class;

    /**
     * hint of the component to inject.
     * @return the hint
     */
    String hint() default "";
}
