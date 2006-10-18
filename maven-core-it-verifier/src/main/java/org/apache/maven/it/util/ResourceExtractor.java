/*
 * Created on Oct 17, 2006
 *
 */
package org.apache.maven.it.util;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;


/* @todo this can be replaced with plexus-archiver */
public class ResourceExtractor {
    
    public static void extractResourcePath(String resourcePath, File dest) throws IOException {
        extractResourcePath(ResourceExtractor.class, resourcePath, dest);
    }
        
    public static void extractResourcePath(Class cl, String resourcePath, File dest)
            throws IOException {
        URL url = cl.getResource(resourcePath);
        if (url == null) throw new IllegalArgumentException("Resource not found: " + resourcePath);
        if ("jar".equalsIgnoreCase(url.getProtocol())) {
            File jarFile = getJarFileFromUrl(url);
            extractResourcePathFromJar(cl, jarFile, resourcePath, dest);
        } else {
            try {
                File resourceFile = new File(new URI(url.toExternalForm()));
                if (resourceFile.isDirectory()) {
                    FileUtils.copyDirectoryStructure(resourceFile, dest);
                } else {
                    FileUtils.copyFile(resourceFile, dest);
                }
            } catch (URISyntaxException e) {
                throw new RuntimeException("Couldn't convert URL to File:" + url, e);
            }
        }
    }
    
    private static void extractResourcePathFromJar(Class cl, File jarFile, String resourcePath, File dest) throws IOException {
        ZipFile z = new ZipFile(jarFile, ZipFile.OPEN_READ);
        String zipStyleResourcePath = resourcePath.substring(1) + "/"; 
        ZipEntry ze = z.getEntry(zipStyleResourcePath);
        if (ze != null) {
            // DGF If it's a directory, then we need to look at all the entries
            for (Enumeration entries = z.entries(); entries.hasMoreElements();) {
                ze = (ZipEntry) entries.nextElement();
                if (ze.getName().startsWith(zipStyleResourcePath)) {
                    String relativePath = ze.getName().substring(zipStyleResourcePath.length());
                    File destFile = new File(dest, relativePath);
                    if (ze.isDirectory()) {
                        destFile.mkdirs();
                    } else {
                        FileOutputStream fos = new FileOutputStream(destFile);
                        IOUtil.copy(z.getInputStream(ze), fos);
                    }
                }
            }
        } else {
            FileOutputStream fos = new FileOutputStream(dest);
            IOUtil.copy(cl.getResourceAsStream(resourcePath), fos);
            
        }
    } 

    private static File getJarFileFromUrl(URL url) {
        if (!"jar".equalsIgnoreCase(url.getProtocol()))
            throw new IllegalArgumentException("This is not a Jar URL:"
                    + url.toString());
        String resourceFilePath = url.getFile();
        int index = resourceFilePath.indexOf("!");
        if (index == -1) {
            throw new RuntimeException("Bug! " + url.toExternalForm()
                    + " does not have a '!'");
        }
        String jarFileURI = resourceFilePath.substring(0, index);
        try {
            File jarFile = new File(new URI(jarFileURI));
            return jarFile;
        } catch (URISyntaxException e) {
            throw new RuntimeException("Bug! URI failed to parse: " + jarFileURI, e);
        }

    }
}
