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
package org.apache.maven.repository;

import java.io.File;
import java.util.EventObject;

/**
 * TransferEvent is used to notify TransferListeners about progress
 * in transfer of resources form/to the repository
 *
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 */
public class ArtifactTransferEvent extends EventObject {
    /**
     * A transfer was attempted, but has not yet commenced.
     */
    public static final int TRANSFER_INITIATED = 0;

    /**
     * A transfer was started.
     */
    public static final int TRANSFER_STARTED = 1;

    /**
     * A transfer is completed.
     */
    public static final int TRANSFER_COMPLETED = 2;

    /**
     * A transfer is in progress.
     */
    public static final int TRANSFER_PROGRESS = 3;

    /**
     * An error occurred during transfer
     */
    public static final int TRANSFER_ERROR = 4;

    /**
     * Indicates GET transfer  (from the repository)
     */
    public static final int REQUEST_GET = 5;

    /**
     * Indicates PUT transfer (to the repository)
     */
    public static final int REQUEST_PUT = 6;

    private int eventType;

    private int requestType;

    private Exception exception;

    private File localFile;

    private ArtifactTransferResource artifact;

    private long transferredBytes;

    private byte[] dataBuffer;

    private int dataOffset;

    private int dataLength;

    public ArtifactTransferEvent(
            String wagon, final int eventType, final int requestType, ArtifactTransferResource artifact) {
        super(wagon);

        setEventType(eventType);

        setRequestType(requestType);

        this.artifact = artifact;
    }

    public ArtifactTransferEvent(
            String wagon, final Exception exception, final int requestType, ArtifactTransferResource artifact) {
        this(wagon, TRANSFER_ERROR, requestType, artifact);

        this.exception = exception;
    }

    public ArtifactTransferResource getResource() {
        return artifact;
    }

    /**
     * @return Returns the exception.
     */
    public Exception getException() {
        return exception;
    }

    /**
     * Returns the request type.
     *
     * @return Returns the request type. The Request type is one of
     *         <code>TransferEvent.REQUEST_GET</code> or <code>TransferEvent.REQUEST_PUT</code>
     */
    public int getRequestType() {
        return requestType;
    }

    /**
     * Sets the request type
     *
     * @param requestType The requestType to set.
     *                    The Request type value should be either
     *                    <code>TransferEvent.REQUEST_GET</code> or <code>TransferEvent.REQUEST_PUT</code>.
     * @throws IllegalArgumentException when
     */
    public void setRequestType(final int requestType) {
        switch (requestType) {
            case REQUEST_PUT:
                break;
            case REQUEST_GET:
                break;
            default:
                throw new IllegalArgumentException("Illegal request type: " + requestType);
        }

        this.requestType = requestType;
    }

    /**
     * @return Returns the eventType.
     */
    public int getEventType() {
        return eventType;
    }

    /**
     * @param eventType The eventType to set.
     */
    public void setEventType(final int eventType) {
        switch (eventType) {
            case TRANSFER_INITIATED:
                break;
            case TRANSFER_STARTED:
                break;
            case TRANSFER_COMPLETED:
                break;
            case TRANSFER_PROGRESS:
                break;
            case TRANSFER_ERROR:
                break;
            default:
                throw new IllegalArgumentException("Illegal event type: " + eventType);
        }

        this.eventType = eventType;
    }

    /**
     * @return Returns the local file.
     */
    public File getLocalFile() {
        return localFile;
    }

    /**
     * @param localFile The local file to set.
     */
    public void setLocalFile(File localFile) {
        this.localFile = localFile;
    }

    public long getTransferredBytes() {
        return transferredBytes;
    }

    public void setTransferredBytes(long transferredBytes) {
        this.transferredBytes = transferredBytes;
    }

    public byte[] getDataBuffer() {
        return dataBuffer;
    }

    public void setDataBuffer(byte[] dataBuffer) {
        this.dataBuffer = dataBuffer;
    }

    public int getDataOffset() {
        return dataOffset;
    }

    public void setDataOffset(int dataOffset) {
        this.dataOffset = dataOffset;
    }

    public int getDataLength() {
        return dataLength;
    }

    public void setDataLength(int dataLength) {
        this.dataLength = dataLength;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(64);

        sb.append("TransferEvent[");

        switch (this.getRequestType()) {
            case REQUEST_GET:
                sb.append("GET");
                break;
            case REQUEST_PUT:
                sb.append("PUT");
                break;
            default:
                sb.append(this.getRequestType());
                break;
        }

        sb.append('|');
        switch (this.getEventType()) {
            case TRANSFER_COMPLETED:
                sb.append("COMPLETED");
                break;
            case TRANSFER_ERROR:
                sb.append("ERROR");
                break;
            case TRANSFER_INITIATED:
                sb.append("INITIATED");
                break;
            case TRANSFER_PROGRESS:
                sb.append("PROGRESS");
                break;
            case TRANSFER_STARTED:
                sb.append("STARTED");
                break;
            default:
                sb.append(this.getEventType());
                break;
        }

        sb.append('|');
        sb.append(this.getLocalFile()).append('|');
        sb.append(']');

        return sb.toString();
    }

    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + eventType;
        result = prime * result + ((exception == null) ? 0 : exception.hashCode());
        result = prime * result + ((localFile == null) ? 0 : localFile.hashCode());
        result = prime * result + requestType;
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }
        final ArtifactTransferEvent other = (ArtifactTransferEvent) obj;
        if (eventType != other.eventType) {
            return false;
        }
        if (exception == null) {
            if (other.exception != null) {
                return false;
            }
        } else if (!exception.getClass().equals(other.exception.getClass())) {
            return false;
        }
        if (requestType != other.requestType) {
            return false;
        } else if (!source.equals(other.source)) {
            return false;
        }
        return true;
    }
}
