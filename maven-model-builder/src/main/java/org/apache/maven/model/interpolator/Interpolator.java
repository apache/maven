package org.apache.maven.model.interpolator;

import java.io.IOException;
import java.util.Properties;

import org.apache.maven.model.PomClassicDomainModel;

public interface Interpolator 
{
    PomClassicDomainModel interpolateDomainModel( PomClassicDomainModel dm, Properties interpolatorProperties )
        throws IOException;
}
