package org.apache.maven.tools.plugin.extractor.java;

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

import org.apache.maven.model.Build;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.tools.plugin.extractor.InvalidParameterException;
import org.apache.maven.tools.plugin.extractor.MojoDescriptorExtractor;
import org.apache.maven.tools.plugin.util.PluginUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @todo add example usage tag that can be shown in the doco
 * @todo need to add validation directives so that systems embedding maven2 can
 *       get validation directives to help users in IDEs.
 */
public class JavaMojoDescriptorExtractor
    implements MojoDescriptorExtractor
{
    public static final String MAVEN_PLUGIN_ID = "maven.plugin.id";

    public static final String MAVEN_PLUGIN_DESCRIPTION = "maven.plugin.description";

    public static final String MAVEN_PLUGIN_INSTANTIATION = "maven.plugin.instantiation";

    public static final String MAVEN_PLUGIN_MODE = "maven.plugin.mode";

    public static final String PARAMETER = "parameter";

    public static final String GOAL = "goal";

    public static final String PHASE = "phase";

    public static final String DISPATCH = "dispatch";

    public static final String GOAL_DESCRIPTION = "description";

    public static final String GOAL_PREREQ = "prereq";

    public static final String GOAL_REQUIRES_DEPENDENCY_RESOLUTION = "requiresDependencyResolution";

    public static final String GOAL_MULTI_EXECUTION_STRATEGY = "attainAlways";

    protected void validateParameter( Parameter parameter, int i ) throws InvalidParameterException
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

    // ----------------------------------------------------------------------
    // Plugin descriptor creation from @tags
    // ----------------------------------------------------------------------

    private MojoDescriptor createMojoDescriptor( JavaSource javaSource, MavenProject project )
    {
        MojoDescriptor mojoDescriptor = new MojoDescriptor();

        JavaClass javaClass = getJavaClass( javaSource );

        mojoDescriptor.setLanguage( "java" );

        mojoDescriptor.setImplementation( javaClass.getFullyQualifiedName() );

        DocletTag tag;

        String pluginId = PluginUtils.pluginId( project );

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
        // Phase name
        // ----------------------------------------------------------------------

        DocletTag phase = javaClass.getTagByName( PHASE );

        if ( phase != null )
        {
            mojoDescriptor.setPhase( phase.getValue() );
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

    public Set execute( MavenProject project ) throws Exception
    {
        JavaDocBuilder builder = new JavaDocBuilder();

        File basedir = project.getBasedir();

        System.out.println( "Project basedir: " + basedir );

        String sourceDir = null;

        Build buildSection = project.getBuild();
        if ( buildSection != null )
        {
            sourceDir = buildSection.getSourceDirectory();
        }

        if ( sourceDir == null )
        {
            File src = new File( basedir, "src/main/java" );
            sourceDir = src.getPath();
        }

        System.out.println( "Source directory for java mojo extraction: " + sourceDir );

        File sourceDirectoryFile = new File( sourceDir );

        builder.addSourceTree( sourceDirectoryFile );

        JavaSource[] javaSources = builder.getSources();

        Set descriptors = new HashSet();

        for ( int i = 0; i < javaSources.length; i++ )
        {
            DocletTag tag = getJavaClass( javaSources[i] ).getTagByName( GOAL );

            if ( tag != null )
            {
                MojoDescriptor mojoDescriptor = createMojoDescriptor( javaSources[i], project );

                // ----------------------------------------------------------------------
                // Validate the descriptor as best we can before allowing it
                // to be processed.
                // ----------------------------------------------------------------------

                List parameters = mojoDescriptor.getParameters();

                for ( int j = 0; j < parameters.size(); j++ )
                {
                    validateParameter( (Parameter) parameters.get( j ), j );
                }

                //                Commented because it causes a VerifyError:
                //                java.lang.VerifyError:
                //                (class:
                // org/apache/maven/tools/plugin/extractor/java/JavaMojoDescriptorExtractor,
                //                method: execute signature:
                // (Ljava/lang/String;Lorg/apache/maven/project/MavenProject;)Ljava/util/Set;)
                //                Incompatible object argument for function call
                //                
                //                Refactored to allow MavenMojoDescriptor.getComponentFactory()
                //                return MavenMojoDescriptor.getMojoDescriptor().getLanguage(),
                //                and removed all usage of MavenMojoDescriptor from extractors.
                //                
                //                
                //                MavenMojoDescriptor mmDescriptor = new
                // MavenMojoDescriptor(mojoDescriptor);
                //                
                //                JavaClass javaClass = getJavaClass(javaSources[i]);
                //                
                //                mmDescriptor.setImplementation(javaClass.getFullyQualifiedName());
                //                
                //                descriptors.add( mmDescriptor );

                descriptors.add( mojoDescriptor );
            }
        }

        return descriptors;
    }

}