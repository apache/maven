/*
 * $Id$
 */

package org.apache.maven.model.v4_0_0;

//---------------------------------/
//- Imported classes and packages -/
//---------------------------------/

import java.util.ArrayList;
import java.util.List;

/**
 * Class PatternSet.
 * 
 * @version $Revision$ $Date$
 */
public class PatternSet
    implements java.io.Serializable
{

    //--------------------------/
    //- Class/Member Variables -/
    //--------------------------/

    /**
     * Field includes
     */
    private String includes;

    /**
     * Field excludes
     */
    private String excludes;

    //-----------/
    //- Methods -/
    //-----------/

    /**
     * Method getExcludes
     */
    public String getExcludes()
    {
        return this.excludes;
    } //-- String getExcludes() 

    /**
     * Method getIncludes
     */
    public String getIncludes()
    {
        return this.includes;
    } //-- String getIncludes() 

    /**
     * Method setExcludes
     * 
     * @param excludes
     */
    public void setExcludes( String excludes )
    {
        this.excludes = excludes;
    } //-- void setExcludes(String) 

    /**
     * Method setIncludes
     * 
     * @param includes
     */
    public void setIncludes( String includes )
    {
        this.includes = includes;
    } //-- void setIncludes(String) 

    public List getDefaultExcludes()
    {
        List defaultExcludes = new ArrayList();
        defaultExcludes.add( "**/*~" );
        defaultExcludes.add( "**/#*#" );
        defaultExcludes.add( "**/.#*" );
        defaultExcludes.add( "**/%*%" );
        defaultExcludes.add( "**/._*" );

        // CVS
        defaultExcludes.add( "**/CVS" );
        defaultExcludes.add( "**/CVS/**" );
        defaultExcludes.add( "**/.cvsignore" );

        // SCCS
        defaultExcludes.add( "**/SCCS" );
        defaultExcludes.add( "**/SCCS/**" );

        // Visual SourceSafe
        defaultExcludes.add( "**/vssver.scc" );

        // Subversion
        defaultExcludes.add( "**/.svn" );
        defaultExcludes.add( "**/.svn/**" );

        // Mac
        defaultExcludes.add( "**/.DS_Store" );
        return defaultExcludes;
    }
}