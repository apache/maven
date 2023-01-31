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
package org.apache.maven.artifact.resolver;

import java.io.File;
import java.io.InputStream;

import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.wagon.providers.file.FileWagon;
import org.apache.maven.wagon.resource.Resource;

/**
 * Wagon used for test cases that annotates some methods. Note that this is not a thread-safe implementation.
 */
public class TestFileWagon extends FileWagon {
    private TestTransferListener testTransferListener;
    private boolean insideGet;

    protected void getTransfer(Resource resource, File destination, InputStream input, boolean closeInput, int maxSize)
            throws TransferFailedException {
        addTransfer("getTransfer " + resource.getName());
        super.getTransfer(resource, destination, input, closeInput, maxSize);
    }

    public void get(String resourceName, File destination)
            throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        addTransfer("get " + resourceName);

        insideGet = true;

        super.get(resourceName, destination);

        insideGet = false;
    }

    private void addTransfer(String resourceName) {
        if (testTransferListener != null) {
            testTransferListener.addTransfer(resourceName);
        }
    }

    public boolean getIfNewer(String resourceName, File destination, long timestamp)
            throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        if (!insideGet) {
            addTransfer("getIfNewer " + resourceName);
        }
        return super.getIfNewer(resourceName, destination, timestamp);
    }

    public void addTransferListener(TransferListener listener) {
        if (listener instanceof TestTransferListener) {
            testTransferListener = (TestTransferListener) listener;
        }
        super.addTransferListener(listener);
    }
}
