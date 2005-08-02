package org.apache.maven.plugin.ear;

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
 * @author <a href="stephane.nicoll@gmail.com">Stephane Nicoll</a>
 * @version $Id$
 */
public final class EarModuleFactory
{

    /**
     * Creates a new {@link EarModule} based on the
     * specified {@link Artifact}.
     *
     * @param artifact the artifact
     * @return an ear module for this artifact
     */
    public static final EarModule newEarModule( Artifact artifact )
    {
        if ( "jar".equals( artifact.getType() ) )
        {
            return new JavaModule( artifact );
        }
        else if ( "ejb".equals( artifact.getType() ) )
        {
            return new EjbModule( artifact );
        }
        else if ( "ejb-client".equals( artifact.getType() ) )
        {
            return new EjbClientModule( artifact );
        }
        else if ( "rar".equals( artifact.getType() ) )
        {
            return new RarModule( artifact );
        }
        else if ( "war".equals( artifact.getType() ) )
        {
            return new WebModule( artifact );
        }
        else
        {
            throw new IllegalStateException( "Could not handle artifact type[" + artifact.getType() + "]" );
        }
    }

}
