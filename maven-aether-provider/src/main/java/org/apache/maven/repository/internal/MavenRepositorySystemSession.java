package org.apache.maven.repository.internal;

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

import org.sonatype.aether.collection.DependencyGraphTransformer;
import org.sonatype.aether.collection.DependencyManager;
import org.sonatype.aether.collection.DependencySelector;
import org.sonatype.aether.collection.DependencyTraverser;
import org.sonatype.aether.util.DefaultRepositorySystemSession;
import org.sonatype.aether.util.artifact.DefaultArtifactType;
import org.sonatype.aether.util.artifact.DefaultArtifactTypeRegistry;
import org.sonatype.aether.util.graph.manager.ClassicDependencyManager;
import org.sonatype.aether.util.graph.selector.AndDependencySelector;
import org.sonatype.aether.util.graph.selector.ExclusionDependencySelector;
import org.sonatype.aether.util.graph.selector.OptionalDependencySelector;
import org.sonatype.aether.util.graph.selector.ScopeDependencySelector;
import org.sonatype.aether.util.graph.transformer.ChainedDependencyGraphTransformer;
import org.sonatype.aether.util.graph.transformer.ConflictMarker;
import org.sonatype.aether.util.graph.transformer.JavaDependencyContextRefiner;
import org.sonatype.aether.util.graph.transformer.JavaEffectiveScopeCalculator;
import org.sonatype.aether.util.graph.transformer.NearestVersionConflictResolver;
import org.sonatype.aether.util.graph.traverser.FatArtifactTraverser;
import org.sonatype.aether.util.repository.DefaultAuthenticationSelector;
import org.sonatype.aether.util.repository.DefaultMirrorSelector;
import org.sonatype.aether.util.repository.DefaultProxySelector;

/**
 * The base Maven repository system session, without environment configuration (authentication, mirror,
 * proxy, ...).
 * 
 * <p><strong>Warning:</strong> This class is not intended for
 * usage by Maven plugins, those should always acquire the current repository system session via
 * <a href="/ref/current/maven-core/apidocs/org/apache/maven/plugin/PluginParameterExpressionEvaluator.html">plugin
 * parameter injection</a>, since the current repository system session is created by Maven in
 * <a href="/ref/current/maven-core/apidocs/org/apache/maven/DefaultMaven.html">
 * <code>DefaultMaven.newRepositorySession(MavenExecutionRequest request)</code></a>.</p>
 * 
 * @author Benjamin Bentmann
 */
public class MavenRepositorySystemSession
    extends DefaultRepositorySystemSession
{

    /**
     * Creates a new Maven repository system session by initializing the session with values typical for
     * Maven-based resolution. In more detail, this constructor configures settings relevant for the processing of
     * dependency graphs, most other settings remain at their generic default value. Use the various setters to further
     * configure the session with authentication, mirror, proxy and other information required for your environment.
     * 
     * @param standalone is this instance expected to be used inside Maven, with Plexus and Maven core components, or
     *   standalone? If standalone, System properties are used and classical Maven artifact handlers are pre-configured
     *   to mimic complete Maven repository system session.
     */
    public MavenRepositorySystemSession( boolean standalone )
    {
        DependencyTraverser depTraverser = new FatArtifactTraverser();
        setDependencyTraverser( depTraverser );

        DependencyManager depManager = new ClassicDependencyManager();
        setDependencyManager( depManager );

        DependencySelector depFilter =
            new AndDependencySelector( new ScopeDependencySelector( "test", "provided" ),
                                       new OptionalDependencySelector(), new ExclusionDependencySelector() );
        setDependencySelector( depFilter );

        DependencyGraphTransformer transformer =
            new ChainedDependencyGraphTransformer( new ConflictMarker(), new JavaEffectiveScopeCalculator(),
                                                   new NearestVersionConflictResolver(),
                                                   new JavaDependencyContextRefiner() );
        setDependencyGraphTransformer( transformer );

        setIgnoreInvalidArtifactDescriptor( true );
        setIgnoreMissingArtifactDescriptor( true );

        if ( standalone )
        {
            setMirrorSelector( new DefaultMirrorSelector() );
            setAuthenticationSelector( new DefaultAuthenticationSelector() );
            setProxySelector( new DefaultProxySelector() );

            DefaultArtifactTypeRegistry stereotypes = new DefaultArtifactTypeRegistry();
            stereotypes.add( new DefaultArtifactType( "pom" ) );
            stereotypes.add( new DefaultArtifactType( "maven-plugin", "jar", "", "java" ) );
            stereotypes.add( new DefaultArtifactType( "jar", "jar", "", "java" ) );
            stereotypes.add( new DefaultArtifactType( "ejb", "jar", "", "java" ) );
            stereotypes.add( new DefaultArtifactType( "ejb-client", "jar", "client", "java" ) );
            stereotypes.add( new DefaultArtifactType( "test-jar", "jar", "tests", "java" ) );
            stereotypes.add( new DefaultArtifactType( "javadoc", "jar", "javadoc", "java" ) );
            stereotypes.add( new DefaultArtifactType( "java-source", "jar", "sources", "java", false, false ) );
            stereotypes.add( new DefaultArtifactType( "war", "war", "", "java", false, true ) );
            stereotypes.add( new DefaultArtifactType( "ear", "ear", "", "java", false, true ) );
            stereotypes.add( new DefaultArtifactType( "rar", "rar", "", "java", false, true ) );
            stereotypes.add( new DefaultArtifactType( "par", "par", "", "java", false, true ) );
            setArtifactTypeRegistry( stereotypes );
    
            setSystemProps( System.getProperties() );
            setConfigProps( System.getProperties() );
        }
    }

}
