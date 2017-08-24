package org.apache.maven.classrealm;

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

import java.util.ServiceLoader;

import javax.script.ScriptEngineFactory;

import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.junit.Test;

public class DefaultClassRealmManagerTest extends PlexusTestCase
{
    private ClassRealmManager classRealmManager;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();
        this.classRealmManager = lookup( ClassRealmManager.class );
    }

    @Override
    protected void customizeContainerConfiguration( ContainerConfiguration configuration )
    {
        configuration.setClassPathScanning( PlexusConstants.SCANNING_INDEX );
    }

    @Test
    public void testMNG6275_pluginRealmDefaultParentClassLoader()
    {
        Plugin plugin = new Plugin();
        plugin.setVersion( "VERSION" );
        
        ClassLoader parent = null;
        
        ClassRealm pluginRealm = classRealmManager.createPluginRealm( plugin, parent, null, null, null );
        ServiceLoader<ScriptEngineFactory> sef = ServiceLoader.load( ScriptEngineFactory.class, pluginRealm );
        assertTrue( sef.iterator().hasNext() );
    }

    @Test
    public void testMNG6275_extensionRealmDefaultParentClassLoader()
    {
        Plugin extension = new Plugin();
        extension.setVersion( "VERSION" );
        
        ClassRealm extensionRealm = classRealmManager.createExtensionRealm( extension, null );
        ServiceLoader<ScriptEngineFactory> sef = ServiceLoader.load( ScriptEngineFactory.class, extensionRealm );
        assertTrue( sef.iterator().hasNext() );
    }

    @Test
    public void testMNG6275_projectRealmDefaultParentClassLoader()
    {
        Model model = new Model();
        
        ClassRealm projectRealm = classRealmManager.createProjectRealm( model, null );
        ServiceLoader<ScriptEngineFactory> sef = ServiceLoader.load( ScriptEngineFactory.class, projectRealm );
        assertTrue( sef.iterator().hasNext() );
    }

    @Test
    public void testMNG6275_mavenApiRealmDefaultParentClassLoader()
    {
        ClassRealm mavenApiRealm = classRealmManager.getMavenApiRealm();
        ServiceLoader<ScriptEngineFactory> sef = ServiceLoader.load( ScriptEngineFactory.class, mavenApiRealm );
        assertTrue( sef.iterator().hasNext() );
    }

    @Test
    public void testMNG6275_coreRealmDefaultParentClassLoader()
    {
        ClassRealm coreRealm = classRealmManager.getCoreRealm();
        ServiceLoader<ScriptEngineFactory> sef = ServiceLoader.load( ScriptEngineFactory.class, coreRealm );
        assertTrue( sef.iterator().hasNext() );
    }
}
