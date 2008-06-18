package org.apache.maven.embedder;

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

import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.repository.ComponentDescriptor;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.Logger;

import java.io.File;
import java.util.Map;

/** @author Jason van Zyl */
public class MavenEmbedderProjectWithExtensionReadingTest
    extends MavenEmbedderTest
{
    public void testProjectWithExtensionsReading()
        throws Exception
    {
        MavenExecutionRequest request = new DefaultMavenExecutionRequest().setShowErrors( true )
            .setPom( new File( basedir, "src/test/resources/pom2.xml" ) )
            // TODO: Remove this!
            .setLoggingLevel( Logger.LEVEL_DEBUG );

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        MavenEmbedder embedder = new ExtendableMavenEmbedder( classLoader );

        // Here we take the artifact handler and programmatically place it into the container

        ComponentDescriptor cd = new ComponentDescriptor();

        cd.setRole( ArtifactHandler.ROLE );

        cd.setRoleHint( "mkleint" );

        cd.setImplementation( MyArtifactHandler.class.getName() );

        embedder.getPlexusContainer().addComponentDescriptor( cd );

        // At this point the artifact handler will be inside the container and
        // Maven internally will pick up the artifact handler and use it accordingly to
        // create the classpath appropriately.

        MavenExecutionResult result = embedder.readProjectWithDependencies( request );

        assertNoExceptions( result );

        // sources, test sources, and the junit jar..

        assertEquals( 3, result.getProject().getTestClasspathElements().size() );
    }

    private class ExtendableMavenEmbedder
        extends MavenEmbedder
    {

        public ExtendableMavenEmbedder( ClassLoader classLoader )
            throws MavenEmbedderException
        {
            super( new DefaultConfiguration()
                .setClassLoader( classLoader )
                .setMavenEmbedderLogger( new MavenEmbedderConsoleLogger() ) );
        }

        protected Map getPluginExtensionComponents( Plugin plugin )
            throws PluginManagerException
        {
            try
            {
                return getPlexusContainer().lookupMap( ArtifactHandler.ROLE );
            }
            catch ( ComponentLookupException e )
            {
                throw new PluginManagerException( plugin, null );
            }

        }

        protected void verifyPlugin( Plugin plugin,
                                     MavenProject project )
        {
            //ignore don't want to actually verify in test
        }
    }

}
