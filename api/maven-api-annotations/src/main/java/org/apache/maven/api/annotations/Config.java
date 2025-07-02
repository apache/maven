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
 * Annotation used to mark fields that represent configuration properties.
 * This annotation provides metadata about how the configuration property
 * should be handled, including its source, type, default value, and whether it's read-only.
 *
 * @since 4.0.0
 */
@Experimental
@Documented
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
public @interface Config {

    /**
     * Specifies the source of the configuration property, which determines when and where the property
     * will be read from or set for consumption in the Maven build lifecycle.
     *
     * The source indicates whether the property is:
     * - Set by Maven itself at startup (SYSTEM_PROPERTIES)
     * - Configured by users through external means like CLI options (USER_PROPERTIES)
     * - Defined in the project's POM file (MODEL)
     *
     * @return the source of the configuration property, defaults to USER_PROPERTIES
     * @see Source for detailed information about each source type and when it's used
     */
    Source source() default Source.USER_PROPERTIES;

    /**
     * Specifies the type of the configuration property.
     *
     * @return the fully qualified class name of the property type, defaults to "java.lang.String"
     */
    String type() default "java.lang.String";

    /**
     * Specifies the default value of the configuration property.
     *
     * @return the default value as a string, defaults to empty string
     */
    String defaultValue() default "";

    /**
     * Specifies whether the configuration property is read-only.
     *
     * @return true if the property is read-only, false otherwise
     */
    boolean readOnly() default false;

    /**
     * Property source, which determines when and where the property will be read from or set for consumption.
     * The source indicates the timing of property evaluation in the Maven build lifecycle and the location
     * where the property value is defined.
     */
    enum Source {
        /**
         * Maven system properties. These properties are evaluated very early during the boot process,
         * typically set by Maven itself and flagged as readOnly=true or by users via maven-system.properties files.
         * System properties are initialized before the build starts and are available throughout the entire Maven
         * execution. They are used for core Maven functionality that needs to be established at startup.
         */
        SYSTEM_PROPERTIES,
        /**
         * Maven user properties. These are properties that users configure through various means such as
         * maven-user.properties files, maven.config files, command line parameters (-D flags), settings.xml,
         * or environment variables. They are evaluated during the build process and represent the primary
         * way for users to customize Maven's behavior at runtime.
         */
        USER_PROPERTIES,
        /**
         * Project model properties. These properties are defined in the project's POM file (pom.xml) and
         * are read from the project model during the build. They represent build-time configuration that
         * is specific to the project and is stored with the project definition itself rather than in
         * external configuration.
         */
        MODEL
    }
}
