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
package org.apache.maven.plugins;

import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Provides;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoParameter;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@MojoTest
public class HelloMojoTest {

    @Inject
    private MavenDIComponent componentMock;

    @Test
    @InjectMojo(goal = "hello")
    @MojoParameter(name = "name", value = "World")
    public void testHello(HelloMojo mojoUnderTest) {

        mojoUnderTest.execute();

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(componentMock).hello(captor.capture());
        assertThat(captor.getValue()).isEqualTo("World");
    }

    @Singleton
    @Provides
    private MavenDIComponent createMavenDIComponent() {
        return mock(MavenDIComponent.class);
    }
}
