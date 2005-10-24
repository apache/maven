package org.apache.maven.plugin.idea;

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

import junit.framework.TestCase;

import org.apache.maven.model.Model;
import org.apache.maven.model.Scm;
import org.apache.maven.project.MavenProject;

/**
 * @author <a href="mailto:jruiz@exist.com">Johnny R. Ruiz III</a>
 * @version $Id: 
 */
public class IdeaMojoTest
    extends TestCase
{

    /*
     * Test method for 'org.apache.maven.plugin.idea.IdeaMojo.getScmType()'
     */
    public void testGetScmType()
    {
        MavenProject project = new MavenProject( new Model() );

        Scm scm = new Scm();

        scm.setConnection( "scm:svn:svn://svn.codehaus.org/mojo/scm/trunk/" );

        project.setScm( scm );

        IdeaMojo idea = new IdeaMojo();

        idea.setProject( project );

        assertEquals( "svn", idea.getScmType() );
    }

    /*
     * Test method for 'org.apache.maven.plugin.idea.IdeaMojo.getScmType(String)'
     */
    public void testGetScmTypeString()
    {
        IdeaMojo idea = new IdeaMojo();

        assertEquals( "svn", idea.getScmType( "scm:svn:svn://svn.codehaus.org/mojo/scm/trunk/" ) );

        assertEquals( null, idea.getScmType( "scm:svn" ) );

        assertEquals( null, idea.getScmType( null ) );
    }

}
