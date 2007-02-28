package org.apache.maven.settings.validation;

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

import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.Settings;

import java.util.Iterator;
import java.util.List;

/**
 * @author Milos Kleint
 */
public class DefaultSettingsValidator
    implements SettingsValidator
{


    public SettingsValidationResult validate( Settings model )
    {
        SettingsValidationResult result = new SettingsValidationResult();
        
        List profiles = model.getProfiles();
        if (profiles != null) {
            Iterator it = profiles.iterator();
            while (it.hasNext()) {
                Profile prof = (Profile)it.next();
                validateRepositories( result, prof.getRepositories(), "repositories.repository" );
                validateRepositories( result, prof.getPluginRepositories(), "pluginRepositories.pluginRepository" );
            }
            
        }


        return result;
    }

    private void validateRepositories( SettingsValidationResult result, List repositories, String prefix )
    {
        for ( Iterator it = repositories.iterator(); it.hasNext(); )
        {
            Repository repository = (Repository) it.next();

            validateStringNotEmpty( prefix + ".id", result, repository.getId() );

            validateStringNotEmpty( prefix + ".url", result, repository.getUrl() );
        }
    }



    // ----------------------------------------------------------------------
    // Field validation
    // ----------------------------------------------------------------------


    private boolean validateStringNotEmpty( String fieldName, SettingsValidationResult result, String string )
    {
        return validateStringNotEmpty( fieldName, result, string, null );
    }

    /**
     * Asserts:
     * <p/>
     * <ul>
     * <li><code>string.length != null</code>
     * <li><code>string.length > 0</code>
     * </ul>
     */
    private boolean validateStringNotEmpty( String fieldName, SettingsValidationResult result, String string, String sourceHint )
    {
        if ( !validateNotNull( fieldName, result, string, sourceHint ) )
        {
            return false;
        }

        if ( string.length() > 0 )
        {
            return true;
        }

        if ( sourceHint != null )
        {
            result.addMessage( "'" + fieldName + "' is missing for " + sourceHint );
        }
        else
        {
            result.addMessage( "'" + fieldName + "' is missing." );
        }


        return false;
    }

    /**
     * Asserts:
     * <p/>
     * <ul>
     * <li><code>string != null</code>
     * </ul>
     */
    private boolean validateNotNull( String fieldName, SettingsValidationResult result, Object object, String sourceHint )
    {
        if ( object != null )
        {
            return true;
        }

        if ( sourceHint != null )
        {
            result.addMessage( "'" + fieldName + "' is missing for " + sourceHint );
        }
        else
        {
            result.addMessage( "'" + fieldName + "' is missing." );
        }

        return false;
    }

}
