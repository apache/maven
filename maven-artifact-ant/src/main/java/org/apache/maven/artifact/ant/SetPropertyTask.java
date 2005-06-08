package org.apache.maven.artifact.ant;

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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.tools.ant.BuildException;
import org.codehaus.plexus.util.introspection.ReflectionValueExtractor;

/**
 * Set a property from a POM.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class SetPropertyTask
    extends AbstractArtifactTask
{
    private String ref;

    private String property;

    private String expression;

    public void execute()
        throws BuildException
    {
        ArtifactRepository localRepo = createLocalArtifactRepository();

        MavenProjectBuilder builder = (MavenProjectBuilder) lookup( MavenProjectBuilder.ROLE );
        Pom pom = buildPom( builder, localRepo );

        if ( expression == null )
        {
            throw new BuildException( "the expression attribute is required" );
        }

        if ( property == null && ref == null )
        {
            throw new BuildException( "the property or ref attribute is required" );
        }

        else if ( property != null && ref != null )
        {
            throw new BuildException( "only one of the property or ref attribute is allowed" );

        }

        Object value = null;
        try
        {
            value = ReflectionValueExtractor.evaluate( expression, pom.getMavenProject() );
        }
        catch ( Exception e )
        {
            throw new BuildException( "Error extracting expression from POM", e );
        }

        if ( property != null )
        {
            getProject().setProperty( property, value.toString() );
        }
        else
        {
            getProject().addReference( ref, value );
        }
    }

    public String getProperty()
    {
        return property;
    }

    public void setProperty( String property )
    {
        this.property = property;
    }

    public String getExpression()
    {
        return expression;
    }

    public void setExpression( String expression )
    {
        this.expression = expression;
    }

    public String getRef()
    {
        return ref;
    }

    public void setRef( String ref )
    {
        this.ref = ref;
    }
}
