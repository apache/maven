package org.apache.maven.project.builder;

import java.io.IOException;
import java.util.List;

public interface Interpolator 
{

	String interpolateXmlString( String xml, List<InterpolatorProperty> interpolatorProperties )
		throws IOException;
		
	PomClassicDomainModel interpolateDomainModel( PomClassicDomainModel dm, List<InterpolatorProperty> interpolatorProperties ) 
		throws IOException ;
		
}
