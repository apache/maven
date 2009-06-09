package org.apache.maven.cli;

import org.apache.maven.wagon.resource.Resource;
import org.apache.maven.wagon.WagonConstants;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.PrintStream;

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

/**
 * Test for {@link ConsoleDownloadMonitor}
 * 
 * @author <a href="mailto:carlos@apache.org">Carlos Sanchez</a>
 * @version $Id$
 */
public class ConsoleDownloadMonitorTest
    extends AbstractConsoleDownloadMonitorTest
{
    ByteArrayOutputStream bout;
    protected void setUp()
        throws Exception
    {
        super.setMonitor( new ConsoleDownloadMonitor() );
        super.setUp();
        bout = new ByteArrayOutputStream();
        monitor.out = new PrintStream(bout);
    }

    public void testTransferProgress()
        throws Exception
    {
        byte[] buffer = new byte[1024];
        monitor.transferProgress( new TransferEventMock(new Resource(), 10000), buffer, 1024 );
        assertEquals("1/9K\r", new String(bout.toByteArray()));
    }

    public void testTransferProgressTwoFiles()
        throws Exception
    {
        byte[] buffer = new byte[2048];
        monitor.transferProgress( new TransferEventMock(new Resource("foo"), 10000), buffer, 1024 );
        assertEquals("1/9K\r", new String(bout.toByteArray()));
        bout.reset();
        monitor.transferProgress( new TransferEventMock(new Resource("bar"), 10000), buffer, 2048 );
        assertEquals("1/9K 2/9K\r", new String(bout.toByteArray()));
        bout.reset();
        monitor.transferProgress( new TransferEventMock(new Resource("bar"), 10000), buffer, 2048 );
        assertEquals("1/9K 4/9K\r", new String(bout.toByteArray()));
        bout.reset();
        monitor.transferProgress( new TransferEventMock(new Resource("foo"), 10000), buffer, 2048 );
        assertEquals("3/9K 4/9K\r", new String(bout.toByteArray()));
    }

    public void testGetDownloadStatusForResource() 
    {
        ConsoleDownloadMonitor cm = (ConsoleDownloadMonitor)monitor;
        assertEquals("200/400b", cm.getDownloadStatusForResource(200, 400));
        assertEquals("1/2K", cm.getDownloadStatusForResource(1024, 2048));
        assertEquals("0/2K", cm.getDownloadStatusForResource(10, 2048));
        assertEquals("10/?", cm.getDownloadStatusForResource(10, WagonConstants.UNKNOWN_LENGTH));
        assertEquals("1024/?", cm.getDownloadStatusForResource(1024, WagonConstants.UNKNOWN_LENGTH));
    }
}
