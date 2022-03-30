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

import org.apache.maven.api.model.Build;
import org.apache.maven.api.model.BuildBase;
import org.apache.maven.api.model.CiManagement;
import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.model.DependencyManagement;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.ModelBase;
import org.apache.maven.api.model.Plugin;
import org.apache.maven.api.model.PluginContainer;
import org.apache.maven.api.model.Profile;
import org.apache.maven.api.model.ReportPlugin;
import org.apache.maven.api.model.Reporting;
import org.apache.maven.model.v4.ModelMerger;

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
    protected void mergeBuild_Extensions( Build.Builder builder,
                                          Build target, Build source, boolean sourceDominant,
                                          Map<Object, Object> context )
    {
        // don't merge
    }


    @Override
    protected void mergeBuildBase_Resources( BuildBase.Builder builder,
                                             BuildBase target, BuildBase source, boolean sourceDominant,
                                             Map<Object, Object> context )
    {
        // don't merge
    }

    @Override
    protected void mergeBuildBase_TestResources( BuildBase.Builder builder,
                                                 BuildBase target, BuildBase source, boolean sourceDominant,
                                                 Map<Object, Object> context )
    {
        // don't merge
    }

    @Override
    protected void mergeCiManagement_Notifiers( CiManagement.Builder builder,
                                                CiManagement target, CiManagement source, boolean sourceDominant,
                                                Map<Object, Object> context )
    {
        // don't merge
    }

    @Override
    protected void mergeDependencyManagement_Dependencies( DependencyManagement.Builder builder,
                                                           DependencyManagement target, DependencyManagement source,
                                                           boolean sourceDominant, Map<Object, Object> context )
    {
        Iterator<Dependency> sourceIterator = source.getDependencies().iterator();
        target.getDependencies().forEach( t -> mergeDependency( t, sourceIterator.next(), sourceDominant,
                                                                         context ) );
    }

    @Override
    protected void mergeDependency_Exclusions( Dependency.Builder builder,
                                               Dependency target, Dependency source, boolean sourceDominant,
                                               Map<Object, Object> context )
    {
        // don't merge
    }

    @Override
    protected void mergeModel_Contributors( Model.Builder builder,
                                            Model target, Model source, boolean sourceDominant,
                                            Map<Object, Object> context )
    {
        // don't merge
    }

    @Override
    protected void mergeModel_Developers( Model.Builder builder,
                                          Model target, Model source, boolean sourceDominant,
                                          Map<Object, Object> context )
    {
        // don't merge
    }

    @Override
    protected void mergeModel_Licenses( Model.Builder builder,
                                        Model target, Model source, boolean sourceDominant,
                                        Map<Object, Object> context )
    {
        // don't merge
    }

    @Override
    protected void mergeModel_MailingLists( Model.Builder builder,
                                            Model target, Model source, boolean sourceDominant,
                                            Map<Object, Object> context )
    {
        // don't merge
    }

    @Override
    protected void mergeModel_Profiles( Model.Builder builder,
                                        Model target, Model source, boolean sourceDominant,
                                        Map<Object, Object> context )
    {
        Iterator<Profile> sourceIterator = source.getProfiles().iterator();
        target.getProfiles().forEach( t -> mergeProfile( t, sourceIterator.next(), sourceDominant,
                                                                  context ) );
    }

    @Override
    protected void mergeModelBase_Dependencies( ModelBase.Builder builder,
                                                ModelBase target, ModelBase source, boolean sourceDominant,
                                                Map<Object, Object> context )
    {
        Iterator<Dependency> sourceIterator = source.getDependencies().iterator();
        target.getDependencies().forEach( t -> mergeDependency( t, sourceIterator.next(), sourceDominant,
                                                                         context ) );
    }

    @Override
    protected void mergeModelBase_PluginRepositories( ModelBase.Builder builder,
                                                      ModelBase target, ModelBase source, boolean sourceDominant,
                                                      Map<Object, Object> context )
    {
        builder.pluginRepositories( source.getPluginRepositories() );
    }

    @Override
    protected void mergeModelBase_Repositories( ModelBase.Builder builder,
                                                ModelBase target, ModelBase source, boolean sourceDominant,
                                                Map<Object, Object> context )
    {
        // don't merge
    }

    @Override
    protected void mergePlugin_Dependencies( Plugin.Builder builder,
                                             Plugin target, Plugin source, boolean sourceDominant,
                                             Map<Object, Object> context )
    {
        Iterator<Dependency> sourceIterator = source.getDependencies().iterator();
        target.getDependencies().forEach( t -> mergeDependency( t, sourceIterator.next(), sourceDominant,
                                                                         context ) );
    }

    @Override
    protected void mergePlugin_Executions( Plugin.Builder builder,
                                           Plugin target, Plugin source, boolean sourceDominant,
                                           Map<Object, Object> context )
    {
        // don't merge
    }

    @Override
    protected void mergeReporting_Plugins( Reporting.Builder builder,
                                           Reporting target, Reporting source, boolean sourceDominant,
                                           Map<Object, Object> context )
    {
        // don't merge
    }

    @Override
    protected void mergeReportPlugin_ReportSets( ReportPlugin.Builder builder,
                                                 ReportPlugin target, ReportPlugin source, boolean sourceDominant,
                                                 Map<Object, Object> context )
    {
        // don't merge
    }

    @Override
    protected void mergePluginContainer_Plugins( PluginContainer.Builder builder,
                                                 PluginContainer target, PluginContainer source,
                                                 boolean sourceDominant, Map<Object, Object> context )
    {
        // don't merge
    }
}
