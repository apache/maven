package plugin;

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

import java.util.Iterator;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

/**
 * @goal check
 * @phase validate
 */
public class MyMojo
    extends AbstractMojo
{

    /**
     * Not used, just an offset to place reactorProjects in the middle.
     * @parameter default-value="${project.build.directory}"
     */
    private String outputDirectory;

    /**
     * @parameter expression="${reactorProjects}"
     * @required
     */
    private List reactorProjects;

    /**
     * Not used, just an offset to place reactorProjects in the middle.
     * @parameter default-value="${project.build.directory}"
     */
    private String outputDirectory2;

    public void execute()
        throws MojoExecutionException
    {
        for ( Iterator it = reactorProjects.iterator(); it.hasNext(); )
        {
            MavenProject project = (MavenProject) it.next();

            String basedir = project.getBasedir().getAbsolutePath();
            List compileSourceRoots = project.getCompileSourceRoots();

            System.out.println( " Compile-source roots for project: " + project + " are: " + project.getCompileSourceRoots() );
            for ( Iterator srcIt = compileSourceRoots.iterator(); srcIt.hasNext(); )
            {
                String srcRoot = (String) srcIt.next();

                if ( !srcRoot.startsWith( basedir ) )
                {
                    throw new MojoExecutionException( "Source root: " + srcRoot + " doesn't begin with project basedir: " + basedir );
                }
            }
        }
    }
}
