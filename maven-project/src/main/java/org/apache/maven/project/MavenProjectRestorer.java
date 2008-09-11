package org.apache.maven.project;

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

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Resource;
import org.apache.maven.project.interpolation.ModelInterpolationException;
import org.apache.maven.project.interpolation.ModelInterpolator;
import org.apache.maven.project.path.PathTranslator;
import org.codehaus.plexus.logging.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

final class MavenProjectRestorer
{

    private ModelInterpolator modelInterpolator;

    private PathTranslator pathTranslator;

    private Logger logger;

    MavenProjectRestorer( PathTranslator pathTranslator, ModelInterpolator modelInterpolator, Logger logger )
    {
        this.pathTranslator = pathTranslator;
        this.modelInterpolator = modelInterpolator;
        this.logger = logger;
    }

    Logger getLogger()
    {
        return logger;
    }

    void restoreDynamicState( MavenProject project, ProjectBuilderConfiguration config )
        throws ModelInterpolationException
    {
        if ( !project.isConcrete() )
        {
            return;
        }
        /*
        restoreBuildRoots( project, config );
        if ( project.getBuild() != null )
        {
            restoreModelBuildSection( project, config );
        }
        restoreDynamicProjectReferences( project, config );

        MavenProject executionProject = project.getExecutionProject();
        if ( executionProject != null && executionProject != project )
        {
            restoreDynamicState( executionProject, config );
        }
        */
        project.setConcrete( false );
    }

    void calculateConcreteState( MavenProject project, ProjectBuilderConfiguration config )
        throws ModelInterpolationException
    {
        if ( project.isConcrete() )
        {
            return;
        }

        Model model = ModelUtils.cloneModel( project.getModel() );

        File basedir = project.getBasedir();

        Model model2 = ModelUtils.cloneModel( model );
        pathTranslator.alignToBaseDirectory( model, basedir );
        project.preserveBuild( model2.getBuild() );

        project.setBuild( model.getBuild() );

       //
        /*
        MavenProject executionProject = project.getExecutionProject();
        if ( executionProject != null && executionProject != project )
        {
            calculateConcreteState( executionProject, config );
        }
        */
        project.setConcrete( true );
    }
}
