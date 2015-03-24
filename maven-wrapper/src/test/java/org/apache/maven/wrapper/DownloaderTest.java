package org.apache.maven.wrapper;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.net.URI;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

public class DownloaderTest {

  private DefaultDownloader download;

  private File testDir;

  private File downloadFile;

  private File rootDir;

  private URI sourceRoot;

  private File remoteFile;

  @Before
  public void setUp() throws Exception {
    download = new DefaultDownloader("mvnw", "aVersion");
    testDir = new File("target/test-files/DownloadTest");
    rootDir = new File(testDir, "root");
    downloadFile = new File(rootDir, "file");
    remoteFile = new File(testDir, "remoteFile");
    FileUtils.write(remoteFile, "sometext");
    sourceRoot = remoteFile.toURI();
  }

  @Test
  public void testDownload() throws Exception {
    assert !downloadFile.exists();
    download.download(sourceRoot, downloadFile);
    assert downloadFile.exists();
    assertEquals("sometext", FileUtils.readFileToString(downloadFile));
  }
}
