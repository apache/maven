package org.apache.maven.tools.repoclean.digest;

import org.apache.maven.tools.repoclean.TestSupport;

import java.io.File;
import java.io.FileReader;
import java.util.Arrays;

import junit.framework.TestCase;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class ArtifactDigestorTest
    extends TestCase
{
    
    private static final String DIGEST_FORMAT_VERIFY_ARTIFACT = "digestFormatVerifyArtifact.jar";
    
    public void testShouldWriteDigestFileInHexNotBinary() throws Exception
    {
        Digestor digestor = new Digestor();
        
        File artifact = TestSupport.getMyResource(this, DIGEST_FORMAT_VERIFY_ARTIFACT);
        
        byte[] rawDigest = digestor.generateArtifactDigest( artifact, Digestor.MD5 );
        
        StringBuffer rawConverted = new StringBuffer(rawDigest.length * 2);
        for ( int i = 0; i < rawDigest.length; i++ )
        {
            String encoded = Integer.toHexString(rawDigest[i] & 0xff);
            if(encoded.length() < 2)
            {
                encoded = "0" + encoded;
            }
            
            rawConverted.append(encoded);
        }
        
        File digestFile = File.createTempFile("repoclean-artifactDigest-formatTest", ".md5");
        
        digestor.createArtifactDigest( artifact, digestFile, Digestor.MD5 );
        
        FileReader reader = new FileReader(digestFile);
        StringBuffer written = new StringBuffer(rawDigest.length * 2);
        
        char[] cbuf = new char[rawDigest.length * 2];
        int read = -1;
        
        while((read = reader.read(cbuf)) > -1)
        {
            written.append(cbuf, 0, read);
        }
        
        reader.close();
        
        assertEquals(rawConverted.length(), written.length());
        
        cbuf = new char[written.length()];
        char[] cbuf2 = new char[cbuf.length];
        
        written.getChars(0, cbuf.length, cbuf, 0);
        rawConverted.getChars(0, cbuf2.length, cbuf2, 0);
        
        assertTrue(Arrays.equals(cbuf, cbuf2));
    }

}
