package org.apache.maven.plugin.eclipse;

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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @author <a href="mailto:kenney@neonics.com">Kenney Westerhof</a>
 * @author <a href="mailto:fgiust@users.sourceforge.net">Fabrizio Giustina</a>
 * @version $Id$
 */
public class EclipseSettingsWriter
{

    private Log log;

    public EclipseSettingsWriter( Log log )
    {
        this.log = log;
    }

    protected void write( File projectBaseDir, File outputDir, MavenProject project )
        throws MojoExecutionException
    {

        // check if it's necessary to create project specific settings
        Properties coreSettings = new Properties();

        String source = EclipseUtils.getPluginSetting( project, "maven-compiler-plugin", "source",
                                                       null ); //$NON-NLS-1$ //$NON-NLS-2$
        String target = EclipseUtils.getPluginSetting( project, "maven-compiler-plugin", "target",
                                                       null ); //$NON-NLS-1$ //$NON-NLS-2$

        if ( source != null && !"1.3".equals( source ) ) //$NON-NLS-1$
        {
            coreSettings.put( "org.eclipse.jdt.core.compiler.source", source ); //$NON-NLS-1$
            coreSettings.put( "org.eclipse.jdt.core.compiler.compliance", source ); //$NON-NLS-1$
        }

        if ( target != null && !"1.2".equals( target ) ) //$NON-NLS-1$
        {
            coreSettings.put( "org.eclipse.jdt.core.compiler.codegen.targetPlatform", target ); //$NON-NLS-1$
        }

        // write the settings, if needed
        if ( !coreSettings.isEmpty() )
        {
            File settingsDir = new File( outputDir, "/.settings" ); //$NON-NLS-1$

            settingsDir.mkdirs();

            coreSettings.put( "eclipse.preferences.version", "1" ); //$NON-NLS-1$ //$NON-NLS-2$

            try
            {
                File coreSettingsFile = new File( settingsDir, "org.eclipse.jdt.core.prefs" ); //$NON-NLS-1$
                coreSettings.store( new FileOutputStream( coreSettingsFile ), null );

                log.info( Messages.getString( "EclipseSettingsWriter.wrotesettings", //$NON-NLS-1$
                                              coreSettingsFile.getAbsolutePath() ) );
            }
            catch ( FileNotFoundException e )
            {
                throw new MojoExecutionException( Messages.getString( "EclipseSettingsWriter.cannotcreatesettings" ),
                                                  e ); //$NON-NLS-1$
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( Messages.getString( "EclipseSettingsWriter.errorwritingsettings" ),
                                                  e ); //$NON-NLS-1$
            }
        }
        else
        {
            log.info( Messages.getString( "EclipseSettingsWriter.usingdefaults" ) ); //$NON-NLS-1$
        }
    }
}
