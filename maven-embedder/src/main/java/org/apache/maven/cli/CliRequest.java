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
package org.apache.maven.cli;

import java.io.File;
import java.nio.file.Path;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.codehaus.plexus.classworlds.ClassWorld;

/**
 * CliRequest
 */
public class CliRequest {
    String[] args;

    CommandLine commandLine;

    ClassWorld classWorld;

    String workingDirectory;

    File multiModuleProjectDirectory;

    Path rootDirectory;

    Path topDirectory;

    boolean debug;

    boolean quiet;

    boolean showErrors = true;

    Properties userProperties = new Properties();

    Properties systemProperties = new Properties();

    MavenExecutionRequest request;

    CliRequest(String[] args, ClassWorld classWorld) {
        this.args = args;
        this.classWorld = classWorld;
        this.request = new DefaultMavenExecutionRequest();
    }

    public String[] getArgs() {
        return args;
    }

    public CommandLine getCommandLine() {
        return commandLine;
    }

    public ClassWorld getClassWorld() {
        return classWorld;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public File getMultiModuleProjectDirectory() {
        return multiModuleProjectDirectory;
    }

    public boolean isDebug() {
        return debug;
    }

    public boolean isQuiet() {
        return quiet;
    }

    public boolean isShowErrors() {
        return showErrors;
    }

    public Properties getUserProperties() {
        return userProperties;
    }

    public Properties getSystemProperties() {
        return systemProperties;
    }

    public MavenExecutionRequest getRequest() {
        return request;
    }

    public void setUserProperties(Properties properties) {
        this.userProperties.putAll(properties);
    }
}
