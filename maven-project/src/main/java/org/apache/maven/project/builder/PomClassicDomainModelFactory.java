package org.apache.maven.project.builder;

import org.apache.maven.shared.model.DomainModelFactory;
import org.apache.maven.shared.model.DomainModel;
import org.apache.maven.shared.model.ModelProperty;
import org.apache.maven.shared.model.ModelMarshaller;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.util.List;
import java.io.IOException;
import java.io.StringReader;

public class PomClassicDomainModelFactory implements DomainModelFactory
{
    public DomainModel createDomainModel(List<ModelProperty> modelProperties) throws IOException
    {
        String xml = null;
        try
        {
            xml = ModelMarshaller.unmarshalModelPropertiesToXml( modelProperties, ProjectUri.baseUri );
            return new PomClassicDomainModel( new MavenXpp3Reader().read( new StringReader( xml ) ) );
        }
        catch ( XmlPullParserException e )
        {
            throw new IOException( e + ":\r\n" + xml );
        }
    }
}
