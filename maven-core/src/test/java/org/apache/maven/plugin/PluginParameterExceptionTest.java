package org.apache.maven.plugin;

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

import java.util.Collections;

import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * MNG-3131
 *
 * @author Robert Scholte
 *
 */
public class PluginParameterExceptionTest
{

    private final String LS = System.lineSeparator();

    @Test
    public void testMissingRequiredStringArrayTypeParameter()
    {
        MojoDescriptor mojoDescriptor = new MojoDescriptor();
        mojoDescriptor.setGoal( "goal" );
        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        pluginDescriptor.setGoalPrefix( "goalPrefix" );
        pluginDescriptor.setArtifactId( "artifactId" );
        mojoDescriptor.setPluginDescriptor( pluginDescriptor );

        Parameter parameter = new Parameter();
        parameter.setType( "java.lang.String[]" );
        parameter.setName( "toAddresses" );

        parameter.setRequired( true );

        PluginParameterException exception =
            new PluginParameterException( mojoDescriptor, Collections.singletonList( parameter ) );

        assertEquals( "One or more required plugin parameters are invalid/missing for 'goalPrefix:goal'" +
                LS + LS +
                "[0] Inside the definition for plugin 'artifactId', specify the following:" +
                LS + LS +
                "<configuration>" + LS +
                "  ..." + LS +
                "  <toAddresses>" + LS +
                "    <item>VALUE</item>" + LS +
                "  </toAddresses>" + LS +
                "</configuration>." + LS, exception.buildDiagnosticMessage() );
    }

    @Test
    public void testMissingRequiredCollectionTypeParameter()
    {
        MojoDescriptor mojoDescriptor = new MojoDescriptor();
        mojoDescriptor.setGoal( "goal" );
        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        pluginDescriptor.setGoalPrefix( "goalPrefix" );
        pluginDescriptor.setArtifactId( "artifactId" );
        mojoDescriptor.setPluginDescriptor( pluginDescriptor );

        Parameter parameter = new Parameter();
        parameter.setType( "java.util.List" );
        parameter.setName( "toAddresses" );

        parameter.setRequired( true );

        PluginParameterException exception =
            new PluginParameterException( mojoDescriptor, Collections.singletonList( parameter ) );

        assertEquals( "One or more required plugin parameters are invalid/missing for 'goalPrefix:goal'" +
                LS + LS +
                "[0] Inside the definition for plugin 'artifactId', specify the following:" +
                LS + LS +
                "<configuration>" + LS +
                "  ..." + LS +
                "  <toAddresses>" + LS +
                "    <item>VALUE</item>" + LS +
                "  </toAddresses>" + LS +
                "</configuration>." + LS, exception.buildDiagnosticMessage() );
    }

    @Test
    public void testMissingRequiredMapTypeParameter()
    {
        MojoDescriptor mojoDescriptor = new MojoDescriptor();
        mojoDescriptor.setGoal( "goal" );
        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        pluginDescriptor.setGoalPrefix( "goalPrefix" );
        pluginDescriptor.setArtifactId( "artifactId" );
        mojoDescriptor.setPluginDescriptor( pluginDescriptor );

        Parameter parameter = new Parameter();
        parameter.setType( "java.util.Map" );
        parameter.setName( "toAddresses" );

        parameter.setRequired( true );

        PluginParameterException exception =
            new PluginParameterException( mojoDescriptor, Collections.singletonList( parameter ) );

        assertEquals( "One or more required plugin parameters are invalid/missing for 'goalPrefix:goal'" +
                LS + LS +
                "[0] Inside the definition for plugin 'artifactId', specify the following:" +
                LS + LS +
                "<configuration>" + LS +
                "  ..." + LS +
                "  <toAddresses>" + LS +
                "    <KEY>VALUE</KEY>" + LS +
                "  </toAddresses>" + LS +
                "</configuration>." + LS, exception.buildDiagnosticMessage() );
    }

    @Test
    public void testMissingRequiredPropertiesTypeParameter()
    {
        MojoDescriptor mojoDescriptor = new MojoDescriptor();
        mojoDescriptor.setGoal( "goal" );
        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        pluginDescriptor.setGoalPrefix( "goalPrefix" );
        pluginDescriptor.setArtifactId( "artifactId" );
        mojoDescriptor.setPluginDescriptor( pluginDescriptor );

        Parameter parameter = new Parameter();
        parameter.setType( "java.util.Properties" );
        parameter.setName( "toAddresses" );

        parameter.setRequired( true );

        PluginParameterException exception =
            new PluginParameterException( mojoDescriptor, Collections.singletonList( parameter ) );

        assertEquals( "One or more required plugin parameters are invalid/missing for 'goalPrefix:goal'" +
                LS + LS +
                "[0] Inside the definition for plugin 'artifactId', specify the following:" +
                LS + LS +
                "<configuration>" + LS +
                "  ..." + LS +
                "  <toAddresses>" + LS +
                "    <property>" + LS +
                "      <name>KEY</name>" + LS +
                "      <value>VALUE</value>" + LS +
                "    </property>" + LS +
                "  </toAddresses>" + LS +
                "</configuration>." + LS, exception.buildDiagnosticMessage() );
    }

}
