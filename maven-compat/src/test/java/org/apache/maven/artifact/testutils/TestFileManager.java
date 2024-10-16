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
package org.apache.maven.artifact.testutils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.codehaus.plexus.util.FileUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Deprecated
public class TestFileManager {

    public static final String TEMP_DIR_PATH = System.getProperty("java.io.tmpdir");

    private List<File> filesToDelete = new ArrayList<>();

    private final String baseFilename;

    private final String fileSuffix;

    private StackTraceElement callerInfo;

    private Thread cleanupWarning;

    private boolean warnAboutCleanup = false;

    public TestFileManager(String baseFilename, String fileSuffix) {
        this.baseFilename = baseFilename;
        this.fileSuffix = fileSuffix;

        initializeCleanupMonitoring();
    }

    private void initializeCleanupMonitoring() {
        callerInfo = new NullPointerException().getStackTrace()[2];

        Runnable warning = this::maybeWarnAboutCleanUp;

        cleanupWarning = new Thread(warning);

        Runtime.getRuntime().addShutdownHook(cleanupWarning);
    }

    private void maybeWarnAboutCleanUp() {
        if (warnAboutCleanup) {
            System.out.println("[WARNING] TestFileManager from: " + callerInfo.getClassName() + " not cleaned up!");
        }
    }

    public void markForDeletion(File toDelete) {
        filesToDelete.add(toDelete);
        warnAboutCleanup = true;
    }

    public synchronized File createTempDir() {
        try {
            Thread.sleep(20);
        } catch (InterruptedException e) {
            // ignore
        }

        File dir = new File(TEMP_DIR_PATH, baseFilename + System.currentTimeMillis());

        dir.mkdirs();
        markForDeletion(dir);

        return dir;
    }

    public synchronized File createTempFile() throws IOException {
        File tempFile = File.createTempFile(baseFilename, fileSuffix);
        tempFile.deleteOnExit();
        markForDeletion(tempFile);

        return tempFile;
    }

    public void cleanUp() throws IOException {
        for (Iterator it = filesToDelete.iterator(); it.hasNext(); ) {
            File file = (File) it.next();

            if (file.exists()) {
                if (file.isDirectory()) {
                    FileUtils.deleteDirectory(file);
                } else {
                    file.delete();
                }
            }

            it.remove();
        }

        warnAboutCleanup = false;
    }

    public void assertFileExistence(File dir, String filename, boolean shouldExist) {
        File file = new File(dir, filename);

        if (shouldExist) {
            assertTrue(file.exists());
        } else {
            assertFalse(file.exists());
        }
    }

    public void assertFileContents(File dir, String filename, String contentsTest, String encoding) throws IOException {
        assertFileExistence(dir, filename, true);

        File file = new File(dir, filename);

        String contents = FileUtils.fileRead(file, encoding);

        assertEquals(contentsTest, contents);
    }

    public File createFile(File dir, String filename, String contents, String encoding) throws IOException {
        File file = new File(dir, filename);

        file.getParentFile().mkdirs();

        FileUtils.fileWrite(file.getPath(), encoding, contents);

        markForDeletion(file);

        return file;
    }

    public String getFileContents(File file, String encoding) throws IOException {
        return FileUtils.fileRead(file, encoding);
    }

    protected void finalize() throws Throwable {
        maybeWarnAboutCleanUp();

        super.finalize();
    }

    public File createFile(String filename, String content, String encoding) throws IOException {
        File dir = createTempDir();
        return createFile(dir, filename, content, encoding);
    }
}
