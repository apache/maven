package util;

import java.io.File;
import java.io.IOException;
import java.util.Vector;

/**
 * Class for scanning a directory for files/directories which match certain
 * criteria.
 * <p/>
 * These criteria consist of selectors and patterns which have been specified.
 * With the selectors you can select which files you want to have included.
 * Files which are not selected are excluded. With patterns you can include
 * or exclude files based on their filename.
 * <p/>
 * The idea is simple. A given directory is recursively scanned for all files
 * and directories. Each file/directory is matched against a set of selectors,
 * including special support for matching against filenames with include and
 * and exclude patterns. Only files/directories which match at least one
 * pattern of the include pattern list or other file selector, and don't match
 * any pattern of the exclude pattern list or fail to match against a required
 * selector will be placed in the list of files/directories found.
 * <p/>
 * When no list of include patterns is supplied, "**" will be used, which
 * means that everything will be matched. When no list of exclude patterns is
 * supplied, an empty list is used, such that nothing will be excluded. When
 * no selectors are supplied, none are applied.
 * <p/>
 * The filename pattern matching is done as follows:
 * The name to be matched is split up in path segments. A path segment is the
 * name of a directory or file, which is bounded by
 * <code>File.separator</code> ('/' under UNIX, '\' under Windows).
 * For example, "abc/def/ghi/xyz.java" is split up in the segments "abc",
 * "def","ghi" and "xyz.java".
 * The same is done for the pattern against which should be matched.
 * <p/>
 * The segments of the name and the pattern are then matched against each
 * other. When '**' is used for a path segment in the pattern, it matches
 * zero or more path segments of the name.
 * <p/>
 * There is a special case regarding the use of <code>File.separator</code>s
 * at the beginning of the pattern and the string to match:<br>
 * When a pattern starts with a <code>File.separator</code>, the string
 * to match must also start with a <code>File.separator</code>.
 * When a pattern does not start with a <code>File.separator</code>, the
 * string to match may not start with a <code>File.separator</code>.
 * When one of these rules is not obeyed, the string will not
 * match.
 * <p/>
 * When a name path segment is matched against a pattern path segment, the
 * following special characters can be used:<br>
 * '*' matches zero or more characters<br>
 * '?' matches one character.
 * <p/>
 * Examples:
 * <p/>
 * "**\*.class" matches all .class files/dirs in a directory tree.
 * <p/>
 * "test\a??.java" matches all files/dirs which start with an 'a', then two
 * more characters and then ".java", in a directory called test.
 * <p/>
 * "**" matches everything in a directory tree.
 * <p/>
 * "**\test\**\XYZ*" matches all files/dirs which start with "XYZ" and where
 * there is a parent directory called test (e.g. "abc\test\def\ghi\XYZ123").
 * <p/>
 * Case sensitivity may be turned off if necessary. By default, it is
 * turned on.
 * <p/>
 * Example of usage:
 * <pre>
 *   String[] includes = {"**\\*.class"};
 *   String[] excludes = {"modules\\*\\**"};
 *   ds.setIncludes(includes);
 *   ds.setExcludes(excludes);
 *   ds.setBasedir(new File("test"));
 *   ds.setCaseSensitive(true);
 *   ds.scan();
 * <p/>
 *   System.out.println("FILES:");
 *   String[] files = ds.getIncludedFiles();
 *   for (int i = 0; i < files.length; i++) {
 *     System.out.println(files[i]);
 *   }
 * </pre>
 * This will scan a directory called test for .class files, but excludes all
 * files in all proper subdirectories of a directory called "modules"
 *
 * @author Arnout J. Kuiper
 *         <a href="mailto:ajkuiper@wxs.nl">ajkuiper@wxs.nl</a>
 * @author Magesh Umasankar
 * @author <a href="mailto:bruce@callenish.com">Bruce Atherton</a>
 * @author <a href="mailto:levylambert@tiscali-dsl.de">Antoine Levy-Lambert</a>
 */
public class DirectoryScanner
{
    /**
     * Patterns which should be excluded by default.
     *
     * @see #addDefaultExcludes()
     */
    protected static final String[] DEFAULTEXCLUDES = {
        // Miscellaneous typical temporary files
        "**/*~",
        "**/#*#",
        "**/.#*",
        "**/%*%",
        "**/._*",

        // CVS
        "**/CVS",
        "**/CVS/**",
        "**/.cvsignore",

        // SCCS
        "**/SCCS",
        "**/SCCS/**",

        // Visual SourceSafe
        "**/vssver.scc",

        // Subversion
        "**/.svn",
        "**/.svn/**",

        // Mac
        "**/.DS_Store"
    };

    /**
     * The base directory to be scanned.
     */
    protected File basedir;

    /**
     * The patterns for the files to be included.
     */
    protected String[] includes;

    /**
     * The patterns for the files to be excluded.
     */
    protected String[] excludes;

    /**
     * The files which matched at least one include and no excludes
     * and were selected.
     */
    protected Vector filesIncluded;

    /**
     * The files which did not match any includes or selectors.
     */
    protected Vector filesNotIncluded;

    /**
     * The files which matched at least one include and at least
     * one exclude.
     */
    protected Vector filesExcluded;

    /**
     * The directories which matched at least one include and no excludes
     * and were selected.
     */
    protected Vector dirsIncluded;

    /**
     * The directories which were found and did not match any includes.
     */
    protected Vector dirsNotIncluded;

    /**
     * The directories which matched at least one include and at least one
     * exclude.
     */
    protected Vector dirsExcluded;

    /**
     * The files which matched at least one include and no excludes and
     * which a selector discarded.
     */
    protected Vector filesDeselected;

    /**
     * The directories which matched at least one include and no excludes
     * but which a selector discarded.
     */
    protected Vector dirsDeselected;

    /**
     * Whether or not our results were built by a slow scan.
     */
    protected boolean haveSlowResults = false;

    /**
     * Whether or not the file system should be treated as a case sensitive
     * one.
     */
    protected boolean isCaseSensitive = true;

    /**
     * Whether or not symbolic links should be followed.
     *
     * @since Ant 1.5
     */
    private boolean followSymlinks = true;

    /**
     * Whether or not everything tested so far has been included.
     */
    protected boolean everythingIncluded = true;

    /**
     * Sole constructor.
     */
    public DirectoryScanner()
    {
    }

    /**
     * Tests whether or not a given path matches the start of a given
     * pattern up to the first "**".
     * <p/>
     * This is not a general purpose test and should only be used if you
     * can live with false positives. For example, <code>pattern=**\a</code>
     * and <code>str=b</code> will yield <code>true</code>.
     *
     * @param pattern The pattern to match against. Must not be
     *                <code>null</code>.
     * @param str     The path to match, as a String. Must not be
     *                <code>null</code>.
     * @return whether or not a given path matches the start of a given
     *         pattern up to the first "**".
     */
    protected static boolean matchPatternStart( String pattern, String str )
    {
        return SelectorUtils.matchPatternStart( pattern, str );
    }

    /**
     * Tests whether or not a given path matches the start of a given
     * pattern up to the first "**".
     * <p/>
     * This is not a general purpose test and should only be used if you
     * can live with false positives. For example, <code>pattern=**\a</code>
     * and <code>str=b</code> will yield <code>true</code>.
     *
     * @param pattern         The pattern to match against. Must not be
     *                        <code>null</code>.
     * @param str             The path to match, as a String. Must not be
     *                        <code>null</code>.
     * @param isCaseSensitive Whether or not matching should be performed
     *                        case sensitively.
     * @return whether or not a given path matches the start of a given
     *         pattern up to the first "**".
     */
    protected static boolean matchPatternStart( String pattern, String str,
                                                boolean isCaseSensitive )
    {
        return SelectorUtils.matchPatternStart( pattern, str, isCaseSensitive );
    }

    /**
     * Tests whether or not a given path matches a given pattern.
     *
     * @param pattern The pattern to match against. Must not be
     *                <code>null</code>.
     * @param str     The path to match, as a String. Must not be
     *                <code>null</code>.
     * @return <code>true</code> if the pattern matches against the string,
     *         or <code>false</code> otherwise.
     */
    protected static boolean matchPath( String pattern, String str )
    {
        return SelectorUtils.matchPath( pattern, str );
    }

    /**
     * Tests whether or not a given path matches a given pattern.
     *
     * @param pattern         The pattern to match against. Must not be
     *                        <code>null</code>.
     * @param str             The path to match, as a String. Must not be
     *                        <code>null</code>.
     * @param isCaseSensitive Whether or not matching should be performed
     *                        case sensitively.
     * @return <code>true</code> if the pattern matches against the string,
     *         or <code>false</code> otherwise.
     */
    protected static boolean matchPath( String pattern, String str,
                                        boolean isCaseSensitive )
    {
        return SelectorUtils.matchPath( pattern, str, isCaseSensitive );
    }

    /**
     * Tests whether or not a string matches against a pattern.
     * The pattern may contain two special characters:<br>
     * '*' means zero or more characters<br>
     * '?' means one and only one character
     *
     * @param pattern The pattern to match against.
     *                Must not be <code>null</code>.
     * @param str     The string which must be matched against the pattern.
     *                Must not be <code>null</code>.
     * @return <code>true</code> if the string matches against the pattern,
     *         or <code>false</code> otherwise.
     */
    public static boolean match( String pattern, String str )
    {
        return SelectorUtils.match( pattern, str );
    }

    /**
     * Tests whether or not a string matches against a pattern.
     * The pattern may contain two special characters:<br>
     * '*' means zero or more characters<br>
     * '?' means one and only one character
     *
     * @param pattern         The pattern to match against.
     *                        Must not be <code>null</code>.
     * @param str             The string which must be matched against the pattern.
     *                        Must not be <code>null</code>.
     * @param isCaseSensitive Whether or not matching should be performed
     *                        case sensitively.
     * @return <code>true</code> if the string matches against the pattern,
     *         or <code>false</code> otherwise.
     */
    protected static boolean match( String pattern, String str,
                                    boolean isCaseSensitive )
    {
        return SelectorUtils.match( pattern, str, isCaseSensitive );
    }

    /**
     * Sets the base directory to be scanned. This is the directory which is
     * scanned recursively. All '/' and '\' characters are replaced by
     * <code>File.separatorChar</code>, so the separator used need not match
     * <code>File.separatorChar</code>.
     *
     * @param basedir The base directory to scan.
     *                Must not be <code>null</code>.
     */
    public void setBasedir( String basedir )
    {
        setBasedir( new File( basedir.replace( '/', File.separatorChar ).replace( '\\', File.separatorChar ) ) );
    }

    /**
     * Sets the base directory to be scanned. This is the directory which is
     * scanned recursively.
     *
     * @param basedir The base directory for scanning.
     *                Should not be <code>null</code>.
     */
    public void setBasedir( File basedir )
    {
        this.basedir = basedir;
    }

    /**
     * Returns the base directory to be scanned.
     * This is the directory which is scanned recursively.
     *
     * @return the base directory to be scanned
     */
    public File getBasedir()
    {
        return basedir;
    }

    /**
     * Sets whether or not the file system should be regarded as case sensitive.
     *
     * @param isCaseSensitive whether or not the file system should be
     *                        regarded as a case sensitive one
     */
    public void setCaseSensitive( boolean isCaseSensitive )
    {
        this.isCaseSensitive = isCaseSensitive;
    }

    /**
     * Sets whether or not symbolic links should be followed.
     *
     * @param followSymlinks whether or not symbolic links should be followed
     */
    public void setFollowSymlinks( boolean followSymlinks )
    {
        this.followSymlinks = followSymlinks;
    }

    /**
     * Sets the list of include patterns to use. All '/' and '\' characters
     * are replaced by <code>File.separatorChar</code>, so the separator used
     * need not match <code>File.separatorChar</code>.
     * <p/>
     * When a pattern ends with a '/' or '\', "**" is appended.
     *
     * @param includes A list of include patterns.
     *                 May be <code>null</code>, indicating that all files
     *                 should be included. If a non-<code>null</code>
     *                 list is given, all elements must be
     *                 non-<code>null</code>.
     */
    public void setIncludes( String[] includes )
    {
        if ( includes == null )
        {
            this.includes = null;
        }
        else
        {
            this.includes = new String[includes.length];
            for ( int i = 0; i < includes.length; i++ )
            {
                String pattern;
                pattern = includes[i].replace( '/', File.separatorChar ).replace( '\\', File.separatorChar );
                if ( pattern.endsWith( File.separator ) )
                {
                    pattern += "**";
                }
                this.includes[i] = pattern;
            }
        }
    }


    /**
     * Sets the list of exclude patterns to use. All '/' and '\' characters
     * are replaced by <code>File.separatorChar</code>, so the separator used
     * need not match <code>File.separatorChar</code>.
     * <p/>
     * When a pattern ends with a '/' or '\', "**" is appended.
     *
     * @param excludes A list of exclude patterns.
     *                 May be <code>null</code>, indicating that no files
     *                 should be excluded. If a non-<code>null</code> list is
     *                 given, all elements must be non-<code>null</code>.
     */
    public void setExcludes( String[] excludes )
    {
        if ( excludes == null )
        {
            this.excludes = null;
        }
        else
        {
            this.excludes = new String[excludes.length];
            for ( int i = 0; i < excludes.length; i++ )
            {
                String pattern;
                pattern = excludes[i].replace( '/', File.separatorChar ).replace( '\\', File.separatorChar );
                if ( pattern.endsWith( File.separator ) )
                {
                    pattern += "**";
                }
                this.excludes[i] = pattern;
            }
        }
    }

    /**
     * Returns whether or not the scanner has included all the files or
     * directories it has come across so far.
     *
     * @return <code>true</code> if all files and directories which have
     *         been found so far have been included.
     */
    public boolean isEverythingIncluded()
    {
        return everythingIncluded;
    }

    /**
     * Scans the base directory for files which match at least one include
     * pattern and don't match any exclude patterns. If there are selectors
     * then the files must pass muster there, as well.
     *
     * @throws IllegalStateException if the base directory was set
     *                               incorrectly (i.e. if it is <code>null</code>, doesn't exist,
     *                               or isn't a directory).
     */
    public void scan() throws IllegalStateException
    {
        if ( basedir == null )
        {
            throw new IllegalStateException( "No basedir set" );
        }
        if ( !basedir.exists() )
        {
            throw new IllegalStateException( "basedir " + basedir
                                             + " does not exist" );
        }
        if ( !basedir.isDirectory() )
        {
            throw new IllegalStateException( "basedir " + basedir
                                             + " is not a directory" );
        }

        if ( includes == null )
        {
            // No includes supplied, so set it to 'matches all'
            includes = new String[1];
            includes[0] = "**";
        }
        if ( excludes == null )
        {
            excludes = new String[0];
        }

        filesIncluded = new Vector();
        filesNotIncluded = new Vector();
        filesExcluded = new Vector();
        filesDeselected = new Vector();
        dirsIncluded = new Vector();
        dirsNotIncluded = new Vector();
        dirsExcluded = new Vector();
        dirsDeselected = new Vector();

        if ( isIncluded( "" ) )
        {
            if ( !isExcluded( "" ) )
            {
                if ( isSelected( "", basedir ) )
                {
                    dirsIncluded.addElement( "" );
                }
                else
                {
                    dirsDeselected.addElement( "" );
                }
            }
            else
            {
                dirsExcluded.addElement( "" );
            }
        }
        else
        {
            dirsNotIncluded.addElement( "" );
        }
        scandir( basedir, "", true );
    }

    /**
     * Top level invocation for a slow scan. A slow scan builds up a full
     * list of excluded/included files/directories, whereas a fast scan
     * will only have full results for included files, as it ignores
     * directories which can't possibly hold any included files/directories.
     * <p/>
     * Returns immediately if a slow scan has already been completed.
     */
    protected void slowScan()
    {
        if ( haveSlowResults )
        {
            return;
        }

        String[] excl = new String[dirsExcluded.size()];
        dirsExcluded.copyInto( excl );

        String[] notIncl = new String[dirsNotIncluded.size()];
        dirsNotIncluded.copyInto( notIncl );

        for ( int i = 0; i < excl.length; i++ )
        {
            if ( !couldHoldIncluded( excl[i] ) )
            {
                scandir( new File( basedir, excl[i] ),
                         excl[i] + File.separator, false );
            }
        }

        for ( int i = 0; i < notIncl.length; i++ )
        {
            if ( !couldHoldIncluded( notIncl[i] ) )
            {
                scandir( new File( basedir, notIncl[i] ),
                         notIncl[i] + File.separator, false );
            }
        }

        haveSlowResults = true;
    }

    /**
     * Scans the given directory for files and directories. Found files and
     * directories are placed in their respective collections, based on the
     * matching of includes, excludes, and the selectors.  When a directory
     * is found, it is scanned recursively.
     *
     * @param dir   The directory to scan. Must not be <code>null</code>.
     * @param vpath The path relative to the base directory (needed to
     *              prevent problems with an absolute path when using
     *              dir). Must not be <code>null</code>.
     * @param fast  Whether or not this call is part of a fast scan.
     * @see #filesIncluded
     * @see #filesNotIncluded
     * @see #filesExcluded
     * @see #dirsIncluded
     * @see #dirsNotIncluded
     * @see #dirsExcluded
     * @see #slowScan
     */
    protected void scandir( File dir, String vpath, boolean fast )
    {
        String[] newfiles = dir.list();

        if ( newfiles == null )
        {
            /*
             * two reasons are mentioned in the API docs for File.list
             * (1) dir is not a directory. This is impossible as
             *     we wouldn't get here in this case.
             * (2) an IO error occurred (why doesn't it throw an exception
             *     then???)
             */
            //throw new Exception( "IO error scanning directory " + dir.getAbsolutePath() );
        }

        if ( !followSymlinks )
        {
            Vector noLinks = new Vector();
            for ( int i = 0; i < newfiles.length; i++ )
            {
                try
                {
                    if ( isSymbolicLink( dir, newfiles[i] ) )
                    {
                        String name = vpath + newfiles[i];
                        File file = new File( dir, newfiles[i] );
                        if ( file.isDirectory() )
                        {
                            dirsExcluded.addElement( name );
                        }
                        else
                        {
                            filesExcluded.addElement( name );
                        }
                    }
                    else
                    {
                        noLinks.addElement( newfiles[i] );
                    }
                }
                catch ( IOException ioe )
                {
                    String msg = "IOException caught while checking "
                        + "for links, couldn't get cannonical path!";
                    // will be caught and redirected to Ant's logging system
                    System.err.println( msg );
                    noLinks.addElement( newfiles[i] );
                }
            }
            newfiles = new String[noLinks.size()];
            noLinks.copyInto( newfiles );
        }

        for ( int i = 0; i < newfiles.length; i++ )
        {
            String name = vpath + newfiles[i];
            File file = new File( dir, newfiles[i] );
            if ( file.isDirectory() )
            {
                if ( isIncluded( name ) )
                {
                    if ( !isExcluded( name ) )
                    {
                        if ( isSelected( name, file ) )
                        {
                            dirsIncluded.addElement( name );
                            if ( fast )
                            {
                                scandir( file, name + File.separator, fast );
                            }
                        }
                        else
                        {
                            everythingIncluded = false;
                            dirsDeselected.addElement( name );
                            if ( fast && couldHoldIncluded( name ) )
                            {
                                scandir( file, name + File.separator, fast );
                            }
                        }

                    }
                    else
                    {
                        everythingIncluded = false;
                        dirsExcluded.addElement( name );
                        if ( fast && couldHoldIncluded( name ) )
                        {
                            scandir( file, name + File.separator, fast );
                        }
                    }
                }
                else
                {
                    everythingIncluded = false;
                    dirsNotIncluded.addElement( name );
                    if ( fast && couldHoldIncluded( name ) )
                    {
                        scandir( file, name + File.separator, fast );
                    }
                }
                if ( !fast )
                {
                    scandir( file, name + File.separator, fast );
                }
            }
            else if ( file.isFile() )
            {
                if ( isIncluded( name ) )
                {
                    if ( !isExcluded( name ) )
                    {
                        if ( isSelected( name, file ) )
                        {
                            filesIncluded.addElement( name );
                        }
                        else
                        {
                            everythingIncluded = false;
                            filesDeselected.addElement( name );
                        }
                    }
                    else
                    {
                        everythingIncluded = false;
                        filesExcluded.addElement( name );
                    }
                }
                else
                {
                    everythingIncluded = false;
                    filesNotIncluded.addElement( name );
                }
            }
        }
    }

    /**
     * Tests whether or not a name matches against at least one include
     * pattern.
     *
     * @param name The name to match. Must not be <code>null</code>.
     * @return <code>true</code> when the name matches against at least one
     *         include pattern, or <code>false</code> otherwise.
     */
    protected boolean isIncluded( String name )
    {
        for ( int i = 0; i < includes.length; i++ )
        {
            if ( matchPath( includes[i], name, isCaseSensitive ) )
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Tests whether or not a name matches the start of at least one include
     * pattern.
     *
     * @param name The name to match. Must not be <code>null</code>.
     * @return <code>true</code> when the name matches against the start of at
     *         least one include pattern, or <code>false</code> otherwise.
     */
    protected boolean couldHoldIncluded( String name )
    {
        for ( int i = 0; i < includes.length; i++ )
        {
            if ( matchPatternStart( includes[i], name, isCaseSensitive ) )
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Tests whether or not a name matches against at least one exclude
     * pattern.
     *
     * @param name The name to match. Must not be <code>null</code>.
     * @return <code>true</code> when the name matches against at least one
     *         exclude pattern, or <code>false</code> otherwise.
     */
    protected boolean isExcluded( String name )
    {
        for ( int i = 0; i < excludes.length; i++ )
        {
            if ( matchPath( excludes[i], name, isCaseSensitive ) )
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Tests whether a name should be selected.
     *
     * @param name the filename to check for selecting
     * @param file the java.io.File object for this filename
     * @return <code>false</code> when the selectors says that the file
     *         should not be selected, <code>true</code> otherwise.
     */
    protected boolean isSelected( String name, File file )
    {
        return true;
    }

    /**
     * Returns the names of the files which matched at least one of the
     * include patterns and none of the exclude patterns.
     * The names are relative to the base directory.
     *
     * @return the names of the files which matched at least one of the
     *         include patterns and none of the exclude patterns.
     */
    public String[] getIncludedFiles()
    {
        String[] files = new String[filesIncluded.size()];
        filesIncluded.copyInto( files );
        return files;
    }

    /**
     * Returns the names of the files which matched none of the include
     * patterns. The names are relative to the base directory. This involves
     * performing a slow scan if one has not already been completed.
     *
     * @return the names of the files which matched none of the include
     *         patterns.
     * @see #slowScan
     */
    public String[] getNotIncludedFiles()
    {
        slowScan();
        String[] files = new String[filesNotIncluded.size()];
        filesNotIncluded.copyInto( files );
        return files;
    }

    /**
     * Returns the names of the files which matched at least one of the
     * include patterns and at least one of the exclude patterns.
     * The names are relative to the base directory. This involves
     * performing a slow scan if one has not already been completed.
     *
     * @return the names of the files which matched at least one of the
     *         include patterns and at at least one of the exclude patterns.
     * @see #slowScan
     */
    public String[] getExcludedFiles()
    {
        slowScan();
        String[] files = new String[filesExcluded.size()];
        filesExcluded.copyInto( files );
        return files;
    }

    /**
     * <p>Returns the names of the files which were selected out and
     * therefore not ultimately included.</p>
     * <p/>
     * <p>The names are relative to the base directory. This involves
     * performing a slow scan if one has not already been completed.</p>
     *
     * @return the names of the files which were deselected.
     * @see #slowScan
     */
    public String[] getDeselectedFiles()
    {
        slowScan();
        String[] files = new String[filesDeselected.size()];
        filesDeselected.copyInto( files );
        return files;
    }

    /**
     * Returns the names of the directories which matched at least one of the
     * include patterns and none of the exclude patterns.
     * The names are relative to the base directory.
     *
     * @return the names of the directories which matched at least one of the
     *         include patterns and none of the exclude patterns.
     */
    public String[] getIncludedDirectories()
    {
        String[] directories = new String[dirsIncluded.size()];
        dirsIncluded.copyInto( directories );
        return directories;
    }

    /**
     * Returns the names of the directories which matched none of the include
     * patterns. The names are relative to the base directory. This involves
     * performing a slow scan if one has not already been completed.
     *
     * @return the names of the directories which matched none of the include
     *         patterns.
     * @see #slowScan
     */
    public String[] getNotIncludedDirectories()
    {
        slowScan();
        String[] directories = new String[dirsNotIncluded.size()];
        dirsNotIncluded.copyInto( directories );
        return directories;
    }

    /**
     * Returns the names of the directories which matched at least one of the
     * include patterns and at least one of the exclude patterns.
     * The names are relative to the base directory. This involves
     * performing a slow scan if one has not already been completed.
     *
     * @return the names of the directories which matched at least one of the
     *         include patterns and at least one of the exclude patterns.
     * @see #slowScan
     */
    public String[] getExcludedDirectories()
    {
        slowScan();
        String[] directories = new String[dirsExcluded.size()];
        dirsExcluded.copyInto( directories );
        return directories;
    }

    /**
     * <p>Returns the names of the directories which were selected out and
     * therefore not ultimately included.</p>
     * <p/>
     * <p>The names are relative to the base directory. This involves
     * performing a slow scan if one has not already been completed.</p>
     *
     * @return the names of the directories which were deselected.
     * @see #slowScan
     */
    public String[] getDeselectedDirectories()
    {
        slowScan();
        String[] directories = new String[dirsDeselected.size()];
        dirsDeselected.copyInto( directories );
        return directories;
    }

    /**
     * Adds default exclusions to the current exclusions set.
     */
    public void addDefaultExcludes()
    {
        int excludesLength = excludes == null ? 0 : excludes.length;
        String[] newExcludes;
        newExcludes = new String[excludesLength + DEFAULTEXCLUDES.length];
        if ( excludesLength > 0 )
        {
            System.arraycopy( excludes, 0, newExcludes, 0, excludesLength );
        }
        for ( int i = 0; i < DEFAULTEXCLUDES.length; i++ )
        {
            newExcludes[i + excludesLength] = DEFAULTEXCLUDES[i].replace( '/',
                                                                          File.separatorChar ).replace( '\\', File.separatorChar );
        }
        excludes = newExcludes;
    }

    /**
     * Checks whether a given file is a symbolic link.
     * <p/>
     * <p>It doesn't really test for symbolic links but whether the
     * canonical and absolute paths of the file are identical - this
     * may lead to false positives on some platforms.</p>
     *
     * @param parent the parent directory of the file to test
     * @param name   the name of the file to test.
     * @since Ant 1.5
     */
    public boolean isSymbolicLink( File parent, String name )
        throws IOException
    {
        File resolvedParent = new File( parent.getCanonicalPath() );
        File toTest = new File( resolvedParent, name );
        return !toTest.getAbsolutePath().equals( toTest.getCanonicalPath() );
    }

}
