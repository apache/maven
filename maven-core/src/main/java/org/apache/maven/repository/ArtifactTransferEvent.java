package org.apache.maven.repository;

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


import java.io.File;
import java.util.EventObject;

import org.apache.maven.repository.Event.Initiated;
import org.apache.maven.repository.Event.Started;
import org.apache.maven.repository.Event.Completed;      
import org.apache.maven.repository.Event.Error;
import org.apache.maven.repository.Event.Progress;
import org.apache.maven.repository.Event.Get;
import org.apache.maven.repository.Event.Put;






/**
 * TransferEvent is used to notify TransferListeners about progress
 * in transfer of resources form/to the repository
 *
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 */
public class ArtifactTransferEvent
    extends EventObject
{
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

    // private int eventType;

    // private int requestType;
    private Event event;
    private Event request;

    private Exception exception;

    private File localFile;

    private ArtifactTransferResource artifact;

    private long transferredBytes;

    private byte[] dataBuffer;

    private int dataOffset;

    private int dataLength;
   

    public ArtifactTransferEvent( String wagon, final int eventType, final int requestType,
                                  ArtifactTransferResource artifact )
    {
        super( wagon );
       
         switch ( eventType )
        {
            case TRANSFER_INITIATED:
                this.event = event.new Initiated(); 
                break;
            case TRANSFER_STARTED:
                this.event = event.new Started();
                break;
            case TRANSFER_COMPLETED:
                this.event = event.new Completed();
                break;
            case TRANSFER_PROGRESS:
                this.event = event.new Progress();
                break;
            case TRANSFER_ERROR:
                this.event = event.new Error();
                break;
            default :
                throw new IllegalArgumentException( "Illegal event type: " + eventType );
        }
        switch ( requestType )
        {
            case REQUEST_GET:
                this.request = request.new Get();
                break;
            case REQUEST_PUT:
                this.request = request.new Put();
                break;
            default :
                throw new IllegalArgumentException( "Illegal request type: " + requestType );
        }

        this.artifact = artifact;
    }

    public ArtifactTransferEvent( String wagon, final Exception exception, final int requestType,
                                  ArtifactTransferResource artifact )
    {
        this( wagon, TRANSFER_ERROR, requestType, artifact );

        this.exception = exception;
    }

    public ArtifactTransferResource getResource()
    {
        return artifact;
    }

   

    /**
     * @return Returns the local file.
     */
    public File getLocalFile()
    {
        return localFile;
    }

    /**
     * @param localFile The local file to set.
     */
    public void setLocalFile( File localFile )
    {
        this.localFile = localFile;
    }

    public long getTransferredBytes()
    {
        return transferredBytes;
    }

    public void setTransferredBytes( long transferredBytes )
    {
        this.transferredBytes = transferredBytes;
    }

    public byte[] getDataBuffer()
    {
        return dataBuffer;
    }

    public void setDataBuffer( byte[] dataBuffer )
    {
        this.dataBuffer = dataBuffer;
    }

    public int getDataOffset()
    {
        return dataOffset;
    }

    public void setDataOffset( int dataOffset )
    {
        this.dataOffset = dataOffset;
    }

    public int getDataLength()
    {
        return dataLength;
    }

    public void setDataLength( int dataLength )
    {
        this.dataLength = dataLength;
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder( 64 );

        sb.append( "TransferEvent[" );
        if ( request != null )
        {
           request.toStringHelper( sb );   
        } 
        

        sb.append( '|' );
        if ( event != null )
        {
           event.toStringHelper( sb );   
        }

        sb.append( '|' );
        sb.append( this.getLocalFile() ).append( '|' );
        sb.append( ']' );

        return sb.toString();
    }

    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + event.getCode();
        result = prime * result + ( ( exception == null ) ? 0 : exception.hashCode() );
        result = prime * result + ( ( localFile == null ) ? 0 : localFile.hashCode() );
        result = prime * result + request.getCode();
        return result;
    }

    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( ( obj == null ) || ( getClass() != obj.getClass() ) )
        {
            return false;
        }
        final ArtifactTransferEvent other = (ArtifactTransferEvent) obj;
        if ( event.getCode() != other.event.getCode() )
        {
            return false;
        }
        if ( exception == null )
        {
            if ( other.exception != null )
            {
                return false;
            }
        }
        else if ( !exception.getClass().equals( other.exception.getClass() ) )
        {
            return false;
        }
        if ( this.request.getCode() != other.request.getCode() )
        {
            return false;
        }
        else
        {
            return source.equals( other.source );
        }
    }

}
