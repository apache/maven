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

import org.sonatype.aether.impl.ArtifactDescriptorReader;
import org.sonatype.aether.impl.internal.DefaultServiceLocator;
import org.sonatype.aether.impl.MetadataGeneratorFactory;
import org.sonatype.aether.impl.VersionRangeResolver;
import org.sonatype.aether.impl.VersionResolver;

/**
 * A simple service locator that is already setup with all components from this library. To acquire a complete
 * repository system, clients need to add some repository connectors for remote transfers. <em>Note:</em> This component
 * is meant to assist those clients that employ the repository systems outside of an IoC container, Maven plugins
 * should instead always use regular dependency injection to acquire the repository system.
 * 
 * @author Benjamin Bentmann
 */
public class MavenServiceLocator
    extends DefaultServiceLocator
{

    /**
     * Creates a new service locator that already knows about all service implementations included in this library.
     */
    public MavenServiceLocator()
    {
        addService( ArtifactDescriptorReader.class, DefaultArtifactDescriptorReader.class );
        addService( VersionResolver.class, DefaultVersionResolver.class );
        addService( VersionRangeResolver.class, DefaultVersionRangeResolver.class );
        addService( MetadataGeneratorFactory.class, SnapshotMetadataGeneratorFactory.class );
        addService( MetadataGeneratorFactory.class, VersionsMetadataGeneratorFactory.class );
    }

}
