package org.apache.maven.lifecycle.plan;

import org.apache.maven.lifecycle.LifecycleLoaderException;
import org.apache.maven.lifecycle.LifecycleSpecificationException;
import org.apache.maven.lifecycle.LifecycleUtils;
import org.apache.maven.lifecycle.MojoBindingUtils;
import org.apache.maven.lifecycle.binding.LifecycleBindingManager;
import org.apache.maven.lifecycle.model.LifecycleBindings;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.apache.maven.lifecycle.statemgmt.StateManagementUtils;
import org.apache.maven.project.MavenProject;

import java.util.Iterator;
import java.util.List;

/**
 * Collection of static utility methods used to work with LifecycleBindings and other collections
 * of MojoBinding instances that make up a build plan.
 */
public final class BuildPlanUtils
{

    private BuildPlanUtils()
    {
    }

    /**
     * Inject a set of {@link BuildPlanModifier} instances into an existing LifecycleBindings instance.
     * This is a generalization of a piece of code present in almost all scenarios where a build
     * plan contains modifiers and is asked to produce an effective list of MojoBinding instances
     * that make up the build process. Simply iterate through the modifiers, and apply each one,
     * replacing the previous LifecycleBindings instance with the result of the current modifier.
     */
    public static LifecycleBindings modifyPlanBindings( LifecycleBindings bindings, List planModifiers )
        throws LifecyclePlannerException
    {
        LifecycleBindings result;

        // if the bindings are completely empty, passing in null avoids an extra instance creation 
        // for the purposes of cloning...
        if ( bindings != null )
        {
            result = LifecycleUtils.cloneBindings( bindings );
        }
        else
        {
            result = new LifecycleBindings();
        }

        for ( Iterator it = planModifiers.iterator(); it.hasNext(); )
        {
            BuildPlanModifier modifier = (BuildPlanModifier) it.next();

            result = modifier.modifyBindings( result );
        }

        return result;
    }

    /**
     * Render an entire build plan to a String.
     * If extendedInfo == true, include each MojoBinding's configuration in the output.
     */
    public static String listBuildPlan( BuildPlan plan, MavenProject project, LifecycleBindingManager lifecycleBindingManager, boolean extendedInfo )
        throws LifecycleSpecificationException, LifecyclePlannerException, LifecycleLoaderException
    {
        List mojoBindings = plan.getPlanMojoBindings( project, lifecycleBindingManager );

        return listBuildPlan( mojoBindings, extendedInfo );
    }

    /**
     * Render a list containing the MojoBinding instances for an entire build plan to a String.
     * If extendedInfo == true, include each MojoBinding's configuration in the output.
     */
    public static String listBuildPlan( List mojoBindings, boolean extendedInfo )
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
     * Append a newline character, add the next line's number, and indent the new line to the
     * appropriate level (which tracks separate forked executions).
     */
    private static void newListingLine( StringBuffer listing, int indentLevel, int counter )
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
     * Format a single MojoBinding for inclusion in a build plan listing. If extendedInfo == true,
     * include the MojoBinding's configuration in the output.
     */
    public static String formatMojoListing( MojoBinding binding, int indentLevel, boolean extendedInfo )
    {
        StringBuffer listing = new StringBuffer();

        listing.append( MojoBindingUtils.toString( binding ) );
        listing.append( " [executionId: " ).append( binding.getExecutionId() ).append( "]" );

        if ( extendedInfo )
        {
            listing.append( "\nOrigin: " ).append( binding.getOrigin() );
            listing.append( "\nConfiguration:\n\t" ).append(
                                                             String.valueOf( binding.getConfiguration() ).replaceAll( "\\n",
                                                                                                                      "\n\t" ) ).append(
                                                                                                                                         '\n' );
        }

        return listing.toString();
    }

}
