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

import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.tools.plugin.extractor.InvalidParameterException;
import org.apache.maven.tools.plugin.extractor.MojoDescriptorExtractor;
import org.codehaus.modello.StringUtils;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.logging.Logger;

import com.thoughtworks.qdox.JavaDocBuilder;
import com.thoughtworks.qdox.model.DocletTag;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaField;
import com.thoughtworks.qdox.model.JavaSource;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

/**
 * @todo add example usage tag that can be shown in the doco
 * @todo need to add validation directives so that systems embedding maven2 can
 * get validation directives to help users in IDEs.
 */
public class JavaMojoDescriptorExtractor
    extends AbstractLogEnabled
    implements MojoDescriptorExtractor
{
    public static final String MAVEN_PLUGIN_ID = "maven.plugin.id";

    public static final String MAVEN_PLUGIN_DESCRIPTION = "maven.plugin.description";

    public static final String MAVEN_PLUGIN_INSTANTIATION = "maven.plugin.instantiation";

    public static final String MAVEN_PLUGIN_MODE = "maven.plugin.mode";

    public static final String PARAMETER = "parameter";

    public static final String PARAMETER_EXPRESSION = "expression";

    public static final String REQUIRED = "required";

    public static final String DEPRECATED = "deprecated";

    public static final String READONLY = "readonly";

    public static final String GOAL = "goal";

    public static final String PHASE = "phase";

    public static final String EXECUTE_PHASE = "executePhase";

    public static final String GOAL_DESCRIPTION = "description";

    public static final String GOAL_REQUIRES_DEPENDENCY_RESOLUTION = "requiresDependencyResolution";

    public static final String GOAL_MULTI_EXECUTION_STRATEGY = "attainAlways";

    protected void validateParameter( Parameter parameter, int i )
        throws InvalidParameterException
    {
        // TODO: remove when backward compatibility is no longer an issue.
        String name = parameter.getName();

        if ( name == null )
        {
            throw new InvalidParameterException( "name", i );
        }

        // TODO: remove when backward compatibility is no longer an issue.
        String type = parameter.getType();

        if ( type == null )
        {
            throw new InvalidParameterException( "type", i );
        }

        // TODO: remove when backward compatibility is no longer an issue.
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

        String pluginId = PluginDescriptor.getPluginIdFromArtifactId( project.getArtifactId() );

        mojoDescriptor.setId( pluginId );

        tag = findInClassHierarchy( javaClass, MAVEN_PLUGIN_DESCRIPTION );

        if ( tag != null )
        {
            mojoDescriptor.setDescription( tag.getValue() );
        }

        tag = findInClassHierarchy( javaClass, MAVEN_PLUGIN_INSTANTIATION );

        if ( tag != null )
        {
            mojoDescriptor.setInstantiationStrategy( tag.getValue() );
        }

        tag = findInClassHierarchy( javaClass, GOAL_MULTI_EXECUTION_STRATEGY );

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

        DocletTag goal = findInClassHierarchy( javaClass, GOAL );

        if ( goal != null )
        {
            mojoDescriptor.setGoal( goal.getValue() );
        }

        // ----------------------------------------------------------------------
        // Phase name
        // ----------------------------------------------------------------------

        DocletTag phase = findInClassHierarchy( javaClass, PHASE );

        if ( phase != null )
        {
            mojoDescriptor.setPhase( phase.getValue() );
        }

        // ----------------------------------------------------------------------
        // Additional phase to execute first
        // ----------------------------------------------------------------------

        DocletTag executePhase = findInClassHierarchy( javaClass, EXECUTE_PHASE );

        if ( executePhase != null )
        {
            mojoDescriptor.setExecutePhase( executePhase.getValue() );
        }

        // ----------------------------------------------------------------------
        // Dependency resolution flag
        // ----------------------------------------------------------------------

        DocletTag requiresDependencyResolution = findInClassHierarchy( javaClass, GOAL_REQUIRES_DEPENDENCY_RESOLUTION );

        if ( requiresDependencyResolution != null )
        {
            String value = requiresDependencyResolution.getValue();
            if ( value == null || value.length() == 0 )
            {
                value = "runtime";
            }
            mojoDescriptor.setRequiresDependencyResolution( value );
        }

        extractParameters( mojoDescriptor, javaClass );

        return mojoDescriptor;
    }

    private DocletTag findInClassHierarchy( JavaClass javaClass, String tagName )
    {
        DocletTag tag = javaClass.getTagByName( tagName );

        if ( tag == null )
        {
            JavaClass superClass = javaClass.getSuperJavaClass();

            if ( superClass != null )
            {
                tag = findInClassHierarchy( superClass, tagName );
            }
        }

        return tag;
    }

    private void extractParameters( MojoDescriptor mojoDescriptor, JavaClass javaClass )
    {
        // ---------------------------------------------------------------------------------
        // We're resolving class-level, ancestor-class-field, local-class-field order here.
        // ---------------------------------------------------------------------------------

        Map rawParams = new TreeMap();

        // for backward compat, we'll toss on the params declared at the class level.
        DocletTag[] classLevelParams = javaClass.getTagsByName( PARAMETER );
        if ( classLevelParams != null )
        {
            for ( int i = 0; i < classLevelParams.length; i++ )
            {
                DocletTag tag = classLevelParams[i];

                String message = "DEPRECATED: Use field-level annotations "
                    + "for parameters instead of class-level annotations. (parameter \'"
                    + tag.getNamedParameter( "name" ) + "\'; class \'" + javaClass.getFullyQualifiedName() + ")";

                Logger logger = getLogger();
                if ( logger != null )
                {
                    logger.warn( message );
                }
                else
                {
                    // we're being used from pluggy, so this is okay...
                    System.err.println( message );
                }

                rawParams.put( tag.getNamedParameter( "name" ), tag );
            }
        }

        extractFieldParameterTags( javaClass, rawParams );

        Set parameters = new HashSet();

        for ( Iterator it = rawParams.entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry entry = (Entry) it.next();
            String paramName = (String) entry.getKey();

            Object val = entry.getValue();

            JavaField field = null;
            DocletTag parameter = null;

            // FIXME: ICK! This is only here for backward compatibility to the class-level annotations of params.
            if ( val instanceof JavaField )
            {
                field = (JavaField) val;

                parameter = field.getTagByName( PARAMETER );
            }
            else
            {
                parameter = (DocletTag) val;
            }

            Parameter pd = new Parameter();

            // if the field is null, then we're using a deprecated annotation pattern...
            // TODO: remove when backward compatibility is no longer an issue.
            if ( field == null )
            {
                pd.setName( paramName );

                pd.setType( parameter.getNamedParameter( "type" ) );

                pd.setDescription( parameter.getNamedParameter( "description" ) );

                pd.setRequired( parameter.getNamedParameter( REQUIRED ).equals( "true" ) ? true : false );

                pd.setDeprecated( parameter.getNamedParameter( DEPRECATED ) );
                
                pd.setDefaultValue( parameter.getNamedParameter( "default" ) );
            }
            else
            {
                pd.setName( paramName );

                pd.setType( field.getType().getValue() );

                pd.setDescription( field.getComment() );

                pd.setRequired( field.getTagByName( REQUIRED ) != null );

                pd.setEditable( field.getTagByName( READONLY ) == null );
                
                DocletTag deprecationTag = field.getTagByName( DEPRECATED );
                if ( deprecationTag != null )
                {
                    pd.setDeprecated( deprecationTag.getValue() );
                }
            }

            String alias = parameter.getNamedParameter( "alias" );

            if ( StringUtils.isEmpty( alias ) )
            {
                pd.setAlias( alias );
            }

            pd.setExpression( parameter.getNamedParameter( PARAMETER_EXPRESSION ) );

            parameters.add( pd );
        }

        if ( !parameters.isEmpty() )
        {
            List paramList = new ArrayList( parameters );

            mojoDescriptor.setParameters( paramList );
        }
    }

    private void extractFieldParameterTags( JavaClass javaClass, Map rawParams )
    {
        // we have to add the parent fields first, so that they will be overwritten by the local fields if
        // that actually happens...
        JavaClass superClass = javaClass.getSuperJavaClass();

        if ( superClass != null )
        {
            extractFieldParameterTags( superClass, rawParams );
        }

        JavaField[] classFields = javaClass.getFields();

        if ( classFields != null )
        {
            for ( int i = 0; i < classFields.length; i++ )
            {
                JavaField field = classFields[i];

                DocletTag paramTag = field.getTagByName( PARAMETER );

                if ( paramTag != null )
                {
                    rawParams.put( field.getName(), field );
                }
            }
        }

    }

    private JavaClass getJavaClass( JavaSource javaSource )
    {
        return javaSource.getClasses()[0];
    }

    public Set execute( MavenProject project )
        throws Exception
    {
        JavaDocBuilder builder = new JavaDocBuilder();

        File basedir = project.getBasedir();

        System.out.println( "Project basedir: " + basedir );

        System.out.println( "Source directory for java mojo extraction: " + project.getCompileSourceRoots() );

        for ( Iterator i = project.getCompileSourceRoots().iterator(); i.hasNext(); )
        {
            builder.addSourceTree( new File( (String) i.next() ) );
        }

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