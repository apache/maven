package org.apache.maven.lifecycle.plan;

import org.apache.maven.lifecycle.LifecycleSpecificationException;
import org.apache.maven.lifecycle.MojoBindingUtils;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.apache.maven.lifecycle.statemgmt.StateManagementUtils;

import java.util.Iterator;
import java.util.List;
import java.util.Stack;

/**
 * Collection of static utility methods used to work with LifecycleBindings and other collections of MojoBinding
 * instances that make up a build plan.
 */
public final class BuildPlanUtils
{

    private BuildPlanUtils()
    {
    }

    /**
     * Render an entire build plan to a String. If extendedInfo == true, include each MojoBinding's configuration in the
     * output.
     */
    public static String listBuildPlan( final BuildPlan plan, final boolean extendedInfo )
        throws LifecycleSpecificationException, LifecyclePlannerException
    {
        List mojos = plan.renderExecutionPlan( new Stack() );
        plan.resetExecutionProgress();

        return listBuildPlan( mojos, extendedInfo );
    }

    /**
     * Render a list containing the MojoBinding instances for an entire build plan to a String. If extendedInfo == true,
     * include each MojoBinding's configuration in the output.
     */
    public static String listBuildPlan( final List mojoBindings, final boolean extendedInfo )
        throws LifecycleSpecificationException, LifecyclePlannerException
    {
        StringBuffer listing = new StringBuffer();
        int indentLevel = 0;

        int counter = 1;
        for ( Iterator it = mojoBindings.iterator(); it.hasNext(); )
        {
            MojoBinding binding = (MojoBinding) it.next();

            if ( StateManagementUtils.isForkedExecutionStartMarker( binding ) )
            {
                newListingLine( listing, indentLevel, counter );
                listing.append( "[fork start]" );

                if ( extendedInfo )
                {
                    listing.append( ' ' ).append( formatMojoListing( binding, indentLevel, extendedInfo ) );
                }

                indentLevel++;
            }
            else if ( StateManagementUtils.isForkedExecutionClearMarker( binding ) )
            {
                indentLevel--;

                newListingLine( listing, indentLevel, counter );
                listing.append( "[fork cleanup]" );

                if ( extendedInfo )
                {
                    listing.append( ' ' ).append( formatMojoListing( binding, indentLevel, extendedInfo ) );
                }
            }
            else if ( StateManagementUtils.isForkedExecutionEndMarker( binding ) )
            {
                indentLevel--;

                newListingLine( listing, indentLevel, counter );
                listing.append( "[fork end]" );

                if ( extendedInfo )
                {
                    listing.append( ' ' ).append( formatMojoListing( binding, indentLevel, extendedInfo ) );
                }

                indentLevel++;
            }
            else
            {
                newListingLine( listing, indentLevel, counter );
                listing.append( formatMojoListing( binding, indentLevel, extendedInfo ) );
            }

            counter++;
        }

        return listing.toString();
    }

    /**
     * Append a newline character, add the next line's number, and indent the new line to the appropriate level (which
     * tracks separate forked executions).
     */
    private static void newListingLine( final StringBuffer listing, final int indentLevel, final int counter )
    {
        listing.append( '\n' );

        listing.append( counter ).append( "." );

        for ( int i = 0; i < indentLevel; i++ )
        {
            listing.append( "  " );
        }

        // adding a minimal offset from the counter (line-number) of the listing.
        listing.append( ' ' );

    }

    /**
     * Format a single MojoBinding for inclusion in a build plan listing. If extendedInfo == true, include the
     * MojoBinding's configuration in the output.
     */
    public static String formatMojoListing( final MojoBinding binding, final int indentLevel, final boolean extendedInfo )
    {
        StringBuffer listing = new StringBuffer();

        listing.append( MojoBindingUtils.toString( binding ) );
        listing.append( " [executionId: " ).append( binding.getExecutionId() ).append( ", phase: " );

        if ( ( binding.getPhase() != null ) && ( binding.getPhase().getName() != null ) )
        {
            listing.append( binding.getPhase().getName() );
        }
        else
        {
            listing.append( "None specified" );
        }

        listing.append( "]" );

        if ( extendedInfo )
        {
            listing.append( "\nOrigin: " ).append( binding.getOrigin() );
            listing.append( "\nOrigin Description: " ).append( binding.getOriginDescription() );
            listing.append( "\nConfiguration:\n\t" ).append(
                                                             String.valueOf( binding.getConfiguration() ).replaceAll(
                                                                                                                      "\\n",
                                                                                                                      "\n\t" ) ).append(
                                                                                                                                         '\n' );
        }

        return listing.toString();
    }

}
