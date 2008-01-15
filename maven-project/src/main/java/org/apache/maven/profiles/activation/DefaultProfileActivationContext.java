package org.apache.maven.profiles.activation;

import org.apache.maven.realm.MavenRealmManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class DefaultProfileActivationContext
    implements ProfileActivationContext
{

    private boolean isCustomActivatorFailureSuppressed;
    private final Properties executionProperties;
    List explicitlyActive;
    List explicitlyInactive;
    private final MavenRealmManager realmManager;
    private List activeByDefault;

    public DefaultProfileActivationContext( MavenRealmManager realmManager, Properties executionProperties, boolean isCustomActivatorFailureSuppressed )
    {
        this.realmManager = realmManager;
        this.executionProperties = executionProperties;
        this.isCustomActivatorFailureSuppressed = isCustomActivatorFailureSuppressed;
    }

    public DefaultProfileActivationContext( Properties executionProperties, boolean isCustomActivatorFailureSuppressed )
    {
        realmManager = null;
        this.executionProperties = executionProperties;
        this.isCustomActivatorFailureSuppressed = isCustomActivatorFailureSuppressed;
    }

    public Properties getExecutionProperties()
    {
        return executionProperties;
    }

    public boolean isCustomActivatorFailureSuppressed()
    {
        return isCustomActivatorFailureSuppressed;
    }

    public void setCustomActivatorFailureSuppressed( boolean suppressed )
    {
        isCustomActivatorFailureSuppressed = suppressed;
    }

    public List getExplicitlyActiveProfileIds()
    {
        if ( explicitlyActive == null )
        {
            return Collections.EMPTY_LIST;
        }

        return explicitlyActive;
    }

    public void setExplicitlyActiveProfileIds( List active )
    {
        explicitlyActive = active;
    }

    public List getExplicitlyInactiveProfileIds()
    {
        if ( explicitlyInactive == null )
        {
            return Collections.EMPTY_LIST;
        }

        return explicitlyInactive;
    }

    public void setExplicitlyInactiveProfileIds( List inactive )
    {
        explicitlyInactive = inactive;
    }

    public MavenRealmManager getRealmManager()
    {
        return realmManager;
    }

    public void setActive( String profileId )
    {
        if ( explicitlyActive == null )
        {
            explicitlyActive = new ArrayList();
        }

        explicitlyActive.add( profileId );
    }

    public void setInactive( String profileId )
    {
        if ( explicitlyInactive == null )
        {
            explicitlyInactive = new ArrayList();
        }

        explicitlyInactive.add( profileId );
    }

    public boolean isExplicitlyActive( String profileId )
    {
        return ( explicitlyActive != null ) && explicitlyActive.contains( profileId );
    }

    public boolean isExplicitlyInactive( String profileId )
    {
        return ( explicitlyInactive != null ) && explicitlyInactive.contains( profileId );
    }

    public List getActiveByDefaultProfileIds()
    {
        if ( activeByDefault == null )
        {
            return Collections.EMPTY_LIST;
        }

        return activeByDefault;
    }

    public boolean isActiveByDefault( String profileId )
    {
        return ( activeByDefault != null ) && activeByDefault.contains( profileId );
    }

    public void setActiveByDefault( String profileId )
    {
        if ( activeByDefault == null )
        {
            activeByDefault = new ArrayList();
        }

        activeByDefault.add( profileId );
    }

    public void setActiveByDefaultProfileIds( List activeByDefault )
    {
        this.activeByDefault = activeByDefault;
    }

}
