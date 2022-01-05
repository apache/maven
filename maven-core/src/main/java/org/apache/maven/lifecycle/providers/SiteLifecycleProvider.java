package org.apache.maven.lifecycle.providers;

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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.maven.lifecycle.Lifecycle;
import org.apache.maven.lifecycle.mapping.LifecyclePhase;

/**
 * {@code site} lifecycle provider.
 */
@Named( "site" )
@Singleton
public final class SiteLifecycleProvider
    implements Provider<Lifecycle>
{
  private static final String LIFECYCLE_ID = "site";

  private static final String[] PHASES = {
      "pre-site",
      "site",
      "post-site",
      "site-deploy"
  };

  private final Lifecycle lifecycle;

  @Inject
  public SiteLifecycleProvider()
  {
    HashMap<String, LifecyclePhase> defaultBindings = new HashMap<>();
    defaultBindings.put( "site", new LifecyclePhase( "org.apache.maven.plugins:maven-site-plugin:3.9.1:site" ) );
    defaultBindings.put( "site-deploy", new LifecyclePhase( "org.apache.maven.plugins:maven-site-plugin:3.9.1:deploy" ) );

    this.lifecycle = new Lifecycle(
        LIFECYCLE_ID,
        Collections.unmodifiableList( Arrays.asList( PHASES ) ),
        Collections.unmodifiableMap( defaultBindings )
    );
  }

  @Override
  public Lifecycle get()
  {
    return lifecycle;
  }
}
