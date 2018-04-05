package org.apache.maven.toolchain.building;

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

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.building.Problem;
import org.apache.maven.building.ProblemCollector;
import org.apache.maven.building.ProblemCollectorFactory;
import org.apache.maven.building.Source;
import org.apache.maven.toolchain.io.ToolchainsParseException;
import org.apache.maven.toolchain.io.ToolchainsReader;
import org.apache.maven.toolchain.merge.MavenToolchainMerger;
import org.apache.maven.toolchain.model.PersistedToolchains;
import org.apache.maven.toolchain.model.TrackableBase;

/**
 * 
 * @author Robert Scholte
 * @since 3.3.0
 */
@Named
@Singleton
public class DefaultToolchainsBuilder
    implements ToolchainsBuilder
{
    private MavenToolchainMerger toolchainsMerger = new MavenToolchainMerger();
    
    @Inject
    private ToolchainsReader toolchainsReader;

    @Override
    public ToolchainsBuildingResult build( ToolchainsBuildingRequest request )
        throws ToolchainsBuildingException
    {
        ProblemCollector problems = ProblemCollectorFactory.newInstance( null );
        
        PersistedToolchains globalToolchains = readToolchains( request.getGlobalToolchainsSource(), request, problems );

        PersistedToolchains userToolchains = readToolchains( request.getUserToolchainsSource(), request, problems );

        toolchainsMerger.merge( userToolchains, globalToolchains, TrackableBase.GLOBAL_LEVEL );
        
        problems.setSource( "" );
        
        if ( hasErrors( problems.getProblems() ) )
        {
            throw new ToolchainsBuildingException( problems.getProblems() );
        }
        
        
        return new DefaultToolchainsBuildingResult( userToolchains, problems.getProblems() );
    }

    private PersistedToolchains readToolchains( Source toolchainsSource, ToolchainsBuildingRequest request,
                                                ProblemCollector problems )
    {
        if ( toolchainsSource == null )
        {
            return new PersistedToolchains();
        }

        PersistedToolchains toolchains;

        try
        {
            Map<String, ?> options = Collections.singletonMap( ToolchainsReader.IS_STRICT, Boolean.TRUE );

            try
            {
                toolchains = toolchainsReader.read( toolchainsSource.getInputStream(), options );
            }
            catch ( ToolchainsParseException e )
            {
                options = Collections.singletonMap( ToolchainsReader.IS_STRICT, Boolean.FALSE );

                toolchains = toolchainsReader.read( toolchainsSource.getInputStream(), options );

                problems.add( Problem.Severity.WARNING, e.getMessage(), e.getLineNumber(), e.getColumnNumber(),
                              e );
            }
        }
        catch ( ToolchainsParseException e )
        {
            problems.add( Problem.Severity.FATAL, "Non-parseable toolchains " + toolchainsSource.getLocation()
                + ": " + e.getMessage(), e.getLineNumber(), e.getColumnNumber(), e );
            return new PersistedToolchains();
        }
        catch ( IOException e )
        {
            problems.add( Problem.Severity.FATAL, "Non-readable toolchains " + toolchainsSource.getLocation()
                + ": " + e.getMessage(), -1, -1, e );
            return new PersistedToolchains();
        }

        return toolchains;
    }
    
    private boolean hasErrors( List<Problem> problems )
    {
        if ( problems != null )
        {
            for ( Problem problem : problems )
            {
                if ( Problem.Severity.ERROR.compareTo( problem.getSeverity() ) >= 0 )
                {
                    return true;
                }
            }
        }

        return false;
    }
}
