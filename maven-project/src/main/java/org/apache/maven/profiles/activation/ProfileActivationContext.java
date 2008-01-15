package org.apache.maven.profiles.activation;

import org.apache.maven.realm.MavenRealmManager;

import java.util.List;
import java.util.Properties;

public interface ProfileActivationContext
{

    List getExplicitlyActiveProfileIds();

    List getExplicitlyInactiveProfileIds();

    MavenRealmManager getRealmManager();

    Properties getExecutionProperties();

    boolean isCustomActivatorFailureSuppressed();

    void setCustomActivatorFailureSuppressed( boolean suppressed );

    void setExplicitlyActiveProfileIds( List inactive );

    void setExplicitlyInactiveProfileIds( List inactive );

    void setActive( String profileId );

    void setInactive( String profileId );

    boolean isExplicitlyActive( String profileId );

    boolean isExplicitlyInactive( String profileId );

    List getActiveByDefaultProfileIds();

    void setActiveByDefaultProfileIds( List activeByDefault );

    void setActiveByDefault( String profileId );

    boolean isActiveByDefault( String profileId );

}
