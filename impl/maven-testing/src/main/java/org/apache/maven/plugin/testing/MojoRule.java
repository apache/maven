package org.apache.maven.plugin.testing;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.InputStream;
import java.util.Map;

import org.apache.maven.api.Session;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.MojoExecutionEvent;
import org.apache.maven.execution.MojoExecutionListener;
import org.apache.maven.execution.scope.internal.MojoExecutionScope;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.session.scope.internal.SessionScope;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.junit.Assert;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * {@link TestRule} for usage with Junit-4.10ff. This is just a wrapper for an embedded 
 * {@link AbstractMojoTestCase}, so all {@code protected} methods of the TestCase are
 * exhibited as {@code public} in the rule. You may annotate single tests methods with
 * {@link WithoutMojo} to prevent the rule from firing.
 *
 * @author Mirko Friedenhagen
 * @since 2.2
 */
public class MojoRule
    implements TestRule
{
    private final AbstractMojoTestCase testCase;
    
    public MojoRule() 
    {
        this( new AbstractMojoTestCase()
        {
        } );
    }

    public MojoRule( AbstractMojoTestCase testCase )
    {
        this.testCase = testCase;
    }

    /**
     * May be overridden in the implementation to do stuff <em>after</em> the embedded test case 
     * is set up but <em>before</em> the current test is actually run.
     *
     * @throws Throwable
     */
    protected void before() throws Throwable
    {
        
    }
    
    /**
     * May be overridden in the implementation to do stuff after the current test was run.
     */
    protected void after() 
    {
        
    }

    public InputStream getPublicDescriptorStream()
        throws Exception
    {
        return testCase.getPublicDescriptorStream();
    }

    public String getPluginDescriptorPath()
    {
        return testCase.getPluginDescriptorPath();
    }

    public String getPluginDescriptorLocation()
    {
        return testCase.getPluginDescriptorLocation();
    }

    public void setupContainer()
    {
        testCase.setupContainer();
    }

    public ContainerConfiguration setupContainerConfiguration()
    {
        return testCase.setupContainerConfiguration();
    }
    
    public PlexusContainer getContainer()
    {
        return testCase.getContainer();
    }    
    
    /**
     * Lookup the mojo leveraging the subproject pom
     *
     * @param goal
     * @param pluginPom
     * @return a Mojo instance
     * @throws Exception
     */
    public <T extends Mojo> T lookupMojo( String goal, String pluginPom )
        throws Exception
    {
        return testCase.lookupMojo( goal, pluginPom );
    }

    /**
     * Lookup an empty mojo
     *
     * @param goal
     * @param pluginPom
     * @return a Mojo instance
     * @throws Exception
     */
    public <T extends Mojo> T lookupEmptyMojo( String goal, String pluginPom )
        throws Exception
    {
        return testCase.lookupEmptyMojo( goal, new File( pluginPom ) );
    }

    /**
     * Lookup the mojo leveraging the actual subprojects pom
     *
     * @param goal
     * @param pom
     * @return a Mojo instance
     * @throws Exception
     */
    public <T extends Mojo> T lookupMojo( String goal, File pom )
        throws Exception
    {
        return testCase.lookupMojo( goal, pom );
    }

    /**
     * Lookup the mojo leveraging the actual subprojects pom
     *
     * @param goal
     * @param pom
     * @return a Mojo instance
     * @throws Exception
     */
    public <T extends Mojo> T lookupEmptyMojo( String goal, File pom )
        throws Exception
    {
        return testCase.lookupEmptyMojo( goal, pom );
    }

    public <T extends Mojo> T lookupMojo( String groupId, String artifactId, String version, String goal,
                               PlexusConfiguration pluginConfiguration )
        throws Exception
    {
        return testCase.lookupMojo( groupId, artifactId, version, goal, pluginConfiguration );
    }

    public <T extends Mojo> T lookupConfiguredMojo( MavenProject project, String goal )
        throws Exception
    {
        return testCase.lookupConfiguredMojo( project, goal );
    }

    public <T extends Mojo> T lookupConfiguredMojo( MavenSession session, MojoExecution execution )
        throws Exception, ComponentConfigurationException
    {
        return testCase.lookupConfiguredMojo( session, execution );
    }

    public MavenSession newMavenSession( MavenProject project )
    {
        return testCase.newMavenSession( project );
    }

    public MojoExecution newMojoExecution( String goal )
    {
        return testCase.newMojoExecution( goal );
    }

    public PlexusConfiguration extractPluginConfiguration( String artifactId, File pom )
        throws Exception
    {
        return testCase.extractPluginConfiguration( artifactId, pom );
    }

    public PlexusConfiguration extractPluginConfiguration( String artifactId, Xpp3Dom pomDom )
        throws Exception
    {
        return testCase.extractPluginConfiguration( artifactId, pomDom );
    }

    public <T extends Mojo> T configureMojo( T mojo, String artifactId, File pom )
        throws Exception
    {
        return testCase.configureMojo( mojo, artifactId, pom );
    }

    public <T extends Mojo> T configureMojo( T mojo, PlexusConfiguration pluginConfiguration )
        throws Exception
    {
        return testCase.configureMojo( mojo, pluginConfiguration );
    }

    /**
     * Convenience method to obtain the value of a variable on a mojo that might not have a getter.
     *
     * NOTE: the caller is responsible for casting to to what the desired type is.
     *
     * @param object
     * @param variable
     * @return object value of variable
     * @throws IllegalArgumentException
     */
    public <T> T getVariableValueFromObject( Object object, String variable )
        throws IllegalAccessException
    {
        return testCase.getVariableValueFromObject( object, variable );
    }

    /**
     * Convenience method to obtain all variables and values from the mojo (including its superclasses)
     *
     * Note: the values in the map are of type Object so the caller is responsible for casting to desired types.
     *
     * @param object
     * @return map of variable names and values
     */
    public Map<String, Object> getVariablesAndValuesFromObject( Object object )
        throws IllegalAccessException
    {
        return testCase.getVariablesAndValuesFromObject( object );
    }

    /**
     * Convenience method to obtain all variables and values from the mojo (including its superclasses)
     *
     * Note: the values in the map are of type Object so the caller is responsible for casting to desired types.
     *
     * @param clazz
     * @param object
     * @return map of variable names and values
     */
    public Map<String, Object> getVariablesAndValuesFromObject( Class<?> clazz, Object object )
        throws IllegalAccessException
    {
        return testCase.getVariablesAndValuesFromObject( clazz, object );
    }

    /**
     * Convenience method to set values to variables in objects that don't have setters
     *
     * @param object
     * @param variable
     * @param value
     * @throws IllegalAccessException
     */
    public <T> void setVariableValueToObject( Object object, String variable, T value )
        throws IllegalAccessException
    {
        testCase.setVariableValueToObject( object, variable, value );
    }

    @Override
    public Statement apply( final Statement base, Description description )
    {
        if ( description.getAnnotation( WithoutMojo.class ) != null ) // skip.
        {
            return base;
        }
        return new Statement()
        {
            @Override
            public void evaluate()
                throws Throwable
            {
                testCase.setUp();
                before();
                try
                {
                    base.evaluate();
                }
                finally
                {
                    after();
                }
            }
        };
    }

    /**
     * @since 3.1.0
     */
    public MavenProject readMavenProject( File basedir )
        throws Exception
    {
        File pom = new File( basedir, "pom.xml" );
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setBaseDirectory( basedir );
        ProjectBuildingRequest configuration = request.getProjectBuildingRequest();
        configuration.setRepositorySession( new DefaultRepositorySystemSession() );
        MavenProject project = lookup( ProjectBuilder.class ).build( pom, configuration ).getProject();
        Assert.assertNotNull( project );
        return project;
    }

    /**
     * @since 3.1.0
     */
    public void executeMojo( File basedir, String goal )
        throws Exception
    {
        MavenProject project = readMavenProject( basedir );
        MavenSession session = newMavenSession( project );
        MojoExecution execution = newMojoExecution( goal );
        executeMojo( session, project, execution );
    }

    /**
     * @since 3.1.0
     */
    public <T extends Mojo> T lookupConfiguredMojo( File basedir, String goal )
        throws Exception, ComponentConfigurationException
    {
        MavenProject project = readMavenProject( basedir );
        MavenSession session = newMavenSession( project );
        MojoExecution execution = newMojoExecution( goal );
        return lookupConfiguredMojo( session, execution );
    }

    /**
     * @since 3.1.0
     */
    public final <T> T lookup( final Class<T> role )
        throws ComponentLookupException
    {
        return getContainer().lookup( role );
    }

    /**
     * @since 3.2.0
     */
    public void executeMojo( MavenProject project, String goal, Xpp3Dom... parameters )
        throws Exception
    {
        MavenSession session = newMavenSession( project );
        executeMojo( session, project, goal, parameters );
    }

    /**
     * @since 3.2.0
     */
    public void executeMojo( MavenSession session, MavenProject project, String goal, Xpp3Dom... parameters )
        throws Exception
    {
        MojoExecution execution = newMojoExecution( goal );
        if ( parameters != null )
        {
            Xpp3Dom configuration = execution.getConfiguration();
            for ( Xpp3Dom parameter : parameters )
            {
                configuration.addChild( parameter );
            }
        }
        executeMojo( session, project, execution );
    }

    /**
     * @since 3.2.0
     */
    public void executeMojo( MavenSession session, MavenProject project, MojoExecution execution )
        throws Exception
    {
        SessionScope sessionScope = lookup( SessionScope.class );
        try
        {
            sessionScope.enter();
            sessionScope.seed( MavenSession.class, session );
            sessionScope.seed( Session.class, session.getSession() );

            MojoExecutionScope executionScope = lookup( MojoExecutionScope.class );
            try
            {
                executionScope.enter();

                executionScope.seed( MavenProject.class, project );
                executionScope.seed( MojoExecution.class, execution );

                Mojo mojo = lookupConfiguredMojo( session, execution );
                mojo.execute();

                MojoExecutionEvent event = new MojoExecutionEvent( session, project, execution, mojo );
                for ( MojoExecutionListener listener : getContainer().lookupList( MojoExecutionListener.class ) )
                {
                    listener.afterMojoExecutionSuccess( event );
                }
            }
            finally
            {
                executionScope.exit();
            }
        }
        finally
        {
            sessionScope.exit();
        }
    }

}
