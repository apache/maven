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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.SettingsProblem.Severity;
import org.apache.maven.settings.building.SettingsProblemCollector;
import org.apache.maven.shared.utils.StringUtils;

/**
 * @author Milos Kleint
 */
@Named
@Singleton
public class DefaultSettingsValidator
    implements SettingsValidator
{

    private static final String ID_REGEX = "[A-Za-z0-9_\\-.]+";

    private static final String ILLEGAL_FS_CHARS = "\\/:\"<>|?*";

    private static final String ILLEGAL_REPO_ID_CHARS = ILLEGAL_FS_CHARS;

    @Override
    public void validate( Settings settings, SettingsProblemCollector problems )
    {
        if ( settings.isUsePluginRegistry() )
        {
            addViolation( problems, Severity.WARNING, "usePluginRegistry", null, "is deprecated and has no effect." );
        }

        List<String> pluginGroups = settings.getPluginGroups();

        if ( pluginGroups != null )
        {
            for ( int i = 0; i < pluginGroups.size(); i++ )
            {
                String pluginGroup = pluginGroups.get( i ).trim();

                if ( StringUtils.isBlank( pluginGroup ) )
                {
                    addViolation( problems, Severity.ERROR, "pluginGroups.pluginGroup[" + i + "]", null,
                                  "must not be empty" );
                }
                else if ( !pluginGroup.matches( ID_REGEX ) )
                {
                    addViolation( problems, Severity.ERROR, "pluginGroups.pluginGroup[" + i + "]", null,
                                  "must denote a valid group id and match the pattern " + ID_REGEX );
                }
            }
        }

        List<Server> servers = settings.getServers();

        if ( servers != null )
        {
            Set<String> serverIds = new HashSet<>();

            for ( int i = 0; i < servers.size(); i++ )
            {
                Server server = servers.get( i );

                validateStringNotEmpty( problems, "servers.server[" + i + "].id", server.getId(), null );

                if ( !serverIds.add( server.getId() ) )
                {
                    addViolation( problems, Severity.WARNING, "servers.server.id", null,
                                  "must be unique but found duplicate server with id " + server.getId() );
                }
            }
        }

        List<Mirror> mirrors = settings.getMirrors();

        if ( mirrors != null )
        {
            for ( Mirror mirror : mirrors )
            {
                validateStringNotEmpty( problems, "mirrors.mirror.id", mirror.getId(), mirror.getUrl() );

                validateBannedCharacters( problems, "mirrors.mirror.id", Severity.WARNING, mirror.getId(), null,
                                          ILLEGAL_REPO_ID_CHARS );

                if ( "local".equals( mirror.getId() ) )
                {
                    addViolation( problems, Severity.WARNING, "mirrors.mirror.id", null, "must not be 'local'"
                        + ", this identifier is reserved for the local repository"
                        + ", using it for other repositories will corrupt your repository metadata." );
                }

                validateStringNotEmpty( problems, "mirrors.mirror.url", mirror.getUrl(), mirror.getId() );

                validateStringNotEmpty( problems, "mirrors.mirror.mirrorOf", mirror.getMirrorOf(), mirror.getId() );
            }
        }

        List<Profile> profiles = settings.getProfiles();

        if ( profiles != null )
        {
            Set<String> profileIds = new HashSet<>();

            for ( Profile profile : profiles )
            {
                if ( !profileIds.add( profile.getId() ) )
                {
                    addViolation( problems, Severity.WARNING, "profiles.profile.id", null,
                                  "must be unique but found duplicate profile with id " + profile.getId() );
                }

                String prefix = "profiles.profile[" + profile.getId() + "].";

                validateRepositories( problems, profile.getRepositories(), prefix + "repositories.repository" );
                validateRepositories( problems, profile.getPluginRepositories(), prefix
                    + "pluginRepositories.pluginRepository" );
            }
        }

        List<Proxy> proxies = settings.getProxies();

        if ( proxies != null )
        {
            Set<String> proxyIds = new HashSet<>();
            
            for ( Proxy proxy : proxies )
            {
                if ( !proxyIds.add( proxy.getId() ) )
                {
                    addViolation( problems, Severity.WARNING, "proxies.proxy.id", null,
                                  "must be unique but found duplicate proxy with id " + proxy.getId() );
                }
                validateStringNotEmpty( problems, "proxies.proxy.host", proxy.getHost(), proxy.getId() );
            }
        }
    }

    private void validateRepositories( SettingsProblemCollector problems, List<Repository> repositories, String prefix )
    {
        Set<String> repoIds = new HashSet<>();

        for ( Repository repository : repositories )
        {
            validateStringNotEmpty( problems, prefix + ".id", repository.getId(), repository.getUrl() );

            validateBannedCharacters( problems, prefix + ".id", Severity.WARNING, repository.getId(), null,
                                      ILLEGAL_REPO_ID_CHARS );

            if ( "local".equals( repository.getId() ) )
            {
                addViolation( problems, Severity.WARNING, prefix + ".id", null, "must not be 'local'"
                    + ", this identifier is reserved for the local repository"
                    + ", using it for other repositories will corrupt your repository metadata." );
            }

            if ( !repoIds.add( repository.getId() ) )
            {
                addViolation( problems, Severity.WARNING, prefix + ".id", null,
                              "must be unique but found duplicate repository with id " + repository.getId() );
            }

            validateStringNotEmpty( problems, prefix + ".url", repository.getUrl(), repository.getId() );

            if ( "legacy".equals( repository.getLayout() ) )
            {
                addViolation( problems, Severity.WARNING, prefix + ".layout", repository.getId(),
                              "uses the unsupported value 'legacy', artifact resolution might fail." );
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
    private static boolean validateStringNotEmpty( SettingsProblemCollector problems, String fieldName, String string,
                                            String sourceHint )
    {
        if ( !validateNotNull( problems, fieldName, string, sourceHint ) )
        {
            return false;
        }

        if ( string.length() > 0 )
        {
            return true;
        }

        addViolation( problems, Severity.ERROR, fieldName, sourceHint, "is missing" );

        return false;
    }

    /**
     * Asserts:
     * <p/>
     * <ul>
     * <li><code>string != null</code>
     * </ul>
     */
    private static boolean validateNotNull( SettingsProblemCollector problems, String fieldName, Object object,
                                            String sourceHint )
    {
        if ( object != null )
        {
            return true;
        }

        addViolation( problems, Severity.ERROR, fieldName, sourceHint, "is missing" );

        return false;
    }

    private static boolean validateBannedCharacters( SettingsProblemCollector problems, String fieldName,
                                                     Severity severity, String string, String sourceHint,
                                                     String banned )
    {
        if ( string != null )
        {
            for ( int i = string.length() - 1; i >= 0; i-- )
            {
                if ( banned.indexOf( string.charAt( i ) ) >= 0 )
                {
                    addViolation( problems, severity, fieldName, sourceHint,
                                  "must not contain any of these characters " + banned + " but found "
                                      + string.charAt( i ) );
                    return false;
                }
            }
        }

        return true;
    }

    private static void addViolation( SettingsProblemCollector problems, Severity severity, String fieldName,
                               String sourceHint, String message )
    {
        StringBuilder buffer = new StringBuilder( 256 );
        buffer.append( '\'' ).append( fieldName ).append( '\'' );

        if ( sourceHint != null )
        {
            buffer.append( " for " ).append( sourceHint );
        }

        buffer.append( ' ' ).append( message );

        problems.add( severity, buffer.toString(), -1, -1, null );
    }

}
