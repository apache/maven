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
package org.apache.maven.model.merge;

import java.util.Arrays;

import org.apache.maven.api.model.Contributor;
import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.model.Model;
import org.apache.maven.model.v4.MavenMerger;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

/**
 * MavenMerger is based on same instances, subclasses should override KeyComputer per type
 *
 *
 */
class MavenMergerTest {
    private MavenMerger mavenMerger = new MavenMerger();

    @Test
    void mergeArtifactId() {
        Model target = Model.newBuilder().artifactId("TARGET").build();

        Model source = Model.newBuilder().artifactId("SOURCE").build();

        Model merged = mavenMerger.merge(target, source, true, null);
        assertThat(merged.getArtifactId(), is("SOURCE"));

        merged = mavenMerger.merge(target, source, false, null);
        assertThat(merged.getArtifactId(), is("TARGET"));
    }

    @Test
    void mergeSameContributors() {
        Contributor contributor =
                Contributor.newBuilder().email("contributor@maven.apache.org").build();

        Model target =
                Model.newBuilder().contributors(Arrays.asList(contributor)).build();

        Model source =
                Model.newBuilder().contributors(Arrays.asList(contributor)).build();

        Model merged = mavenMerger.merge(target, source, true, null);

        assertThat(merged.getContributors(), contains(contributor));
    }

    @Test
    void mergeSameDependencies() {
        Dependency dependency = Dependency.newBuilder()
                .groupId("groupId")
                .artifactId("artifactId")
                .type("type")
                .build();

        Model target =
                Model.newBuilder().dependencies(Arrays.asList(dependency)).build();

        Model source =
                Model.newBuilder().dependencies(Arrays.asList(dependency)).build();

        Model merged = mavenMerger.merge(target, source, true, null);

        assertThat(merged.getDependencies(), contains(dependency));
    }

    /*
    @Test
    public void mergeDescription()
    {
        Model target = new Model();
        target.setDescription( "TARGET" );

        Model source = new Model();
        source.setDescription( "SOURCE" );

        modelMerger.merge( target, source, true, null );
        assertThat( target.getDescription(), is( "SOURCE" ) );

        target.setDescription( "TARGET" );
        modelMerger.merge( target, source, false, null );
        assertThat( target.getDescription(), is( "TARGET" ) );
    }

    @Test
    public void mergeSameDevelopers()
    {
        Developer developer = new Developer();
        developer.setId( "devid" );

        Model target = new Model();
        target.setDevelopers( Arrays.asList( developer ) );

        Model source = new Model();
        source.setDevelopers( Arrays.asList( developer ) );

        modelMerger.merge( target, source, true, null );

        assertThat( target.getDevelopers(), contains( developer ) );
    }

    @Test
    public void mergeSameExcludes()
    {
        PatternSet target = new PatternSet();
        target.setExcludes( Arrays.asList( "first", "second", "third" ) );
        PatternSet source = new PatternSet();
        source.setExcludes( Arrays.asList( "first", "second", "third" ) );

        modelMerger.mergePatternSet_Excludes( target, source, true, null );

        assertThat( target.getExcludes(), contains( "first", "second", "third" ) );
    }

    @Test
    public void mergeSameFilters()
    {
        Build target = new Build();
        target.setFilters( Arrays.asList( "first", "second", "third" ) );
        Build source = new Build();
        source.setFilters( Arrays.asList( "first", "second", "third" ) );

        modelMerger.mergeBuild( target, source, true, null );

        assertThat( target.getFilters(), contains( "first", "second", "third" ) );
    }

    @Test
    public void mergeSameGoals()
    {
        PluginExecution target = new PluginExecution();
        target.setGoals( Arrays.asList( "first", "second", "third" ) );
        PluginExecution source = new PluginExecution();
        source.setGoals( Arrays.asList( "first", "second", "third" ) );

        modelMerger.mergePluginExecution( target, source, true, null );

        assertThat( target.getGoals(), contains( "first", "second", "third" ) );
    }

    @Test
    public void mergeGroupId()
    {
        Model target = new Model();
        target.setGroupId( "TARGET" );

        Model source = new Model();
        source.setGroupId( "SOURCE" );

        modelMerger.merge( target, source, true, null );
        assertThat( target.getGroupId(), is( "SOURCE" ) );

        target.setGroupId( "TARGET" );
        modelMerger.merge( target, source, false, null );
        assertThat( target.getGroupId(), is( "TARGET" ) );
    }

    @Test
    public void mergeInceptionYear()
    {
        Model target = new Model();
        target.setInceptionYear( "TARGET" );

        Model source = new Model();
        source.setInceptionYear( "SOURCE" );

        modelMerger.merge( target, source, true, null );
        assertThat( target.getInceptionYear(), is( "SOURCE" ) );

        target.setInceptionYear( "TARGET" );
        modelMerger.merge( target, source, false, null );
        assertThat( target.getInceptionYear(), is( "TARGET" ) );
    }

    @Test
    public void mergeSameIncludes()
    {
        PatternSet target = new PatternSet();
        target.setIncludes( Arrays.asList( "first", "second", "third" ) );
        PatternSet source = new PatternSet();
        source.setIncludes( Arrays.asList( "first", "second", "third" ) );

        modelMerger.mergePatternSet_Includes( target, source, true, null );

        assertThat( target.getIncludes(), contains( "first", "second", "third" ) );
    }

    @Test
    public void mergeSameMailingLists()
    {
        MailingList mailingList = new MailingList();
        mailingList.setName( "name" );

        Model target = new Model();
        target.setMailingLists( Arrays.asList( mailingList ) );

        Model source = new Model();
        source.setMailingLists( Arrays.asList( mailingList ) );

        modelMerger.merge( target, source, true, null );

        assertThat( target.getMailingLists(), contains( mailingList ) );
    }

    @Test
    public void mergeModelVersion()
    {
        Model target = new Model();
        target.setModelVersion( "TARGET" );

        Model source = new Model();
        source.setModelVersion( "SOURCE" );

        modelMerger.merge( target, source, true, null );
        assertThat( target.getModelVersion(), is( "SOURCE" ) );

        target.setModelVersion( "TARGET" );;
        modelMerger.merge( target, source, false, null );
        assertThat( target.getModelVersion(), is( "TARGET" ) );
    }

    @Test
    public void mergeSameModules()
    {
        Model target = new Model();
        target.setModules( Arrays.asList( "first", "second", "third" ) );
        Model source = new Model();
        source.setModules( Arrays.asList( "first", "second", "third" ) );

        modelMerger.merge( target, source, true, null );

        assertThat( target.getModules(), contains( "first", "second", "third" ) );
    }

    @Test
    public void mergeName()
    {
        Model target = new Model();
        target.setName( "TARGET" );

        Model source = new Model();
        source.setName( "SOURCE" );

        modelMerger.merge( target, source, true, null );
        assertThat( target.getName(), is( "SOURCE" ) );

        target.setName( "TARGET" );;
        modelMerger.merge( target, source, false, null );
        assertThat( target.getName(), is( "TARGET" ) );
    }

    @Test
    public void mergeSameOtherArchives()
    {
        MailingList target = new MailingList();
        target.setOtherArchives( Arrays.asList( "first", "second", "third" ) );
        MailingList source = new MailingList();
        source.setOtherArchives( Arrays.asList( "first", "second", "third" ) );

        modelMerger.mergeMailingList( target, source, true, null );

        assertThat( target.getOtherArchives(), contains( "first", "second", "third" ) );
    }

    @Test
    public void mergePackaging()
    {
        Model target = new Model();
        target.setPackaging( "TARGET" );

        Model source = new Model();
        source.setPackaging( "SOURCE" );

        modelMerger.merge( target, source, true, null );
        assertThat( target.getPackaging(), is( "SOURCE" ) );

        target.setPackaging( "TARGET" );;
        modelMerger.merge( target, source, false, null );
        assertThat( target.getPackaging(), is( "TARGET" ) );
    }

    @Test
    public void mergeSamePluginRepositories()
    {
        Repository repository = new Repository();
        repository.setId( "repository" );

        Model target = new Model();
        target.setPluginRepositories( Arrays.asList( repository ) );

        Model source = new Model();
        source.setPluginRepositories( Arrays.asList( repository ) );

        modelMerger.merge( target, source, true, null );

        assertThat( target.getPluginRepositories(), contains( repository ) );
    }

    @Test
    public void mergeSameProfiles()
    {
        Profile profile = new Profile();
        profile.setId( "profile" );

        Model target = new Model();
        target.setProfiles( Arrays.asList( profile ) );

        Model source = new Model();
        source.setProfiles( Arrays.asList( profile ) );

        modelMerger.merge( target, source, true, null );

        assertThat( target.getProfiles(), contains( profile ) );
    }

    @Test
    public void mergeSameReports()
    {
        ReportSet target = new ReportSet();
        target.setReports( Arrays.asList( "first", "second", "third" ) );
        ReportSet source = new ReportSet();
        source.setReports( Arrays.asList( "first", "second", "third" ) );

        modelMerger.mergeReportSet( target, source, true, null );

        assertThat( target.getReports(), contains( "first", "second", "third" ) );
    }

    @Test
    public void mergeSameRepositories()
    {
        Repository repository = new Repository();
        repository.setId( "repository" );

        Model target = new Model();
        target.setRepositories( Arrays.asList( repository ) );

        Model source = new Model();
        source.setRepositories( Arrays.asList( repository ) );

        modelMerger.merge( target, source, true, null );

        assertThat( target.getRepositories(), contains( repository ) );
    }

    @Test
    public void mergeSameRoles()
    {
        Contributor target = new Contributor();
        target.setRoles( Arrays.asList( "first", "second", "third" ) );
        Contributor source = new Contributor();
        source.setRoles( Arrays.asList( "first", "second", "third" ) );

        modelMerger.mergeContributor_Roles( target, source, true, null );

        assertThat( target.getRoles(), contains( "first", "second", "third" ) );
    }

    @Test
    public void mergeUrl()
    {
        Model target = new Model();
        target.setUrl( "TARGET" );;

        Model source = new Model();
        source.setUrl( "SOURCE" );

        modelMerger.merge( target, source, true, null );
        assertThat( target.getUrl(), is( "SOURCE" ) );

        target.setUrl( "TARGET" );;
        modelMerger.merge( target, source, false, null );
        assertThat( target.getUrl(), is( "TARGET" ) );
    }

    @Test
    public void mergeVersion()
    {
        Model target = new Model();
        target.setVersion( "TARGET" );;

        Model source = new Model();
        source.setVersion( "SOURCE" );

        modelMerger.merge( target, source, true, null );
        assertThat( target.getVersion(), is( "SOURCE" ) );

        target.setVersion( "TARGET" );;
        modelMerger.merge( target, source, false, null );
        assertThat( target.getVersion(), is( "TARGET" ) );
    }

    @Test
    public void testMergedModelSerialization() throws Exception {
        Model target = new Model();
        Model source = new Model();
        target.setLicenses(new ArrayList<License>());
        License lic1 = new License();
        License lic2 = new License();
        target.getLicenses().add(lic1);
        source.setLicenses(new ArrayList<License>());
        source.getLicenses().add(lic2);

        new MavenMerger().mergeModel(target, source, false, null);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(target);
    }
     */
}
