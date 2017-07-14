import java.net.*;
import java.io.*;
import java.nio.channels.*;

public class MavenWrapperDownloader {

    public static void main(String args[]) {
        System.out.println("- Downloader started");
        String url="http://central.maven.org/maven2/io/takari/maven-wrapper/0.2.1/maven-wrapper-0.2.1.jar";
        File baseDirectory = new File(args[0]);
        System.out.println("- Using base directory: " + baseDirectory.getAbsolutePath());
        File outputFile = new File(baseDirectory.getAbsolutePath(), ".mvn/wrapper/maven-wrapper.jar");
        if(!outputFile.getParentFile().exists()) {
            outputFile.getParentFile().mkdirs();
        }
        System.out.println("- Downloading to: " + outputFile.getAbsolutePath());
        try {
            downloadFileFromURL(url, outputFile);
            System.out.println("Done");
            System.exit(0);
        } catch (Throwable e) {
            System.out.println("- Error downloading");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void downloadFileFromURL(String urlString, File destination) throws Exception {
        URL website = new URL(urlString);
        ReadableByteChannel rbc;
        rbc = Channels.newChannel(website.openStream());
        FileOutputStream fos = new FileOutputStream(destination);
        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        fos.close();
        rbc.close();
    }

}
