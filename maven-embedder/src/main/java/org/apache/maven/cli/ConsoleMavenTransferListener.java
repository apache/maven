package org.apache.maven.cli;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.repository.ArtifactTransferEvent;

/**
 * Console download progress meter.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class ConsoleMavenTransferListener
    extends AbstractMavenTransferListener
{

    @Override
    protected void doProgress( ArtifactTransferEvent transferEvent )
    {
        long total = transferEvent.getResource().getContentLength();
        long complete = transferEvent.getTransferredBytes();

        // TODO [BP]: Sys.out may no longer be appropriate, but will \r work with getLogger()?
        if ( total >= 1024 )
        {
            System.out.print( ( complete / 1024 ) + "/" + ( total == -1 ? "?" : ( total / 1024 ) + " KB" ) + "\r" );
        }
        else
        {
            System.out.print( complete + "/" + ( total == -1 ? "?" : total + " B" ) + "\r" );
        }
    }

}
