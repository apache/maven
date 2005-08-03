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
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * A base class for EAR-processing related tasks.
 *
 * @author <a href="stephane.nicoll@gmail.com">Stephane Nicoll</a>
 * @version $Id $
 */
public abstract class AbstractEarMojo
    extends AbstractMojo
{

    public static final String APPLICATION_XML_URI = "META-INF/application.xml";

    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * The ear modules configuration.
     *
     * @parameter
     */
    private EarModule[] modules;

    /**
     * Directory that resources are copied to during the build.
     *
     * @parameter expression="${project.build.directory}/${project.build.finalName}"
     * @required
     */
    private String workDirectory;

    private List earModules;

    private File buildDir;

    public void execute()
        throws MojoExecutionException
    {
        getLog().debug( "Resolving ear modules ..." );

        if ( modules != null && modules.length > 0 )
        {

            // Let's validate user-defined modules
            EarModule module = null;
            try
            {
                for ( int i = 0; i < modules.length; i++ )
                {
                    module = (EarModule) modules[i];
                    getLog().debug( "Resolving ear module[" + module + "]" );
                    module.resolveArtifact( project.getArtifacts() );
                }
            }
            catch ( EarPluginException e )
            {
                throw new MojoExecutionException( "Failed to initialize ear modules", e );
            }
            earModules = new ArrayList( Arrays.asList( modules ) );
        }
        else
        {
            earModules = new ArrayList();
        }

        // Let's add other modules
        Set artifacts = project.getArtifacts();
        for ( Iterator iter = artifacts.iterator(); iter.hasNext(); )
        {
            Artifact artifact = (Artifact) iter.next();

            // Artifact is not yet registered and it has neither test, nor a
            // provided scope
            if ( !isArtifactRegistered( artifact, earModules ) &&
                 !Artifact.SCOPE_TEST.equals( artifact.getScope() ) &&
                 !Artifact.SCOPE_PROVIDED.equals( artifact.getScope() )
            {
                EarModule module = EarModuleFactory.newEarModule( artifact );
                earModules.add( module );
            }
        }

    }

    protected List getModules()
    {
        if ( earModules == null )
        {
            throw new IllegalStateException( "Ear modules have not been initialized" );
        }
        return earModules;
    }

    protected File getBuildDir()
    {
        if ( buildDir == null )
        {
            buildDir = new File( workDirectory );
        }
        return buildDir;
    }

    protected MavenProject getProject()
    {
        return project;
    }

    protected String getWorkDirectory()
    {
        return workDirectory;
    }

    private static boolean isArtifactRegistered( Artifact a, List currentList )
    {
        Iterator i = currentList.iterator();
        while ( i.hasNext() )
        {
            EarModule em = (EarModule) i.next();
            if ( em.getArtifact().equals( a ) )
            {
                return true;
            }
        }
        return false;
    }
}
