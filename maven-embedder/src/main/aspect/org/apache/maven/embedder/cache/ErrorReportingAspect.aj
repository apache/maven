package org.apache.maven.embedder.cache;

import org.aspectj.lang.Aspects;

import java.io.StringWriter;
import java.io.PrintWriter;

import org.apache.maven.BuildFailureException;
import org.apache.maven.cli.CLIReportingUtils;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.errors.CoreErrorReporter;
import org.apache.maven.errors.CoreReporterManager;
import org.apache.maven.errors.DefaultCoreErrorReporter;
import org.apache.maven.project.error.ProjectReporterManager;
import org.apache.maven.project.error.ProjectErrorReporter;
import org.apache.maven.project.ProjectBuildingException;

public privileged aspect ErrorReportingAspect
{

    private pointcut embedderCalls():
        execution( public * MavenEmbedder.*( .. ) );

    // TODO: Use MavenExecutionRequest to allow configuration of the reporters to be used.
    Object around():
        embedderCalls() && !cflow( embedderCalls() )
    {
        try
        {
            return proceed();
        }
        finally
        {
            ProjectReporterManager.clearReporter();
            CoreReporterManager.clearReporter();
        }
    }

    boolean around( ProjectBuildingException e, boolean showStackTraces, StringWriter writer ):
        execution( private static boolean CLIReportingUtils.handleProjectBuildingException( ProjectBuildingException, boolean, StringWriter ) )
        && args( e, showStackTraces, writer )
    {
        ProjectErrorReporter projectErrorReporter = ProjectReporterManager.getReporter();

        if ( projectErrorReporter == null )
        {
            return proceed( e, showStackTraces, writer );
        }
        else
        {
            Throwable reportingError = projectErrorReporter.findReportedException( e );

            boolean result = false;

            if ( reportingError != null )
            {
                writer.write( projectErrorReporter.getFormattedMessage( reportingError ) );

                if ( showStackTraces )
                {
                    writer.write( CLIReportingUtils.NEWLINE );
                    writer.write( CLIReportingUtils.NEWLINE );
                    Throwable cause = projectErrorReporter.getRealCause( reportingError );
                    if ( cause != null )
                    {
                        cause.printStackTrace( new PrintWriter( writer ) );
                    }
                }

                writer.write( CLIReportingUtils.NEWLINE );
                writer.write( CLIReportingUtils.NEWLINE );

                result = true;
            }
            else
            {
                result = proceed( e, showStackTraces, writer );
            }

            return result;
        }
    }

    boolean around( BuildFailureException e, boolean showStackTraces, StringWriter writer ):
        execution( private static boolean CLIReportingUtils.handleBuildFailureException( BuildFailureException, boolean, StringWriter ) )
        && args( e, showStackTraces, writer )
    {
        CoreErrorReporter coreErrorReporter = CoreReporterManager.getReporter();

        if ( coreErrorReporter == null )
        {
            return proceed( e, showStackTraces, writer );
        }
        else
        {
            System.out.println( "Checking core error reporter for help." );

            Throwable reportingError = coreErrorReporter.findReportedException( e );

            boolean result = false;

            if ( reportingError != null )
            {
                writer.write( coreErrorReporter.getFormattedMessage( reportingError ) );

                if ( showStackTraces )
                {
                    writer.write( CLIReportingUtils.NEWLINE );
                    writer.write( CLIReportingUtils.NEWLINE );
                    Throwable cause = coreErrorReporter.getRealCause( reportingError );
                    if ( cause != null )
                    {
                        cause.printStackTrace( new PrintWriter( writer ) );
                    }
                }

                writer.write( CLIReportingUtils.NEWLINE );
                writer.write( CLIReportingUtils.NEWLINE );

                result = true;
            }
            else
            {
                result = proceed( e, showStackTraces, writer );
            }

            return result;
        }
    }

}
