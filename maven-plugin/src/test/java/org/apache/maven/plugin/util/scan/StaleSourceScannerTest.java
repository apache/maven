package org.apache.maven.plugin.util.scan;

import org.apache.maven.plugin.util.scan.StaleSourceScanner;
import org.apache.maven.plugin.util.scan.mapping.SuffixMapping;
import org.codehaus.plexus.util.IOUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Set;

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

/**
 * @author jdcasey
 */
public class StaleSourceScannerTest
    extends TestCase
{
    private static final String TESTFILE_DEST_MARKER_FILE = StaleSourceScanner.class.getName().replace('.', '/') + "-testMarker.txt";

    // test 1.
    public void testWithDefaultConstructorShouldFindOneStaleSource() throws Exception
    {
        File base = new File(getTestBaseDir(), "test1");
        
        long now = System.currentTimeMillis();
        
        File targetFile = new File(base, "file.xml");
        
        writeFile(targetFile);
        
        targetFile.setLastModified(now - 60000);
        
        File sourceFile = new File(base, "file.java");
        
        writeFile(sourceFile);
        
        sourceFile.setLastModified(now);
        
        SuffixMapping mapping = new SuffixMapping(".java", ".xml");
        
        StaleSourceScanner scanner = new StaleSourceScanner();
        
        scanner.addSourceMapping(mapping);
        
        Set result = scanner.getIncludedSources(base, base);
        
        assertEquals("wrong number of stale sources returned.", 1, result.size());
        
        assertTrue("expected stale source file not found in result", result.contains(sourceFile));
    }
    
    // test 2.
    public void testWithDefaultConstructorShouldNotFindStaleSources() throws Exception
    {
        File base = new File(getTestBaseDir(), "test2");
        
        long now = System.currentTimeMillis();
        
        File sourceFile = new File(base, "file.java");
        
        writeFile(sourceFile);
        
        sourceFile.setLastModified(now - 60000);
        
        File targetFile = new File(base, "file.xml");
        
        writeFile(targetFile);
        
        targetFile.setLastModified(now);
        
        SuffixMapping mapping = new SuffixMapping(".java", ".xml");
        
        StaleSourceScanner scanner = new StaleSourceScanner();
        
        scanner.addSourceMapping(mapping);
        
        Set result = scanner.getIncludedSources(base, base);
        
        assertEquals("wrong number of stale sources returned.", 0, result.size());
        
        assertFalse("expected stale source file not found in result", result.contains(sourceFile));
    }
    
    // test 3.
    public void testWithDefaultConstructorShouldFindStaleSourcesBecauseOfMissingTargetFile() throws Exception
    {
        File base = new File(getTestBaseDir(), "test3");
        
        long now = System.currentTimeMillis();
        
        File sourceFile = new File(base, "file.java");
        
        writeFile(sourceFile);
        
        SuffixMapping mapping = new SuffixMapping(".java", ".xml");
        
        StaleSourceScanner scanner = new StaleSourceScanner();
        
        scanner.addSourceMapping(mapping);
        
        Set result = scanner.getIncludedSources(base, base);
        
        assertEquals("wrong number of stale sources returned.", 1, result.size());
        
        assertTrue("expected stale source file not found in result", result.contains(sourceFile));
    }
    
    // test 4.
    public void testWithDefaultConstructorShouldFindStaleSourcesOneBecauseOfMissingTargetAndOneBecauseOfStaleTarget() throws Exception
    {
        File base = new File(getTestBaseDir(), "test4");
        
        long now = System.currentTimeMillis();
        
        File targetFile = new File(base, "file2.xml");
        
        writeFile(targetFile);
        
        targetFile.setLastModified(now - 60000);
        
        File sourceFile = new File(base, "file.java");
        
        writeFile(sourceFile);
        
        File sourceFile2 = new File(base, "file2.java");
        
        writeFile(sourceFile2);
        
        sourceFile2.setLastModified(now);
        
        SuffixMapping mapping = new SuffixMapping(".java", ".xml");
        
        StaleSourceScanner scanner = new StaleSourceScanner();
        
        scanner.addSourceMapping(mapping);
        
        Set result = scanner.getIncludedSources(base, base);
        
        assertEquals("wrong number of stale sources returned.", 2, result.size());
        
        assertTrue("expected stale source file not found in result", result.contains(sourceFile));
        
        assertTrue("expected stale source file not found in result", result.contains(sourceFile2));
    }
    
    // test 5.
    public void testWithDefaultConstructorShouldFindOneStaleSourcesWithStaleTargetAndOmitUpToDateSource() throws Exception
    {
        File base = new File(getTestBaseDir(), "test5");
        
        long now = System.currentTimeMillis();
        
        // target/source (1) should result in source being included.
        
        // write the target file first, and set the lastmod to some time in the 
        // past to ensure this.
        File targetFile = new File(base, "file.xml");
        
        writeFile(targetFile);
        
        targetFile.setLastModified(now - 60000);
        
        // now write the source file, and set the lastmod to now.
        File sourceFile = new File(base, "file.java");
        
        writeFile(sourceFile);
        
        sourceFile.setLastModified(now);
        
        // target/source (2) should result in source being omitted.
        
        // write the source file first, and set the lastmod to some time in the
        // past to ensure this.
        File sourceFile2 = new File(base, "file2.java");
        
        writeFile(sourceFile2);
        
        sourceFile2.setLastModified(now - 60000);
        
        // now write the target file, with lastmod of now.
        File targetFile2 = new File(base, "file2.xml");
        
        writeFile(targetFile2);
        
        targetFile2.setLastModified(now);
        
        SuffixMapping mapping = new SuffixMapping(".java", ".xml");
        
        StaleSourceScanner scanner = new StaleSourceScanner();
        
        scanner.addSourceMapping(mapping);
        
        Set result = scanner.getIncludedSources(base, base);
        
        assertEquals("wrong number of stale sources returned.", 1, result.size());
        
        assertTrue("expected stale source file not found in result", result.contains(sourceFile));
    }
    
    // test 6.
    public void testConstructedWithMsecsShouldReturnOneSourceFileOfTwoDueToLastMod() throws Exception
    {
        File base = new File(getTestBaseDir(), "test6");
        
        long now = System.currentTimeMillis();
        
        File targetFile = new File(base, "file.xml");
        
        writeFile(targetFile);
        
        // should be within the threshold of lastMod for stale sources. 
        targetFile.setLastModified(now - 8000);
        
        File sourceFile = new File(base, "file.java");
        
        writeFile(sourceFile);
        
        // modified 'now' for comparison with the above target file. 
        sourceFile.setLastModified(now);
        
        File targetFile2 = new File(base, "file2.xml");
        
        writeFile(targetFile2);
        
        targetFile2.setLastModified(now - 12000);
        
        File sourceFile2 = new File(base, "file2.java");
        
        writeFile(sourceFile2);
        
        // modified 'now' for comparison to above target file.
        sourceFile2.setLastModified(now);
        
        SuffixMapping mapping = new SuffixMapping(".java", ".xml");
        
        StaleSourceScanner scanner = new StaleSourceScanner(10000);
        
        scanner.addSourceMapping(mapping);
        
        Set result = scanner.getIncludedSources(base, base);
        
        assertEquals("wrong number of stale sources returned.", 1, result.size());
        
        assertTrue("expected stale source file not found in result", result.contains(sourceFile2));
        
        assertFalse("expected stale source file not found in result", result.contains(sourceFile));
    }
    
    // test 7.
    public void testConstructedWithMsecsIncludesAndExcludesShouldReturnOneSourceFileOfThreeDueToIncludePattern() throws Exception
    {
        File base = new File(getTestBaseDir(), "test7");
        
        long now = System.currentTimeMillis();
        
        File targetFile = new File(base, "file.xml");
        
        writeFile(targetFile);
        
        // should be within the threshold of lastMod for stale sources. 
        targetFile.setLastModified(now - 12000);
        
        File sourceFile = new File(base, "file.java");
        
        writeFile(sourceFile);
        
        // modified 'now' for comparison with the above target file. 
        sourceFile.setLastModified(now);
        
        File targetFile2 = new File(base, "file2.xml");
        
        writeFile(targetFile2);
        
        targetFile2.setLastModified(now - 12000);
        
        File sourceFile2 = new File(base, "file2.java");
        
        writeFile(sourceFile2);
        
        // modified 'now' for comparison to above target file.
        sourceFile2.setLastModified(now);
        
        File targetFile3 = new File(base, "file3.xml");
        
        writeFile(targetFile3);
        
        targetFile3.setLastModified(now - 12000);
        
        File sourceFile3 = new File(base, "file3.java");
        
        writeFile(sourceFile3);
        
        // modified 'now' for comparison to above target file.
        sourceFile3.setLastModified(now);
        
        SuffixMapping mapping = new SuffixMapping(".java", ".xml");
        
        StaleSourceScanner scanner = new StaleSourceScanner(0, Collections.singleton("*3.java"), Collections.EMPTY_SET);
        
        scanner.addSourceMapping(mapping);
        
        Set result = scanner.getIncludedSources(base, base);
        
        assertEquals("wrong number of stale sources returned.", 1, result.size());
        
        assertFalse("expected stale source file not found in result", result.contains(sourceFile));
        
        assertFalse("unexpected stale source file found in result", result.contains(sourceFile2));
        
        assertTrue("unexpected stale source file found in result", result.contains(sourceFile3));
    }
    
    // test 8.
    public void testConstructedWithMsecsIncludesAndExcludesShouldReturnTwoSourceFilesOfThreeDueToExcludePattern() throws Exception
    {
        File base = new File(getTestBaseDir(), "test8");
        
        long now = System.currentTimeMillis();
        
        File targetFile = new File(base, "fileX.xml");
        
        writeFile(targetFile);
        
        // should be within the threshold of lastMod for stale sources. 
        targetFile.setLastModified(now - 12000);
        
        File sourceFile = new File(base, "fileX.java");
        
        writeFile(sourceFile);
        
        // modified 'now' for comparison with the above target file. 
        sourceFile.setLastModified(now);
        
        File targetFile2 = new File(base, "file2.xml");
        
        writeFile(targetFile2);
        
        targetFile2.setLastModified(now - 12000);
        
        File sourceFile2 = new File(base, "file2.java");
        
        writeFile(sourceFile2);
        
        // modified 'now' for comparison to above target file.
        sourceFile2.setLastModified(now);
        
        File targetFile3 = new File(base, "file3.xml");
        
        writeFile(targetFile3);
        
        targetFile3.setLastModified(now - 12000);
        
        File sourceFile3 = new File(base, "file3.java");
        
        writeFile(sourceFile3);
        
        // modified 'now' for comparison to above target file.
        sourceFile3.setLastModified(now);
        
        SuffixMapping mapping = new SuffixMapping(".java", ".xml");
        
        StaleSourceScanner scanner = new StaleSourceScanner(0, Collections.singleton("**/*"), Collections.singleton("*X.*"));
        
        scanner.addSourceMapping(mapping);
        
        Set result = scanner.getIncludedSources(base, base);
        
        assertEquals("wrong number of stale sources returned.", 2, result.size());
        
        assertFalse("unexpected stale source file found in result", result.contains(sourceFile));
        
        assertTrue("expected stale source not file found in result", result.contains(sourceFile2));
        
        assertTrue("expected stale source not file found in result", result.contains(sourceFile3));
    }
    
    private File getTestBaseDir()
    {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        URL markerResource = cl.getResource(TESTFILE_DEST_MARKER_FILE);
        
        File basedir = null;
        if(markerResource != null)
        {
            File marker = new File(markerResource.getPath());
            basedir = marker.getParentFile().getAbsoluteFile();
        }
        else
        {
            // punt.
            System.out.println("Cannot find marker file: \'" + TESTFILE_DEST_MARKER_FILE + "\' in classpath. Using '.' for basedir.");
            basedir = new File(".").getAbsoluteFile();
        }
        
        return basedir;
    }
    
    private void writeFile(File file) throws IOException
    {
        FileWriter fWriter = null;
        try
        {
            File parent = file.getParentFile();
            if(!parent.exists())
            {
                parent.mkdirs();
            }
            
            file.deleteOnExit();
            
            fWriter = new FileWriter(file);
            fWriter.write("This is just a test file.");
        }
        finally
        {
            IOUtil.close(fWriter);
        }
    }
    
}
