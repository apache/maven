package org.apache.maven.archetype;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.repository.ArtifactRepository;

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

    void createArchetype( String archetypeGroupId, String archetypeArtifactId, String archetypeVersion,
                          ArtifactRepository localRepository, List remoteRepositories, Map parameters )
        throws ArchetypeNotFoundException, ArchetypeDescriptorException, ArchetypeTemplateProcessingException;
}
