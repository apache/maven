package org.apache.maven.embedder;

/** @author Jason van Zyl */
public class DefaultConfigurationValidationResult
    implements ConfigurationValidationResult
{
    private boolean userSettingsFilePresent = true;

    private boolean userSettingsFileParses = true;

    private boolean globalSettingsFilePresent = true;

    private boolean globalSettingsFileParses = true;

    public boolean isValid()
    {
        return userSettingsFilePresent && userSettingsFileParses && globalSettingsFilePresent &&
            globalSettingsFileParses;
    }

    public boolean isUserSettingsFilePresent()
    {
        return userSettingsFilePresent;
    }

    public void setUserSettingsFilePresent( boolean userSettingsFilePresent )
    {
        this.userSettingsFilePresent = userSettingsFilePresent;
    }

    public boolean isUserSettingsFileParses()
    {
        return userSettingsFileParses;
    }

    public void setUserSettingsFileParses( boolean userSettingsFileParses )
    {
        this.userSettingsFileParses = userSettingsFileParses;
    }

    public boolean isGlobalSettingsFilePresent()
    {
        return globalSettingsFilePresent;
    }

    public void setGlobalSettingsFilePresent( boolean globalSettingsFilePresent )
    {
        this.globalSettingsFilePresent = globalSettingsFilePresent;
    }

    public boolean isGlobalSettingsFileParses()
    {
        return globalSettingsFileParses;
    }

    public void setGlobalSettingsFileParses( boolean globalSettingsFileParses )
    {
        this.globalSettingsFileParses = globalSettingsFileParses;
    }

    public void display()
    {
        System.out.println( "userSettingsFilePresent = " + userSettingsFilePresent );
        System.out.println( "globalSettingsFileParses = " + globalSettingsFileParses );
        System.out.println( "globalSettingsFilePresent = " + globalSettingsFilePresent );
        System.out.println( "globalSettingsFileParses = " + globalSettingsFileParses );
    }
}
