package org.apache.maven.embedder.cache;

import org.aspectj.lang.Aspects;

import java.io.StringWriter;
import java.io.PrintWriter;

import org.apache.maven.BuildFailureException;
import org.apache.maven.cli.CLIReportingUtils;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.errors.CoreErrorReporter;
import org.apache.maven.errors.CoreReporterManagerAspect;
import org.apache.maven.errors.DefaultCoreErrorReporter;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.project.aspect.ProjectReporterManagerAspect;
import org.apache.maven.project.error.DefaultProjectErrorReporter;
import org.apache.maven.project.error.ProjectErrorReporter;
import org.apache.maven.project.ProjectBuildingException;

public privileged aspect ErrorReportingAspect
{

    private ProjectErrorReporter projectErrorReporter;

    private CoreErrorReporter coreErrorReporter;

    private pointcut embedderCalls():
        execution( public MavenExecutionResult MavenEmbedder.*( .. ) );

    // TODO: Use MavenExecutionRequest to allow configuration of the reporters to be used.
    before():
        embedderCalls() && !cflow( embedderCalls() )
    {
        projectErrorReporter = new DefaultProjectErrorReporter();

        ProjectReporterManagerAspect prma = (ProjectReporterManagerAspect) Aspects.aspectOf( ProjectReporterManagerAspect.class );
        prma.setReporter( projectErrorReporter );

        coreErrorReporter = new DefaultCoreErrorReporter();

        CoreReporterManagerAspect crma = (CoreReporterManagerAspect) Aspects.aspectOf( CoreReporterManagerAspect.class );
        crma.setReporter( coreErrorReporter );
    }

    boolean around( ProjectBuildingException e, boolean showStackTraces, StringWriter writer ):
        execution( private static boolean CLIReportingUtils.handleProjectBuildingException( ProjectBuildingException, boolean, StringWriter ) )
        && args( e, showStackTraces, writer )
    {
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
                    cause.printStackTrace( new PrintWriter( writer ) );
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
        if ( coreErrorReporter == null )
        {
            return proceed( e, showStackTraces, writer );
        }
        else
        {
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
                    cause.printStackTrace( new PrintWriter( writer ) );
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
