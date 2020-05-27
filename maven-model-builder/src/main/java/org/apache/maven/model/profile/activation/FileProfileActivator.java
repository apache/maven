package org.apache.maven.model.profile.activation;

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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.model.Activation;
import org.apache.maven.model.ActivationFile;
import org.apache.maven.model.Profile;
import org.apache.maven.model.building.ModelProblem.Severity;
import org.apache.maven.model.building.ModelProblem.Version;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.building.ModelProblemCollectorRequest;
import org.apache.maven.model.path.ProfileActivationFilePathInterpolator;
import org.apache.maven.model.profile.ProfileActivationContext;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.util.StringUtils;

/**
 * Determines profile activation based on the existence/absence of some file.
 * File name interpolation support is limited to <code>${basedir}</code> (since Maven 3,
 * see <a href="https://issues.apache.org/jira/browse/MNG-2363">MNG-2363</a>),
 * System properties and request properties.
 * <code>${project.basedir}</code> is intentionally not supported as this form would suggest that other
 * <code>${project.*}</code> expressions can be used, which is however beyond the design.
 *
 * @author Benjamin Bentmann
 * @see ActivationFile
 * @see org.apache.maven.model.validation.DefaultModelValidator#validateRawModel
 */
@Named( "file" )
@Singleton
public class FileProfileActivator
    implements ProfileActivator
{

    @Inject
    private ProfileActivationFilePathInterpolator profileActivationFilePathInterpolator;

    public FileProfileActivator setProfileActivationFilePathInterpolator(
            ProfileActivationFilePathInterpolator profileActivationFilePathInterpolator )
    {
        this.profileActivationFilePathInterpolator = profileActivationFilePathInterpolator;
        return this;
    }

    @Override
    public boolean isActive( Profile profile, ProfileActivationContext context, ModelProblemCollector problems )
    {
        Activation activation = profile.getActivation();

        if ( activation == null )
        {
            return false;
        }

        ActivationFile file = activation.getFile();

        if ( file == null )
        {
            return false;
        }

        String path;
        boolean missing;

        if ( StringUtils.isNotEmpty( file.getExists() ) )
        {
            path = file.getExists();
            missing = false;
        }
        else if ( StringUtils.isNotEmpty( file.getMissing() ) )
        {
            path = file.getMissing();
            missing = true;
        }
        else
        {
            return false;
        }

        try
        {
            path = profileActivationFilePathInterpolator.interpolate( path, context );
        }
        catch ( InterpolationException e )
        {
            problems.add( new ModelProblemCollectorRequest( Severity.ERROR, Version.BASE )
                    .setMessage( "Failed to interpolate file location " + path + " for profile " + profile.getId()
                            + ": " + e.getMessage() )
                    .setLocation( file.getLocation( missing ? "missing" : "exists" ) )
                    .setException( e ) );
            return false;
        }

        if ( path == null )
        {
            return false;
        }

        File f = new File( path );

        if ( !f.isAbsolute() )
        {
            return false;
        }

        boolean fileExists = f.exists();

        return missing ? !fileExists : fileExists;
    }

    @Override
    public boolean presentInConfig( Profile profile, ProfileActivationContext context, ModelProblemCollector problems )
    {
        Activation activation = profile.getActivation();

        if ( activation == null )
        {
            return false;
        }

        ActivationFile file = activation.getFile();

        if ( file == null )
        {
            return false;
        }
        return true;
    }

}
