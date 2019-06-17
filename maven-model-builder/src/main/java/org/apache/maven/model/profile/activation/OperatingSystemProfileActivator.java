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

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.model.Activation;
import org.apache.maven.model.ActivationOS;
import org.apache.maven.model.Profile;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.profile.ProfileActivationContext;
import org.codehaus.plexus.util.Os;

/**
 * Determines profile activation based on the operating system of the current runtime platform.
 *
 * @author Benjamin Bentmann
 * @see ActivationOS
 */
@Named( "os" )
@Singleton
public class OperatingSystemProfileActivator
    implements ProfileActivator
{

    @Override
    public boolean isActive( Profile profile, ProfileActivationContext context, ModelProblemCollector problems )
    {
        Activation activation = profile.getActivation();

        if ( activation == null )
        {
            return false;
        }

        ActivationOS os = activation.getOs();

        if ( os == null )
        {
            return false;
        }

        boolean active = ensureAtLeastOneNonNull( os );

        if ( active && os.getFamily() != null )
        {
            active = determineFamilyMatch( os.getFamily() );
        }
        if ( active && os.getName() != null )
        {
            active = determineNameMatch( os.getName() );
        }
        if ( active && os.getArch() != null )
        {
            active = determineArchMatch( os.getArch() );
        }
        if ( active && os.getVersion() != null )
        {
            active = determineVersionMatch( os.getVersion() );
        }

        return active;
    }

    @Override
    public boolean presentInConfig( Profile profile, ProfileActivationContext context, ModelProblemCollector problems )
    {
        Activation activation = profile.getActivation();

        if ( activation == null )
        {
            return false;
        }

        ActivationOS os = activation.getOs();

        if ( os == null )
        {
            return false;
        }
        return true;
    }

    private boolean ensureAtLeastOneNonNull( ActivationOS os )
    {
        return os.getArch() != null || os.getFamily() != null || os.getName() != null || os.getVersion() != null;
    }

    private boolean determineVersionMatch( String version )
    {
        String test = version;
        boolean reverse = false;

        if ( test.startsWith( "!" ) )
        {
            reverse = true;
            test = test.substring( 1 );
        }

        boolean result = Os.isVersion( test );

        return reverse ? !result : result;
    }

    private boolean determineArchMatch( String arch )
    {
        String test = arch;
        boolean reverse = false;

        if ( test.startsWith( "!" ) )
        {
            reverse = true;
            test = test.substring( 1 );
        }

        boolean result = Os.isArch( test );

        return reverse ? !result : result;
    }

    private boolean determineNameMatch( String name )
    {
        String test = name;
        boolean reverse = false;

        if ( test.startsWith( "!" ) )
        {
            reverse = true;
            test = test.substring( 1 );
        }

        boolean result = Os.isName( test );

        return reverse ? !result : result;
    }

    private boolean determineFamilyMatch( String family )
    {
        String test = family;
        boolean reverse = false;

        if ( test.startsWith( "!" ) )
        {
            reverse = true;
            test = test.substring( 1 );
        }

        boolean result = Os.isFamily( test );

        return reverse ? !result : result;
    }

}
