package org.apache.maven.model.versioning;

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

import java.util.Objects;

import org.apache.maven.model.Model;

/**
 * Gathers model version information.
 *
 * @author Christian Schulte
 * @since 3.4
 */
public final class ModelVersions
{

    /**
     * Creates a new {@code ModelVersions} instance.
     */
    private ModelVersions()
    {
        super();
    }

    /**
     * Constant for model version {@code 4.0.0}.
     */
    public static final String V4_0_0 = "4.0.0";

    /**
     * Constant for model version {@code 4.1.0}.
     */
    public static final String V4_1_0 = "4.1.0";

    /**
     * Tests whether dependency management import version ranges are supported for a given {@code Model}.
     *
     * @param model The {@code Model} to test.
     *
     * @return {@code true}, if dependency management import version ranges are supported for {@code model};
     * {@code false}, if dependency management import version ranges are not supported for {@code model}.
     */
    public static boolean supportsDependencyManagementImportVersionRanges( final Model model )
    {
        // [MNG-4463] Version ranges cannot be used for artifacts with 'import' scope
        return isGreaterOrEqual( model, V4_1_0 );
    }

    /**
     * Tests whether dependency management import exclusions are supported for a given {@code Model}.
     *
     * @param model The {@code Model} to test.
     *
     * @return {@code true}, if dependency management import exclusions are supported for {@code model};
     * {@code false}, if dependency management import exclusions are not supported for {@code model}.
     */
    public static boolean supportsDependencyManagementImportExclusions( final Model model )
    {
        // [MNG-5600] Dependency management import should support exclusions.
        return isGreaterOrEqual( model, V4_1_0 );
    }

    /**
     * Tests whether dependency management import relocations are supported for a given {@code Model}.
     *
     * @param model The {@code Model} to test.
     *
     * @return {@code true}, if dependency management import relocations are supported for {@code model};
     * {@code false}, if dependency management import relocations are not supported for {@code model}.
     */
    public static boolean supportsDependencyManagementImportRelocations( final Model model )
    {
        // [MNG-5527] Dependency management import should support relocations.
        return isGreaterOrEqual( model, V4_1_0 );
    }

    /**
     * Tests whether dependency management import inheritance processing is supported for a given {@code Model}.
     *
     * @param model The {@code Model} to test.
     *
     * @return {@code true}, if dependency management import inheritance processing is supported for {@code model};
     * {@code false}, if dependency management import inheritance processing is not supported for {@code model}.
     */
    public static boolean supportsDependencyManagementImportInheritanceProcessing( final Model model )
    {
        // [MNG-5971] Imported dependencies should be available to inheritance processing
        return isGreaterOrEqual( model, V4_1_0 );
    }

    private static boolean isGreaterOrEqual( final Model model, final String version )
    {
        Objects.requireNonNull( model, "model" );
        Objects.requireNonNull( version, "version" );

        if ( null != model.getModelVersion() )
        {
            switch ( model.getModelVersion() )
            {
                case V4_0_0:
                    return V4_0_0.equals( version );
                case V4_1_0:
                    return V4_0_0.equals( version ) || V4_1_0.equals( version );
                default:
                    throw new AssertionError( String.format( "Unsupported model version '%s'.", version ) );
            }
        }

        // [MNG-666] need to be able to operate on a Maven 1 repository
        //   Handles null as the lowest version possible.
        return false;
    }

}
