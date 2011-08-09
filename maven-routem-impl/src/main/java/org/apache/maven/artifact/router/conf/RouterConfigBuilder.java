package org.apache.maven.artifact.router.conf;

import static org.apache.maven.artifact.router.conf.ArtifactRouterOption.*;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class RouterConfigBuilder
{

    private File routesFile;

    private RouterSource groupSource;

    private RouterSource mirrorSource;

    private String discoveryStrategy = ArtifactRouterConfiguration.NO_DISCOVERY_STRATEGIES;

    private String selectionStrategy = ArtifactRouterConfiguration.WEIGHTED_RANDOM_STRATEGY;

    private boolean disabled = false;

    private Set<ArtifactRouterOption> options = new HashSet<ArtifactRouterOption>();

    ArtifactRouterConfiguration build()
    {
        return new DefaultArtifactRouterConfiguration( routesFile, groupSource, mirrorSource, discoveryStrategy,
                                                       selectionStrategy, disabled, options );
    }

    RouterConfigBuilder withRoutesFile( File routesFile )
    {
        this.routesFile = routesFile;
        return this;
    }

    RouterConfigBuilder withGroupSource( RouterSource groupSource )
    {
        this.groupSource = groupSource;
        return this;
    }

    RouterConfigBuilder withMirrorSource( RouterSource mirrorSource )
    {
        this.mirrorSource = mirrorSource;
        return this;
    }

    RouterConfigBuilder withGroupSource( String id, String url )
    {
        this.groupSource = new RouterSource( id, url );
        return this;
    }

    RouterConfigBuilder withMirrorSource( String id, String url )
    {
        this.mirrorSource = new RouterSource( id, url );
        return this;
    }

    RouterConfigBuilder withDiscoveryStrategy( String discoveryStrategy )
    {
        this.discoveryStrategy = discoveryStrategy;
        return this;
    }
    
    RouterConfigBuilder withSelectionStrategy( String selectionStrategy )
    {
        this.selectionStrategy = selectionStrategy;
        return this;
    }
    
    RouterConfigBuilder disabled()
    {
        this.disabled = true;
        return this;
    }
    
    RouterConfigBuilder withEnabled( boolean enabled )
    {
        this.disabled = !enabled;
        return this;
    }
    
    RouterConfigBuilder withOffline( boolean enable )
    {
        return withOption( offline, enable );
    }
    
    RouterConfigBuilder withClear( boolean enable )
    {
        return withOption( clear, enable );
    }

    RouterConfigBuilder withUpdate( boolean enable )
    {
        return withOption( update, enable );
    }

    RouterConfigBuilder withOption( ArtifactRouterOption option, boolean enable )
    {
        if ( enable )
        {
            options.add( option );
        }
        else
        {
            options.remove( option );
        }
        
        return this;
    }

}
