package org.apache.maven.repository.internal;

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

import org.eclipse.aether.impl.MetadataResolver;
import org.eclipse.aether.impl.RepositoryEventDispatcher;
import org.eclipse.aether.impl.SyncContextFactory;
import org.eclipse.aether.spi.log.LoggerFactory;

/**
 * This class is for backward compatibility only. Don't use it anymore.
 * <p>
 * Formally this implementation was the default {@link VersionRangeResolver} implementation and was registered without a
 * <i>hint</i> (means default) instead.
 * </p>
 * <p>
 * The version range algorithm has moved to {@link MavenDefaultVersionRangeResolver} and now this implementation
 * delegate all calls to {@link StrategyVersionRangeResolver}.
 * </p>
 *
 * @author Benjamin Bentmann
 *
 * @deprecated For backward compatibility only. Use {@link MavenDefaultVersionRangeResolver} instead.
 */
@Deprecated
public class DefaultVersionRangeResolver extends StrategyVersionRangeResolver
{

  DefaultVersionRangeResolver( MetadataResolver metadataResolver, SyncContextFactory syncContextFactory,
          RepositoryEventDispatcher repositoryEventDispatcher, LoggerFactory loggerFactory )
  {
    // do nothing
  }

}
