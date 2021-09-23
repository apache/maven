package org.apache.maven.lifecycle.mapping;

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
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

/**
 * Alternate implementation of {@link LifecycleMapping} as default one is too much plexus-suited.
 */
public final class LifecycleMappingImpl
    implements LifecycleMapping
{
  private final Map<String, Lifecycle> lifecycles;

  public LifecycleMappingImpl( final List<Lifecycle> lifecycles )
  {
    this.lifecycles = Collections.unmodifiableMap(
        lifecycles.stream().collect( toMap( Lifecycle::getId, l -> l ) )
    );
  }

  @Override
  public Map<String, Lifecycle> getLifecycles()
  {
    return lifecycles;
  }

  @Deprecated
  @Override
  public List<String> getOptionalMojos( final String lifecycle )
  {
    return null;
  }

  @Deprecated
  @Override
  public Map<String, String> getPhases( final String lifecycle )
  {
    return null;
  }
}
