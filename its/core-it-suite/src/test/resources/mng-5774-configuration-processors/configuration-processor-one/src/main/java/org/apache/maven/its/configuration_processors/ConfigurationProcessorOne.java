package org.apache.maven.its.configuration_processors;

import org.apache.maven.cli.CliRequest;
import org.apache.maven.cli.configuration.ConfigurationProcessor;
import org.codehaus.plexus.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component( role = ConfigurationProcessor.class, hint = "maven-core-it-one" )
public class ConfigurationProcessorOne
    implements ConfigurationProcessor
{
    private Logger logger = LoggerFactory.getLogger( ConfigurationProcessorOne.class );

    public void process( CliRequest request )
        throws Exception
    {
        logger.info( "ConfigurationProcessorOne.process()" );
        request.getUserProperties().put( "answer", "yes" );
    }
}
