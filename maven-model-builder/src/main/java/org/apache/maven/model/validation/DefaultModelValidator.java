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
import java.util.List;
import java.util.Map;

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
            validateStringNotEmpty( "parent.groupId", problems, false, parent.getGroupId() );

            validateStringNotEmpty( "parent.artifactId", problems, false, parent.getArtifactId() );

            validateStringNotEmpty( "parent.version", problems, false, parent.getVersion() );

            if ( parent.getGroupId().equals( model.getGroupId() )
                && parent.getArtifactId().equals( model.getArtifactId() ) )
            {
                addViolation( problems, false, "The parent element cannot have the same ID as the project." );
            }
        }

        if ( request.getValidationLevel() >= ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_2_0 )
        {
            validateEnum( "modelVersion", problems, false, model.getModelVersion(), null, "4.0.0" );
            validateStringNoExpression( "groupId", problems, true, model.getGroupId() );
            validateStringNoExpression( "artifactId", problems, true, model.getArtifactId() );
            validateStringNoExpression( "version", problems, true, model.getVersion() );

            validateDependencies( problems, model.getDependencies(), "dependencies.dependency", request );

            if ( model.getDependencyManagement() != null )
            {
                validateDependencies( problems, model.getDependencyManagement().getDependencies(),
                                      "dependencyManagement.dependencies.dependency", request );
            }

            validateRepositories( problems, model.getRepositories(), "repositories.repository", request );

            validateRepositories( problems, model.getPluginRepositories(), "pluginRepositories.pluginRepository", request );

            for ( Profile profile : model.getProfiles() )
            {
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
        validateStringNotEmpty( "modelVersion", problems, false, model.getModelVersion() );

        validateId( "groupId", problems, model.getGroupId() );

        validateId( "artifactId", problems, model.getArtifactId() );

        validateStringNotEmpty( "packaging", problems, false, model.getPackaging() );

        if ( !model.getModules().isEmpty() && !"pom".equals( model.getPackaging() ) )
        {
            addViolation( problems, false, "Packaging '" + model.getPackaging() + "' is invalid. Aggregator projects "
                + "require 'pom' as packaging." );
        }

        Parent parent = model.getParent();
        if ( parent != null )
        {
            if ( parent.getGroupId().equals( model.getGroupId() )
                && parent.getArtifactId().equals( model.getArtifactId() ) )
            {
                addViolation( problems, false, "The parent element cannot have the same ID as the project." );
            }
        }

        validateStringNotEmpty( "version", problems, false, model.getVersion() );

        boolean warnOnBadBoolean = request.getValidationLevel() < ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_3_0;
        boolean warnOnBadDependencyScope = request.getValidationLevel() < ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_3_0;

        for ( Dependency d : model.getDependencies() )
        {
            validateId( "dependencies.dependency.artifactId", problems, d.getArtifactId() );

            validateId( "dependencies.dependency.groupId", problems, d.getGroupId() );

            validateStringNotEmpty( "dependencies.dependency.type", problems, false, d.getType(), d.getManagementKey() );

            validateStringNotEmpty( "dependencies.dependency.version", problems, false, d.getVersion(),
                                    d.getManagementKey() );

            if ( "system".equals( d.getScope() ) )
            {
                String systemPath = d.getSystemPath();

                if ( StringUtils.isEmpty( systemPath ) )
                {
                    addViolation( problems, false, "For dependency " + d + ": system-scoped dependency must specify systemPath." );
                }
                else
                {
                    if ( !new File( systemPath ).isAbsolute() )
                    {
                        addViolation( problems, false, "For dependency " + d + ": system-scoped dependency must "
                            + "specify an absolute path systemPath." );
                    }
                }
            }
            else if ( StringUtils.isNotEmpty( d.getSystemPath() ) )
            {
                addViolation( problems, false,
                    "For dependency " + d + ": only dependency with system scope can specify systemPath." );
            }

            if ( request.getValidationLevel() >= ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_2_0 )
            {
                validateBoolean( "dependencies.dependency.optional", problems, warnOnBadBoolean, d.getOptional(),
                                 d.getManagementKey() );

                /*
                 * TODO: Extensions like Flex Mojos use custom scopes like "merged", "internal", "external", etc. In
                 * order to don't break backward-compat with those, only warn but don't error our.
                 */
                validateEnum( "dependencies.dependency.scope", problems, true, d.getScope(),
                              d.getManagementKey(), "provided", "compile", "runtime", "test", "system" );
            }
        }

        DependencyManagement mgmt = model.getDependencyManagement();
        if ( mgmt != null )
        {
            for ( Dependency d : mgmt.getDependencies() )
            {
                validateSubElementStringNotEmpty( d, "dependencyManagement.dependencies.dependency.artifactId", problems,
                                                  d.getArtifactId() );

                validateSubElementStringNotEmpty( d, "dependencyManagement.dependencies.dependency.groupId", problems,
                                                  d.getGroupId() );

                if ( "system".equals( d.getScope() ) )
                {
                    String systemPath = d.getSystemPath();

                    if ( StringUtils.isEmpty( systemPath ) )
                    {
                        addViolation( problems, false,
                            "For managed dependency " + d + ": system-scoped dependency must specify systemPath." );
                    }
                    else
                    {
                        if ( !new File( systemPath ).isAbsolute() )
                        {
                            addViolation( problems, false, "For managed dependency " + d + ": system-scoped dependency must "
                                + "specify an absolute path systemPath." );
                        }
                    }
                }
                else if ( StringUtils.isNotEmpty( d.getSystemPath() ) )
                {
                    addViolation( problems, false,
                        "For managed dependency " + d + ": only dependency with system scope can specify systemPath." );
                }

                if ( request.getValidationLevel() >= ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_2_0 )
                {
                    validateBoolean( "dependencyManagement.dependencies.dependency.optional", problems,
                                     warnOnBadBoolean, d.getOptional(), d.getManagementKey() );
                }
            }
        }

        if ( request.getValidationLevel() >= ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_2_0 )
        {
            boolean warnOnMissingPluginVersion =
                request.getValidationLevel() < ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_3_1;

            Build build = model.getBuild();
            if ( build != null )
            {
                for ( Plugin p : build.getPlugins() )
                {
                    validateStringNotEmpty( "build.plugins.plugin.artifactId", problems, false, p.getArtifactId() );

                    validateStringNotEmpty( "build.plugins.plugin.groupId", problems, false, p.getGroupId() );

                    validateStringNotEmpty( "build.plugins.plugin.version", problems, warnOnMissingPluginVersion,
                                            p.getVersion(), p.getKey() );

                    validateBoolean( "build.plugins.plugin.inherited", problems, warnOnBadBoolean, p.getInherited(),
                                     p.getKey() );

                    validateBoolean( "build.plugins.plugin.extensions", problems, warnOnBadBoolean, p.getExtensions(),
                                     p.getKey() );

                    for ( Dependency d : p.getDependencies() )
                    {
                        validateEnum( "build.plugins.plugin[" + p.getKey() + "].dependencies.dependency.scope",
                                      problems, warnOnBadDependencyScope, d.getScope(), d.getManagementKey(),
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
                    validateStringNotEmpty( "reporting.plugins.plugin.artifactId", problems, false, p.getArtifactId() );

                    validateStringNotEmpty( "reporting.plugins.plugin.groupId", problems, false, p.getGroupId() );

                    validateStringNotEmpty( "reporting.plugins.plugin.version", problems, warnOnMissingPluginVersion,
                                            p.getVersion(), p.getKey() );
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
                validateRepositoryLayout( problems, distMgmt.getRepository(), "distributionManagement.repository",
                                          request );
                validateRepositoryLayout( problems, distMgmt.getSnapshotRepository(),
                                          "distributionManagement.snapshotRepository", request );
            }
        }
    }

    private boolean validateId( String fieldName, ModelProblemCollector problems, String id )
    {
        if ( !validateStringNotEmpty( fieldName, problems, false, id ) )
        {
            return false;
        }
        else
        {
            boolean match = id.matches( ID_REGEX );
            if ( !match )
            {
                addViolation( problems, false, "'" + fieldName + "' with value '" + id + "' does not match a valid id pattern." );
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
                addViolation( problems, false, "'" + prefix + ".classifier' must be empty for imported POM: " + key );
            }

            Dependency existing = index.get( key );

            if ( existing != null )
            {
                boolean warning = request.getValidationLevel() < ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_3_0;

                addViolation( problems, warning, "'" + prefix + ".(groupId:artifactId:type:classifier)' must be unique: "
                    + key + " -> " + existing.getVersion() + " vs " + dependency.getVersion() );
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
            validateStringNotEmpty( prefix + ".id", problems, false, repository.getId() );

            validateStringNotEmpty( prefix + "[" + repository.getId() + "].url", problems, false, repository.getUrl() );

            String key = repository.getId();

            Repository existing = index.get( key );

            if ( existing != null )
            {
                boolean warning = request.getValidationLevel() < ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_3_0;

                addViolation( problems, warning, "'" + prefix + ".id' must be unique: " + repository.getId() + " -> "
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
            addViolation( problems, true, "'" + prefix + ".layout = legacy' is deprecated: " + repository.getId() );
        }
    }

    private void validateResources( ModelProblemCollector problems, List<Resource> resources, String prefix, ModelBuildingRequest request )
    {
        boolean warnOnBadBoolean = request.getValidationLevel() < ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_3_0;

        for ( Resource resource : resources )
        {
            validateStringNotEmpty( prefix + ".directory", problems, false, resource.getDirectory() );

            validateBoolean( prefix + ".filtering", problems, warnOnBadBoolean, resource.getFiltering(),
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
                        addViolation( problems, false, collisionException.getMessage() );
                    }
                }
            }
        }
    }

    // ----------------------------------------------------------------------
    // Field validation
    // ----------------------------------------------------------------------

    private boolean validateStringNoExpression( String fieldName, ModelProblemCollector problems, boolean warning,
                                                String string )
    {
        if ( !hasExpression( string ) )
        {
            return true;
        }

        addViolation( problems, warning, "'" + fieldName + "' contains an expression but should be a constant." );

        return false;
    }

    private boolean hasExpression( String value )
    {
        return value != null && value.indexOf( "${" ) >= 0;
    }

    private boolean validateStringNotEmpty( String fieldName, ModelProblemCollector problems, boolean warning, String string )
    {
        return validateStringNotEmpty( fieldName, problems, warning, string, null );
    }

    /**
     * Asserts:
     * <p/>
     * <ul>
     * <li><code>string != null</code>
     * <li><code>string.length > 0</code>
     * </ul>
     */
    private boolean validateStringNotEmpty( String fieldName, ModelProblemCollector problems, boolean warning,
                                            String string, String sourceHint )
    {
        if ( !validateNotNull( fieldName, problems, warning, string, sourceHint ) )
        {
            return false;
        }

        if ( string.length() > 0 )
        {
            return true;
        }

        if ( sourceHint != null )
        {
            addViolation( problems, warning, "'" + fieldName + "' is missing for " + sourceHint );
        }
        else
        {
            addViolation( problems, warning, "'" + fieldName + "' is missing." );
        }

        return false;
    }

    /**
     * Asserts:
     * <p/>
     * <ul>
     * <li><code>string != null</code>
     * <li><code>string.length > 0</code>
     * </ul>
     */
    private boolean validateSubElementStringNotEmpty( Object subElementInstance, String fieldName,
                                                      ModelProblemCollector problems, String string )
    {
        if ( !validateSubElementNotNull( subElementInstance, fieldName, problems, string ) )
        {
            return false;
        }

        if ( string.length() > 0 )
        {
            return true;
        }

        addViolation( problems, false, "In " + subElementInstance + ":\n\n       -> '" + fieldName + "' is missing." );

        return false;
    }

    /**
     * Asserts:
     * <p/>
     * <ul>
     * <li><code>string != null</code>
     * </ul>
     */
    private boolean validateNotNull( String fieldName, ModelProblemCollector problems, boolean warning, Object object, String sourceHint )
    {
        if ( object != null )
        {
            return true;
        }

        if ( sourceHint != null )
        {
            addViolation( problems, warning, "'" + fieldName + "' is missing for " + sourceHint );
        }
        else
        {
            addViolation( problems, warning, "'" + fieldName + "' is missing." );
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
    private boolean validateSubElementNotNull( Object subElementInstance, String fieldName,
                                               ModelProblemCollector problems, Object object )
    {
        if ( object != null )
        {
            return true;
        }

        addViolation( problems, false, "In " + subElementInstance + ":\n\n       -> '" + fieldName + "' is missing." );

        return false;
    }

    private boolean validateBoolean( String fieldName, ModelProblemCollector problems, boolean warning, String string,
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
            addViolation( problems, warning, "'" + fieldName + "' must be 'true' or 'false' for " + sourceHint );
        }
        else
        {
            addViolation( problems, warning, "'" + fieldName + "' must be 'true' or 'false'." );
        }

        return false;
    }

    private boolean validateEnum( String fieldName, ModelProblemCollector problems, boolean warning, String string,
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
            addViolation( problems, warning, "'" + fieldName + "' must be one of " + values + " for " + sourceHint );
        }
        else
        {
            addViolation( problems, warning, "'" + fieldName + "' must be one of " + values );
        }

        return false;
    }

    private void addViolation( ModelProblemCollector problems, boolean warning, String message )
    {
        if ( warning )
        {
            problems.addWarning( message );
        }
        else
        {
            problems.addError( message );
        }
    }

}
