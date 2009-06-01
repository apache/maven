package org.apache.maven.model.profile.activator;

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

import java.io.File;

import org.apache.maven.model.Activation;
import org.apache.maven.model.ActivationFile;
import org.apache.maven.model.Profile;
import org.apache.maven.model.profile.ProfileActivationContext;
import org.apache.maven.model.profile.ProfileActivationException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.interpolation.MapBasedValueSource;
import org.codehaus.plexus.util.interpolation.RegexBasedInterpolator;

/**
 * Determines profile activation based on the existence/absence of some file.
 * 
 * @author Benjamin Bentmann
 */
@Component( role = ProfileActivator.class, hint = "file" )
public class FileProfileActivator
    implements ProfileActivator
{

    public boolean isActive( Profile profile, ProfileActivationContext context )
        throws ProfileActivationException
    {
        boolean active = false;

        Activation activation = profile.getActivation();

        if ( activation != null )
        {
            ActivationFile file = activation.getFile();

            if ( file != null )
            {
                RegexBasedInterpolator interpolator = new RegexBasedInterpolator();
                interpolator.addValueSource( new MapBasedValueSource( context.getExecutionProperties() ) );

                String existingPath = file.getExists();
                String missingPath = file.getMissing();

                if ( StringUtils.isNotEmpty( existingPath ) )
                {
                    try
                    {
                        existingPath = StringUtils.replace( interpolator.interpolate( existingPath, "" ), "\\", "/" );
                    }
                    catch ( Exception e )
                    {
                        throw new ProfileActivationException( "Failed to interpolate file location for profile "
                            + profile.getId() + ": " + existingPath, profile );
                    }
                    active = new File( existingPath ).exists();
                }
                else if ( StringUtils.isNotEmpty( missingPath ) )
                {
                    try
                    {
                        missingPath = StringUtils.replace( interpolator.interpolate( missingPath, "" ), "\\", "/" );
                    }
                    catch ( Exception e )
                    {
                        throw new ProfileActivationException( "Failed to interpolate file location for profile "
                            + profile.getId() + ": " + existingPath, profile );
                    }
                    active = !new File( missingPath ).exists();
                }

            }
        }

        return active;
    }

}
