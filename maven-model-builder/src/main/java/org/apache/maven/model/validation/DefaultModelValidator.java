package org.apache.maven.model.validation;

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

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Profile;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.Reporting;
import org.apache.maven.model.Repository;
import org.apache.maven.model.Resource;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblem.Severity;
import org.apache.maven.model.building.ModelProblemCollector;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.util.StringUtils;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 */
@Component(role = ModelValidator.class )
public class DefaultModelValidator
    implements ModelValidator
{

    private static final String ID_REGEX = "[A-Za-z0-9_\\-.]+";

    public void validateRawModel( Model model, ModelBuildingRequest request, ModelProblemCollector problems )
    {
        Parent parent = model.getParent();
        if ( parent != null )
        {
            validateStringNotEmpty( "parent.groupId", problems, Severity.FATAL, parent.getGroupId() );

            validateStringNotEmpty( "parent.artifactId", problems, Severity.FATAL, parent.getArtifactId() );

            validateStringNotEmpty( "parent.version", problems, Severity.FATAL, parent.getVersion() );

            if ( equals( parent.getGroupId(), model.getGroupId() )
                && equals( parent.getArtifactId(), model.getArtifactId() ) )
            {
                addViolation( problems, Severity.ERROR, "The parent element cannot have the same ID as the project." );
            }
        }

        if ( request.getValidationLevel() >= ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_2_0 )
        {
            validateEnum( "modelVersion", problems, Severity.ERROR, model.getModelVersion(), null, "4.0.0" );
            validateStringNoExpression( "groupId", problems, Severity.WARNING, model.getGroupId() );
            validateStringNoExpression( "artifactId", problems, Severity.WARNING, model.getArtifactId() );
            validateStringNoExpression( "version", problems, Severity.WARNING, model.getVersion() );

            validateDependencies( problems, model.getDependencies(), "dependencies.dependency", request );

            if ( model.getDependencyManagement() != null )
            {
                validateDependencies( problems, model.getDependencyManagement().getDependencies(),
                                      "dependencyManagement.dependencies.dependency", request );
            }

            validateRepositories( problems, model.getRepositories(), "repositories.repository", request );

            validateRepositories( problems, model.getPluginRepositories(), "pluginRepositories.pluginRepository", request );

            Set<String> profileIds = new HashSet<String>();

            for ( Profile profile : model.getProfiles() )
            {
                if ( !profileIds.add( profile.getId() ) )
                {
                    addViolation( problems, Severity.ERROR, "profiles.profile.id must be unique"
                        + " but found duplicate profile with id " + profile.getId() );
                }

                validateDependencies( problems, profile.getDependencies(), "profiles.profile[" + profile.getId()
                    + "].dependencies.dependency", request );

                if ( profile.getDependencyManagement() != null )
                {
                    validateDependencies( problems, profile.getDependencyManagement().getDependencies(),
                                          "profiles.profile[" + profile.getId()
                                              + "].dependencyManagement.dependencies.dependency", request );
                }

                validateRepositories( problems, profile.getRepositories(), "profiles.profile[" + profile.getId()
                    + "].repositories.repository", request );

                validateRepositories( problems, profile.getPluginRepositories(), "profiles.profile[" + profile.getId()
                    + "].pluginRepositories.pluginRepository", request );
            }
        }
    }

    public void validateEffectiveModel( Model model, ModelBuildingRequest request, ModelProblemCollector problems )
    {
        validateStringNotEmpty( "modelVersion", problems, Severity.ERROR, model.getModelVersion() );

        validateId( "groupId", problems, model.getGroupId() );

        validateId( "artifactId", problems, model.getArtifactId() );

        validateStringNotEmpty( "packaging", problems, Severity.ERROR, model.getPackaging() );

        if ( !model.getModules().isEmpty() && !"pom".equals( model.getPackaging() ) )
        {
            addViolation( problems, Severity.ERROR, "Packaging '" + model.getPackaging()
                + "' is invalid. Aggregator projects " + "require 'pom' as packaging." );
        }

        validateStringNotEmpty( "version", problems, Severity.ERROR, model.getVersion() );

        Severity errOn30 = getSeverity( request, ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_3_0 );

        for ( Dependency d : model.getDependencies() )
        {
            validateId( "dependencies.dependency.artifactId", problems, d.getArtifactId(), d.getManagementKey() );

            validateId( "dependencies.dependency.groupId", problems, d.getGroupId(), d.getManagementKey() );

            validateStringNotEmpty( "dependencies.dependency.type", problems, Severity.ERROR, d.getType(),
                                    d.getManagementKey() );

            validateStringNotEmpty( "dependencies.dependency.version", problems, Severity.ERROR, d.getVersion(),
                                    d.getManagementKey() );

            if ( "system".equals( d.getScope() ) )
            {
                String systemPath = d.getSystemPath();

                if ( StringUtils.isEmpty( systemPath ) )
                {
                    addViolation( problems, Severity.ERROR, "For dependency " + d.getManagementKey()
                        + ": system-scoped dependency must specify systemPath." );
                }
                else
                {
                    if ( !new File( systemPath ).isAbsolute() )
                    {
                        addViolation( problems, Severity.ERROR, "For dependency " + d.getManagementKey()
                            + ": system-scoped dependency must specify an absolute systemPath but is " + systemPath );
                    }
                }
            }
            else if ( StringUtils.isNotEmpty( d.getSystemPath() ) )
            {
                addViolation( problems, Severity.ERROR, "For dependency " + d.getManagementKey()
                    + ": only dependency with system scope can specify systemPath." );
            }

            if ( request.getValidationLevel() >= ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_2_0 )
            {
                validateVersion( "dependencies.dependency.version", problems, errOn30, d.getVersion(),
                                 d.getManagementKey() );

                validateBoolean( "dependencies.dependency.optional", problems, errOn30, d.getOptional(),
                                 d.getManagementKey() );

                /*
                 * TODO: Extensions like Flex Mojos use custom scopes like "merged", "internal", "external", etc. In
                 * order to don't break backward-compat with those, only warn but don't error out.
                 */
                validateEnum( "dependencies.dependency.scope", problems, Severity.WARNING, d.getScope(),
                              d.getManagementKey(), "provided", "compile", "runtime", "test", "system" );
            }
        }

        DependencyManagement mgmt = model.getDependencyManagement();
        if ( mgmt != null )
        {
            for ( Dependency d : mgmt.getDependencies() )
            {
                validateStringNotEmpty( "dependencyManagement.dependencies.dependency.artifactId", problems,
                                        Severity.ERROR, d.getArtifactId(), d.getManagementKey() );

                validateStringNotEmpty( "dependencyManagement.dependencies.dependency.groupId", problems,
                                        Severity.ERROR, d.getGroupId(), d.getManagementKey() );

                if ( "system".equals( d.getScope() ) )
                {
                    String systemPath = d.getSystemPath();

                    if ( StringUtils.isEmpty( systemPath ) )
                    {
                        addViolation( problems, Severity.ERROR, "For managed dependency " + d.getManagementKey()
                            + ": system-scoped dependency must specify systemPath." );
                    }
                    else
                    {
                        if ( !new File( systemPath ).isAbsolute() )
                        {
                            addViolation( problems, Severity.ERROR, "For managed dependency " + d.getManagementKey()
                                + ": system-scoped dependency must specify an absolute systemPath but is " + systemPath );
                        }
                    }
                }
                else if ( StringUtils.isNotEmpty( d.getSystemPath() ) )
                {
                    addViolation( problems, Severity.ERROR, "For managed dependency " + d.getManagementKey()
                        + ": only dependency with system scope can specify systemPath." );
                }

                if ( request.getValidationLevel() >= ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_2_0 )
                {
                    validateBoolean( "dependencyManagement.dependencies.dependency.optional", problems, errOn30,
                                     d.getOptional(), d.getManagementKey() );
                }
            }
        }

        if ( request.getValidationLevel() >= ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_2_0 )
        {
            Set<String> modules = new HashSet<String>();
            for ( String module : model.getModules() )
            {
                if ( !modules.add( module ) )
                {
                    addViolation( problems, Severity.ERROR, "Duplicate child module: " + module );
                }
            }

            Severity errOn31 = getSeverity( request, ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_3_1 );

            Build build = model.getBuild();
            if ( build != null )
            {
                for ( Plugin p : build.getPlugins() )
                {
                    validateStringNotEmpty( "build.plugins.plugin.artifactId", problems, Severity.ERROR,
                                            p.getArtifactId() );

                    validateStringNotEmpty( "build.plugins.plugin.groupId", problems, Severity.ERROR, p.getGroupId() );

                    validatePluginVersion( "build.plugins.plugin.version", problems, p.getVersion(), p.getKey(),
                                           request );

                    validateBoolean( "build.plugins.plugin.inherited", problems, errOn30, p.getInherited(),
                                     p.getKey() );

                    validateBoolean( "build.plugins.plugin.extensions", problems, errOn30, p.getExtensions(),
                                     p.getKey() );

                    for ( Dependency d : p.getDependencies() )
                    {
                        validateEnum( "build.plugins.plugin[" + p.getKey() + "].dependencies.dependency.scope",
                                      problems, errOn30, d.getScope(), d.getManagementKey(),
                                      "compile", "runtime", "system" );
                    }
                }

                validateResources( problems, build.getResources(), "build.resources.resource", request );

                validateResources( problems, build.getTestResources(), "build.testResources.testResource", request );
            }

            Reporting reporting = model.getReporting();
            if ( reporting != null )
            {
                for ( ReportPlugin p : reporting.getPlugins() )
                {
                    validateStringNotEmpty( "reporting.plugins.plugin.artifactId", problems, Severity.ERROR,
                                            p.getArtifactId() );

                    validateStringNotEmpty( "reporting.plugins.plugin.groupId", problems, Severity.ERROR,
                                            p.getGroupId() );

                    validateStringNotEmpty( "reporting.plugins.plugin.version", problems, errOn31, p.getVersion(),
                                            p.getKey() );
                }
            }

            forcePluginExecutionIdCollision( model, problems );

            for ( Repository repository : model.getRepositories() )
            {
                validateRepositoryLayout( problems, repository, "repositories.repository", request );
            }

            for ( Repository repository : model.getPluginRepositories() )
            {
                validateRepositoryLayout( problems, repository, "pluginRepositories.pluginRepository", request );
            }

            DistributionManagement distMgmt = model.getDistributionManagement();
            if ( distMgmt != null )
            {
                if ( distMgmt.getStatus() != null )
                {
                    addViolation( problems, Severity.ERROR, "'distributionManagement.status' must not be specified" );
                }

                validateRepositoryLayout( problems, distMgmt.getRepository(), "distributionManagement.repository",
                                          request );
                validateRepositoryLayout( problems, distMgmt.getSnapshotRepository(),
                                          "distributionManagement.snapshotRepository", request );
            }
        }
    }

    private boolean validateId( String fieldName, ModelProblemCollector problems, String id )
    {
        return validateId( fieldName, problems, id, null );
    }

    private boolean validateId( String fieldName, ModelProblemCollector problems, String id, String sourceHint )
    {
        if ( !validateStringNotEmpty( fieldName, problems, Severity.ERROR, id, sourceHint ) )
        {
            return false;
        }
        else
        {
            boolean match = id.matches( ID_REGEX );
            if ( !match )
            {
                addViolation( problems, Severity.ERROR, "'" + fieldName + "'"
                    + ( sourceHint != null ? " for " + sourceHint : "" ) + " with value '" + id
                    + "' does not match a valid id pattern." );
            }
            return match;
        }
    }

    private void validateDependencies( ModelProblemCollector problems, List<Dependency> dependencies, String prefix,
                                       ModelBuildingRequest request )
    {
        Map<String, Dependency> index = new HashMap<String, Dependency>();

        for ( Dependency dependency : dependencies )
        {
            String key = dependency.getManagementKey();

            if ( "pom".equals( dependency.getType() ) && "import".equals( dependency.getScope() )
                && StringUtils.isNotEmpty( dependency.getClassifier() ) )
            {
                addViolation( problems, Severity.ERROR, "'" + prefix + ".classifier' must be empty for imported POM: " + key );
            }
            else if ( "system".equals( dependency.getScope() ) )
            {
                String sysPath = dependency.getSystemPath();
                if ( StringUtils.isNotEmpty( sysPath ) && !hasExpression( sysPath ) )
                {
                    addViolation( problems, Severity.WARNING, "'" + prefix
                        + ".systemPath' should use a variable instead of a hard-coded path: " + key + " -> " + sysPath );
                }
            }

            Dependency existing = index.get( key );

            if ( existing != null )
            {
                Severity errOn30 = getSeverity( request, ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_3_0 );

                String msg;
                if ( equals( existing.getVersion(), dependency.getVersion() ) )
                {
                    msg =
                        "duplicate declaration of version "
                            + StringUtils.defaultString( dependency.getVersion(), "(?)" );
                }
                else
                {
                    msg =
                        "version " + StringUtils.defaultString( existing.getVersion(), "(?)" ) + " vs "
                            + StringUtils.defaultString( dependency.getVersion(), "(?)" );
                }

                addViolation( problems, errOn30, "'" + prefix
                    + ".(groupId:artifactId:type:classifier)' must be unique: " + key + " -> " + msg );
            }
            else
            {
                index.put( key, dependency );
            }
        }
    }

    private void validateRepositories( ModelProblemCollector problems, List<Repository> repositories, String prefix,
                                       ModelBuildingRequest request )
    {
        Map<String, Repository> index = new HashMap<String, Repository>();

        for ( Repository repository : repositories )
        {
            validateStringNotEmpty( prefix + ".id", problems, Severity.ERROR, repository.getId() );

            validateStringNotEmpty( prefix + "[" + repository.getId() + "].url", problems, Severity.ERROR,
                                    repository.getUrl() );

            String key = repository.getId();

            Repository existing = index.get( key );

            if ( existing != null )
            {
                Severity errOn30 = getSeverity( request, ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_3_0 );

                addViolation( problems, errOn30, "'" + prefix + ".id' must be unique: " + repository.getId() + " -> "
                    + existing.getUrl() + " vs " + repository.getUrl() );
            }
            else
            {
                index.put( key, repository );
            }
        }
    }

    private void validateRepositoryLayout( ModelProblemCollector problems, Repository repository, String prefix,
                                           ModelBuildingRequest request )
    {
        if ( repository != null && "legacy".equals( repository.getLayout() ) )
        {
            addViolation( problems, Severity.WARNING, "'" + prefix + ".layout = legacy' is deprecated: "
                + repository.getId() );
        }
    }

    private void validateResources( ModelProblemCollector problems, List<Resource> resources, String prefix, ModelBuildingRequest request )
    {
        Severity errOn30 = getSeverity( request, ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_3_0 );

        for ( Resource resource : resources )
        {
            validateStringNotEmpty( prefix + ".directory", problems, Severity.ERROR, resource.getDirectory() );

            validateBoolean( prefix + ".filtering", problems, errOn30, resource.getFiltering(),
                             resource.getDirectory() );
        }
    }

    private void forcePluginExecutionIdCollision( Model model, ModelProblemCollector problems )
    {
        Build build = model.getBuild();

        if ( build != null )
        {
            List<Plugin> plugins = build.getPlugins();

            if ( plugins != null )
            {
                for ( Plugin plugin : plugins )
                {
                    // this will force an IllegalStateException, even if we don't have to do inheritance assembly.
                    try
                    {
                        plugin.getExecutionsAsMap();
                    }
                    catch ( IllegalStateException collisionException )
                    {
                        addViolation( problems, Severity.ERROR, collisionException.getMessage() );
                    }
                }
            }
        }
    }

    // ----------------------------------------------------------------------
    // Field validation
    // ----------------------------------------------------------------------

    private boolean validateStringNoExpression( String fieldName, ModelProblemCollector problems, Severity severity,
                                                String string )
    {
        if ( !hasExpression( string ) )
        {
            return true;
        }

        addViolation( problems, severity, "'" + fieldName + "' contains an expression but should be a constant." );

        return false;
    }

    private boolean hasExpression( String value )
    {
        return value != null && value.indexOf( "${" ) >= 0;
    }

    private boolean validateStringNotEmpty( String fieldName, ModelProblemCollector problems, Severity severity,
                                            String string )
    {
        return validateStringNotEmpty( fieldName, problems, severity, string, null );
    }

    /**
     * Asserts:
     * <p/>
     * <ul>
     * <li><code>string != null</code>
     * <li><code>string.length > 0</code>
     * </ul>
     */
    private boolean validateStringNotEmpty( String fieldName, ModelProblemCollector problems, Severity severity,
                                            String string, String sourceHint )
    {
        if ( !validateNotNull( fieldName, problems, severity, string, sourceHint ) )
        {
            return false;
        }

        if ( string.length() > 0 )
        {
            return true;
        }

        if ( sourceHint != null )
        {
            addViolation( problems, severity, "'" + fieldName + "' is missing for " + sourceHint );
        }
        else
        {
            addViolation( problems, severity, "'" + fieldName + "' is missing." );
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
    private boolean validateNotNull( String fieldName, ModelProblemCollector problems, Severity severity,
                                     Object object, String sourceHint )
    {
        if ( object != null )
        {
            return true;
        }

        if ( sourceHint != null )
        {
            addViolation( problems, severity, "'" + fieldName + "' is missing for " + sourceHint );
        }
        else
        {
            addViolation( problems, severity, "'" + fieldName + "' is missing." );
        }

        return false;
    }

    private boolean validateBoolean( String fieldName, ModelProblemCollector problems, Severity severity, String string,
                                     String sourceHint )
    {
        if ( string == null || string.length() <= 0 )
        {
            return true;
        }

        if ( "true".equalsIgnoreCase( string ) || "false".equalsIgnoreCase( string ) )
        {
            return true;
        }

        if ( sourceHint != null )
        {
            addViolation( problems, severity, "'" + fieldName + "' must be 'true' or 'false' for " + sourceHint
                + " but is '" + string + "'." );
        }
        else
        {
            addViolation( problems, severity, "'" + fieldName + "' must be 'true' or 'false' but is '" + string + "'." );
        }

        return false;
    }

    private boolean validateEnum( String fieldName, ModelProblemCollector problems, Severity severity, String string,
                                  String sourceHint, String... validValues )
    {
        if ( string == null || string.length() <= 0 )
        {
            return true;
        }

        List<String> values = Arrays.asList( validValues );

        if ( values.contains( string ) )
        {
            return true;
        }

        if ( sourceHint != null )
        {
            addViolation( problems, severity, "'" + fieldName + "' must be one of " + values + " for " + sourceHint
                + " but is '" + string + "'." );
        }
        else
        {
            addViolation( problems, severity, "'" + fieldName + "' must be one of " + values + " but is '" + string
                + "'." );
        }

        return false;
    }

    private boolean validateVersion( String fieldName, ModelProblemCollector problems, Severity severity, String string,
                                     String sourceHint )
    {
        if ( string == null || string.length() <= 0 )
        {
            return true;
        }

        if ( !hasExpression( string ) )
        {
            return true;
        }

        if ( sourceHint != null )
        {
            addViolation( problems, severity, "'" + fieldName + "' must be a valid version for " + sourceHint
                + " but is '" + string + "'." );
        }
        else
        {
            addViolation( problems, severity, "'" + fieldName + "' must be a valid version but is '" + string + "'." );
        }

        return false;
    }

    private boolean validatePluginVersion( String fieldName, ModelProblemCollector problems, String string,
                                           String sourceHint, ModelBuildingRequest request )
    {
        Severity errOn30 = getSeverity( request, ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_3_0 );
        Severity errOn31 = getSeverity( request, ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_3_1 );

        if ( !validateNotNull( fieldName, problems, errOn31, string, sourceHint ) )
        {
            return false;
        }

        if ( string.length() > 0 && !hasExpression( string ) && !"RELEASE".equals( string )
            && !"LATEST".equals( string ) )
        {
            return true;
        }

        if ( sourceHint != null )
        {
            addViolation( problems, errOn30, "'" + fieldName + "' must be a valid version for " + sourceHint
                + " but is '" + string + "'." );
        }
        else
        {
            addViolation( problems, errOn30, "'" + fieldName + "' must be a valid version but is '" + string + "'." );
        }

        return false;
    }

    private static void addViolation( ModelProblemCollector problems, Severity severity, String message )
    {
        problems.add( severity, message, null );
    }

    private static boolean equals( String s1, String s2 )
    {
        return StringUtils.clean( s1 ).equals( StringUtils.clean( s2 ) );
    }

    private static Severity getSeverity( ModelBuildingRequest request, int errorThreshold )
    {
        return getSeverity( request.getValidationLevel(), errorThreshold );
    }

    private static Severity getSeverity( int validationLevel, int errorThreshold )
    {
        if ( validationLevel < errorThreshold )
        {
            return Severity.WARNING;
        }
        else
        {
            return Severity.ERROR;
        }
    }

}
