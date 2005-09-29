package org.apache.maven.plugin.coreit;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @goal test-basedir
 * @phase compile
 */
public class TestBasedirMojo
    extends AbstractMojo
{
    private static final int DELETE_RETRY_SLEEP_MILLIS = 10;
    
    /**
     * @parameter expression="${basedir}/target/child-basedir"
     * @required
     */
    private String childBasedir;

    /**
     * @parameter expression="${project.parent.basedir}/parent-basedir"
     * @required
     */
    private String parentBasedir;

    public void execute()
        throws MojoExecutionException
    {
        write( new File( childBasedir ) );

        write( new File( parentBasedir ) );
    }

    private void write( File f )
        throws MojoExecutionException
    {
        if ( !f.getParentFile().exists() )
        {
            f.getParentFile().mkdirs();
        }
        
        try
        {
            FileWriter w = new FileWriter( f );

            w.write( f.getPath() );

            w.close(); 
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error writing verification file.", e );
        }                
    }
}
