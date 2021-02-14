package org.apache.maven.model.building;

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

import java.util.Iterator;
import java.util.Map;

import org.apache.maven.model.Build;
import org.apache.maven.model.BuildBase;
import org.apache.maven.model.CiManagement;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.ModelBase;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginContainer;
import org.apache.maven.model.Profile;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.Reporting;
import org.apache.maven.model.merge.ModelMerger;

/**
 * As long as Maven controls the BuildPomXMLFilter, the entities that need merging are known.
 * All others can simply be copied from source to target to restore the locationTracker
 *
 * @author Robert Scholte
 * @since 4.0.0
 */
class FileToRawModelMerger extends ModelMerger
{

    @Override
    protected void mergeBuild_Extensions( Build target, Build source, boolean sourceDominant,
                                          Map<Object, Object> context )
    {
        // don't merge
    }


    @Override
    protected void mergeBuildBase_Resources( BuildBase target, BuildBase source, boolean sourceDominant,
                                             Map<Object, Object> context )
    {
        // don't merge
    }

    @Override
    protected void mergeBuildBase_TestResources( BuildBase target, BuildBase source, boolean sourceDominant,
                                                 Map<Object, Object> context )
    {
        // don't merge
    }

    @Override
    protected void mergeCiManagement_Notifiers( CiManagement target, CiManagement source, boolean sourceDominant,
                                                Map<Object, Object> context )
    {
        // don't merge
    }

    @Override
    protected void mergeDependencyManagement_Dependencies( DependencyManagement target, DependencyManagement source,
                                                           boolean sourceDominant, Map<Object, Object> context )
    {
        Iterator<Dependency> sourceIterator = source.getDependencies().iterator();
        target.getDependencies().stream().forEach( t -> mergeDependency( t, sourceIterator.next(), sourceDominant,
                                                                         context ) );
    }

    @Override
    protected void mergeDependency_Exclusions( Dependency target, Dependency source, boolean sourceDominant,
                                               Map<Object, Object> context )
    {
        // don't merge
    }

    @Override
    protected void mergeModel_Contributors( Model target, Model source, boolean sourceDominant,
                                            Map<Object, Object> context )
    {
        // don't merge
    }

    @Override
    protected void mergeModel_Developers( Model target, Model source, boolean sourceDominant,
                                          Map<Object, Object> context )
    {
        // don't merge
    }

    @Override
    protected void mergeModel_Licenses( Model target, Model source, boolean sourceDominant,
                                        Map<Object, Object> context )
    {
        // don't merge
    }

    @Override
    protected void mergeModel_MailingLists( Model target, Model source, boolean sourceDominant,
                                            Map<Object, Object> context )
    {
        // don't merge
    }

    @Override
    protected void mergeModel_Profiles( Model target, Model source, boolean sourceDominant,
                                        Map<Object, Object> context )
    {
        Iterator<Profile> sourceIterator = source.getProfiles().iterator();
        target.getProfiles().stream().forEach( t -> mergeProfile( t, sourceIterator.next(), sourceDominant,
                                                                  context ) );
    }

    @Override
    protected void mergeModelBase_Dependencies( ModelBase target, ModelBase source, boolean sourceDominant,
                                                Map<Object, Object> context )
    {
        Iterator<Dependency> sourceIterator = source.getDependencies().iterator();
        target.getDependencies().stream().forEach( t -> mergeDependency( t, sourceIterator.next(), sourceDominant,
                                                                         context ) );
    }

    @Override
    protected void mergeModelBase_PluginRepositories( ModelBase target, ModelBase source, boolean sourceDominant,
                                                      Map<Object, Object> context )
    {
        target.setPluginRepositories( source.getPluginRepositories() );
    }

    @Override
    protected void mergeModelBase_Repositories( ModelBase target, ModelBase source, boolean sourceDominant,
                                                Map<Object, Object> context )
    {
        // don't merge
    }

    @Override
    protected void mergePlugin_Dependencies( Plugin target, Plugin source, boolean sourceDominant,
                                             Map<Object, Object> context )
    {
        Iterator<Dependency> sourceIterator = source.getDependencies().iterator();
        target.getDependencies().stream().forEach( t -> mergeDependency( t, sourceIterator.next(), sourceDominant,
                                                                         context ) );
    }

    @Override
    protected void mergePlugin_Executions( Plugin target, Plugin source, boolean sourceDominant,
                                           Map<Object, Object> context )
    {
        // don't merge
    }

    @Override
    protected void mergeReporting_Plugins( Reporting target, Reporting source, boolean sourceDominant,
                                           Map<Object, Object> context )
    {
        // don't merge
    }

    @Override
    protected void mergeReportPlugin_ReportSets( ReportPlugin target, ReportPlugin source, boolean sourceDominant,
                                                 Map<Object, Object> context )
    {
        // don't merge
    }

    @Override
    protected void mergePluginContainer_Plugins( PluginContainer target, PluginContainer source,
                                                 boolean sourceDominant, Map<Object, Object> context )
    {
        // don't merge
    }
}
