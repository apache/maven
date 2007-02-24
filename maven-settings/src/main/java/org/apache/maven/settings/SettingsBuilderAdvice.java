package org.apache.maven.settings;

public class SettingsBuilderAdvice
{

    private boolean defaultUserLocationEnabled = true;

    private boolean defaultGlobalLocationEnabled = true;

    public boolean isDefaultGlobalLocationEnabled()
    {
        return defaultGlobalLocationEnabled;
    }

    public boolean isDefaultUserLocationEnabled()
    {
        return defaultUserLocationEnabled;
    }

    public void setDefaultGlobalLocationEnabled( boolean defaultGlobalLocationEnabled )
    {
        this.defaultGlobalLocationEnabled = defaultGlobalLocationEnabled;
    }

    public void setDefaultUserLocationEnabled( boolean defaultUserLocationEnabled )
    {
        this.defaultUserLocationEnabled = defaultUserLocationEnabled;
    }

}
