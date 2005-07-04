package org.apache.maven.plugin.ear.module;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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

import org.apache.maven.artifact.Artifact;

/**
 * Builds an {@link EarModule} based on an <tt>Artifact</tt>.
 *
 * @author Stephane Nicoll <stephane.nicoll@gmail.com>
 * @author $Author: sni $ (last edit)
 * @version $Revision: 1.2 $
 */
public final class EarModuleFactory
{

    /**
     * Creates a new {@link EarModule} based on the specified <tt>Artifact</tt>.
     *
     * @param artifact the artifact
     * @return an ear module for this artifact
     */
    public static final EarModule newEarModule( Artifact artifact )
    {
        if ( "jar".equals( artifact.getType() ) )
        {
            return new JavaModule( getUri( artifact ), artifact );
        }
        else if ( "ejb".equals( artifact.getType() ) )
        {
            return new EjbModule( getUri( artifact ), artifact );
        }
        else if ( "rar".equals( artifact.getType() ) )
        {
            return new RarModule( getUri( artifact ), artifact );
        }
        else if ( "war".equals( artifact.getType() ) )
        {
            return new WebModule( getUri( artifact ), artifact, getContextRoot( artifact ) );
        }
        else
        {
            throw new IllegalStateException( "Could not handle artifact type[" + artifact.getType() + "]" );
        }
    }

    /**
     * Returns the URI for the specifed <tt>Artifact</tt>.
     *
     * @param artifact the artifact
     * @return the URI of this artifact in the EAR file
     * @TODO handle custom URI - for now it returns the file name
     */
    private static String getUri( Artifact artifact )
    {
        return artifact.getFile().getName();
    }

    /**
     * Returns the context root for the specifed war <tt>Artifact</tt>.
     *
     * @param artifact the artifact
     * @return the context root of the artifact
     * @TODO handle custom context root - for now it returns the artifact Id
     */
    private static String getContextRoot( Artifact artifact )
    {
        if ( artifact.getType().equals( "war" ) )
        {
            return "/" + artifact.getArtifactId();
        }
        else
        {
            throw new IllegalStateException(
                "Could not get context-root for an artifact with type[" + artifact.getType() + "]" );
        }
    }
}
