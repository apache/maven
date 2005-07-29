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

import org.apache.maven.plugin.descriptor.InvalidParameterException;
import org.apache.maven.plugin.descriptor.InvalidPluginDescriptorException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.tools.plugin.extractor.MojoDescriptorExtractor;
import org.codehaus.modello.StringUtils;
import org.codehaus.plexus.logging.AbstractLogEnabled;

import com.thoughtworks.qdox.JavaDocBuilder;
import com.thoughtworks.qdox.model.DocletTag;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaField;
import com.thoughtworks.qdox.model.JavaSource;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
    public static final String MAVEN_PLUGIN_INSTANTIATION = "instantiationStrategy";
    
    public static final String CONFIGURATOR = "configurator";

    public static final String PARAMETER = "parameter";

    public static final String PARAMETER_EXPRESSION = "expression";

    public static final String PARAMETER_DEFAULT_VALUE = "default-value";

    public static final String REQUIRED = "required";

    public static final String DEPRECATED = "deprecated";

    public static final String READONLY = "readonly";

    public static final String GOAL = "goal";

    public static final String PHASE = "phase";

    public static final String EXECUTE = "execute";

    public static final String GOAL_DESCRIPTION = "description";

    public static final String GOAL_REQUIRES_DEPENDENCY_RESOLUTION = "requiresDependencyResolution";

    public static final String GOAL_REQUIRES_PROJECT = "requiresProject";
    
    public static final String GOAL_IS_AGGREGATOR = "aggregator";

    public static final String GOAL_REQUIRES_ONLINE = "requiresOnline";
    
    public static final String GOAL_INHERIT_BY_DEFAULT = "inheritByDefault";

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
    // Mojo descriptor creation from @tags
    // ----------------------------------------------------------------------

    private MojoDescriptor createMojoDescriptor( JavaSource javaSource, PluginDescriptor pluginDescriptor ) throws InvalidPluginDescriptorException
    {
        MojoDescriptor mojoDescriptor = new MojoDescriptor();
        mojoDescriptor.setPluginDescriptor( pluginDescriptor );

        JavaClass javaClass = getJavaClass( javaSource );

        mojoDescriptor.setLanguage( "java" );

        mojoDescriptor.setImplementation( javaClass.getFullyQualifiedName() );

        mojoDescriptor.setDescription( javaClass.getComment() );

        DocletTag tag = findInClassHierarchy( javaClass, MAVEN_PLUGIN_INSTANTIATION );

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
        // Configurator hint
        // ----------------------------------------------------------------------

        DocletTag configurator = findInClassHierarchy( javaClass, CONFIGURATOR );

        if ( configurator != null )
        {
            mojoDescriptor.setComponentConfigurator( configurator.getValue() );
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

        DocletTag oldExecutePhase = findInClassHierarchy( javaClass, "executePhase" );
        if ( oldExecutePhase != null )
        {
            getLogger().warn( "DEPRECATED: @executePhase is deprecated, please use @execute" );
            mojoDescriptor.setExecutePhase( oldExecutePhase.getValue() );
        }

        DocletTag execute = findInClassHierarchy( javaClass, EXECUTE );

        if ( execute != null )
        {
            String executePhase = execute.getNamedParameter( "phase" );
            if ( executePhase == null )
            {
                throw new InvalidPluginDescriptorException( "@execute tag requires a 'phase' parameter" );
            }
            mojoDescriptor.setExecutePhase( executePhase );

            String lifecycle = execute.getNamedParameter( "lifecycle" );
            if ( lifecycle != null )
            {
                mojoDescriptor.setExecuteLifecycle( lifecycle );
            }
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
            mojoDescriptor.setDependencyResolutionRequired( value );
        }

        // ----------------------------------------------------------------------
        // Project flag
        // ----------------------------------------------------------------------

        DocletTag requiresProject = findInClassHierarchy( javaClass, GOAL_REQUIRES_PROJECT );

        if ( requiresProject != null )
        {
            mojoDescriptor.setProjectRequired( true );
        }

        // ----------------------------------------------------------------------
        // Aggregator flag
        // ----------------------------------------------------------------------

        DocletTag aggregator = findInClassHierarchy( javaClass, GOAL_IS_AGGREGATOR );

        if ( aggregator != null )
        {
            mojoDescriptor.setAggregator( true );
        }

        // ----------------------------------------------------------------------
        // Online flag
        // ----------------------------------------------------------------------

        DocletTag requiresOnline = findInClassHierarchy( javaClass, GOAL_REQUIRES_ONLINE );

        if ( requiresOnline != null )
        {
            mojoDescriptor.setOnlineRequired( true );
        }

        // ----------------------------------------------------------------------
        // inheritByDefault flag
        // ----------------------------------------------------------------------

        DocletTag inheritByDefault = findInClassHierarchy( javaClass, GOAL_INHERIT_BY_DEFAULT );

        if ( inheritByDefault != null )
        {
            mojoDescriptor.setInheritedByDefault( Boolean.valueOf( inheritByDefault.getValue() ).booleanValue() );
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

    private void extractParameters( MojoDescriptor mojoDescriptor, JavaClass javaClass ) throws InvalidPluginDescriptorException
    {
        // ---------------------------------------------------------------------------------
        // We're resolving class-level, ancestor-class-field, local-class-field order here.
        // ---------------------------------------------------------------------------------

        Map rawParams = extractFieldParameterTags( javaClass );

        for ( Iterator it = rawParams.entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry entry = (Entry) it.next();
            String paramName = (String) entry.getKey();

            JavaField field = (JavaField) entry.getValue();

            DocletTag parameter = field.getTagByName( PARAMETER );

            Parameter pd = new Parameter();

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

            String alias = parameter.getNamedParameter( "alias" );

            if ( !StringUtils.isEmpty( alias ) )
            {
                pd.setAlias( alias );
            }

            pd.setExpression( parameter.getNamedParameter( PARAMETER_EXPRESSION ) );

            pd.setDefaultValue( parameter.getNamedParameter( PARAMETER_DEFAULT_VALUE ) );

            mojoDescriptor.addParameter( pd );
        }
    }

    private Map extractFieldParameterTags( JavaClass javaClass )
    {
        Map rawParams;

        // we have to add the parent fields first, so that they will be overwritten by the local fields if
        // that actually happens...
        JavaClass superClass = javaClass.getSuperJavaClass();

        if ( superClass != null )
        {
            rawParams = extractFieldParameterTags( superClass );
        }
        else
        {
            rawParams = new TreeMap();
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
        return rawParams;
    }

    private JavaClass getJavaClass( JavaSource javaSource )
    {
        return javaSource.getClasses()[0];
    }

    public List execute( MavenProject project, PluginDescriptor pluginDescriptor )
        throws InvalidPluginDescriptorException
    {
        JavaDocBuilder builder = new JavaDocBuilder();

        for ( Iterator i = project.getCompileSourceRoots().iterator(); i.hasNext(); )
        {
            builder.addSourceTree( new File( (String) i.next() ) );
        }

        JavaSource[] javaSources = builder.getSources();

        List descriptors = new ArrayList();

        for ( int i = 0; i < javaSources.length; i++ )
        {
            JavaClass javaClass = getJavaClass( javaSources[i] );

            DocletTag tag = javaClass.getTagByName( GOAL );

            if ( tag != null )
            {
                MojoDescriptor mojoDescriptor = createMojoDescriptor( javaSources[i], pluginDescriptor );

                // ----------------------------------------------------------------------
                // Validate the descriptor as best we can before allowing it
                // to be processed.
                // ----------------------------------------------------------------------

                List parameters = mojoDescriptor.getParameters();

                if ( parameters != null )
                {
                    for ( int j = 0; j < parameters.size(); j++ )
                    {
                        validateParameter( (Parameter) parameters.get( j ), j );
                    }
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
