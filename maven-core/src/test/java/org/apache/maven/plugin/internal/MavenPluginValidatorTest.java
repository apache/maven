package org.apache.maven.plugin.internal;

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

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.AbstractCoreMavenComponentTestCase;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.internal.MavenPluginValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import javax.inject.Inject;

/**
 * @author Michael Simacek
 */
public class MavenPluginValidatorTest extends AbstractCoreMavenComponentTestCase
{
    @Inject
    private MavenPluginValidator mavenPluginValidator;

    protected String getProjectsDirectory()
    {
        return "src/test/projects/default-maven";
    }

    @Test
    public void testValidate()
    {
        Artifact plugin = new DefaultArtifact( "org.apache.maven.its.plugins", "maven-it-plugin", "0.1", "compile",
                "jar", null, new DefaultArtifactHandler() );
        PluginDescriptor descriptor = new PluginDescriptor();
        descriptor.setGroupId( "org.apache.maven.its.plugins" );
        descriptor.setArtifactId( "maven-it-plugin" );
        descriptor.setVersion( "0.1" );
        List<String> errors = new ArrayList<>();
        mavenPluginValidator.validate( plugin, descriptor, errors );
        assertTrue( errors.isEmpty() );
    }

    @Test
    public void testInvalidGroupId()
    {
        Artifact plugin = new DefaultArtifact( "org.apache.maven.its.plugins", "maven-it-plugin", "0.1", "compile",
                "jar", null, new DefaultArtifactHandler() );
        PluginDescriptor descriptor = new PluginDescriptor();
        descriptor.setGroupId( "org.apache.maven.its.plugins.invalid" );
        descriptor.setArtifactId( "maven-it-plugin" );
        descriptor.setVersion( "0.1" );
        List<String> errors = new ArrayList<>();
        mavenPluginValidator.validate( plugin, descriptor, errors );
        assertFalse( errors.isEmpty() );
    }

    @Test
    public void testInvalidArtifactId()
    {
        Artifact plugin = new DefaultArtifact( "org.apache.maven.its.plugins", "maven-it-plugin", "0.1", "compile",
                "jar", null, new DefaultArtifactHandler() );
        PluginDescriptor descriptor = new PluginDescriptor();
        descriptor.setGroupId( "org.apache.maven.its.plugins" );
        descriptor.setArtifactId( "maven-it-plugin.invalid" );
        descriptor.setVersion( "0.1" );
        List<String> errors = new ArrayList<>();
        mavenPluginValidator.validate( plugin, descriptor, errors );
        assertFalse( errors.isEmpty() );
    }

    @Test
    public void testInvalidVersion()
    {
        Artifact plugin = new DefaultArtifact( "org.apache.maven.its.plugins", "maven-it-plugin", "0.1", "compile",
                "jar", null, new DefaultArtifactHandler() );
        PluginDescriptor descriptor = new PluginDescriptor();
        descriptor.setGroupId( "org.apache.maven.its.plugins" );
        descriptor.setArtifactId( "maven-it-plugin" );
        List<String> errors = new ArrayList<>();
        mavenPluginValidator.validate( plugin, descriptor, errors );
        assertFalse( errors.isEmpty() );
    }
}
