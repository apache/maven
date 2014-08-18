package org.apache.maven.cli.event;

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

import static org.apache.maven.cli.CLIReportingUtils.formatDuration;
import static org.apache.maven.cli.CLIReportingUtils.formatTimestamp;

import org.apache.maven.execution.AbstractExecutionListener;
import org.apache.maven.execution.BuildFailure;
import org.apache.maven.execution.BuildSuccess;
import org.apache.maven.execution.BuildSummary;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logs execution events to logger, eventually user-supplied.
 *
 * @author Benjamin Bentmann
 */
public class ExecutionEventLogger
    extends AbstractExecutionListener
{
    private final Logger logger;

    private static final int LINE_LENGTH = 72;
    private static final int MAX_PADDED_BUILD_TIME_DURATION_LENGTH = 9;
    private static final int MAX_PROJECT_NAME_LENGTH = 52;

    public ExecutionEventLogger()
    {
        logger = LoggerFactory.getLogger( ExecutionEventLogger.class );
    }

    // TODO should we deprecate?
    public ExecutionEventLogger( Logger logger )
    {
        if ( logger == null )
        {
            throw new IllegalArgumentException( "logger missing" );
        }

        this.logger = logger;
    }

    private static String chars( char c, int count )
    {
        StringBuilder buffer = new StringBuilder( count );

        for ( int i = count; i > 0; i-- )
        {
            buffer.append( c );
        }

        return buffer.toString();
    }

    @Override
    public void projectDiscoveryStarted( ExecutionEvent event )
    {
        if ( logger.isInfoEnabled() )
        {
            logger.info( "Scanning for projects..." );
        }
    }

    @Override
    public void sessionStarted( ExecutionEvent event )
    {
        if ( logger.isInfoEnabled() && event.getSession().getProjects().size() > 1 )
        {
            logger.info( chars( '-', LINE_LENGTH ) );

            logger.info( "Reactor Build Order:" );

            logger.info( "" );

            for ( MavenProject project : event.getSession().getProjects() )
            {
                logger.info( project.getName() );
            }
        }
    }

    @Override
    public void sessionEnded( ExecutionEvent event )
    {
        if ( logger.isInfoEnabled() )
        {
            if ( event.getSession().getProjects().size() > 1 )
            {
                logReactorSummary( event.getSession() );
            }

            logResult( event.getSession() );

            logStats( event.getSession() );

            logger.info( chars( '-', LINE_LENGTH ) );
        }
    }

    private void logReactorSummary( MavenSession session )
    {
        logger.info( chars( '-', LINE_LENGTH ) );

        logger.info( "Reactor Summary:" );

        logger.info( "" );

        MavenExecutionResult result = session.getResult();

        for ( MavenProject project : session.getProjects() )
        {
            StringBuilder buffer = new StringBuilder( 128 );

            buffer.append( project.getName() );
            buffer.append( ' ' );

            if ( buffer.length() <= MAX_PROJECT_NAME_LENGTH )
            {
                while ( buffer.length() < MAX_PROJECT_NAME_LENGTH )
                {
                    buffer.append( '.' );
                }
                buffer.append( ' ' );
            }

            BuildSummary buildSummary = result.getBuildSummary( project );

            if ( buildSummary == null )
            {
                buffer.append( "SKIPPED" );
            }
            else if ( buildSummary instanceof BuildSuccess )
            {
                buffer.append( "SUCCESS [" );
                String buildTimeDuration = formatDuration( buildSummary.getTime() );
                int padSize = MAX_PADDED_BUILD_TIME_DURATION_LENGTH - buildTimeDuration.length();
                if ( padSize > 0 )
                {
                    buffer.append( chars( ' ', padSize ) );
                }
                buffer.append( buildTimeDuration );
                buffer.append( "]" );
            }
            else if ( buildSummary instanceof BuildFailure )
            {
                buffer.append( "FAILURE [" );
                String buildTimeDuration = formatDuration( buildSummary.getTime() );
                int padSize = MAX_PADDED_BUILD_TIME_DURATION_LENGTH - buildTimeDuration.length();
                if ( padSize > 0 )
                {
                    buffer.append( chars( ' ', padSize ) );
                }
                buffer.append( buildTimeDuration );
                buffer.append( "]" );
            }

            logger.info( buffer.toString() );
        }
    }

    private void logResult( MavenSession session )
    {
        logger.info( chars( '-', LINE_LENGTH ) );

        if ( session.getResult().hasExceptions() )
        {
            logger.info( "BUILD FAILURE" );
        }
        else
        {
            logger.info( "BUILD SUCCESS" );
        }
    }

    private void logStats( MavenSession session )
    {
        logger.info( chars( '-', LINE_LENGTH ) );

        long finish = System.currentTimeMillis();

        long time = finish - session.getRequest().getStartTime().getTime();

        String wallClock = session.getRequest().getDegreeOfConcurrency() > 1 ? " (Wall Clock)" : "";

        logger.info( "Total time: " + formatDuration( time ) + wallClock );

        logger.info( "Finished at: " + formatTimestamp( finish ) );

        System.gc();

        Runtime r = Runtime.getRuntime();

        long mb = 1024 * 1024;

        logger.info( "Final Memory: " + ( r.totalMemory() - r.freeMemory() ) / mb + "M/" + r.totalMemory() / mb + "M" );
    }

    @Override
    public void projectSkipped( ExecutionEvent event )
    {
        if ( logger.isInfoEnabled() )
        {
            logger.info( chars( ' ', LINE_LENGTH ) );
            logger.info( chars( '-', LINE_LENGTH ) );

            logger.info( "Skipping " + event.getProject().getName() );
            logger.info( "This project has been banned from the build due to previous failures." );

            logger.info( chars( '-', LINE_LENGTH ) );
        }
    }

    @Override
    public void projectStarted( ExecutionEvent event )
    {
        if ( logger.isInfoEnabled() )
        {
            logger.info( chars( ' ', LINE_LENGTH ) );
            logger.info( chars( '-', LINE_LENGTH ) );

            logger.info( "Building " + event.getProject().getName() + " " + event.getProject().getVersion() );

            logger.info( chars( '-', LINE_LENGTH ) );
        }
    }

    @Override
    public void mojoSkipped( ExecutionEvent event )
    {
        if ( logger.isWarnEnabled() )
        {
            logger.warn( "Goal " + event.getMojoExecution().getGoal()
                + " requires online mode for execution but Maven is currently offline, skipping" );
        }
    }

    /**
     * <pre>--- mojo-artifactId:version:goal (mojo-executionId) @ project-artifactId ---</pre>
     */
    @Override
    public void mojoStarted( ExecutionEvent event )
    {
        if ( logger.isInfoEnabled() )
        {
            StringBuilder buffer = new StringBuilder( 128 );

            buffer.append( "--- " );
            append( buffer, event.getMojoExecution() );
            append( buffer, event.getProject() );
            buffer.append( " ---" );

            logger.info( "" );
            logger.info( buffer.toString() );
        }
    }

    /**
     * <pre>>>> mojo-artifactId:version:goal (mojo-executionId) > :forked-goal @ project-artifactId >>></pre>
     * <pre>>>> mojo-artifactId:version:goal (mojo-executionId) > [lifecycle]phase @ project-artifactId >>></pre>
     */
    @Override
    public void forkStarted( ExecutionEvent event )
    {
        if ( logger.isInfoEnabled() )
        {
            StringBuilder buffer = new StringBuilder( 128 );

            buffer.append( ">>> " );
            append( buffer, event.getMojoExecution() );
            buffer.append( " > " );
            appendForkInfo( buffer, event.getMojoExecution().getMojoDescriptor() );
            append( buffer, event.getProject() );
            buffer.append( " >>>" );

            logger.info( "" );
            logger.info( buffer.toString() );
        }
    }

    // CHECKSTYLE_OFF: LineLength
    /**
     * <pre>&lt;&lt;&lt; mojo-artifactId:version:goal (mojo-executionId) &lt; :forked-goal @ project-artifactId &lt;&lt;&lt;</pre>
     * <pre>&lt;&lt;&lt; mojo-artifactId:version:goal (mojo-executionId) &lt; [lifecycle]phase @ project-artifactId &lt;&lt;&lt;</pre>
     */
    // CHECKSTYLE_ON: LineLength
    @Override
    public void forkSucceeded( ExecutionEvent event )
    {
        if ( logger.isInfoEnabled() )
        {
            StringBuilder buffer = new StringBuilder( 128 );

            buffer.append( "<<< " );
            append( buffer, event.getMojoExecution() );
            buffer.append( " < " );
            appendForkInfo( buffer, event.getMojoExecution().getMojoDescriptor() );
            append( buffer, event.getProject() );
            buffer.append( " <<<" );

            logger.info( "" );
            logger.info( buffer.toString() );
        }
    }

    private void append( StringBuilder buffer, MojoExecution me )
    {
        buffer.append( me.getArtifactId() ).append( ':' ).append( me.getVersion() );
        buffer.append( ':' ).append( me.getGoal() );
        if ( me.getExecutionId() != null )
        {
            buffer.append( " (" ).append( me.getExecutionId() ).append( ')' );
        }
    }

    private void appendForkInfo( StringBuilder buffer, MojoDescriptor md )
    {
        if ( StringUtils.isNotEmpty( md.getExecutePhase() ) )
        {
            // forked phase
            if ( StringUtils.isNotEmpty( md.getExecuteLifecycle() ) )
            {
                buffer.append( '[' );
                buffer.append( md.getExecuteLifecycle() );
                buffer.append( ']' );
            }
            buffer.append( md.getExecutePhase() );
        }
        else
        {
            // forked goal
            buffer.append( ':' );
            buffer.append( md.getExecuteGoal() );
        }
    }

    private void append( StringBuilder buffer, MavenProject project )
    {
        buffer.append( " @ " ).append( project.getArtifactId() );
    }

    @Override
    public void forkedProjectStarted( ExecutionEvent event )
    {
        if ( logger.isInfoEnabled() && event.getMojoExecution().getForkedExecutions().size() > 1 )
        {
            logger.info( chars( ' ', LINE_LENGTH ) );
            logger.info( chars( '>', LINE_LENGTH ) );

            logger.info( "Forking " + event.getProject().getName() + " " + event.getProject().getVersion() );

            logger.info( chars( '>', LINE_LENGTH ) );
        }
    }

}
