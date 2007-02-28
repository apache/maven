package org.apache.maven.embedder.configuration;

/** @author Jason van Zyl */
public interface ConfigurationValidationResult
{
    boolean isValid();

    boolean isUserSettingsFilePresent();

    void setUserSettingsFilePresent( boolean userSettingsFilePresent );

    boolean isUserSettingsFileParses();

    void setUserSettingsFileParses( boolean userSettingsFileParses );

    boolean isGlobalSettingsFilePresent();

    void setGlobalSettingsFilePresent( boolean globalSettingsFilePresent );

    boolean isGlobalSettingsFileParses();

    void setGlobalSettingsFileParses( boolean globalSettingsFileParses );

    void display();
}
