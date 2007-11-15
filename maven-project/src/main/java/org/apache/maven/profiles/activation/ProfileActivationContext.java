package org.apache.maven.profiles.activation;

import java.util.Properties;

public interface ProfileActivationContext
{

    Properties getExecutionProperties();

    boolean isCustomActivatorFailureSuppressed();

    void setCustomActivatorFailureSuppressed( boolean suppressed );

}
