package org.apache.maven.caching.checksum.input;

import java.nio.file.Path;

public class InputFile
{

    public Path path;
    public String reference;

    public InputFile( Path path, String reference )
    {

        this.path = path;
        this.reference = reference;
    }
}
