/*
 * Copyright (c) 2004 Your Corporation. All Rights Reserved.
 */
package org.apache.maven.archetype;

import org.apache.maven.artifact.repository.ArtifactRepository;

import java.util.Map;
import java.util.Set;
import java.io.File;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public interface Archetype
{
    String ROLE = Archetype.class.getName();

    String ARCHETYPE_DESCRIPTOR = "META-INF/archetype.xml";

    String ARCHETYPE_RESOURCES = "archetype-resources";

    String ARCHETYPE_POM = "pom.xml";

    void createArchetype( String archetypeId, ArtifactRepository localRepository, Set remoteRepositories, Map parameters )
        throws ArchetypeNotFoundException, ArchetypeDescriptorException, ArchetypeTemplateProcessingException;
}
