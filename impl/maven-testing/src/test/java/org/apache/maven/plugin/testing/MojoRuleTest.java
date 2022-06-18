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

import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;

import java.io.StringReader;
import java.util.Map;
import org.junit.Rule;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Mirko Friedenhagen
 */
public class MojoRuleTest
    
{
    private boolean beforeWasCalled = false;

    @Rule
    public final MojoRule rule = new MojoRule() {

        @Override
        protected void before()
        {
            beforeWasCalled = true;
        }      
    };

    private PlexusConfiguration pluginConfiguration;

    /** {@inheritDoc} */
    @Before
    public void setUp()
        throws Exception
    {

        String pom = "<project>" +
                "<build>" +
                "<plugins>" +
                "<plugin>" +
                "<artifactId>maven-simple-plugin</artifactId>" +
                "<configuration>" +
                "<keyOne>valueOne</keyOne>" +
                "<keyTwo>valueTwo</keyTwo>" +
                "</configuration>" +
                "</plugin>" +
                "</plugins>" +
                "</build>" +
                "</project>";

        Xpp3Dom pomDom = Xpp3DomBuilder.build( new StringReader( pom ) );

        pluginConfiguration = rule.extractPluginConfiguration( "maven-simple-plugin", pomDom );
    }

    /**
     */
    @Test
    public void testPluginConfigurationExtraction()
    {
        assertEquals( "valueOne", pluginConfiguration.getChild( "keyOne" ).getValue() );

        assertEquals( "valueTwo", pluginConfiguration.getChild( "keyTwo" ).getValue() );
    }

    /**
     * @throws Exception if any
     */
    @Test
    public void testMojoConfiguration()
        throws Exception
    {
        SimpleMojo mojo = new SimpleMojo();

        mojo = (SimpleMojo) rule.configureMojo( mojo, pluginConfiguration );

        assertEquals( "valueOne", mojo.getKeyOne() );

        assertEquals( "valueTwo", mojo.getKeyTwo() );
    }

    /**
     * @throws Exception if any
     */
    @Test
    public void testVariableAccessWithoutGetter()
        throws Exception
    {
        SimpleMojo mojo = new SimpleMojo();

        mojo = (SimpleMojo) rule.configureMojo( mojo, pluginConfiguration );

        assertEquals( "valueOne", rule.getVariableValueFromObject( mojo, "keyOne" ) );

        assertEquals( "valueTwo", rule.getVariableValueFromObject( mojo, "keyTwo" ) );
    }

    /**
     * @throws Exception if any
     */
     @Test
     public void testVariableAccessWithoutGetter2()
        throws Exception
     {
        SimpleMojo mojo = new SimpleMojo();

        mojo = (SimpleMojo) rule.configureMojo( mojo, pluginConfiguration );

        Map<String, Object> map = rule.getVariablesAndValuesFromObject( mojo );

        assertEquals( "valueOne", map.get( "keyOne" ) );

        assertEquals( "valueTwo", map.get( "keyTwo" ) );
    }

    /**
     * @throws Exception if any
     */
    @Test
    public void testSettingMojoVariables()
        throws Exception
    {
        SimpleMojo mojo = new SimpleMojo();

        mojo = (SimpleMojo) rule.configureMojo( mojo, pluginConfiguration );

        rule.setVariableValueToObject( mojo, "keyOne", "myValueOne" );

        assertEquals( "myValueOne", rule.getVariableValueFromObject( mojo, "keyOne" ) );

    }

    @Test
    @WithoutMojo
    public void testNoRuleWrapper()
    {
        assertFalse( "before executed although WithMojo annotation was added", beforeWasCalled );
    }

    @Test    
    public void testWithRuleWrapper()
    {
        assertTrue( "before executed because WithMojo annotation was not added", beforeWasCalled );
    }
}
