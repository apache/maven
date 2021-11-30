package org.apache.maven.caching;

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

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.caching.xml.CacheConfig;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

@Singleton
@Named
public class DefaultRestoredArtifactHandler implements RestoredArtifactHandler
{

    private static final Logger LOGGER = LoggerFactory.getLogger( DefaultRestoredArtifactHandler.class );
    private static final String DIR_NAME = "cache-build-tmp";

    private final boolean adjustMetaInfVersion;

    @Inject
    public DefaultRestoredArtifactHandler( CacheConfig cacheConfig )
    {
        this.adjustMetaInfVersion = cacheConfig.adjustMetaInfVersion();
    }

    @Override
    public Path adjustArchiveArtifactVersion( MavenProject project, String originalArtifactVersion, Path artifactFile )
            throws IOException
    {
        if ( !adjustMetaInfVersion )
        {
            //option is disabled in cache configuration, return file as is
            return artifactFile;
        }

        File file = artifactFile.toFile();
        if ( project.getVersion().equals( originalArtifactVersion ) || !CacheUtils.isArchive( file ) )
        {
            //versions of artifact and building project are the same or this is not an archive, return file as is
            return artifactFile;
        }

        File tempDirName = Paths.get( project.getBuild().getDirectory() )
                .normalize()
                .resolve( DIR_NAME )
                .toFile();
        if ( tempDirName.mkdirs() )
        {
            LOGGER.debug( "Temporary directory to restore artifact was created [artifactFile={}, "
                            + "originalVersion={}, tempDir={}]",
                    artifactFile, originalArtifactVersion, tempDirName );
        }

        String currentVersion = project.getVersion();
        File tmpJarFile = File.createTempFile( artifactFile.toFile().getName(),
                '.' + FilenameUtils.getExtension( file.getName() ), tempDirName );
        tmpJarFile.deleteOnExit();
        String originalImplVersion = Attributes.Name.IMPLEMENTATION_VERSION + ": " + originalArtifactVersion;
        String implVersion = Attributes.Name.IMPLEMENTATION_VERSION + ": " + currentVersion;
        String commonXmlOriginalVersion = "<version>" + originalArtifactVersion + "</version>";
        String commonXmlVersion = "<version>" + currentVersion + "</version>";
        String originalPomPropsVersion = "version=" + originalArtifactVersion;
        String pomPropsVersion = "version=" + currentVersion;
        try ( JarFile jarFile = new JarFile( artifactFile.toFile() ) )
        {
            try ( JarOutputStream jos = new JarOutputStream(
                    new BufferedOutputStream( new FileOutputStream( tmpJarFile ) ) ) )
            {
                //Copy original jar file to the temporary one.
                Enumeration<JarEntry> jarEntries = jarFile.entries();
                byte[] buffer = new byte[1024];
                while ( jarEntries.hasMoreElements() )
                {
                    JarEntry entry = jarEntries.nextElement();
                    String entryName = entry.getName();

                    if ( entryName.startsWith( "META-INF/maven" )
                            && ( entryName.endsWith( "plugin.xml" ) || entryName.endsWith( "plugin-help.xml" ) ) )
                    {
                        replaceEntry( jarFile, entry, commonXmlOriginalVersion, commonXmlVersion, jos );
                        continue;
                    }

                    if ( entryName.endsWith( "pom.xml" ) )
                    {
                        replaceEntry( jarFile, entry, commonXmlOriginalVersion, commonXmlVersion, jos );
                        continue;
                    }

                    if ( entryName.endsWith( "pom.properties" ) )
                    {
                        replaceEntry( jarFile, entry, originalPomPropsVersion, pomPropsVersion, jos );
                        continue;
                    }

                    if ( JarFile.MANIFEST_NAME.equals( entryName ) )
                    {
                        replaceEntry( jarFile, entry, originalImplVersion, implVersion, jos );
                        continue;
                    }
                    jos.putNextEntry( entry );
                    try ( InputStream entryInputStream = jarFile.getInputStream( entry ) )
                    {
                        int bytesRead;
                        while ( ( bytesRead = entryInputStream.read( buffer ) ) != -1 )
                        {
                            jos.write( buffer, 0, bytesRead );
                        }
                    }
                }
            }
        }
        return tmpJarFile.toPath();
    }

    private static void replaceEntry( JarFile jarFile, JarEntry entry,
                                      String toReplace, String replacement, JarOutputStream jos ) throws IOException
    {
        String fullManifest = IOUtils.toString( jarFile.getInputStream( entry ), StandardCharsets.UTF_8.name() );
        String modified = fullManifest.replaceAll( toReplace, replacement );

        byte[] bytes = modified.getBytes( StandardCharsets.UTF_8 );
        JarEntry newEntry = new JarEntry( entry.getName() );
        jos.putNextEntry( newEntry );
        jos.write( bytes );
    }

}
