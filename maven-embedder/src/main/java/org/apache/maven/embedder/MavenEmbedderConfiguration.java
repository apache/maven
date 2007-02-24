package org.apache.maven.embedder;
/*
 * Copyright 2001-2005 The Apache Software Foundation.
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

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Properties;

import org.apache.maven.settings.SettingsBuilderAdvice;
import org.codehaus.plexus.classworlds.ClassWorld;

/**
 * Configuration of embedder, used when starting up.
 *
 * @author mkleint
 */
public interface MavenEmbedderConfiguration
{
    /*
    * Add profile to activate.
    */
    MavenEmbedderConfiguration addActiveProfile( String profile );

    /*
     * Add profile to inactivate.
     */
    MavenEmbedderConfiguration addInactiveProfile( String profile );

    /*
    * Add a list of String instances with names of profiles to activate.
    */
    MavenEmbedderConfiguration addActiveProfiles( List profiles );

    /*
    * Add a list of String instances with names of profiles to inactivate.
    */
    MavenEmbedderConfiguration addInactiveProfiles( List profiles );

    /*
    * Set location of the user settings file to use for the embedder.
    */
    MavenEmbedderConfiguration setUserSettingsFile( File user );

    /*
     * Set location of the global settings file to use for the embedder.
     */
    MavenEmbedderConfiguration setGlobalSettingsFile( File global );

    /**
     * Set a customizer callback implemetation that will be given a chance to modify the plexus container
     * on startup.
     */
    MavenEmbedderConfiguration setConfigurationCustomizer( ContainerCustomizer customizer );

    /** set the system properties to be used during the lifecycle of the embedder. Excluding the time when executing the project, then the properties from MavenExecutionRequestare used. */
    MavenEmbedderConfiguration setSystemProperties( Properties properties );

    List getActiveProfiles();

    List getInactiveProfiles();

    File getUserSettingsFile();

    File getGlobalSettingsFile();

    ContainerCustomizer getContainerCustomizer();

    Properties getSystemProperties();

    void addExtension( URL url );

    List getExtensions();

    MavenEmbedderConfiguration setMavenEmbedderLogger( MavenEmbedderLogger logger );

    MavenEmbedderLogger getMavenEmbedderLogger();

    ClassWorld getClassWorld();

    MavenEmbedderConfiguration setClassWorld( ClassWorld classWorld );

    MavenEmbedderConfiguration setClassLoader( ClassLoader loader );

    SettingsBuilderAdvice getSettingsBuilderAdvice();
    
    void setSettingsBuilderAdvice( SettingsBuilderAdvice advice );
}
