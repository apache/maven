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

import java.io.IOException;

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

import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.replace;

/**
 * FileProfileActivator
 */
@Deprecated
public class FileProfileActivator
    extends DetectedProfileActivator
    implements LogEnabled
{
    private Logger logger;

    protected boolean canDetectActivation( Profile profile )
    {
        return profile.getActivation() != null && profile.getActivation().getFile() != null;
    }

    public boolean isActive( Profile profile )
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

            try
            {
                if ( isNotEmpty( fileString ) )
                {
                    fileString = replace( interpolator.interpolate( fileString, "" ), "\\", "/" );
                    return FileUtils.fileExists( fileString );
                }

                // check if the file is missing, if it is then the profile will be active
                fileString = actFile.getMissing();

                if ( isNotEmpty( fileString ) )
                {
                    fileString = replace( interpolator.interpolate( fileString, "" ), "\\", "/" );
                    return !FileUtils.fileExists( fileString );
                }
            }
            catch ( InterpolationException e )
            {
                if ( logger.isDebugEnabled() )
                {
                    logger.debug( "Failed to interpolate missing file location for profile activator: " + fileString,
                                  e );
                }
                else
                {
                    logger.warn( "Failed to interpolate missing file location for profile activator: " + fileString
                        + ". Run in debug mode (-X) for more information." );
                }
            }
        }

        return false;
    }

    public void enableLogging( Logger logger )
    {
        this.logger = logger;
    }
}
