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
package org.apache.maven.toolchain.java;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.maven.toolchain.DefaultToolchain;
import org.apache.maven.toolchain.model.ToolchainModel;
import org.apache.maven.utils.Os;
import org.slf4j.Logger;

/**
 * JDK toolchain implementation.
 *
 * @since 2.0.9, renamed from DefaultJavaToolChain in 3.2.4
 */
public class JavaToolchainImpl extends DefaultToolchain implements JavaToolchain {
    private String javaHome;

    public static final String KEY_JAVAHOME = "jdkHome"; // NOI18N

    JavaToolchainImpl(ToolchainModel model, Logger logger) {
        super(model, "jdk", logger);
    }

    public String getJavaHome() {
        return javaHome;
    }

    public void setJavaHome(String javaHome) {
        this.javaHome = javaHome;
    }

    public String toString() {
        return "JDK[" + getJavaHome() + "]";
    }

    public String findTool(String toolName) {
        Path toRet = findTool(toolName, Paths.get(getJavaHome()).normalize());
        if (toRet != null) {
            return toRet.toAbsolutePath().toString();
        }
        return null;
    }

    private static Path findTool(String toolName, Path installDir) {
        Path bin = installDir.resolve("bin"); // NOI18N
        if (Files.isDirectory(bin)) {
            if (Os.IS_WINDOWS) {
                Path tool = bin.resolve(toolName + ".exe");
                if (Files.exists(tool)) {
                    return tool;
                }
            }
            Path tool = bin.resolve(toolName);
            if (Files.exists(tool)) {
                return tool;
            }
        }
        return null;
    }
}
