package org.apache.maven.model.interpolator;

import java.io.IOException;
import java.util.List;

import org.apache.maven.model.PomClassicDomainModel;

public interface Interpolator 
{

	String interpolateXmlString( String xml, List<InterpolatorProperty> interpolatorProperties )
		throws IOException;
		
	PomClassicDomainModel interpolateDomainModel( PomClassicDomainModel dm, List<InterpolatorProperty> interpolatorProperties ) 
		throws IOException ;
		
}
