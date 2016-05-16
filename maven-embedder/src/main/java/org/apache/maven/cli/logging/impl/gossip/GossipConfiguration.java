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

import org.sonatype.gossip.Gossip;

import org.apache.maven.cli.logging.BaseSlf4jConfiguration;

/**
 * Configuration for Gossip.
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @since 3.4.0
 */
public class GossipConfiguration
    extends BaseSlf4jConfiguration
{
    @Override
    public void setRootLoggerLevel( Level level )
    {
        org.sonatype.gossip.Level value;
        switch ( level )
        {
            case DEBUG:
                value = org.sonatype.gossip.Level.DEBUG;
                break;

            case INFO:
                value = org.sonatype.gossip.Level.INFO;
                break;

            default:
                value = org.sonatype.gossip.Level.ERROR;
                break;
        }
        Gossip.getInstance().getRoot().setLevel( value );
    }

    @Override
    public void activate()
    {
        // empty
    }
}
