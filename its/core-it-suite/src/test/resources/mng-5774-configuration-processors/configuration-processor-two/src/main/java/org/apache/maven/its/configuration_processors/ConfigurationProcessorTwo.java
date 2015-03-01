package org.apache.maven.its.configuration_processors;

import org.apache.maven.cli.CliRequest;
import org.apache.maven.cli.configuration.ConfigurationProcessor;
import org.codehaus.plexus.component.annotations.Component;

@Component( role = ConfigurationProcessor.class, hint = "maven-core-it-two" )
public class ConfigurationProcessorTwo
    implements ConfigurationProcessor
{
    public void process( CliRequest request )
        throws Exception
    {
    }
}
