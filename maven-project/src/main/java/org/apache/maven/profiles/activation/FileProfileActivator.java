package org.apache.maven.profiles.activation;

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

import org.apache.maven.model.Activation;
import org.apache.maven.model.ActivationFile;
import org.apache.maven.model.Profile;
import org.codehaus.plexus.interpolation.EnvarBasedValueSource;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.MapBasedValueSource;
import org.codehaus.plexus.interpolation.RegexBasedInterpolator;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

import java.io.IOException;

public class FileProfileActivator
    implements ProfileActivator, LogEnabled
{

    private Logger logger;

    public boolean canDetermineActivation( Profile profile, ProfileActivationContext context )
    {
        return ( profile.getActivation() != null ) && ( profile.getActivation().getFile() != null );
    }

    public boolean isActive( Profile profile, ProfileActivationContext context )
    {
        Activation activation = profile.getActivation();

        ActivationFile actFile = activation.getFile();

        if ( actFile != null )
        {
            // check if the file exists, if it does then the profile will be active
            String fileString = actFile.getExists();

            RegexBasedInterpolator interpolator = new RegexBasedInterpolator();
            try
            {
                interpolator.addValueSource( new EnvarBasedValueSource() );
            }
            catch ( IOException e )
            {
                // ignored
            }
            interpolator.addValueSource( new MapBasedValueSource( System.getProperties() ) );

            if ( StringUtils.isNotEmpty( fileString ) )
            {
                try
                {
                    fileString = StringUtils.replace( interpolator.interpolate( fileString, "" ), "\\", "/" );
                }
                catch ( InterpolationException e )
                {
                    if ( logger.isDebugEnabled() )
                    {
                        logger.debug( "Failed to interpolate exists file location for profile activator: " + fileString, e );
                    }
                    else
                    {
                        logger.warn( "Failed to interpolate exists file location for profile activator: " + fileString + ". Run in debug mode (-X) for more information." );
                    }
                }

                boolean result = FileUtils.fileExists( fileString );

                if ( logger != null )
                {
                    logger.debug( "FileProfileActivator: Checking file existence for: " + fileString + ". Result: " + result );
                }

                return result;
            }

            // check if the file is missing, if it is then the profile will be active
            fileString = actFile.getMissing();

            if ( StringUtils.isNotEmpty( fileString ) )
            {
                try
                {
                    fileString = StringUtils.replace( interpolator.interpolate( fileString, "" ), "\\", "/" );
                }
                catch ( InterpolationException e )
                {
                    if ( logger.isDebugEnabled() )
                    {
                        logger.debug( "Failed to interpolate missing file location for profile activator: " + fileString, e );
                    }
                    else
                    {
                        logger.warn( "Failed to interpolate missing file location for profile activator: " + fileString + ". Run in debug mode (-X) for more information." );
                    }
                }

                boolean result = !FileUtils.fileExists( fileString );

                if ( logger != null )
                {
                    logger.debug( "FileProfileActivator: Checking file is missing for: " + fileString + ". Result: " + result );
                }

                return result;
            }
        }
        else
        {
            if ( logger != null )
            {
                logger.debug( "FileProfileActivator: no file specified. Skipping activation." );
            }
        }

        return false;
    }

    public void enableLogging( Logger logger )
    {
        this.logger = logger;
    }
}
