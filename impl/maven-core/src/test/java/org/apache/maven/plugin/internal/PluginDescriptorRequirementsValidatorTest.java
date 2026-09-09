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
package org.apache.maven.plugin.internal;

import java.util.Collections;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.PluginValidationManager;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.codehaus.plexus.component.repository.ComponentRequirement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PluginDescriptorRequirementsValidatorTest {

    @Mock
    private PluginValidationManager pluginValidationManager;

    @Mock
    private MavenSession mavenSession;

    @Mock
    private MojoDescriptor mojoDescriptor;

    @InjectMocks
    private PluginDescriptorRequirementsValidator validator;

    private final Class<?> mojoClass = PluginDescriptorRequirementsValidatorTest.class;

    @Test
    void testValidateReportsIssueWhenMojoHasRequirements() {
        when(mojoDescriptor.getRequirements()).thenReturn(List.of(new ComponentRequirement()));

        validator.validate(mavenSession, mojoDescriptor, mojoClass, null, null);

        verify(pluginValidationManager)
                .reportPluginMojoValidationIssue(
                        eq(PluginValidationManager.IssueLocality.EXTERNAL),
                        eq(mavenSession),
                        eq(mojoDescriptor),
                        eq(mojoClass),
                        contains("Plugin uses Plexus Component requirements"));
    }

    @Test
    void testValidateDoesNotReportIssueWhenMojoHasNoRequirements() {
        when(mojoDescriptor.getRequirements()).thenReturn(Collections.emptyList());

        validator.validate(mavenSession, mojoDescriptor, mojoClass, null, null);

        verify(pluginValidationManager, never())
                .reportPluginMojoValidationIssue(
                        any(PluginValidationManager.IssueLocality.class),
                        any(MavenSession.class),
                        any(MojoDescriptor.class),
                        any(Class.class),
                        any(String.class));
    }
}
