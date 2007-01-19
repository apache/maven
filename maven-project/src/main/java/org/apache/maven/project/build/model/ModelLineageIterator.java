package org.apache.maven.project.build.model;

import org.apache.maven.model.Model;

import java.io.File;
import java.util.Iterator;
import java.util.List;

/**
 * Iterator that gives access to all information associated with each model in a ModelLineage.
 * The result of the next() method is the Model instance itself, but you can also retrieve this
 * Model instance using getModel() below.
 * 
 * @author jdcasey
 */
public interface ModelLineageIterator
    extends Iterator
{
    
    /**
     * Retrieve the Model instance associated with the current position in the ModelLineage.
     * This is the same return value as the next() method.
     */
    Model getModel();
    
    /**
     * Retrieve the POM File associated with the current position in the ModelLineage
     */
    File getPOMFile();
    
    /**
     * Retrieve the remote ArtifactRepository instances associated with the current position 
     * in the ModelLineage.
     */
    List getArtifactRepositories();

}
