package org.apache.maven.lifecycle.mapping.providers;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.Collections;
import java.util.HashMap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.maven.lifecycle.mapping.DefaultLifecycleMapping;
import org.apache.maven.lifecycle.mapping.Lifecycle;
import org.apache.maven.lifecycle.mapping.LifecycleMapping;
import org.apache.maven.lifecycle.mapping.LifecyclePhase;

@Named( "maven-plugin" )
@Singleton
public final class MavenPluginLifecycleMappingProvider
    implements Provider<LifecycleMapping>
{
  private final LifecycleMapping lifecycleMapping;

  @Inject
  public MavenPluginLifecycleMappingProvider()
  {
    HashMap<String, LifecyclePhase> lifecyclePhases = new HashMap<>();
    lifecyclePhases.put(
        "process-resources",
        new LifecyclePhase( "org.apache.maven.plugins:maven-resources-plugin:3.2.0:resources" )
    );
    lifecyclePhases.put(
        "compile",
        new LifecyclePhase( "org.apache.maven.plugins:maven-compiler-plugin:3.8.1:compile" )
    );
    lifecyclePhases.put(
        "process-classes",
        new LifecyclePhase( "org.apache.maven.plugins:maven-plugin-plugin:3.6.0:descriptor" )
    );
    lifecyclePhases.put(
        "process-test-resources",
        new LifecyclePhase( "org.apache.maven.plugins:maven-resources-plugin:3.2.0:testResources" )
    );
    lifecyclePhases.put(
        "test-compile",
        new LifecyclePhase( "org.apache.maven.plugins:maven-compiler-plugin:3.8.1:testCompile" )
    );
    lifecyclePhases.put(
        "test",
        new LifecyclePhase( "org.apache.maven.plugins:maven-surefire-plugin:3.0.0-M5:test" )
    );
    lifecyclePhases.put(
        "package",
        new LifecyclePhase( "org.apache.maven.plugins:maven-jar-plugin:3.2.0:jar" )
    );
    lifecyclePhases.put(
        "install",
        new LifecyclePhase( "org.apache.maven.plugins:maven-install-plugin:3.0.0-M1:install" )
    );
    lifecyclePhases.put(
        "deploy",
        new LifecyclePhase( "org.apache.maven.plugins:maven-deploy-plugin:3.0.0-M1:deploy" )
    );

    Lifecycle lifecycle = new Lifecycle();
    lifecycle.setId( "default" );
    lifecycle.setLifecyclePhases( Collections.unmodifiableMap( lifecyclePhases ) );

    this.lifecycleMapping = new DefaultLifecycleMapping(
        Collections.singletonList(
            lifecycle
        )
    );
  }

  @Override
  public LifecycleMapping get()
  {
    return lifecycleMapping;
  }
}
