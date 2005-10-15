package org.apache.maven.plugin.eclipse;

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


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * A Maven2 plugin to ensure that the classpath variable MAVEN_REPO exists in the Eclipse environment.
 *
 * @goal add-maven-repo
 */
public class AddMavenRepoMojo extends AbstractMojo
{
    /**
     * Location of the <code>Eclipse</code> workspace that holds your configuration and source.
     * 
     * On Windows, this will be the <code>workspace</code> directory under your eclipse
     *     installation. For example, if you installed eclipse into <code>c:\eclipse</code>, the
     *     workspace is <code>c:\eclipse\workspace</code>.
     *
     * @parameter expression="${eclipse.workspace}"
     * @required
     */
    private String workspace;
    
    /**
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;

    public void execute() throws MojoExecutionException
    {
        workspace += "X";
        
        File workDir = new File( workspace, ".metadata/.plugins/org.eclipse.core.runtime/.settings" );
        
        workDir.mkdirs();
        
        File f = new File( workDir.getAbsolutePath(), "org.eclipse.jdt.core.prefs" );
        
        try
        {
            FileWriter fWriter = new FileWriter( f );
        
            fWriter.write( "\norg.eclipse.jdt.core.classpathVariable.M2_REPO=" + localRepository.getBasedir() );

            fWriter.flush();

            fWriter.close();
        }
        catch( IOException ioe )
        {
            throw new MojoExecutionException( "Unable to write to file: " + f.getAbsolutePath() );
        }
    }
}
