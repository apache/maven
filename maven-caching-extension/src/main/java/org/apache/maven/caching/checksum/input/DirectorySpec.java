package org.apache.maven.caching.checksum.input;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DirectorySpec {
    private Path baseDir;
    private List<String> includes;
    private List<String> excludes;

    public DirectorySpec(Path baseDir) {
        this.baseDir = baseDir;
    }

    public Path getDirectory() {
        return baseDir;
    }

    public List<String> getExcludes() {
        return excludes;
    }

    public List<String> getIncludes() {
        return includes;
    }

    public void excludeDirs( Path... paths )
    {
        excludes.addAll( recursiveDirectoryGlob( paths ) );
    }

    public void includeDirs( Path... paths )
    {
        includes.addAll( recursiveDirectoryGlob( paths ) );
    }

    private List<String> recursiveDirectoryGlob( Path[] paths )
    {
        return Arrays.stream( paths )
                .filter( Files::isDirectory )
                .map( this::normalizedAbsolutePath )
                .map( path -> "glob:" + path + "/**" )
                .collect(Collectors.toList());
    }

    private Path normalizedAbsolutePath( Path path )
    {
        return path.isAbsolute() ? path.normalize() : baseDir.resolve( path ).normalize().toAbsolutePath();
    }


    public void addExclude(List<String> matchingExpressions) {
        excludes.addAll(matchingExpressions);
    }

    public void addInclude(List<String> matchingExpressions) {
        includes.addAll(matchingExpressions);
    }
}
