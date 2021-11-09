package org.apache.maven.caching;

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

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.caching.xml.buildinfo.BuildInfoType;
import org.apache.maven.caching.xml.buildinfo.CompletedExecutionType;
import org.apache.maven.caching.xml.buildinfo.DigestItemType;
import org.apache.maven.caching.xml.buildinfo.ProjectsInputInfoType;
import org.apache.maven.caching.xml.buildinfo.PropertyValueType;
import org.apache.maven.caching.xml.buildsdiff.BuildDiffType;
import org.apache.maven.caching.xml.buildsdiff.MismatchType;
import org.apache.maven.caching.xml.CacheConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Utility class for comparing 2 builds
 */
public class CacheDiff
{

    private final CacheConfig config;
    private final BuildInfoType current;
    private final BuildInfoType baseline;
    private final LinkedList<MismatchType> report;

    public CacheDiff( BuildInfoType current, BuildInfoType baseline, CacheConfig config )
    {
        this.current = current;
        this.baseline = baseline;
        this.config = config;
        this.report = new LinkedList<>();
    }

    public BuildDiffType compare()
    {

        if ( !StringUtils.equals( current.getHashFunction(), baseline.getHashFunction() ) )
        {
            addNewMismatch(
                    "hashFunction",
                    current.getHashFunction(),
                    baseline.getHashFunction(),
                    "Different algorithms render caches not comparable and cached could not be reused",
                    "Ensure the same algorithm as remote"
            );
        }
        compareEffectivePoms( current.getProjectsInputInfo(), baseline.getProjectsInputInfo() );
        compareExecutions( current.getExecutions(), baseline.getExecutions() );
        compareFiles( current.getProjectsInputInfo(), baseline.getProjectsInputInfo() );
        compareDependencies( current.getProjectsInputInfo(), baseline.getProjectsInputInfo() );

        final BuildDiffType buildDiffType = new BuildDiffType();
        buildDiffType.getMismatches().addAll( report );
        return buildDiffType;
    }

    private void compareEffectivePoms( ProjectsInputInfoType current, ProjectsInputInfoType baseline )
    {
        Optional<DigestItemType> currentPom = findPom( current );
        String currentPomHash = currentPom.isPresent() ? currentPom.get().getHash() : null;

        Optional<DigestItemType> baseLinePom = findPom( baseline );
        String baselinePomHash = baseLinePom.isPresent() ? baseLinePom.get().getHash() : null;

        if ( !StringUtils.equals( currentPomHash, baselinePomHash ) )
        {
            addNewMismatch(
                    "effectivePom", currentPomHash, baselinePomHash,
                    "Difference in effective pom suggests effectively different builds which cannot be reused",
                    "Compare raw content of effective poms and eliminate differences. "
                            + "See How-To for common techniques"
            );
        }
    }

    public static Optional<DigestItemType> findPom( ProjectsInputInfoType projectInputs )
    {
        for ( DigestItemType digestItemType : projectInputs.getItems() )
        {
            if ( "pom".equals( digestItemType.getType() ) )
            {
                return Optional.of( digestItemType );

            }
        }
        return Optional.absent();
    }

    private void compareFiles( ProjectsInputInfoType current, ProjectsInputInfoType baseline )
    {

        final Map<String, DigestItemType> currentFiles = new HashMap<>();
        for ( DigestItemType item : current.getItems() )
        {
            if ( "file".equals( item.getType() ) )
            {
                currentFiles.put( item.getValue(), item );
            }
        }

        final Map<String, DigestItemType> baselineFiles = new HashMap<>();
        for ( DigestItemType item : baseline.getItems() )
        {
            if ( "file".equals( item.getType() ) )
            {
                baselineFiles.put( item.getValue(), item );
            }
        }

        final Sets.SetView<String> currentVsBaseline = Sets.difference( currentFiles.keySet(), baselineFiles.keySet() );
        final Sets.SetView<String> baselineVsCurrent = Sets.difference( baselineFiles.keySet(), currentFiles.keySet() );

        if ( !currentVsBaseline.isEmpty() || !baselineVsCurrent.isEmpty() )
        {
            addNewMismatch( "source files",
                    "Remote and local cache contain different sets of input files. "
                            + "Added files: " + currentVsBaseline + ". Removed files: " + baselineVsCurrent,
                    "To match remote and local caches should have identical file sets."
                            + " Unnecessary and transient files must be filtered out to make file sets match"
                            + " - see configuration guide"
            );
            return;
        }

        for ( Map.Entry<String, DigestItemType> entry : currentFiles.entrySet() )
        {
            String filePath = entry.getKey();
            DigestItemType currentFile = entry.getValue();
            // should be null safe because sets are compared above for differences
            final DigestItemType baselineFile = baselineFiles.get( filePath );
            if ( !StringUtils.equals( currentFile.getHash(), baselineFile.getHash() ) )
            {

                String reason = "File content is different.";
                if ( currentFile.getEol() != null && baselineFile.getEol() != null && !StringUtils.equals(
                        baselineFile.getEol(), currentFile.getEol() ) )
                {
                    reason += " Different line endings detected (text files relevant). "
                            + "Remote: " + baselineFile.getEol() + ", local: " + currentFile.getEol() + ".";
                }
                if ( currentFile.getCharset() != null && baselineFile.getCharset() != null && !StringUtils.equals(
                        baselineFile.getCharset(), currentFile.getCharset() ) )
                {
                    reason += " Different charset detected (text files relevant). "
                            + "Remote: " + baselineFile.getEol() + ", local: " + currentFile.getEol() + ".";
                }

                addNewMismatch( filePath, currentFile.getHash(), baselineFile.getHash(), reason,
                        "Different content manifests different build outcome. "
                                + "Ensure that difference is not caused by environment specifics, like line separators"
                );
            }
        }
    }

    private void compareDependencies( ProjectsInputInfoType current, ProjectsInputInfoType baseline )
    {
        final Map<String, DigestItemType> currentDependencies = new HashMap<>();
        for ( DigestItemType digestItemType : current.getItems() )
        {
            if ( "dependency".equals( digestItemType.getType() ) )
            {
                currentDependencies.put( digestItemType.getValue(), digestItemType );
            }
        }
        final Map<String, DigestItemType> baselineDependencies = new HashMap<>();
        for ( DigestItemType item : baseline.getItems() )
        {
            if ( "dependency".equals( item.getType() ) )
            {
                baselineDependencies.put( item.getValue(), item );
            }
        }

        final Sets.SetView<String> currentVsBaseline =
                Sets.difference( currentDependencies.keySet(), baselineDependencies.keySet() );
        final Sets.SetView<String> baselineVsCurrent =
                Sets.difference( baselineDependencies.keySet(), currentDependencies.keySet() );

        if ( !currentVsBaseline.isEmpty() || !baselineVsCurrent.isEmpty() )
        {
            addNewMismatch( "dependencies files",
                    "Remote and local builds contain different sets of dependencies and cannot be matched. "
                            + "Added dependencies: " + currentVsBaseline + ". Removed dependencies: "
                            + baselineVsCurrent,
                    "Remote and local builds should have identical dependencies. "
                            + "The difference manifests changes in downstream dependencies or introduced snapshots."
            );
            return;
        }

        for ( Map.Entry<String, DigestItemType> entry : currentDependencies.entrySet() )
        {
            String dependencyKey = entry.getKey();
            DigestItemType currentDependency = entry.getValue();
            // null safe - sets compared for differences above
            final DigestItemType baselineDependency = baselineDependencies.get( dependencyKey );
            if ( !StringUtils.equals( currentDependency.getHash(), baselineDependency.getHash() ) )
            {
                addNewMismatch( dependencyKey, currentDependency.getHash(), baselineDependency.getHash(),
                        "Downstream project or snapshot changed",
                        "Find downstream project and investigate difference in the downstream project. "
                                + "Enable fail fast mode and single threaded execution to simplify debug."
                );
            }
        }
    }


    private void compareExecutions( List<CompletedExecutionType> current, List<CompletedExecutionType> baseline )
    {
        Map<String, CompletedExecutionType> baselineExecutionsByKey = new HashMap<>();
        for ( CompletedExecutionType completedExecutionType : baseline )
        {
            baselineExecutionsByKey.put( completedExecutionType.getExecutionKey(), completedExecutionType );
        }

        Map<String, CompletedExecutionType> currentExecutionsByKey = new HashMap<>();
        for ( CompletedExecutionType e1 : current )
        {
            currentExecutionsByKey.put( e1.getExecutionKey(), e1 );
        }

        // such situation normally means different poms and mismatch in effective poms,
        // but in any case it is helpful to report
        for ( CompletedExecutionType baselineExecution : baseline )
        {
            if ( !currentExecutionsByKey.containsKey( baselineExecution.getExecutionKey() ) )
            {
                addNewMismatch(
                        baselineExecution.getExecutionKey(),
                        "Baseline build contains excessive plugin " + baselineExecution.getExecutionKey(),
                        "Different set of plugins produces different build results. "
                                + "Exclude non-critical plugins or make sure plugin sets match"
                );
            }
        }

        for ( CompletedExecutionType currentExecution : current )
        {
            if ( !baselineExecutionsByKey.containsKey( currentExecution.getExecutionKey() ) )
            {
                addNewMismatch(
                        currentExecution.getExecutionKey(),
                        "Cached build doesn't contain plugin " + currentExecution.getExecutionKey(),
                        "Different set of plugins produces different build results. "
                                + "Filter out non-critical plugins or make sure remote cache always run full build "
                                + "with all plugins"
                );
                continue;
            }

            final CompletedExecutionType baselineExecution =
                    baselineExecutionsByKey.get( currentExecution.getExecutionKey() );
            comparePlugins( currentExecution, baselineExecution );
        }
    }

    private void comparePlugins( CompletedExecutionType current, CompletedExecutionType baseline )
    {
        // TODO add support for skip values
        final List<PropertyValueType> trackedProperties = new ArrayList<>();
        for ( PropertyValueType propertyValueType : current.getProperties() )
        {
            if ( propertyValueType.isTracked() )
            {
                trackedProperties.add( propertyValueType );
            }
        }
        if ( trackedProperties.isEmpty() )
        {
            return;
        }

        final Map<String, PropertyValueType> baselinePropertiesByName = new HashMap<>();
        for ( PropertyValueType propertyValueType : baseline.getProperties() )
        {
            baselinePropertiesByName.put( propertyValueType.getName(), propertyValueType );
        }

        for ( PropertyValueType p : trackedProperties )
        {
            final PropertyValueType baselineValue = baselinePropertiesByName.get( p.getName() );
            if ( baselineValue == null || !StringUtils.equals( baselineValue.getValue(), p.getValue() ) )
            {
                addNewMismatch(
                        p.getName(),
                        p.getValue(),
                        baselineValue == null ? null : baselineValue.getValue(),
                        "Plugin: " + current.getExecutionKey()
                                + " has mismatch in tracked property and cannot be reused",
                        "Align properties between remote and local build or remove property from tracked "
                                + "list if mismatch could be tolerated. In some cases it is possible to add skip value "
                                + "to ignore lax mismatch"
                );
            }
        }
    }

    private void addNewMismatch( String item, String current, String baseline, String reason,
                                 String resolution )
    {
        final MismatchType mismatch = new MismatchType();
        mismatch.setItem( item );
        mismatch.setCurrent( current );
        mismatch.setBaseline( baseline );
        mismatch.setReason( reason );
        mismatch.setResolution( resolution );
        report.add( mismatch );
    }

    private void addNewMismatch( String property, String reason, String resolution )
    {
        final MismatchType mismatchType = new MismatchType();
        mismatchType.setItem( property );
        mismatchType.setReason( reason );
        mismatchType.setResolution( resolution );
        report.add( mismatchType );
    }
}
