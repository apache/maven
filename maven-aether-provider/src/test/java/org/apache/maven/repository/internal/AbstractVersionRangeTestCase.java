/*
 * Copyright 2015 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.maven.repository.internal;

import java.util.Arrays;
import java.util.List;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.version.VersionScheme;

import static org.apache.maven.repository.internal.AbstractRepositoryTestCase.newTestRepository;

/**
 *
 *
 */
public abstract class AbstractVersionRangeTestCase extends AbstractRepositoryTestCase
{

  protected final VersionRangeRequest request = new VersionRangeRequest();
  protected final VersionScheme versionScheme = new GenericVersionScheme();

  @Override
  protected void setUp()
          throws Exception
  {
    super.setUp();
    request.addRepository( newTestRepository() );
  }

  @Override
  protected void tearDown()
          throws Exception
  {
    super.tearDown();
  }

  protected static boolean contains_SNAPSHOT_Versions( final List<Version> versions )
  {
    if ( null == versions || versions.isEmpty() )
    {
      return false;
    }
    return Arrays.deepToString( versions.toArray() ).contains( "SNAPSHOT" );
  }

}
