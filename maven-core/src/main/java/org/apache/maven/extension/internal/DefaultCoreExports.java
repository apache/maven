package org.apache.maven.extension.internal;

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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.project.ExtensionDescriptor;
import org.apache.maven.project.ExtensionDescriptorBuilder;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.util.IOUtil;

import com.google.common.collect.ImmutableSet;

/**
 * @since 3.2.6
 */
@Named
@Singleton
public class DefaultCoreExports
{
    private static final ExtensionDescriptorBuilder builder = new ExtensionDescriptorBuilder();

    private final Set<String> artifacts;

    private final Set<String> packages;

    @Inject
    public DefaultCoreExports( PlexusContainer container )
        throws IOException
    {
        Set<String> artifacts = new LinkedHashSet<String>();
        Set<String> packages = new LinkedHashSet<String>();

        Enumeration<URL> extensions =
            container.getContainerRealm().getResources( builder.getExtensionDescriptorLocation() );
        while ( extensions.hasMoreElements() )
        {
            InputStream is = extensions.nextElement().openStream();
            try
            {
                ExtensionDescriptor descriptor = builder.build( is );

                artifacts.addAll( descriptor.getExportedArtifacts() );
                packages.addAll( descriptor.getExportedPackages() );
            }
            finally
            {
                IOUtil.close( is );
            }
        }
        this.artifacts = ImmutableSet.copyOf( artifacts );
        this.packages = ImmutableSet.copyOf( packages );
    }

    /**
     * Returns artifacts exported by Maven core and core extensions. Artifacts are identified by their
     * groupId:artifactId.
     */
    public Set<String> getExportedArtifacts()
    {
        return artifacts;
    }

    /**
     * Returns packages exported by Maven core and core extensions.
     */
    public Set<String> getExportedPackages()
    {
        return packages;
    }
}
