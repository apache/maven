package org.apache.maven.plugin.generator;

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

import com.thoughtworks.qdox.JavaDocBuilder;
import com.thoughtworks.qdox.model.DocletTag;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaSource;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/**
 * @todo add example usage tag that can be shown in the doco
 * @todo need to add validation directives so that systems embedding
 * maven2 can get validation directives to help users in IDEs.
 */
public abstract class AbstractGenerator
    implements Generator
{
    public static final String MAVEN_PLUGIN_ID = "maven.plugin.id";

    public static final String MAVEN_PLUGIN_DESCRIPTION = "maven.plugin.description";

    public static final String MAVEN_PLUGIN_INSTANTIATION = "maven.plugin.instantiation";

    public static final String MAVEN_PLUGIN_MODE = "maven.plugin.mode";

    public static final String PARAMETER = "parameter";

    public static final String GOAL = "goal";

    public static final String DISPATCH = "dispatch";    

    public static final String GOAL_DESCRIPTION = "description";

    public static final String GOAL_PREREQ = "prereq";

    public static final String GOAL_REQUIRES_DEPENDENCY_RESOLUTION = "requiresDependencyResolution";

    public static final String GOAL_MULTI_EXECUTION_STRATEGY = "attainAlways";

    protected abstract void processPluginDescriptors( MojoDescriptor[] pluginDescriptors, String destinationDirectory, Xpp3Dom pomDom )
        throws Exception;

    protected Xpp3Dom readModel( String pom )
        throws Exception
    {
        return Xpp3DomBuilder.build( new FileReader( pom ) );
    }

    public void execute( String sourceDirectory, String destinationDirectory, String pom )
        throws Exception
    {
        Xpp3Dom pomDom = readModel( pom ); 

        JavaDocBuilder builder = new JavaDocBuilder();

        File sourceDirectoryFile = new File( sourceDirectory );

        builder.addSourceTree( sourceDirectoryFile );

        JavaSource[] javaSources = builder.getSources();

        List mojoDescriptors = new ArrayList();

        for ( int i = 0; i < javaSources.length; i++ )
        {
            DocletTag tag = getJavaClass( javaSources[i] ).getTagByName( GOAL );

            if ( tag != null )
            {
                MojoDescriptor mojoDescriptor = createMojoDescriptor( javaSources[i], pomDom );

                // ----------------------------------------------------------------------
                // Validate the descriptor as best we can before allowing it
                // to be processed.
                // ----------------------------------------------------------------------

                List parameters = mojoDescriptor.getParameters();

                for ( int j = 0; j < parameters.size(); j++ )
                {
                    validateParameter( (Parameter) parameters.get( j ), j );
                }

                mojoDescriptors.add( mojoDescriptor );
            }
        }

        MojoDescriptor[] mojos = (MojoDescriptor[]) mojoDescriptors.toArray( new MojoDescriptor[mojoDescriptors.size()] );

        processPluginDescriptors( mojos, destinationDirectory, pomDom );
    }

    protected void validateParameter( Parameter parameter, int i )
        throws InvalidParameterException
    {
        String name = parameter.getName();

        if ( name == null )
        {
            throw new InvalidParameterException( "name", i );
        }

        String type = parameter.getType();

        if ( type == null )
        {
            throw new InvalidParameterException( "type", i );
        }

        boolean required = parameter.isRequired();

        String validator = parameter.getValidator();

        if ( validator == null )
        {
            throw new InvalidParameterException( "validator", i );
        }

        String expression = parameter.getExpression();

        if ( expression == null )
        {
            throw new InvalidParameterException( "expression", i );
        }

        String description = parameter.getDescription();

        if ( description == null )
        {
            throw new InvalidParameterException( "description", i );
        }
    }

    protected String pluginId( Xpp3Dom pomDom )
    {
        // ----------------------------------------------------------------------
        // We will take the id from the artifactId of the POM. The artifactId is
        // always of the form maven-<pluginId>-plugin so we can extract the
        // pluginId from the artifactId.
        // ----------------------------------------------------------------------

        String artifactId = pomDom.getChild( "artifactId" ).getValue();

        int firstHyphen = artifactId.indexOf( "-" );

        int lastHyphen = artifactId.lastIndexOf( "-" );

        String pluginId = artifactId.substring( firstHyphen + 1, lastHyphen );

        return pluginId;
    }

    // ----------------------------------------------------------------------
    // Plugin descriptor creation from @tags
    // ----------------------------------------------------------------------

    private MojoDescriptor createMojoDescriptor( JavaSource javaSource, Xpp3Dom pomDom )
    {
        MojoDescriptor mojoDescriptor = new MojoDescriptor();

        JavaClass javaClass = getJavaClass( javaSource );

        mojoDescriptor.setImplementation( javaClass.getFullyQualifiedName() );

        DocletTag tag;

        String pluginId = pluginId( pomDom );

        mojoDescriptor.setId( pluginId );

        tag = javaClass.getTagByName( MAVEN_PLUGIN_DESCRIPTION );

        if ( tag != null )
        {
            mojoDescriptor.setDescription( tag.getValue() );
        }

        tag = javaClass.getTagByName( MAVEN_PLUGIN_INSTANTIATION );

        if ( tag != null )
        {
            mojoDescriptor.setInstantiationStrategy( tag.getValue() );
        }

        tag = javaClass.getTagByName( GOAL_MULTI_EXECUTION_STRATEGY );

        if ( tag != null )
        {
            mojoDescriptor.setExecutionStrategy( MojoDescriptor.MULTI_PASS_EXEC_STRATEGY );
        }
        else
        {
            mojoDescriptor.setExecutionStrategy( MojoDescriptor.SINGLE_PASS_EXEC_STRATEGY );
        }

        // ----------------------------------------------------------------------
        // Goal name
        // ----------------------------------------------------------------------

        DocletTag goal = javaClass.getTagByName( GOAL );

        if ( goal != null )
        {
            mojoDescriptor.setGoal( goal.getValue() );
        }

        // ----------------------------------------------------------------------
        // Dependency resolution flag
        // ----------------------------------------------------------------------

        DocletTag requiresDependencyResolution = javaClass.getTagByName( GOAL_REQUIRES_DEPENDENCY_RESOLUTION );

        if ( requiresDependencyResolution != null )
        {
            mojoDescriptor.setRequiresDependencyResolution( true );
        }

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        DocletTag[] parameterTags = javaClass.getTagsByName( PARAMETER );

        List parameters = new ArrayList();

        for ( int j = 0; j < parameterTags.length; j++ )
        {
            DocletTag parameter = parameterTags[j];

            Parameter pd = new Parameter();

            pd.setName( parameter.getNamedParameter( "name" ) );

            pd.setType( parameter.getNamedParameter( "type" ) );

            pd.setRequired( parameter.getNamedParameter( "required" ).equals( "true" ) ? true : false );

            pd.setValidator( parameter.getNamedParameter( "validator" ) );

            pd.setExpression( parameter.getNamedParameter( "expression" ) );

            pd.setDescription( parameter.getNamedParameter( "description" ) );

            pd.setDefaultValue( parameter.getNamedParameter( "default" ) );

            parameters.add( pd );
        }

        mojoDescriptor.setParameters( parameters );

        // ----------------------------------------------------------------------
        // Prereqs must in the form pluginId:goalName
        // ----------------------------------------------------------------------

        DocletTag[] goalPrereqTags = javaClass.getTagsByName( GOAL_PREREQ );

        List goalPrereqs = new ArrayList();

        for ( int j = 0; j < goalPrereqTags.length; j++ )
        {
            DocletTag goalPrereq = goalPrereqTags[j];

            goalPrereqs.add( goalPrereq.getValue() );
        }

        mojoDescriptor.setPrereqs( goalPrereqs );

        return mojoDescriptor;
    }

    private JavaClass getJavaClass( JavaSource javaSource )
    {
        return javaSource.getClasses()[0];
    }
}