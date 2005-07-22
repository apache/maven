package org.apache.maven.project.validation;

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

import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.Reporting;

import java.util.Iterator;
import java.util.List;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 */
public class DefaultModelValidator
    implements ModelValidator
{
    ///////////////////////////////////////////////////////////////////////////
    // ModelValidator Implementation

    public ModelValidationResult validate( Model model )
    {
        ModelValidationResult result = new ModelValidationResult();

        validateStringNotEmpty( "modelVersion", result, model.getModelVersion() );

        validateStringNotEmpty( "groupId", result, model.getGroupId() );

        validateStringNotEmpty( "artifactId", result, model.getArtifactId() );

        validateStringNotEmpty( "packaging", result, model.getPackaging() );

        validateStringNotEmpty( "version", result, model.getVersion() );

        for ( Iterator it = model.getDependencies().iterator(); it.hasNext(); )
        {
            Dependency d = (Dependency) it.next();

            validateStringNotEmpty( "dependencies.dependency.artifactId", result, d.getArtifactId() );

            validateStringNotEmpty( "dependencies.dependency.groupId", result, d.getGroupId() );

            validateStringNotEmpty( "dependencies.dependency.type", result, d.getType() );

            validateStringNotEmpty( "dependencies.dependency.version", result, d.getVersion() );
        }

        DependencyManagement mgmt = model.getDependencyManagement();
        if ( mgmt != null )
        {
            for ( Iterator it = mgmt.getDependencies().iterator(); it.hasNext(); )
            {
                Dependency d = (Dependency) it.next();

                validateStringNotEmpty( "dependencyManagement.dependencies.dependency.artifactId", result,
                                        d.getArtifactId() );

                validateStringNotEmpty( "dependencyManagement.dependencies.dependency.groupId", result,
                                        d.getGroupId() );

                validateStringNotEmpty( "dependencyManagement.dependencies.dependency.version", result,
                                        d.getVersion() );
            }
        }

        Build build = model.getBuild();
        if ( build != null )
        {
            for ( Iterator it = build.getPlugins().iterator(); it.hasNext(); )
            {
                Plugin p = (Plugin) it.next();

                validateStringNotEmpty( "build.plugins.plugin.artifactId", result, p.getArtifactId() );

                validateStringNotEmpty( "build.plugins.plugin.groupId", result, p.getGroupId() );
            }
        }

        Reporting reporting = model.getReporting();
        if ( reporting != null )
        {
            for ( Iterator it = reporting.getPlugins().iterator(); it.hasNext(); )
            {
                ReportPlugin p = (ReportPlugin) it.next();

                validateStringNotEmpty( "reporting.plugins.plugin.artifactId", result, p.getArtifactId() );

                validateStringNotEmpty( "reporting.plugins.plugin.groupId", result, p.getGroupId() );
            }
        }

        forcePluginExecutionIdCollision( model, result );

        return result;
    }

    private void forcePluginExecutionIdCollision( Model model, ModelValidationResult result )
    {
        Build build = model.getBuild();

        if ( build != null )
        {
            List plugins = build.getPlugins();

            if ( plugins != null )
            {
                for ( Iterator it = plugins.iterator(); it.hasNext(); )
                {
                    Plugin plugin = (Plugin) it.next();

                    // this will force an IllegalStateException, even if we don't have to do inheritance assembly.
                    try
                    {
                        plugin.getExecutionsAsMap();
                    }
                    catch ( IllegalStateException collisionException )
                    {
                        result.addMessage( collisionException.getMessage() );
                    }
                }
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Field validator

    /**
     * Asserts:
     * <p/>
     * <ul>
     * <li><code>string.length != null</code>
     * <li><code>string.length > 0</code>
     * </ul>
     */
    private boolean validateStringNotEmpty( String fieldName, ModelValidationResult result, String string )
    {
        if ( !validateNotNull( fieldName, result, string ) )
        {
            return false;
        }

        if ( string.length() > 0 )
        {
            return true;
        }

        result.addMessage( "'" + fieldName + "' is empty." );

        return false;
    }

    /**
     * Asserts:
     * <p/>
     * <ul>
     * <li><code>string != null</code>
     * </ul>
     */
    private boolean validateNotNull( String fieldName, ModelValidationResult result, Object object )
    {
        if ( object != null )
        {
            return true;
        }

        result.addMessage( "'" + fieldName + "' is missing." );

        return false;
    }
}
