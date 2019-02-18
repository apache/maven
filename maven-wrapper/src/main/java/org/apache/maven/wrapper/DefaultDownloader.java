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

import static org.apache.maven.wrapper.MavenWrapperMain.MVNW_PASSWORD;
import static org.apache.maven.wrapper.MavenWrapperMain.MVNW_USERNAME;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

/**
 * @author Hans Dockter
 */
public class DefaultDownloader implements Downloader {
  private static final int PROGRESS_CHUNK = 500000;

  private static final int BUFFER_SIZE = 10000;

  private final String applicationName;

  private final String applicationVersion;

  public DefaultDownloader(String applicationName, String applicationVersion) {
    this.applicationName = applicationName;
    this.applicationVersion = applicationVersion;
    configureProxyAuthentication();
    configureAuthentication();
  }

  private void configureProxyAuthentication() {
    if (System.getProperty("http.proxyUser") != null) {
      Authenticator.setDefault(new SystemPropertiesProxyAuthenticator());
    }
  }
  
  private void configureAuthentication() {
    if (System.getProperty("MVNW_USERNAME") != null && System.getProperty("MVNW_PASSWORD") != null && System.getProperty("http.proxyUser") == null) {
      Authenticator.setDefault(new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(System.getProperty("MVNW_USERNAME"), System.getProperty("MVNW_PASSWORD").toCharArray());
            }
        });
    }
  }

  public void download(URI address, File destination) throws Exception {
    if (destination.exists()) {
      return;
    }
    destination.getParentFile().mkdirs();

    downloadInternal(address, destination);
  }

  private void downloadInternal(URI address, File destination) throws Exception {
    OutputStream out = null;
    URLConnection conn;
    InputStream in = null;
    try {
      URL url = address.toURL();
      out = new BufferedOutputStream(new FileOutputStream(destination));
      conn = url.openConnection();
      addBasicAuthentication(address, conn);
      final String userAgentValue = calculateUserAgent();
      conn.setRequestProperty("User-Agent", userAgentValue);
      in = conn.getInputStream();
      byte[] buffer = new byte[BUFFER_SIZE];
      int numRead;
      long progressCounter = 0;
      while ((numRead = in.read(buffer)) != -1) {
        progressCounter += numRead;
        if (progressCounter / PROGRESS_CHUNK > 0) {
          Logger.info(".");
          progressCounter = progressCounter - PROGRESS_CHUNK;
        }
        out.write(buffer, 0, numRead);
      }
    } finally {
      Logger.info("");
      if (in != null) {
        in.close();
      }
      if (out != null) {
        out.close();
      }
    }
  }

  private void addBasicAuthentication(URI address, URLConnection connection) throws IOException {
    String userInfo = calculateUserInfo(address);
    if (userInfo == null) {
        return;
    }
    if (!"https".equals(address.getScheme())) {
       Logger.warn("WARNING Using HTTP Basic Authentication over an insecure connection to download the Maven distribution. Please consider using HTTPS.");
    }
    connection.setRequestProperty("Authorization", "Basic " + base64Encode(userInfo));
  }

  /**
     * Base64 encode user info for HTTP Basic Authentication.
     *
     * Try to use {@literal java.util.Base64} encoder which is available starting with Java 8.
     * Fallback to {@literal javax.xml.bind.DatatypeConverter} from JAXB which is available starting with Java 6 but is not anymore in Java 9.
     * Fortunately, both of these two Base64 encoders implement the right Base64 flavor, the one that does not split the output in multiple lines.
     *
     * @param userInfo user info
     * @return Base64 encoded user info
     * @throws RuntimeException if no public Base64 encoder is available on this JVM
     */
    private String base64Encode(String userInfo) {
      ClassLoader loader = getClass().getClassLoader();
      try {
          Method getEncoderMethod = loader.loadClass("java.util.Base64").getMethod("getEncoder");
          Method encodeMethod = loader.loadClass("java.util.Base64$Encoder").getMethod("encodeToString", byte[].class);
          Object encoder = getEncoderMethod.invoke(null);
          return (String) encodeMethod.invoke(encoder, new Object[]{userInfo.getBytes("UTF-8")});
      } catch (Exception java7OrEarlier) {
          try {
              Method encodeMethod = loader.loadClass("javax.xml.bind.DatatypeConverter").getMethod("printBase64Binary", byte[].class);
              return (String) encodeMethod.invoke(null, new Object[]{userInfo.getBytes("UTF-8")});
          } catch (Exception java5OrEarlier) {
              throw new RuntimeException("Downloading Maven distributions with HTTP Basic Authentication is not supported on your JVM.", java5OrEarlier);
          }
      }
  }

  private String calculateUserInfo(URI uri) {
    String username = System.getenv(MVNW_USERNAME);
    String password = System.getenv(MVNW_PASSWORD);
    if (username != null && password != null) {
        return username + ':' + password;
    }
    return uri.getUserInfo();
}

  private String calculateUserAgent() {
    String appVersion = applicationVersion;

    String javaVendor = System.getProperty("java.vendor");
    String javaVersion = System.getProperty("java.version");
    String javaVendorVersion = System.getProperty("java.vm.version");
    String osName = System.getProperty("os.name");
    String osVersion = System.getProperty("os.version");
    String osArch = System.getProperty("os.arch");
    return String.format("%s/%s (%s;%s;%s) (%s;%s;%s)", applicationName, appVersion, osName, osVersion, osArch, javaVendor, javaVersion, javaVendorVersion);
  }

  private static class SystemPropertiesProxyAuthenticator extends Authenticator {
    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
      return new PasswordAuthentication(System.getProperty("http.proxyUser"), System.getProperty("http.proxyPassword", "").toCharArray());
    }
  }
}
