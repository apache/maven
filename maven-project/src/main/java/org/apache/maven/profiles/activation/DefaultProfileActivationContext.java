package org.apache.maven.profiles.activation;

import java.util.Properties;

public class DefaultProfileActivationContext
    implements ProfileActivationContext
{

    private boolean isCustomActivatorFailureSuppressed;
    private final Properties executionProperties;

    public DefaultProfileActivationContext( Properties executionProperties, boolean isCustomActivatorFailureSuppressed )
    {
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

}
