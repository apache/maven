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

import java.util.Iterator;
import java.util.Set;

/**
 * A base implementation of an {@link EarModule}.
 *
 * @author <a href="stephane.nicoll@gmail.com">Stephane Nicoll</a>
 * @version $Id$
 */
public abstract class AbstractEarModule
    implements EarModule
{

    protected static final String MODULE_ELEMENT = "module";

    private String uri;

    private Artifact artifact;

    // Those are set by the configuration

    private String groupId;

    private String artifactId;

    private String bundleDir;

    private String bundleFileName;

    /**
     * Empty constructor to be used when the module
     * is built based on the configuration.
     */
    public AbstractEarModule()
    {
    }

    /**
     * Creates an ear module from the artifact.
     *
     * @param a the artifact
     */
    public AbstractEarModule( Artifact a )
    {
        this.artifact = a;
        this.groupId = a.getGroupId();
        this.artifactId = a.getArtifactId();
        this.bundleDir = null;
    }

    public void resolveArtifact( Set artifacts )
        throws EarPluginException
    {
        if ( artifact != null )
        {
            return;
        }
        else
        {
            if ( groupId == null || artifactId == null )
            {
                throw new EarPluginException(
                    "Could not resolve artifact[" + groupId + ":" + artifactId + ":" + getType() + "]" );
            }

            Iterator i = artifacts.iterator();
            while ( i.hasNext() )
            {
                Artifact a = (Artifact) i.next();
                if ( a.getGroupId().equals( groupId ) && a.getArtifactId().equals( artifactId ) &&
                    a.getType().equals( getType() ) )
                {
                    artifact = a;
                    return;
                }
            }

            // Artifact has not been found
            throw new EarPluginException( "Artifact[" + groupId + ":" + artifactId + ":" + getType() + "] " +
                "is not a dependency of the project." );
        }
    }

    /**
     * Returns the type associated to the module.
     *
     * @return the artifact's type of the module
     */
    protected abstract String getType();

    public Artifact getArtifact()
    {
        return artifact;
    }

    public String getUri()
    {
        if ( uri == null )
        {
            if ( getBundleDir() == null )
            {
                uri = getBundleFileName();
            }
            else
            {
                uri = getBundleDir() + getBundleFileName();
            }
        }
        return uri;
    }

    /**
     * Returns the artifact's groupId.
     *
     * @return the group Id
     */
    public String getGroupId()
    {
        return groupId;
    }

    /**
     * Returns the artifact's Id.
     *
     * @return the artifact Id
     */
    public String getArtifactId()
    {
        return artifactId;
    }

    /**
     * Returns the bundle directory. If null, the module
     * is bundled in the root of the EAR.
     *
     * @return the custom bundle directory
     */
    public String getBundleDir()
    {
        if ( bundleDir != null )
        {
            bundleDir = cleanBundleDir( bundleDir );
        }
        return bundleDir;
    }

    /**
     * Returns the bundle file name. If null, the artifact's
     * file name is returned.
     *
     * @return the bundle file name
     */
    public String getBundleFileName()
    {
        if ( bundleFileName == null )
        {
            bundleFileName = artifact.getFile().getName();
        }
        return bundleFileName;
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append( getType() ).append( ":" ).append( groupId ).append( ":" ).append( artifactId );
        if ( artifact != null )
        {
            sb.append( ":" ).append( artifact.getVersion() );
        }
        return sb.toString();
    }

    /**
     * Cleans the bundle directory so that it might be used
     * properly.
     *
     * @param bundleDir the bundle directory to clean
     * @return the cleaned bundle directory
     */
    private static String cleanBundleDir( String bundleDir )
    {
        if ( bundleDir == null )
        {
            return bundleDir;
        }

        // Using slashes
        bundleDir = bundleDir.replace( '\\', '/' );

        // Remove '/' prefix if any so that directory is a relative path
        if ( bundleDir.startsWith( " / " ) )
        {
            bundleDir = bundleDir.substring( 1, bundleDir.length() );
        }

        // Adding '/' suffix to specify a directory structure
        if ( !bundleDir.endsWith( "/" ) )
        {
            bundleDir = bundleDir + "/";
        }

        System.out.println( "Bundle dir[" + bundleDir + "]" );

        return bundleDir;
    }
}
