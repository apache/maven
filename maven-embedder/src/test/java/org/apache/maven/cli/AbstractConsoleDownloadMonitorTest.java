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

import junit.framework.TestCase;

import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.providers.file.FileWagon;
import org.apache.maven.wagon.repository.Repository;
import org.apache.maven.wagon.resource.Resource;

/**
 * Test for {@link AbstractConsoleDownloadMonitor}
 * 
 * @author <a href="mailto:carlos@apache.org">Carlos Sanchez</a>
 * @version $Id$
 */
public abstract class AbstractConsoleDownloadMonitorTest
    extends TestCase
{
    protected AbstractConsoleDownloadMonitor monitor;

    public AbstractConsoleDownloadMonitor getMonitor()
    {
        return monitor;
    }

    public void testTransferInitiated()
        throws Exception
    {
        monitor.transferInitiated( new TransferEventMock() );
    }

    public void testTransferStarted()
        throws Exception
    {
        monitor.transferStarted( new TransferEventMock() );
    }

    public void testTransferProgress()
        throws Exception
    {
        byte[] buffer = new byte[1000];
        monitor.transferProgress( new TransferEventMock(), buffer, 1000 );
    }

    public void testTransferCompleted()
        throws Exception
    {
        monitor.transferCompleted( new TransferEventMock() );
    }

    public void testTransferError()
        throws Exception
    {
        monitor.transferError( new TransferEventMock( new RuntimeException() ) );
    }

    public void testDebug()
        throws Exception
    {
        monitor.debug( "msg" );
    }

    private class TransferEventMock
        extends TransferEvent
    {
        public TransferEventMock()
            throws ConnectionException, AuthenticationException
        {
            super( new FileWagon(), new Resource(), TransferEvent.TRANSFER_INITIATED, TransferEvent.REQUEST_GET );
            getResource().setContentLength( 100000 );
            Repository repository = new Repository();
            getWagon().connect( repository );
        }

        public TransferEventMock( Exception exception )
            throws ConnectionException, AuthenticationException
        {
            super( new FileWagon(), new Resource(), exception, TransferEvent.REQUEST_GET );
            getResource().setContentLength( 100000 );
            Repository repository = new Repository();
            getWagon().connect( repository );
        }
    }
}
