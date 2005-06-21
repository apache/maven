package org.apache.maven.profiles.activation;

import org.apache.maven.model.Activation;
import org.apache.maven.model.ActivationProperty;
import org.apache.maven.model.Profile;
import org.codehaus.plexus.util.StringUtils;

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

public class SystemPropertyProfileActivator
    extends DetectedProfileActivator
{
    protected boolean canDetectActivation( Profile profile )
    {
        return profile.getActivation() != null && profile.getActivation().getProperty() != null;
    }

    public boolean isActive( Profile profile )
    {
        Activation activation = profile.getActivation();

        ActivationProperty property = activation.getProperty();

        if ( property != null )
        {
            String sysValue = System.getProperty( property.getName() );

            String propValue = property.getValue();
            if ( StringUtils.isNotEmpty( propValue ) )
            {
                // we have a value, so it has to match the system value...
                return propValue.equals( sysValue );
            }
            else
            {
                return StringUtils.isNotEmpty( sysValue );
            }
        }

        return false;
    }

}
