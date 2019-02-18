/*
 * Copyright 2007-present the original author or authors.
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
package org.apache.maven.wrapper;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author Hans Dockter
 */
public class Installer {
  public static final String DEFAULT_DISTRIBUTION_PATH = "wrapper/dists";

  private final Downloader download;

  private final PathAssembler pathAssembler;

  public Installer(Downloader download, PathAssembler pathAssembler) {
    this.download = download;
    this.pathAssembler = pathAssembler;
  }

  public File createDist(WrapperConfiguration configuration) throws Exception {
    URI distributionUrl = configuration.getDistribution();
    boolean alwaysDownload = configuration.isAlwaysDownload();
    boolean alwaysUnpack = configuration.isAlwaysUnpack();

    PathAssembler.LocalDistribution localDistribution = pathAssembler.getDistribution(configuration);

    File localZipFile = localDistribution.getZipFile();
    boolean downloaded = false;
    if (alwaysDownload || !localZipFile.exists()) {
      File tmpZipFile = new File(localZipFile.getParentFile(), localZipFile.getName() + ".part");
      tmpZipFile.delete();
      Logger.info("Downloading " + distributionUrl);
      download.download(distributionUrl, tmpZipFile);
      tmpZipFile.renameTo(localZipFile);
      downloaded = true;
    }

    File distDir = localDistribution.getDistributionDir();
    List<File> dirs = listDirs(distDir);

    if (downloaded || alwaysUnpack || dirs.isEmpty()) {
      for (File dir : dirs) {
        Logger.info("Deleting directory " + dir.getAbsolutePath());
        deleteDir(dir);
      }
      Logger.info("Unzipping " + localZipFile.getAbsolutePath() + " to " + distDir.getAbsolutePath());
      unzip(localZipFile, distDir);
      dirs = listDirs(distDir);
      if (dirs.isEmpty()) {
        throw new RuntimeException(String.format("Maven distribution '%s' does not contain any directories. Expected to find exactly 1 directory.", distributionUrl));
      }
      setExecutablePermissions(dirs.get(0));
    }
    if (dirs.size() != 1) {
      throw new RuntimeException(String.format("Maven distribution '%s' contains too many directories. Expected to find exactly 1 directory.", distributionUrl));
    }
    return dirs.get(0);
  }

  private List<File> listDirs(File distDir) {
    List<File> dirs = new ArrayList<File>();
    if (distDir.exists()) {
      for (File file : distDir.listFiles()) {
        if (file.isDirectory()) {
          dirs.add(file);
        }
      }
    }
    return dirs;
  }

  private void setExecutablePermissions(File mavenHome) {
    if (isWindows()) {
      return;
    }
    File mavenCommand = new File(mavenHome, "bin/mvn");
    String errorMessage = null;
    try {
      ProcessBuilder pb = new ProcessBuilder("chmod", "755", mavenCommand.getCanonicalPath());
      Process p = pb.start();
      if (p.waitFor() == 0) {
        Logger.info("Set executable permissions for: " + mavenCommand.getAbsolutePath());
      } else {
        BufferedReader is = new BufferedReader(new InputStreamReader(p.getInputStream()));
        Formatter stdout = new Formatter();
        String line;
        while ((line = is.readLine()) != null) {
          stdout.format("%s%n", line);
        }
        errorMessage = stdout.toString();
      }
    } catch (IOException e) {
      errorMessage = e.getMessage();
    } catch (InterruptedException e) {
      errorMessage = e.getMessage();
    }
    if (errorMessage != null) {
      Logger.warn("Could not set executable permissions for: " + mavenCommand.getAbsolutePath());
      Logger.warn("Please do this manually if you want to use maven.");
    }
  }

  private boolean isWindows() {
    String osName = System.getProperty("os.name").toLowerCase(Locale.US);
    if (osName.indexOf("windows") > -1) {
      return true;
    }
    return false;
  }

  private boolean deleteDir(File dir) {
    if (dir.isDirectory()) {
      String[] children = dir.list();
      for (int i = 0; i < children.length; i++) {
        boolean success = deleteDir(new File(dir, children[i]));
        if (!success) {
          return false;
        }
      }
    }

    // The directory is now empty so delete it
    return dir.delete();
  }

  public void unzip(File zip, File dest) throws IOException {
    Enumeration entries;
    ZipFile zipFile;

    zipFile = new ZipFile(zip);

    entries = zipFile.entries();

    while (entries.hasMoreElements()) {
      ZipEntry entry = (ZipEntry) entries.nextElement();

      if (entry.isDirectory()) {
        (new File(dest, entry.getName())).mkdirs();
        continue;
      }

      new File(dest, entry.getName()).getParentFile().mkdirs();
      copyInputStream(zipFile.getInputStream(entry), new BufferedOutputStream(new FileOutputStream(new File(dest, entry.getName()))));
    }
    zipFile.close();
  }

  public void copyInputStream(InputStream in, OutputStream out) throws IOException {
    byte[] buffer = new byte[1024];
    int len;

    while ((len = in.read(buffer)) >= 0) {
      out.write(buffer, 0, len);
    }

    in.close();
    out.close();
  }

}
