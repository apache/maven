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

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.caching.xml.build.Build;
import org.apache.maven.caching.xml.build.CompletedExecution;
import org.apache.maven.caching.xml.build.DigestItem;
import org.apache.maven.caching.xml.build.ProjectsInputInfo;
import org.apache.maven.caching.xml.build.PropertyValue;
import org.apache.maven.caching.xml.diff.Diff;
import org.apache.maven.caching.xml.diff.Mismatch;
import org.apache.maven.caching.xml.CacheConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility class for comparing 2 builds
 */
public class CacheDiff
{

    private final CacheConfig config;
    private final Build current;
    private final Build baseline;
    private final LinkedList<Mismatch> report;

    public CacheDiff( Build current, Build baseline, CacheConfig config )
    {
        this.current = current;
        this.baseline = baseline;
        this.config = config;
        this.report = new LinkedList<>();
    }

    public Diff compare()
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

        final Diff buildDiffType = new Diff();
        buildDiffType.getMismatches().addAll( report );
        return buildDiffType;
    }

    private void compareEffectivePoms( ProjectsInputInfo current, ProjectsInputInfo baseline )
    {
        Optional<DigestItem> currentPom = findPom( current );
        String currentPomHash = currentPom.map( DigestItem::getHash ).orElse( null );

        Optional<DigestItem> baseLinePom = findPom( baseline );
        String baselinePomHash = baseLinePom.map( DigestItem::getHash ).orElse( null );

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

    public static Optional<DigestItem> findPom( ProjectsInputInfo projectInputs )
    {
        for ( DigestItem digestItemType : projectInputs.getItems() )
        {
            if ( "pom".equals( digestItemType.getType() ) )
            {
                return Optional.of( digestItemType );

            }
        }
        return Optional.empty();
    }

    private void compareFiles( ProjectsInputInfo current, ProjectsInputInfo baseline )
    {
        final Map<String, DigestItem> currentFiles = current.getItems().stream()
                .filter( item -> "file".equals( item.getType() ) )
                .collect( Collectors.toMap( DigestItem::getValue, item -> item ) );

        final Map<String, DigestItem> baselineFiles = baseline.getItems().stream()
                .filter( item -> "file".equals( item.getType() ) )
                .collect( Collectors.toMap( DigestItem::getValue, item -> item ) );

        if ( !Objects.equals( currentFiles.keySet(), baselineFiles.keySet() ) )
        {
            Set<String> currentVsBaseline = diff( currentFiles.keySet(), baselineFiles.keySet() );
            Set<String> baselineVsCurrent = diff( baselineFiles.keySet(), currentFiles.keySet() );

            addNewMismatch( "source files",
                    "Remote and local cache contain different sets of input files. "
                            + "Added files: " + currentVsBaseline + ". Removed files: " + baselineVsCurrent,
                    "To match remote and local caches should have identical file sets."
                            + " Unnecessary and transient files must be filtered out to make file sets match"
                            + " - see configuration guide"
            );
            return;
        }

        for ( Map.Entry<String, DigestItem> entry : currentFiles.entrySet() )
        {
            String filePath = entry.getKey();
            DigestItem currentFile = entry.getValue();
            // should be null safe because sets are compared above for differences
            final DigestItem baselineFile = baselineFiles.get( filePath );
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

    private void compareDependencies( ProjectsInputInfo current, ProjectsInputInfo baseline )
    {
        final Map<String, DigestItem> currentDependencies = current.getItems().stream()
                .filter( item -> "dependency".equals( item.getType() ) )
                .collect( Collectors.toMap( DigestItem::getValue, item -> item ) );

        final Map<String, DigestItem> baselineDependencies = baseline.getItems().stream()
                .filter( item -> "dependency".equals( item.getType() ) )
                .collect( Collectors.toMap( DigestItem::getValue, item -> item ) );

        if ( !Objects.equals( currentDependencies.keySet(), baselineDependencies.keySet() ) )
        {
            Set<String> currentVsBaseline = diff( currentDependencies.keySet(), baselineDependencies.keySet() );
            Set<String> baselineVsCurrent = diff( baselineDependencies.keySet(), currentDependencies.keySet() );

            addNewMismatch( "dependencies files",
                    "Remote and local builds contain different sets of dependencies and cannot be matched. "
                            + "Added dependencies: " + currentVsBaseline + ". Removed dependencies: "
                            + baselineVsCurrent,
                    "Remote and local builds should have identical dependencies. "
                            + "The difference manifests changes in downstream dependencies or introduced snapshots."
            );
            return;
        }

        for ( Map.Entry<String, DigestItem> entry : currentDependencies.entrySet() )
        {
            String dependencyKey = entry.getKey();
            DigestItem currentDependency = entry.getValue();
            // null safe - sets compared for differences above
            final DigestItem baselineDependency = baselineDependencies.get( dependencyKey );
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


    private void compareExecutions( List<CompletedExecution> current, List<CompletedExecution> baseline )
    {
        Map<String, CompletedExecution> baselineExecutionsByKey = new HashMap<>();
        for ( CompletedExecution completedExecutionType : baseline )
        {
            baselineExecutionsByKey.put( completedExecutionType.getExecutionKey(), completedExecutionType );
        }

        Map<String, CompletedExecution> currentExecutionsByKey = new HashMap<>();
        for ( CompletedExecution e1 : current )
        {
            currentExecutionsByKey.put( e1.getExecutionKey(), e1 );
        }

        // such situation normally means different poms and mismatch in effective poms,
        // but in any case it is helpful to report
        for ( CompletedExecution baselineExecution : baseline )
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

        for ( CompletedExecution currentExecution : current )
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

            final CompletedExecution baselineExecution =
                    baselineExecutionsByKey.get( currentExecution.getExecutionKey() );
            comparePlugins( currentExecution, baselineExecution );
        }
    }

    private void comparePlugins( CompletedExecution current, CompletedExecution baseline )
    {
        // TODO add support for skip values
        final List<PropertyValue> trackedProperties = new ArrayList<>();
        for ( PropertyValue propertyValueType : current.getProperties() )
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

        final Map<String, PropertyValue> baselinePropertiesByName = new HashMap<>();
        for ( PropertyValue propertyValueType : baseline.getProperties() )
        {
            baselinePropertiesByName.put( propertyValueType.getName(), propertyValueType );
        }

        for ( PropertyValue p : trackedProperties )
        {
            final PropertyValue baselineValue = baselinePropertiesByName.get( p.getName() );
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
        final Mismatch mismatch = new Mismatch();
        mismatch.setItem( item );
        mismatch.setCurrent( current );
        mismatch.setBaseline( baseline );
        mismatch.setReason( reason );
        mismatch.setResolution( resolution );
        report.add( mismatch );
    }

    private void addNewMismatch( String property, String reason, String resolution )
    {
        final Mismatch mismatchType = new Mismatch();
        mismatchType.setItem( property );
        mismatchType.setReason( reason );
        mismatchType.setResolution( resolution );
        report.add( mismatchType );
    }

    private static <T> Set<T> diff( Set<T> a, Set<T> b )
    {
        return a.stream()
                .filter( v -> !b.contains( v ) )
                .collect( Collectors.toSet() );

    }
}
