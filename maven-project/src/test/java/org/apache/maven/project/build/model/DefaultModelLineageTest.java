package org.apache.maven.project.build.model;

import org.apache.maven.project.build.model.DefaultModelLineage;
import org.apache.maven.project.build.model.ModelLineage;


public class DefaultModelLineageTest
    extends AbstractModelLineageTest
{

    protected ModelLineage newModelLineage()
    {
        return new DefaultModelLineage();
    }

}
