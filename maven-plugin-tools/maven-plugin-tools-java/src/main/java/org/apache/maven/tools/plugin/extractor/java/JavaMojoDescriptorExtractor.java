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
import com.thoughtworks.qdox.model.JavaField;
import com.thoughtworks.qdox.model.JavaSource;
import org.apache.maven.plugin.descriptor.InvalidParameterException;
import org.apache.maven.plugin.descriptor.InvalidPluginDescriptorException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.descriptor.Requirement;
import org.apache.maven.project.MavenProject;
import org.apache.maven.tools.plugin.extractor.MojoDescriptorExtractor;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


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

    /**
     * This indicates the base name of the bean properties used to read/write this parameter's value.
     * So:
     *
     * @parameter property="project"
     *
     * Would say there is a getProject() method and a setProject(Project) method. Here the field
     * name would not be the basis for the parameter's name. This mode of operation will allow the
     * mojos to be usable as beans and will be the promoted form of use.
     */
    public static final String PARAMETER_PROPERTY = "property";

    public static final String REQUIRED = "required";

    public static final String DEPRECATED = "deprecated";

    public static final String READONLY = "readonly";

    public static final String GOAL = "goal";

    public static final String PHASE = "phase";

    public static final String EXECUTE = "execute";

    public static final String GOAL_DESCRIPTION = "description";

    public static final String GOAL_REQUIRES_DEPENDENCY_RESOLUTION = "requiresDependencyResolution";

    public static final String GOAL_REQUIRES_PROJECT = "requiresProject";

    public static final String GOAL_REQUIRES_REPORTS = "requiresReports";

    public static final String GOAL_IS_AGGREGATOR = "aggregator";

    public static final String GOAL_REQUIRES_ONLINE = "requiresOnline";

    public static final String GOAL_INHERIT_BY_DEFAULT = "inheritByDefault";

    public static final String GOAL_MULTI_EXECUTION_STRATEGY = "attainAlways";

    public static final String GOAL_REQUIRES_DIRECT_INVOCATION = "requiresDirectInvocation";

    private static final String COMPONENT = "component";

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

    private MojoDescriptor createMojoDescriptor( JavaSource javaSource, PluginDescriptor pluginDescriptor )
        throws InvalidPluginDescriptorException
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

        DocletTag execute = findInClassHierarchy( javaClass, EXECUTE );

        if ( execute != null )
        {
            String executePhase = execute.getNamedParameter( "phase" );
            String executeGoal = execute.getNamedParameter( "goal" );

            if ( executePhase == null && executeGoal == null )
            {
                throw new InvalidPluginDescriptorException( "@execute tag requires a 'phase' or 'goal' parameter" );
            }
            else if ( executePhase != null && executeGoal != null )
            {
                throw new InvalidPluginDescriptorException(
                    "@execute tag can have only one of a 'phase' or 'goal' parameter" );
            }
            mojoDescriptor.setExecutePhase( executePhase );
            mojoDescriptor.setExecuteGoal( executeGoal );

            String lifecycle = execute.getNamedParameter( "lifecycle" );

            if ( lifecycle != null )
            {
                mojoDescriptor.setExecuteLifecycle( lifecycle );
                if ( mojoDescriptor.getExecuteGoal() != null )
                {
                    throw new InvalidPluginDescriptorException(
                        "@execute lifecycle requires a phase instead of a goal" );
                }
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
            String requiresProjectValue = requiresProject.getValue();

            if ( requiresProjectValue != null )
            {
                mojoDescriptor.setProjectRequired( Boolean.valueOf( requiresProjectValue ).booleanValue() );
            }
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
        // requiresDirectInvocation flag
        // ----------------------------------------------------------------------

        DocletTag requiresDirectInvocation = findInClassHierarchy( javaClass, GOAL_REQUIRES_DIRECT_INVOCATION );

        if ( requiresDirectInvocation != null )
        {
            mojoDescriptor.setDirectInvocationOnly( true );
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

    private void extractParameters( MojoDescriptor mojoDescriptor, JavaClass javaClass )
        throws InvalidPluginDescriptorException
    {
        // ---------------------------------------------------------------------------------
        // We're resolving class-level, ancestor-class-field, local-class-field order here.
        // ---------------------------------------------------------------------------------

        Map rawParams = extractFieldParameterTags( javaClass );

        for ( Iterator it = rawParams.entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) it.next();

            JavaField field = (JavaField) entry.getValue();

            Parameter pd = new Parameter();

            pd.setType( field.getType().getValue() );

            pd.setDescription( field.getComment() );

            DocletTag componentTag = field.getTagByName( COMPONENT );

            if ( componentTag != null )
            {
                String role = componentTag.getNamedParameter( "role" );

                if ( role == null )
                {
                    role = field.getType().toString();
                }

                String roleHint = componentTag.getNamedParameter( "roleHint" );

                pd.setRequirement( new Requirement( role, roleHint ) );

                pd.setName( (String) entry.getKey() );
            }
            else
            {
                DocletTag parameter = field.getTagByName( PARAMETER );

                // ----------------------------------------------------------------------
                // We will look for a property name here first and use that if present
                // i.e:
                //
                // @parameter property="project"
                //
                // Which will become the name used for the configuration element which
                // will in turn will allow plexus to use the corresponding setter.
                // ----------------------------------------------------------------------

                String property = parameter.getNamedParameter( PARAMETER_PROPERTY );

                if ( !StringUtils.isEmpty( property ) )
                {
                    pd.setName( property );
                }
                else
                {
                    pd.setName( (String) entry.getKey() );
                }

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

                if ( "${reports}".equals( pd.getExpression() ) )
                {
                    mojoDescriptor.setRequiresReports( true );
                }

                pd.setDefaultValue( parameter.getNamedParameter( PARAMETER_DEFAULT_VALUE ) );
            }

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

                if ( field.getTagByName( PARAMETER ) != null || field.getTagByName( COMPONENT ) != null )
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
