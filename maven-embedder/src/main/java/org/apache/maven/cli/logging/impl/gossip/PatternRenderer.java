package org.apache.maven.cli.logging.impl.gossip;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import com.planet57.gossip.Event;
import com.planet57.gossip.Level;

/**
 * Specialized {@link com.planet57.gossip.render.PatternRenderer} to cope with Maven specifics.
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @since 3.4.0
 */
public class PatternRenderer
    extends com.planet57.gossip.render.PatternRenderer
{
    protected static final String WARNING = "WARNING";

    @Override
    protected void renderLevel( final Event event, final StringBuilder buff )
    {
        assert event != null;
        assert buff != null;

        Level level = event.getLevel();

        // Maven uses WARNING instead of WARN
        if ( level == Level.WARN )
        {
            buff.append( WARNING );
        }
        else
        {
            buff.append( level.name() );
        }
    }
}
