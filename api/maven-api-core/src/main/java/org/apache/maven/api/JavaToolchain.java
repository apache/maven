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
package org.apache.maven.api;

import org.apache.maven.api.annotations.Experimental;

/**
 * Represents a Java toolchain in the Maven build system.
 *
 * <p>A Java toolchain is a specific type of toolchain that provides access
 * to Java development tools, such as the Java compiler and Java runtime
 * environment. This interface allows users to define and configure
 * Java-related toolchains that can be utilized during the build process
 * in Maven.</p>
 *
 * <p>Java toolchains are defined in the Maven toolchains.xml file and can
 * be referenced in the project's POM file. This enables developers to
 * specify the exact versions of Java tools they wish to use, ensuring
 * consistency across different build environments.</p>
 *
 * @since 4.0.0
 * @see Toolchain
 * @see org.apache.maven.api.services.ToolchainManager
 */
@Experimental
public interface JavaToolchain extends Toolchain {

    String getJavaHome();
}
