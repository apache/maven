package org.apache.maven.plugin;

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

import org.apache.maven.Maven;
import org.apache.maven.MavenTestCase;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptorBuilder;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.plugin.MavenPluginDescriptor;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.ArrayList;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class PluginTest
    extends MavenTestCase
{
    PluginDescriptorBuilder builder = new PluginDescriptorBuilder();

    PluginManager pluginMM;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        pluginMM = (PluginManager) getContainer().lookup( PluginManager.ROLE );
    }

    private void registerPlugin( String pluginDescriptor )
        throws Exception
    {
        InputStream is = PluginTest.class.getResourceAsStream( pluginDescriptor );

        PluginDescriptor pd = builder.build( new InputStreamReader( is ) );

        getContainer().addComponentDescriptor( new MavenMojoDescriptor( (MojoDescriptor) pd.getMojos().get( 0 ) ) );

        pluginMM.processPluginDescriptor( new MavenPluginDescriptor( pd ) );
    }

    public void testFakeOut()
    {
    }

    // This is a problem because it triggers the core to run which requires transitive deps
    // and so we go hunting for everything here which is not really desired. We can probably
    // rely on the integration tests we just need to make sure the internals are working
    // here.

    public void xtestIntegratedPlugin()
        throws Exception
    {
        registerPlugin( "integrated-plugin.xml" );

        Maven maven = (Maven) lookup( Maven.ROLE );

        List goals = new ArrayList();

        goals.add( "integrated-execute" );

        maven.execute( new File( System.getProperty( "user.dir" ), "pom.xml" ), goals );

        IntegratedPlugin integratedPlugin = (IntegratedPlugin) getContainer().lookup( Plugin.ROLE, "integrated-plugin" );

        assertTrue( integratedPlugin.hasExecuted() );

        assertEquals( "Maven", integratedPlugin.getName() );

        assertEquals( "maven-core", integratedPlugin.getArtifactId() );

        assertEquals( "bar", integratedPlugin.getFoo() );
    }
}