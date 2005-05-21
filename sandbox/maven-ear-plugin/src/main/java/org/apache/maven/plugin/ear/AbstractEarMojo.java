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
import org.apache.maven.plugin.ear.module.EarModule;
import org.apache.maven.plugin.ear.module.EarModuleFactory;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.ArrayList;
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
     * @description "the maven project to use"
     */
    private MavenProject project;

    /**
     * Directory that resources are copied to during the build.
     *
     * @parameter expression="${project.build.directory}/${project.build.finalName}"
     * @required
     */
    private String earDirectory;

    private List modules;

    private File buildDir;

    protected List getModules()
    {
        if ( modules == null )
        {
            // Gather modules and copy them
            modules = new ArrayList();
            Set artifacts = project.getArtifacts();
            for ( Iterator iter = artifacts.iterator(); iter.hasNext(); )
            {
                Artifact artifact = (Artifact) iter.next();
                if ( !Artifact.SCOPE_TEST.equals( artifact.getScope() ) )
                {
                    EarModule module = EarModuleFactory.newEarModule( artifact );
                    modules.add( module );
                }
            }
        }
        return modules;
    }

    protected File getBuildDir()
    {
        if ( buildDir == null )
        {
            buildDir = new File( earDirectory );
        }
        return buildDir;
    }

    protected MavenProject getProject()
    {
        return project;
    }

    protected String getEarDirectory()
    {
        return earDirectory;
    }
}
