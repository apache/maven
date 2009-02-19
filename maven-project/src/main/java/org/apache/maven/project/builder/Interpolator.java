package org.apache.maven.project.builder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Model;
import org.apache.maven.shared.model.InterpolatorProperty;
import org.apache.maven.shared.model.ModelMarshaller;
import org.apache.maven.shared.model.ModelProperty;
import org.apache.maven.shared.model.ModelTransformerContext;

public class Interpolator
{

    // Only used by the plugin manager
    public static String interpolateXmlString( String xml, List<InterpolatorProperty> interpolatorProperties )
        throws IOException
    {
        List<ModelProperty> modelProperties = ModelMarshaller.marshallXmlToModelProperties( new ByteArrayInputStream( xml.getBytes() ), ProjectUri.baseUri, PomTransformer.URIS );

        Map<String, String> aliases = new HashMap<String, String>();
        aliases.put( "project.", "pom." );

        List<InterpolatorProperty> ips = new ArrayList<InterpolatorProperty>( interpolatorProperties );
        ips.addAll( ModelTransformerContext.createInterpolatorProperties( modelProperties, ProjectUri.baseUri, aliases, PomInterpolatorTag.PROJECT_PROPERTIES.name(), false, false ) );

        for ( ModelProperty mp : modelProperties )
        {
            if ( mp.getUri().startsWith( ProjectUri.properties ) && mp.getValue() != null )
            {
                String uri = mp.getUri();
                ips.add( new InterpolatorProperty( "${" + uri.substring( uri.lastIndexOf( "/" ) + 1, uri.length() ) + "}", mp.getValue() ) );
            }
        }

        ModelTransformerContext.interpolateModelProperties( modelProperties, ips );
        return ModelMarshaller.unmarshalModelPropertiesToXml( modelProperties, ProjectUri.baseUri );
    }
}