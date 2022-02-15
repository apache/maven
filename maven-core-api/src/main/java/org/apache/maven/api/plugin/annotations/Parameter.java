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
 * Used to configure your Mojo parameters to be injected by
 * <a href="/ref/current/maven-core/apidocs/org/apache/maven/plugin/MavenPluginManager.html">
 * <code>MavenPluginManager.getConfiguredMojo(...)</code></a>.
 * <p>
 * Beans injected into Mojo parameters are prepared by <a href="https://www.eclipse.org/sisu/">Sisu</a> JSR330-based
 * container: this annotation is only effective on fields of the Mojo class itself, nested bean injection
 * requires Sisu or JSR330 annotations.
 *
 * @author Olivier Lamy
 * @since 3.0
 */
@Documented
@Retention( RetentionPolicy.CLASS )
@Target( { ElementType.FIELD } )
@Inherited
public @interface Parameter
{
    /**
     * name of the bean property used to get/set the field: by default, field name is used.
     * @return the name of the bean property
     */
    String name() default "";

    /**
     * alias supported to get parameter value.
     * @return the alias
     */
    String alias() default "";

    /**
     * Property to use to retrieve a value. Can come from <code>-D</code> execution, setting properties or pom
     * properties.
     * @return property name
     */
    String property() default "";

    /**
     * parameter default value, may contain <code>${...}</code> expressions which will be interpreted at
     * inject time: see
     * <a href="/ref/current/maven-core/apidocs/org/apache/maven/plugin/PluginParameterExpressionEvaluator.html">
     * PluginParameterExpressionEvaluator</a>. 
     * @return the default value
     */
    String defaultValue() default "";

    /**
     * is the parameter required?
     * @return <code>true</code> if the Mojo should fail when the parameter cannot be injected
     */
    boolean required() default false;

    /**
     * Specifies that this parameter cannot be configured directly by the user (as in the case of POM-specified
     * configuration). This is useful when you want to force the user to use common POM elements rather than plugin
     * configurations, as in the case where you want to use the artifact's final name as a parameter. In this case, you
     * want the user to modify <code>&lt;build&gt;&lt;finalName/&gt;&lt;/build&gt;</code> rather than specifying a value
     * for finalName directly in the plugin configuration section. It is also useful to ensure that - for example - a
     * List-typed parameter which expects items of type Artifact doesn't get a List full of Strings.
     * 
     * @return <code>true</code> if the user should not be allowed to configure the parameter directly
     */
    boolean readonly() default false;
}
