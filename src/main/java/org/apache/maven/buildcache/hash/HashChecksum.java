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
package org.apache.maven.buildcache.hash;

import java.io.IOException;
import java.nio.file.Path;

/**
 * HashChecksum
 */
public class HashChecksum
{

    private final Hash.Algorithm algorithm;
    private final Hash.Checksum checksum;

    HashChecksum( Hash.Algorithm algorithm, Hash.Checksum checksum )
    {
        this.algorithm = algorithm;
        this.checksum = checksum;
    }

    public String update( Path path ) throws IOException
    {
        return updateAndEncode( algorithm.hash( path ) );
    }

    public String update( byte[] bytes )
    {
        return updateAndEncode( algorithm.hash( bytes ) );
    }

    /**
     * @param hexHash hash value in hex format. This method doesn't accept generic text - could result in error
     */
    public String update( String hexHash )
    {
        return updateAndEncode( HexUtils.decode( hexHash ) );
    }

    private String updateAndEncode( byte[] hash )
    {
        checksum.update( hash );
        return HexUtils.encode( hash );
    }

    public String digest()
    {
        return HexUtils.encode( checksum.digest() );
    }
}
