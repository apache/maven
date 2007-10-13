package org.apache.maven;

/**
 * Exception indicating that Maven has no instructions for what to execute. This
 * happens when no goals are specified on the command line, and there is no
 * defaultGoal specified in the POM itself.
 *
 * @author jdcasey
 *
 */
public class NoGoalsSpecifiedException
    extends BuildFailureException
{

    public NoGoalsSpecifiedException( String message )
    {
        super( message );
    }

}
