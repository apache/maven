package org.apache.maven.plugin.util.scan.mapping;

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

import org.apache.maven.plugin.util.scan.InclusionScanException;
import org.apache.maven.plugin.util.scan.mapping.SuffixMapping;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

/**
 * @author jdcasey
 */
public class SuffixMappingTest
    extends TestCase
{

    public void testShouldReturnSingleClassFileForSingleJavaFile() throws InclusionScanException
    {
        String base = "path/to/file";
        
        File basedir = new File(".");
        
        SuffixMapping mapping = new SuffixMapping(".java", ".class");
        
        Set results = mapping.getTargetFiles(basedir, base + ".java");
        
        assertEquals("Returned wrong number of target files.", 1, results.size());
        assertEquals("Target file is wrong.", new File(basedir, base + ".class"), results.iterator().next());
    }
    
    public void testShouldNotReturnClassFileWhenSourceFileHasWrongSuffix() throws InclusionScanException
    {
        String base = "path/to/file";
        
        File basedir = new File(".");
        
        SuffixMapping mapping = new SuffixMapping(".java", ".class");
        
        Set results = mapping.getTargetFiles(basedir, base + ".xml");
        
        assertTrue("Returned wrong number of target files.", results.isEmpty());
    }
    
    public void testShouldReturnOneClassFileAndOneXmlFileForSingleJavaFile() throws InclusionScanException
    {
        String base = "path/to/file";
        
        File basedir = new File(".");
        
        Set targets = new HashSet();
        targets.add(".class");
        targets.add(".xml");
        
        SuffixMapping mapping = new SuffixMapping(".java", targets);
        
        Set results = mapping.getTargetFiles(basedir, base + ".java");
        
        assertEquals("Returned wrong number of target files.", 2, results.size());
        
        assertTrue("Targets do not contain class target.", results.contains(new File(basedir, base + ".class")));
        
        assertTrue("Targets do not contain class target.", results.contains(new File(basedir, base + ".xml")));
        
    }
    
    public void testShouldReturnNoTargetFilesWhenSourceFileHasWrongSuffix() throws InclusionScanException
    {
        String base = "path/to/file";
        
        File basedir = new File(".");
        
        Set targets = new HashSet();
        targets.add(".class");
        targets.add(".xml");
        
        SuffixMapping mapping = new SuffixMapping(".java", targets);
        
        Set results = mapping.getTargetFiles(basedir, base + ".apt");
        
        assertTrue("Returned wrong number of target files.", results.isEmpty());
        
    }
    
}
