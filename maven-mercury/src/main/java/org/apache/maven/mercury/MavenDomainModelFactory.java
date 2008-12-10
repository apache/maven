package org.apache.maven.mercury;

import org.apache.maven.shared.model.DomainModelFactory;
import org.apache.maven.shared.model.DomainModel;
import org.apache.maven.shared.model.ModelProperty;

import java.util.List;
import java.io.IOException;

public class MavenDomainModelFactory
    implements DomainModelFactory
{

    public DomainModel createDomainModel( List<ModelProperty> modelProperties )
        throws IOException
    {
        return new MavenDomainModel( modelProperties );
    }
}
