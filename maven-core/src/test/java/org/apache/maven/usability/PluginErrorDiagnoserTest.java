package org.apache.maven.usability;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import junit.framework.TestCase;

import org.apache.maven.plugin.PluginConfigurationException;
import org.apache.maven.plugin.PluginParameterException;
import org.apache.maven.plugin.descriptor.DuplicateParameterException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PluginErrorDiagnoserTest
    extends TestCase
{

    private PluginConfigurationDiagnoser diagnoser = new PluginConfigurationDiagnoser();

    private PluginParameterException buildException( String prefix, String goal, List params )
        throws DuplicateParameterException
    {
        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        pluginDescriptor.setArtifactId( "maven-test-plugin" );
        pluginDescriptor.setGroupId( "org.apache.maven.plugins" );
        pluginDescriptor.setVersion( "1.0" );

        pluginDescriptor.setGoalPrefix( prefix );

        MojoDescriptor mojoDescriptor = new MojoDescriptor();
        mojoDescriptor.setGoal( goal );
        mojoDescriptor.setPluginDescriptor( pluginDescriptor );

        mojoDescriptor.setParameters( params );

        return new PluginParameterException( mojoDescriptor, params );
    }

    public void testShouldDiagnoseInvalidPluginConfiguration()
    {
        printMethodHeader();

        ComponentConfigurationException cce = new ComponentConfigurationException(
                                                                                   "Class \'org.apache.maven.plugin.jar.JarMojo\' does not contain a field named \'addClasspath\'" );
        
        PluginDescriptor pd = new PluginDescriptor();
        pd.setGroupId("testGroup");
        pd.setArtifactId("testArtifact");
        
        PluginConfigurationException pce = new PluginConfigurationException( pd, "test", cce );

        assertTrue( diagnoser.canDiagnose( pce ) );

        String userMessage = diagnoser.diagnose( pce );

        System.out.println( userMessage );

        assertNotNull( userMessage );
    }

    public void testShouldBeAbleToDiagnosePluginParameterExceptions()
        throws DuplicateParameterException
    {
        Parameter param = new Parameter();
        param.setName( "testName" );
        param.setAlias( "testAlias" );
        param.setExpression( "${project.build.finalName}" );
        param.setEditable( true );

        PluginParameterException error = buildException( "test", "test", Collections.singletonList( param ) );

        assertTrue( diagnoser.canDiagnose( error ) );
    }

    public void testParamWithOneReportsExpressionAndOneProjectBasedExpression()
        throws DuplicateParameterException
    {
        printMethodHeader();

        List params = new ArrayList();

        Parameter param = new Parameter();

        param.setName( "param1" );

        param.setExpression( "${reports}" );

        param.setEditable( false );

        params.add( param );

        Parameter param2 = new Parameter();

        param2.setName( "param2" );

        param2.setExpression( "${project.build.finalName}" );

        param2.setEditable( false );

        params.add( param2 );

        PluginParameterException error = buildException( "test", "test", params );

        String userMessage = diagnoser.diagnose( error );

        System.out.println( userMessage );

        assertNotNull( userMessage );
    }

    public void testParamWithNonActiveExpression()
        throws DuplicateParameterException
    {
        printMethodHeader();

        Parameter param = new Parameter();
        param.setName( "testName" );
        param.setAlias( "testAlias" );
        param.setExpression( "${project.build.finalName" );
        param.setEditable( true );

        PluginParameterException error = buildException( "test", "test", Collections.singletonList( param ) );

        String userMessage = diagnoser.diagnose( error );

        System.out.println( userMessage );

        assertNotNull( userMessage );
    }

    public void testParamWithoutExpression()
        throws DuplicateParameterException
    {
        printMethodHeader();

        Parameter param = new Parameter();
        param.setName( "testName" );
        param.setAlias( "testAlias" );
        param.setEditable( true );

        PluginParameterException error = buildException( "test", "test", Collections.singletonList( param ) );

        String userMessage = diagnoser.diagnose( error );

        System.out.println( userMessage );

        assertNotNull( userMessage );
    }

    public void testParamWithOneLocalRepositoryExpression()
        throws DuplicateParameterException
    {
        printMethodHeader();

        Parameter param = new Parameter();
        param.setName( "testName" );
        param.setAlias( "testAlias" );
        param.setExpression( "${localRepository}" );
        param.setEditable( false );

        PluginParameterException error = buildException( "test", "test", Collections.singletonList( param ) );

        String userMessage = diagnoser.diagnose( error );

        System.out.println( userMessage );

        assertNotNull( userMessage );
    }

    public void testParamWithOneSystemPropertyExpression()
        throws DuplicateParameterException
    {
        printMethodHeader();

        Parameter param = new Parameter();
        param.setName( "testName" );
        param.setAlias( "testAlias" );
        param.setExpression( "${maven.mode.online}" );
        param.setEditable( false );

        PluginParameterException error = buildException( "test", "test", Collections.singletonList( param ) );

        String userMessage = diagnoser.diagnose( error );

        System.out.println( userMessage );

        assertNotNull( userMessage );
    }

    public void testParamWithOneProjectBasedExpression()
        throws DuplicateParameterException
    {
        printMethodHeader();

        Parameter param = new Parameter();
        param.setName( "testName" );
        param.setAlias( "testAlias" );
        param.setExpression( "${project.build.finalName}" );
        param.setEditable( true );

        PluginParameterException error = buildException( "test", "test", Collections.singletonList( param ) );

        String userMessage = diagnoser.diagnose( error );

        System.out.println( userMessage );

        assertNotNull( userMessage );
    }

    public void testParamWithOneProjectAPIBasedExpression()
        throws DuplicateParameterException
    {
        printMethodHeader();

        Parameter param = new Parameter();
        param.setName( "testName" );
        param.setExpression( "${project.distributionManagementArtifactRepository}" );
        param.setRequired( true );
        param.setEditable( false );

        PluginParameterException error = buildException( "test", "test", Collections.singletonList( param ) );

        String userMessage = diagnoser.diagnose( error );

        System.out.println( userMessage );

        assertNotNull( userMessage );
    }

    public void testNonEditableParamWithOneProjectBasedExpression()
        throws DuplicateParameterException
    {
        printMethodHeader();

        Parameter param = new Parameter();
        param.setName( "testName" );
        param.setAlias( "testAlias" );
        param.setExpression( "${project.build.finalName}" );
        param.setEditable( false );

        PluginParameterException error = buildException( "test", "test", Collections.singletonList( param ) );

        String userMessage = diagnoser.diagnose( error );

        System.out.println( userMessage );

        assertNotNull( userMessage );
    }

    private void printMethodHeader()
    {
        IllegalArgumentException marker = new IllegalArgumentException();

        System.out.println( "---------------------------------------------------------------------\n"
            + "Visual output for " + marker.getStackTrace()[1].getMethodName()
            + ":\n---------------------------------------------------------------------" );
    }

}
