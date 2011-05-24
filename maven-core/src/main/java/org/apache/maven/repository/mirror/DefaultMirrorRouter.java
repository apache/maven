/*
 * Copyright 2010 Red Hat, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.maven.repository.mirror;

import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.repository.automirror.MirrorRoute;
import org.apache.maven.repository.automirror.MirrorRoutingTable;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;

@Component( role = MirrorRouter.class )
final class DefaultMirrorRouter
    implements MirrorRouter
{

    @Requirement
    private Logger logger;

    @Requirement
    private LegacySupport legacySupport;

    protected Logger getLogger()
    {
        return logger;
    }

    public MirrorRoute getWeightedRandomSuggestion( final String canonicalUrl )
    {
        final MirrorRoutingTable routingTable = legacySupport.getSession().getMirrorRoutingTable();
        return routingTable.getWeightedRandomSuggestion( canonicalUrl );
    }

}
