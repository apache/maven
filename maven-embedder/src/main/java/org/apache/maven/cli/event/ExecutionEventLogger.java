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
import static org.fusesource.jansi.Ansi.ansi;

import org.apache.commons.lang3.Validate;
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
import org.fusesource.jansi.Ansi;
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
    private static final int MAX_PROJECT_NAME_LENGTH = LINE_LENGTH - MAX_PADDED_BUILD_TIME_DURATION_LENGTH - 12;

    public ExecutionEventLogger()
    {
        logger = LoggerFactory.getLogger( ExecutionEventLogger.class );
    }

    // TODO should we deprecate?
    public ExecutionEventLogger( Logger logger )
    {
        this.logger = Validate.notNull( logger, "logger cannot be null" );
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

    private void infoLine( char c )
    {
        infoMain( chars( c, LINE_LENGTH ) );
    }

    private void infoMain( String msg )
    {
        logger.info( ansi().fgBlue().a( msg ).reset().toString() );
    }

    @Override
    public void projectDiscoveryStarted( ExecutionEvent event )
    {
        if ( logger.isInfoEnabled() )
        {
            logger.info( ansi().fgBlue().a( "Scanning for projects..." ).reset().toString() );
        }
    }

    @Override
    public void sessionStarted( ExecutionEvent event )
    {
        if ( logger.isInfoEnabled() && event.getSession().getProjects().size() > 1 )
        {
            infoLine( '-' );

            infoMain( "Reactor Build Order:" );

            logger.info( "" );

            for ( MavenProject project : event.getSession().getProjects() )
            {
                logger.info( ansi().fgBlue().a( project.getName() ).reset().toString() );
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

            infoLine( '-' );
        }
    }

    private void logReactorSummary( MavenSession session )
    {
        infoLine( '-' );

        infoMain( "Reactor Summary:" );

        logger.info( "" );

        MavenExecutionResult result = session.getResult();

        for ( MavenProject project : session.getProjects() )
        {
            BuildSummary buildSummary = result.getBuildSummary( project );
            Ansi ansi = ansi();

            if ( buildSummary == null )
            {
                ansi.fgYellow();
            }
            else if ( buildSummary instanceof BuildSuccess )
            {
                ansi.fgGreen();
            }
            else if ( buildSummary instanceof BuildFailure )
            {
                ansi.fgRed();
            }

            ansi.a( project.getName() );
            ansi.a( ' ' );

            int dots = MAX_PROJECT_NAME_LENGTH - project.getName().length();

            for ( int i = 0; i < dots; i++ )
            {
                ansi.a( '.' );
            }

            ansi.a( ' ' );

            if ( buildSummary == null )
            {
                ansi.a( "SKIPPED" );
            }
            else if ( buildSummary instanceof BuildSuccess )
            {
                ansi.a( "SUCCESS" );
                ansi.a( " [" );
                String buildTimeDuration = formatDuration( buildSummary.getTime() );
                int padSize = MAX_PADDED_BUILD_TIME_DURATION_LENGTH - buildTimeDuration.length();
                if ( padSize > 0 )
                {
                    ansi.a( chars( ' ', padSize ) );
                }
                ansi.a( buildTimeDuration );
                ansi.a( ']' );
            }
            else if ( buildSummary instanceof BuildFailure )
            {
                ansi.a( "FAILURE" );
                ansi.a( " [" );
                String buildTimeDuration = formatDuration( buildSummary.getTime() );
                int padSize = MAX_PADDED_BUILD_TIME_DURATION_LENGTH - buildTimeDuration.length();
                if ( padSize > 0 )
                {
                    ansi.a( chars( ' ', padSize ) );
                }
                ansi.a( buildTimeDuration );
                ansi.a( ']' );
            }

            ansi.reset();
            logger.info( ansi.toString() );
        }
    }

    private void logResult( MavenSession session )
    {
        infoLine( '-' );
        Ansi ansi = ansi();

        if ( session.getResult().hasExceptions() )
        {
            ansi.fgRed().a( "BUILD FAILURE" );
        }
        else
        {
            ansi.fgGreen().a( "BUILD SUCCESS" );
        }
        logger.info( ansi.reset().toString() );
    }

    private void logStats( MavenSession session )
    {
        infoLine( '-' );

        long finish = System.currentTimeMillis();

        long time = finish - session.getRequest().getStartTime().getTime();

        String wallClock = session.getRequest().getDegreeOfConcurrency() > 1 ? " (Wall Clock)" : "";

        logger.info( ansi().fgBlue().a( "Total time: " + formatDuration( time ) + wallClock ).reset().toString() );

        logger.info( ansi().fgBlue().a( "Finished at: " + formatTimestamp( finish ) ).reset().toString() );

        System.gc();

        Runtime r = Runtime.getRuntime();

        long mb = 1024 * 1024;

        logger.info( ansi().fgBlue().a( "Final Memory: " + ( r.totalMemory() - r.freeMemory() ) / mb + "M/"
                                            + r.totalMemory() / mb + "M" ).reset().toString() );
    }

    @Override
    public void projectSkipped( ExecutionEvent event )
    {
        if ( logger.isInfoEnabled() )
        {
            logger.info( "" );
            infoLine( '-' );

            infoMain( "Skipping " + event.getProject().getName() );
            logger.info( ansi().fgBlue().a( "This project has been banned from the build due to previous failures." ).
                reset().toString() );

            infoLine( '-' );
        }
    }

    @Override
    public void projectStarted( ExecutionEvent event )
    {
        if ( logger.isInfoEnabled() )
        {
            logger.info( "" );
            infoLine( '-' );

            infoMain( "Building " + event.getProject().getName() + " " + event.getProject().getVersion() );

            infoLine( '-' );
        }
    }

    @Override
    public void mojoSkipped( ExecutionEvent event )
    {
        if ( logger.isWarnEnabled() )
        {
            logger.warn( ansi().fgYellow().
                a( "Goal " + event.getMojoExecution().getGoal()
                       + " requires online mode for execution but Maven is currently offline, skipping" ).
                reset().toString() );

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
            logger.info( "" );

            Ansi ansi = ansi().fgBlue().a( "--- " ).reset();
            append( ansi, event.getMojoExecution() );
            append( ansi, event.getProject() );
            ansi.fgBlue().a( " ---" ).reset();

            logger.info( ansi.toString() );
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
            logger.info( "" );

            Ansi ansi = ansi().fgBlue().a( ">>> " ).reset();
            append( ansi, event.getMojoExecution() );
            ansi.fgBlue().a( " > " ).reset();
            appendForkInfo( ansi, event.getMojoExecution().getMojoDescriptor() );
            append( ansi, event.getProject() );
            ansi.fgBlue().a( " >>>" ).reset();

            logger.info( ansi.toString() );
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
            logger.info( "" );

            Ansi ansi = ansi().fgBlue().a( "<<< " ).reset();
            append( ansi, event.getMojoExecution() );
            ansi.fgBlue().a( " < " ).reset();
            appendForkInfo( ansi, event.getMojoExecution().getMojoDescriptor() );
            append( ansi, event.getProject() );
            ansi.fgBlue().a( " <<<" ).reset();

            logger.info( ansi.toString() );
        }
    }

    private void append( Ansi ansi, MojoExecution me )
    {
        ansi.fgBlue().a( me.getArtifactId() ).a( ':' ).a( me.getVersion() );
        ansi.a( ':' ).a( me.getGoal() ).reset();
        if ( me.getExecutionId() != null )
        {
            ansi.fgBlue().a( " (" ).a( me.getExecutionId() ).a( ')' ).reset();
        }
    }

    private void appendForkInfo( Ansi ansi, MojoDescriptor md )
    {
        ansi.fgBlue();
        if ( StringUtils.isNotEmpty( md.getExecutePhase() ) )
        {
            // forked phase
            if ( StringUtils.isNotEmpty( md.getExecuteLifecycle() ) )
            {
                ansi.a( '[' );
                ansi.a( md.getExecuteLifecycle() );
                ansi.a( ']' );
            }
            ansi.a( md.getExecutePhase() );
        }
        else
        {
            // forked goal
            ansi.a( ':' );
            ansi.a( md.getExecuteGoal() );
        }
        ansi.reset();
    }

    private void append( Ansi ansi, MavenProject project )
    {
        ansi.a( " @ " ).fgCyan().a( project.getArtifactId() ).reset();
    }

    @Override
    public void forkedProjectStarted( ExecutionEvent event )
    {
        if ( logger.isInfoEnabled() && event.getMojoExecution().getForkedExecutions().size() > 1 )
        {
            logger.info( "" );
            infoLine( '>' );

            infoMain( "Forking " + event.getProject().getName() + " " + event.getProject().getVersion() );

            infoLine( '>' );
        }
    }
}
