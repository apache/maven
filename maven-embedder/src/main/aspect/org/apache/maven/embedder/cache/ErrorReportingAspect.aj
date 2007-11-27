package org.apache.maven.embedder.cache;

import org.aspectj.lang.Aspects;

import java.io.StringWriter;
import java.io.PrintWriter;

import org.apache.maven.cli.CLIReportingUtils;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.project.aspect.PBEDerivativeReporterAspect;
import org.apache.maven.project.error.DefaultProjectErrorReporter;
import org.apache.maven.project.error.ProjectErrorReporter;
import org.apache.maven.project.ProjectBuildingException;

public privileged aspect ErrorReportingAspect
{

    private ProjectErrorReporter projectErrorReporter;

    private pointcut embedderCalls():
        execution( public MavenExecutionResult MavenEmbedder.*( .. ) );

    before():
        embedderCalls() && !cflow( embedderCalls() )
    {
        projectErrorReporter = new DefaultProjectErrorReporter();

        PBEDerivativeReporterAspect pbeDerivativeReporterAspect = Aspects.aspectOf( PBEDerivativeReporterAspect.class );
        pbeDerivativeReporterAspect.setProjectErrorReporter( projectErrorReporter );
    }

    private pointcut cliReportingUtilsCalls():
        execution( * CLIReportingUtils.*( .. ) );

    before():
        cliReportingUtilsCalls()
        && !cflow( cliReportingUtilsCalls() )
        && !cflow( embedderCalls() )
    {
        projectErrorReporter = new DefaultProjectErrorReporter();

        PBEDerivativeReporterAspect pbeDerivativeReporterAspect = Aspects.aspectOf( PBEDerivativeReporterAspect.class );
        pbeDerivativeReporterAspect.setProjectErrorReporter( projectErrorReporter );
    }

    boolean around( ProjectBuildingException e, boolean showStackTraces, StringWriter writer ):
        execution( private static boolean CLIReportingUtils.handleProjectBuildingException( ProjectBuildingException, boolean, StringWriter ) )
        && args( e, showStackTraces, writer )
    {
//        if ( projectErrorReporter == null )
//        {
//            return proceed( e, showStackTraces, writer );
//        }
//        else
//        {
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
//        }
    }

}
