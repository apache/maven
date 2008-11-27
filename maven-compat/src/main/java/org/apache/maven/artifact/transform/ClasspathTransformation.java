package org.apache.maven.artifact.transform;

import org.apache.maven.artifact.ArtifactScopeEnum;
import org.apache.maven.artifact.resolver.metadata.MetadataGraph;


/**
 * Helper class to conver an Md Graph into some form of a classpath
 * 
 * @author <a href="oleg@codehaus.org">Oleg Gusakov</a>
 *
 */
public interface ClasspathTransformation
{
    String ROLE = ClasspathTransformation.class.getName();

    /**
     * Transform Graph into a Collection of metadata objects that
     * could serve as a classpath for a particular scope
     * 
     * @param dirtyGraph - dependency graph
     * @param scope - which classpath to extract
     * @param resolve - whether to resolve artifacts.
     * @return Collection of metadata objects in the linked subgraph of the graph which 
     *             contains the graph.getEntry() vertice
     */
    ClasspathContainer transform( MetadataGraph dirtyGraph,
                                  ArtifactScopeEnum scope,
                                  boolean resolve)
    throws MetadataGraphTransformationException;
}
