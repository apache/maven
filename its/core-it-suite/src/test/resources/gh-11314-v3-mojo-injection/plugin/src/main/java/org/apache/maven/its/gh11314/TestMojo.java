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
package org.apache.maven.its.gh11314;

import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.toolchain.ToolchainFactory;

/**
 * A test Mojo that requires injection of ToolchainFactory from the Maven container.
 * This reproduces the issue where V3 Mojos cannot be injected with v3 API beans
 * when only v4 API implementations are available.
 *
 * Tests both named injection (@Named("jdk")) and toolchain manager functionality.
 */
@Mojo(name = "test-goal")
public class TestMojo extends AbstractMojo {

    /**
     * The ToolchainFactory from the Maven container.
     * This field requires injection of the v3 API ToolchainFactory with "jdk" hint.
     */
    @Inject
    @Named("jdk")
    private ToolchainFactory jdkFactory;

    @Override
    public void execute() throws MojoExecutionException {
        if (jdkFactory == null) {
            throw new MojoExecutionException("JDK ToolchainFactory was not injected!");
        }
        getLog().info("JDK ToolchainFactory successfully injected: "
                + jdkFactory.getClass().getName());
    }
}
