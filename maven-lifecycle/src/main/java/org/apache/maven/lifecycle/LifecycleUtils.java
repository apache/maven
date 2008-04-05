package org.apache.maven.lifecycle;

import org.apache.maven.lifecycle.model.BuildBinding;
import org.apache.maven.lifecycle.model.CleanBinding;
import org.apache.maven.lifecycle.model.LifecycleBinding;
import org.apache.maven.lifecycle.model.LifecycleBindings;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.apache.maven.lifecycle.model.Phase;
import org.apache.maven.lifecycle.model.SiteBinding;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class LifecycleUtils
{

    private LifecycleUtils()
    {
    }

    public static void setupTrackingInfo( final LifecycleBindings bindings )
    {
        for ( Iterator bindingIt = bindings.getBindingList().iterator(); bindingIt.hasNext(); )
        {
            LifecycleBinding binding = (LifecycleBinding) bindingIt.next();

            if ( binding == null )
            {
                continue;
            }

            LinkedHashMap phaseMap = binding.getOrderedPhaseMapping();
            for ( Iterator phaseIt = phaseMap.entrySet().iterator(); phaseIt.hasNext(); )
            {
                Map.Entry entry = (Entry) phaseIt.next();
                Phase phase = (Phase) entry.getValue();

                String phaseName = (String) entry.getKey();

                if ( phase != null )
                {
                    phase.setLifecycleInfo( phaseName, binding );
                }
            }
        }
    }

    public static void setOrigin( final LifecycleBindings bindings, final String origin )
    {
        for ( Iterator bindingIt = bindings.getBindingList().iterator(); bindingIt.hasNext(); )
        {
            LifecycleBinding binding = (LifecycleBinding) bindingIt.next();

            if ( binding == null )
            {
                continue;
            }

            for ( Iterator phaseIt = binding.getPhasesInOrder().iterator(); phaseIt.hasNext(); )
            {
                Phase phase = (Phase) phaseIt.next();

                if ( phase == null )
                {
                    continue;
                }

                for ( Iterator mojoIt = phase.getBindings().iterator(); mojoIt.hasNext(); )
                {
                    MojoBinding mojoBinding = (MojoBinding) mojoIt.next();

                    mojoBinding.setOrigin( origin );
                }
            }
        }
    }

    public static List getMojoBindingListForLifecycle( final String stopPhase, final LifecycleBindings bindings )
        throws NoSuchPhaseException
    {
        LifecycleBinding binding = findLifecycleBindingForPhase( stopPhase, bindings );

        if ( binding == null )
        {
            throw new NoSuchPhaseException( stopPhase, "Phase not found in any lifecycle." );
        }

        return getMojoBindingListForLifecycle( stopPhase, binding );
    }

    public static List getMojoBindingListForLifecycle( final String stopPhase, final LifecycleBinding lifecycle )
        throws NoSuchPhaseException
    {
        List phaseNames = lifecycle.getPhaseNamesInOrder();

        int idx = phaseNames.indexOf( stopPhase );

        if ( idx < 0 )
        {
            throw new NoSuchPhaseException( stopPhase, "Phase not found in lifecycle: " + lifecycle.getId() );
        }

        List phases = lifecycle.getPhasesInOrder();

        List bindings = new ArrayList();
        for ( int i = 0; i <= idx; i++ )
        {
            Phase phase = (Phase) phases.get( i );
            List phaseBindings = phase.getBindings();

            if ( ( phaseBindings != null ) && !phaseBindings.isEmpty() )
            {
                bindings.addAll( phaseBindings );
            }
        }

        return bindings;
    }

    /**
     * @return null if the phase is not contained in any of the lifecycles.
     */
    public static LifecycleBinding findLifecycleBindingForPhase( final String phaseName,
                                                                 final LifecycleBindings lifecycles )
    {
        List lifecyclesAvailable = lifecycles.getBindingList();

        for ( Iterator it = lifecyclesAvailable.iterator(); it.hasNext(); )
        {
            LifecycleBinding lifecycle = (LifecycleBinding) it.next();

            if ( lifecycle.getPhaseNamesInOrder().indexOf( phaseName ) > -1 )
            {
                return lifecycle;
            }
        }

        return null;
    }

    public static void removeMojoBinding( final String phaseName, final MojoBinding mojoBinding,
                                          final LifecycleBinding lifecycleBinding, final boolean considerExecutionId )
        throws NoSuchPhaseException
    {
        List phaseNames = lifecycleBinding.getPhaseNamesInOrder();

        int idx = phaseNames.indexOf( phaseName );

        if ( idx < 0 )
        {
            throw new NoSuchPhaseException( phaseName, "Phase: " + phaseName + " not found in lifecycle: "
                            + lifecycleBinding.getId() );
        }

        List phases = lifecycleBinding.getPhasesInOrder();

        Phase phase = (Phase) phases.get( idx );

        if ( phase != null )
        {
            List mojoBindings = phase.getBindings();

            String targetKey = MojoBindingUtils.createMojoBindingKey( mojoBinding, considerExecutionId );

            for ( Iterator it = mojoBindings.iterator(); it.hasNext(); )
            {
                MojoBinding candidate = (MojoBinding) it.next();

                String candidateKey = MojoBindingUtils.createMojoBindingKey( candidate, considerExecutionId );
                if ( candidateKey.equals( targetKey ) )
                {
                    it.remove();
                }
            }

            phase.setBindings( mojoBindings );
        }
    }

    public static void addMojoBinding( final String phaseName, final MojoBinding mojoBinding,
                                       final LifecycleBinding lifecycleBinding ) throws NoSuchPhaseException
    {
        List phaseNames = lifecycleBinding.getPhaseNamesInOrder();

        int idx = phaseNames.indexOf( phaseName );

        if ( idx < 0 )
        {
            throw new NoSuchPhaseException( phaseName, "Phase: " + phaseName + " not found in lifecycle: "
                            + lifecycleBinding.getId() );
        }

        List phases = lifecycleBinding.getPhasesInOrder();

        Phase phase = (Phase) phases.get( idx );
        phase.addBinding( mojoBinding );
        mojoBinding.setLifecycleInfo( phase );
    }

    public static void addMojoBinding( final String phaseName, final MojoBinding mojo, final LifecycleBindings bindings )
        throws LifecycleSpecificationException
    {
        LifecycleBinding binding = findLifecycleBindingForPhase( phaseName, bindings );

        if ( binding == null )
        {
            throw new NoSuchPhaseException( phaseName, "Phase not found in any lifecycle: " + phaseName );
        }

        addMojoBinding( phaseName, mojo, binding );
    }

    public static LifecycleBindings mergeBindings( final LifecycleBindings existingBindings,
                                                   final LifecycleBindings newBindings,
                                                   final LifecycleBindings defaultBindings,
                                                   final boolean mergeConfigIfExecutionIdMatches )
    {
        return mergeBindings( existingBindings, newBindings, defaultBindings, mergeConfigIfExecutionIdMatches, false );
    }

    public static LifecycleBindings mergeBindings( final LifecycleBindings existingBindings,
                                                   final LifecycleBindings newBindings,
                                                   final LifecycleBindings defaultBindings,
                                                   final boolean mergeConfigIfExecutionIdMatches,
                                                   final boolean reverseConfigMergeDirection )
    {
        LifecycleBindings result = new LifecycleBindings();
        result.setPackaging( newBindings.getPackaging() );

        CleanBinding cb = (CleanBinding) cloneBinding( existingBindings.getCleanBinding() );
        if ( ( defaultBindings != null ) && isNullOrEmpty( cb ) )
        {
            cb = (CleanBinding) cloneBinding( defaultBindings.getCleanBinding() );
        }

        if ( cb == null )
        {
            cb = new CleanBinding();
        }

        result.setCleanBinding( cb );

        BuildBinding bb = (BuildBinding) cloneBinding( existingBindings.getBuildBinding() );
        if ( ( defaultBindings != null ) && isNullOrEmpty( bb ) )
        {
            bb = (BuildBinding) cloneBinding( defaultBindings.getBuildBinding() );
        }

        if ( bb == null )
        {
            bb = new BuildBinding();
        }

        result.setBuildBinding( bb );

        SiteBinding sb = (SiteBinding) cloneBinding( existingBindings.getSiteBinding() );
        if ( ( defaultBindings != null ) && isNullOrEmpty( sb ) )
        {
            sb = (SiteBinding) cloneBinding( defaultBindings.getSiteBinding() );
        }

        if ( sb == null )
        {
            sb = new SiteBinding();
        }

        result.setSiteBinding( sb );

        for ( Iterator bindingIt = newBindings.getBindingList().iterator(); bindingIt.hasNext(); )
        {
            LifecycleBinding lifecycleBinding = (LifecycleBinding) bindingIt.next();

            if ( lifecycleBinding != null )
            {
                List phaseNames = lifecycleBinding.getPhaseNamesInOrder();
                List phases = lifecycleBinding.getPhasesInOrder();

                for ( int i = 0; i < phases.size(); i++ )
                {
                    Phase phase = (Phase) phases.get( i );
                    String name = (String) phaseNames.get( i );

                    if ( ( phase != null ) && ( phase.getBindings() != null ) && !phase.getBindings().isEmpty() )
                    {
                        for ( Iterator phaseIt = phase.getBindings().iterator(); phaseIt.hasNext(); )
                        {
                            MojoBinding mojoBinding = (MojoBinding) phaseIt.next();

                            mojoBinding = cloneMojoBinding( mojoBinding );

                            if ( mergeConfigIfExecutionIdMatches )
                            {
                                MojoBinding matchingBinding =
                                    findMatchingMojoBinding( mojoBinding, existingBindings, true );

                                if ( matchingBinding != null )
                                {
                                    Xpp3Dom existingConfig = new Xpp3Dom( (Xpp3Dom) matchingBinding.getConfiguration() );

                                    Xpp3Dom configuration;
                                    if ( reverseConfigMergeDirection )
                                    {
                                        configuration =
                                            Xpp3Dom.mergeXpp3Dom( existingConfig,
                                                                  (Xpp3Dom) mojoBinding.getConfiguration() );
                                    }
                                    else
                                    {
                                        configuration =
                                            Xpp3Dom.mergeXpp3Dom( (Xpp3Dom) mojoBinding.getConfiguration(),
                                                                  existingConfig );
                                    }

                                    mojoBinding.setConfiguration( configuration );

                                    if ( ( mojoBinding.getOrigin() == null ) && ( matchingBinding.getOrigin() != null ) )
                                    {
                                        mojoBinding.setOrigin( matchingBinding.getOrigin() );
                                    }

                                    LifecycleBinding resultBinding = findLifecycleBindingForPhase( name, result );

                                    try
                                    {
                                        removeMojoBinding( name, matchingBinding, resultBinding, true );
                                    }
                                    catch ( NoSuchPhaseException e )
                                    {
                                        IllegalStateException error =
                                            new IllegalStateException(
                                                                       e.getMessage()
                                                                                       + "\nSomething strange is going on. Merging should not encounter such inconsistencies." );

                                        error.initCause( e );

                                        throw error;
                                    }
                                }
                            }

                            try
                            {
                                addMojoBinding( name, mojoBinding, result );
                            }
                            catch ( LifecycleSpecificationException e )
                            {
                                // NOTE: this shouldn't happen as long as normal components are used
                                // to create/read these LifecycleBindings instances.
                                IllegalArgumentException error =
                                    new IllegalArgumentException( "Project bindings are invalid. Reason: "
                                                    + e.getMessage() );

                                error.initCause( e );

                                throw error;
                            }
                        }
                    }
                }
            }
        }

        return result;
    }

    private static boolean isNullOrEmpty( final LifecycleBinding binding )
    {
        if ( binding == null )
        {
            return true;
        }

        for ( Iterator it = binding.getPhasesInOrder().iterator(); it.hasNext(); )
        {
            Phase phase = (Phase) it.next();

            if ( !phase.getBindings().isEmpty() )
            {
                return false;
            }
        }

        return true;
    }

    public static MojoBinding findMatchingMojoBinding( final MojoBinding mojoBinding,
                                                       final LifecycleBindings inBindings,
                                                       final boolean considerExecutionId )
    {
        String key = MojoBindingUtils.createMojoBindingKey( mojoBinding, considerExecutionId );

        return (MojoBinding) mapMojoBindingsByKey( inBindings, considerExecutionId ).get( key );
    }

    private static Map mapMojoBindingsByKey( final LifecycleBindings bindings, final boolean considerExecutionId )
    {
        Map byKey = new HashMap();

        for ( Iterator bindingIt = bindings.getBindingList().iterator(); bindingIt.hasNext(); )
        {
            LifecycleBinding binding = (LifecycleBinding) bindingIt.next();

            if ( binding != null )
            {
                for ( Iterator phaseIt = binding.getPhasesInOrder().iterator(); phaseIt.hasNext(); )
                {
                    Phase phase = (Phase) phaseIt.next();

                    if ( phase != null )
                    {
                        for ( Iterator mojoIt = phase.getBindings().iterator(); mojoIt.hasNext(); )
                        {
                            MojoBinding mojoBinding = (MojoBinding) mojoIt.next();

                            byKey.put( MojoBindingUtils.createMojoBindingKey( mojoBinding, considerExecutionId ),
                                       mojoBinding );
                        }
                    }
                }
            }
        }

        return byKey;
    }

    public static void removeMojoBindings( final List toRemove, final LifecycleBindings bindings,
                                           final boolean considerExecutionId ) throws NoSuchPhaseException
    {
        if ( bindings.getCleanBinding() != null )
        {
            removeMojoBindings( toRemove, bindings.getCleanBinding(), considerExecutionId );
        }

        if ( bindings.getBuildBinding() != null )
        {
            removeMojoBindings( toRemove, bindings.getBuildBinding(), considerExecutionId );
        }

        if ( bindings.getSiteBinding() != null )
        {
            removeMojoBindings( toRemove, bindings.getSiteBinding(), considerExecutionId );
        }
    }

    public static void removeMojoBindings( final List toRemove, final LifecycleBinding removeFrom,
                                           final boolean considerExecutionId ) throws NoSuchPhaseException
    {
        // remove where gid:aid:goal matches.
        List targets = new ArrayList();
        for ( Iterator it = toRemove.iterator(); it.hasNext(); )
        {
            MojoBinding binding = (MojoBinding) it.next();

            targets.add( MojoBindingUtils.createMojoBindingKey( binding, considerExecutionId ) );
        }

        List phases = removeFrom.getPhasesInOrder();

        for ( int i = 0; i < phases.size(); i++ )
        {
            Phase phase = (Phase) phases.get( i );
            List phaseBindings = phase.getBindings();

            for ( Iterator mojoIt = phaseBindings.iterator(); mojoIt.hasNext(); )
            {
                MojoBinding binding = (MojoBinding) mojoIt.next();
                String key = MojoBindingUtils.createMojoBindingKey( binding, considerExecutionId );
                if ( targets.contains( key ) )
                {
                    mojoIt.remove();
                }
            }

            phase.setBindings( phaseBindings );
        }
    }

    public static LifecycleBindings cloneBindings( final LifecycleBindings bindings )
    {
        LifecycleBindings result = new LifecycleBindings();

        if ( bindings.getCleanBinding() != null )
        {
            result.setCleanBinding( (CleanBinding) cloneBinding( bindings.getCleanBinding() ) );
        }

        if ( bindings.getBuildBinding() != null )
        {
            result.setBuildBinding( (BuildBinding) cloneBinding( bindings.getBuildBinding() ) );
        }

        if ( bindings.getSiteBinding() != null )
        {
            result.setSiteBinding( (SiteBinding) cloneBinding( bindings.getSiteBinding() ) );
        }

        return result;
    }

    public static LifecycleBinding cloneBinding( final LifecycleBinding binding )
    {
        if ( binding == null )
        {
            return null;
        }

        LifecycleBinding result;
        if ( binding instanceof CleanBinding )
        {
            result = new CleanBinding();
        }
        else if ( binding instanceof SiteBinding )
        {
            result = new SiteBinding();
        }
        else if ( binding instanceof BuildBinding )
        {
            result = new BuildBinding();
        }
        else
        {
            throw new IllegalArgumentException( "Unrecognized LifecycleBinding type: " + binding.getClass().getName()
                            + "; cannot clone." );
        }

        List phases = binding.getPhasesInOrder();
        List names = binding.getPhaseNamesInOrder();

        for ( int i = 0; i < phases.size(); i++ )
        {
            Phase phase = (Phase) phases.get( i );
            String phaseName = (String) names.get( i );

            for ( Iterator mojoIt = phase.getBindings().iterator(); mojoIt.hasNext(); )
            {
                MojoBinding originalBinding = (MojoBinding) mojoIt.next();

                MojoBinding newBinding = cloneMojoBinding( originalBinding );

                try
                {
                    addMojoBinding( phaseName, newBinding, result );
                }
                catch ( NoSuchPhaseException e )
                {
                    IllegalStateException error =
                        new IllegalStateException(
                                                   e.getMessage()
                                                                   + "\nSomething strange is going on. Cloning should not encounter such inconsistencies." );

                    error.initCause( e );

                    throw error;
                }
            }
        }

        return result;
    }

    public static MojoBinding cloneMojoBinding( final MojoBinding binding )
    {
        MojoBinding result = new MojoBinding();

        result.setGroupId( binding.getGroupId() );
        result.setArtifactId( binding.getArtifactId() );
        result.setVersion( binding.getVersion() );
        result.setConfiguration( binding.getConfiguration() );
        result.setExecutionId( binding.getExecutionId() );
        result.setGoal( binding.getGoal() );
        result.setOrigin( binding.getOrigin() );

        return result;
    }

    public static Phase findPhaseForMojoBinding( final MojoBinding mojoBinding,
                                                 final LifecycleBindings lifecycleBindings,
                                                 final boolean considerExecutionId )
    {
        String targetKey = MojoBindingUtils.createMojoBindingKey( mojoBinding, considerExecutionId );

        for ( Iterator lifecycleIt = lifecycleBindings.getBindingList().iterator(); lifecycleIt.hasNext(); )
        {
            LifecycleBinding binding = (LifecycleBinding) lifecycleIt.next();

            for ( Iterator phaseIt = binding.getPhasesInOrder().iterator(); phaseIt.hasNext(); )
            {
                Phase phase = (Phase) phaseIt.next();

                for ( Iterator mojoIt = phase.getBindings().iterator(); mojoIt.hasNext(); )
                {
                    MojoBinding candidate = (MojoBinding) mojoIt.next();
                    String key = MojoBindingUtils.createMojoBindingKey( candidate, considerExecutionId );
                    if ( key.equals( targetKey ) )
                    {
                        return phase;
                    }
                }
            }
        }

        return null;
    }

    public static boolean isMojoBindingPresent( final MojoBinding binding, final List candidates,
                                                final boolean considerExecutionId )
    {
        String key = MojoBindingUtils.createMojoBindingKey( binding, considerExecutionId );

        for ( Iterator it = candidates.iterator(); it.hasNext(); )
        {
            MojoBinding candidate = (MojoBinding) it.next();

            String candidateKey = MojoBindingUtils.createMojoBindingKey( candidate, considerExecutionId );

            if ( candidateKey.equals( key ) )
            {
                return true;
            }
        }

        return false;
    }

    public static boolean isValidPhaseName( final String phaseName )
    {
        LifecycleBindings test = new LifecycleBindings();
        for ( Iterator it = test.getBindingList().iterator(); it.hasNext(); )
        {
            LifecycleBinding binding = (LifecycleBinding) it.next();

            if ( binding.getPhaseNamesInOrder().contains( phaseName ) )
            {
                return true;
            }
        }

        return false;
    }

    public static List getValidPhaseNames()
    {
        List phaseNames = new ArrayList();

        LifecycleBindings bindings = new LifecycleBindings();
        for ( Iterator bindingIt = bindings.getBindingList().iterator(); bindingIt.hasNext(); )
        {
            LifecycleBinding binding = (LifecycleBinding) bindingIt.next();

            for ( Iterator phaseNameIt = binding.getPhaseNamesInOrder().iterator(); phaseNameIt.hasNext(); )
            {
                phaseNames.add( phaseNameIt.next() );
            }
        }

        return phaseNames;
    }

    public static List getValidBuildPhaseNames()
    {
        List phaseNames = new ArrayList();

        LifecycleBinding binding = new BuildBinding();

        for ( Iterator phaseNameIt = binding.getPhaseNamesInOrder().iterator(); phaseNameIt.hasNext(); )
        {
            phaseNames.add( phaseNameIt.next() );
        }

        return phaseNames;
    }

    public static List getValidCleanPhaseNames()
    {
        List phaseNames = new ArrayList();

        LifecycleBinding binding = new CleanBinding();

        for ( Iterator phaseNameIt = binding.getPhaseNamesInOrder().iterator(); phaseNameIt.hasNext(); )
        {
            phaseNames.add( phaseNameIt.next() );
        }

        return phaseNames;
    }

    public static List getValidSitePhaseNames()
    {
        List phaseNames = new ArrayList();

        LifecycleBinding binding = new SiteBinding();

        for ( Iterator phaseNameIt = binding.getPhaseNamesInOrder().iterator(); phaseNameIt.hasNext(); )
        {
            phaseNames.add( phaseNameIt.next() );
        }

        return phaseNames;
    }

    /**
     * @deprecated Use {@link MojoBindingUtils#createMojoBindingKey(MojoBinding, boolean)} instead.
     */
    public static String createMojoBindingKey( final MojoBinding mojoBinding, final boolean considerExecutionId )
    {
        return MojoBindingUtils.createMojoBindingKey( mojoBinding, considerExecutionId );
    }
}
