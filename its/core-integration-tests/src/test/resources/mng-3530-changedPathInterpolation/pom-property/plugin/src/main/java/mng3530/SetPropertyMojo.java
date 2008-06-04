package mng3530;

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

import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

/**
 * Modify the POM property myDirectory, contained in the current
 * project instance. This and the corresponding {@link ValidatePropertyMojo} should
 * prove or disprove the ability to have POM-property modifications ripple through
 * the project's values, such as plugin configurations.
 *
 * @goal set
 * @phase compile
 */
public class SetPropertyMojo
    implements Mojo
{
    public static final String MODIFIED_PROPERTY_VALUE = "modified";

    /**
     * Project instance to modify.
     *
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    private Log log;

    public void execute()
        throws MojoExecutionException
    {
        getLog().info( "Before modification, myDirectory is: " + project.getProperties().getProperty( "myDirectory" ) );

        project.getProperties().setProperty( "myDirectory", MODIFIED_PROPERTY_VALUE );

        getLog().info( "After modification, myDirectory is: " + project.getProperties().getProperty( "myDirectory" ) );
        getLog().info( "Modifications complete." );
    }

    public Log getLog()
    {
        return log;
    }

    public void setLog( Log log )
    {
        this.log = log;
    }
}
