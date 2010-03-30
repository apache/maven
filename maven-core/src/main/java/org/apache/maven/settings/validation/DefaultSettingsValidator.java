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

import java.util.List;

import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.SettingsProblem;
import org.apache.maven.settings.building.SettingsProblemCollector;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.util.StringUtils;

/**
 * @author Milos Kleint
 */
@Component(role = SettingsValidator.class)
public class DefaultSettingsValidator
    implements SettingsValidator
{

    private static final String ID_REGEX = "[A-Za-z0-9_\\-.]+";

    public void validate( Settings settings, SettingsProblemCollector problems )
    {
        if ( settings.isUsePluginRegistry() )
        {
            addWarn( problems, "'usePluginRegistry' is deprecated and has no effect." );
        }

        List<String> pluginGroups = settings.getPluginGroups();

        if ( pluginGroups != null )
        {
            for ( int i = 0; i < pluginGroups.size(); i++ )
            {
                String pluginGroup = pluginGroups.get( i ).trim();

                if ( StringUtils.isBlank( pluginGroup ) )
                {
                    addError( problems, "'pluginGroups.pluginGroup[" + i + "]' must not be empty." );
                }
                else if ( !pluginGroup.matches( ID_REGEX ) )
                {
                    addError( problems, "'pluginGroups.pluginGroup[" + i
                        + "]' must denote a valid group id and match the pattern " + ID_REGEX );
                }
            }
        }

        List<Server> servers = settings.getServers();

        if ( servers != null )
        {
            for ( int i = 0; i < servers.size(); i++ )
            {
                Server server = servers.get( i );

                validateStringNotEmpty( problems, "servers.server[" + i + "].id", server.getId(), null );
            }
        }

        List<Mirror> mirrors = settings.getMirrors();

        if ( mirrors != null )
        {
            for ( Mirror mirror : mirrors )
            {
                validateStringNotEmpty( problems, "mirrors.mirror.id", mirror.getId(), mirror.getUrl() );

                validateStringNotEmpty( problems, "mirrors.mirror.url", mirror.getUrl(), mirror.getId() );

                validateStringNotEmpty( problems, "mirrors.mirror.mirrorOf", mirror.getMirrorOf(), mirror.getId() );
            }
        }

        List<Profile> profiles = settings.getProfiles();

        if ( profiles != null )
        {
            for ( Profile profile : profiles )
            {
                validateRepositories( problems, profile.getRepositories(), "repositories.repository" );
                validateRepositories( problems, profile.getPluginRepositories(), "pluginRepositories.pluginRepository" );
            }
        }
    }

    private void validateRepositories( SettingsProblemCollector problems, List<Repository> repositories, String prefix )
    {
        for ( Repository repository : repositories )
        {
            validateStringNotEmpty( problems, prefix + ".id", repository.getId(), repository.getUrl() );

            validateStringNotEmpty( problems, prefix + ".url", repository.getUrl(), repository.getId() );

            if ( "legacy".equals( repository.getLayout() ) )
            {
                addWarn( problems, "'" + prefix + ".layout' for " + repository.getId()
                    + " uses the deprecated value 'legacy'." );
            }
        }
    }

    // ----------------------------------------------------------------------
    // Field validation
    // ----------------------------------------------------------------------

    /**
     * Asserts:
     * <p/>
     * <ul>
     * <li><code>string.length != null</code>
     * <li><code>string.length > 0</code>
     * </ul>
     */
    private boolean validateStringNotEmpty( SettingsProblemCollector problems, String fieldName, String string, String sourceHint )
    {
        if ( !validateNotNull( problems, fieldName, string, sourceHint ) )
        {
            return false;
        }

        if ( string.length() > 0 )
        {
            return true;
        }

        String msg;
        if ( sourceHint != null )
        {
            msg = "'" + fieldName + "' is missing for " + sourceHint;
        }
        else
        {
            msg = "'" + fieldName + "' is missing.";
        }
        addError( problems, msg );

        return false;
    }

    /**
     * Asserts:
     * <p/>
     * <ul>
     * <li><code>string != null</code>
     * </ul>
     */
    private boolean validateNotNull( SettingsProblemCollector problems, String fieldName, Object object,
                                     String sourceHint )
    {
        if ( object != null )
        {
            return true;
        }

        String msg;
        if ( sourceHint != null )
        {
            msg = "'" + fieldName + "' is missing for " + sourceHint;
        }
        else
        {
            msg = "'" + fieldName + "' is missing.";
        }
        addError( problems, msg );

        return false;
    }

    private void addError( SettingsProblemCollector problems, String msg )
    {
        problems.add( SettingsProblem.Severity.ERROR, msg, -1, -1, null );
    }

    private void addWarn( SettingsProblemCollector problems, String msg )
    {
        problems.add( SettingsProblem.Severity.WARNING, msg, -1, -1, null );
    }

}
