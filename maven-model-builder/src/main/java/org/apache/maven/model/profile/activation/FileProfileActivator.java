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
import org.apache.maven.model.path.PathTranslator;
import org.apache.maven.model.path.ProfileActivationFilePathInterpolator;
import org.apache.maven.model.profile.ProfileActivationContext;
import org.codehaus.plexus.interpolation.AbstractValueSource;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.MapBasedValueSource;
import org.codehaus.plexus.interpolation.RegexBasedInterpolator;
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

    private PathTranslator pathTranslator;

    public FileProfileActivator setProfileActivationFilePathInterpolator(
            ProfileActivationFilePathInterpolator profileActivationFilePathInterpolator )
    {
        this.profileActivationFilePathInterpolator = profileActivationFilePathInterpolator;
        return this;
    }

    /**
     * Sets {@link PathTranslator}. Used by legacy code that is NOT using container, but are instantiating this class
     * manually (clearly, a misuse). Still, to
     *
     * @deprecated This setter is to be removed in next 3.9.x and later versions of Maven.
     */
    @Deprecated
    public FileProfileActivator setPathTranslator( PathTranslator pathTranslator )
    {
        this.pathTranslator = pathTranslator;
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

        // MANUALLY CONSTRUCTED in legacy code:
        // this is preserving "old" way of working, if for example class is instantiated manually
        // instead using DI. This IF is ONLY here in 3.8.x line, and is NOT to be forward ported to later
        // Maven versions.
        if ( pathTranslator != null )
        {
            RegexBasedInterpolator interpolator = new RegexBasedInterpolator();

            final File basedir = context.getProjectDirectory();

            if ( basedir != null )
            {
                interpolator.addValueSource( new AbstractValueSource( false )
                {
                    @Override
                    public Object getValue( String expression )
                    {
                        /*
                         * NOTE: We intentionally only support ${basedir} and not ${project.basedir} as the latter form
                         * would suggest that other project.* expressions can be used which is however beyond the design
                         */
                        if ( "basedir".equals( expression ) )
                        {
                            return basedir.getAbsolutePath();
                        }
                        return null;
                    }
                } );
            }
            else if ( path.contains( "${basedir}" ) )
            {
                return false;
            }

            interpolator.addValueSource( new MapBasedValueSource( context.getProjectProperties() ) );

            interpolator.addValueSource( new MapBasedValueSource( context.getUserProperties() ) );

            interpolator.addValueSource( new MapBasedValueSource( context.getSystemProperties() ) );

            try
            {
                path = interpolator.interpolate( path, "" );
            }
            catch ( Exception e )
            {
                problems.add( new ModelProblemCollectorRequest( Severity.ERROR, Version.BASE )
                        .setMessage( "Failed to interpolate file location " + path + " for profile " + profile.getId()
                                + ": " + e.getMessage() )
                        .setLocation( file.getLocation( missing ? "missing" : "exists" ) )
                        .setException( e ) );
                return false;
            }

            path = pathTranslator.alignToBaseDirectory( path, basedir );

            // replace activation value with interpolated value
            if ( missing )
            {
                file.setMissing( path );
            }
            else
            {
                file.setExists( path );
            }

            File f = new File( path );

            if ( !f.isAbsolute() )
            {
                return false;
            }

            boolean fileExists = f.exists();

            return missing ? !fileExists : fileExists;
        }
        else // container constructed
        {

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

            return missing != f.exists();
        }
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

        return file != null;
    }

}
