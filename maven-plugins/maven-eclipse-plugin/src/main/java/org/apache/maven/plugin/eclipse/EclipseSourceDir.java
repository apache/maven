package org.apache.maven.plugin.eclipse;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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

/**
 * Represent an eclipse source dir. Eclipse has no "main", "test" or "resource" concepts, so two source dirs with the
 * same path are equal.
 * @author <a href="mailto:fgiust@users.sourceforge.net">Fabrizio Giustina</a>
 * @version $Id$
 */
public class EclipseSourceDir
    implements Comparable
{

    private String path;

    private String output;

    private String include;

    private String exclude;

    private boolean test;

    public EclipseSourceDir( String path, String output, boolean test, String include, String exclude )
    {
        this.path = path;
        this.output = output;
        this.test = test;
        this.include = include;
        this.exclude = exclude;
    }

    /**
     * Getter for <code>exclude</code>.
     * @return Returns the exclude.
     */
    public String getExclude()
    {
        return this.exclude;
    }

    /**
     * Setter for <code>exclude</code>.
     * @param exclude The exclude to set.
     */
    public void setExclude( String exclude )
    {
        this.exclude = exclude;
    }

    /**
     * Getter for <code>include</code>.
     * @return Returns the include.
     */
    public String getInclude()
    {
        return this.include;
    }

    /**
     * Setter for <code>include</code>.
     * @param include The include to set.
     */
    public void setInclude( String include )
    {
        this.include = include;
    }

    /**
     * Getter for <code>output</code>.
     * @return Returns the output.
     */
    public String getOutput()
    {
        return this.output;
    }

    /**
     * Setter for <code>output</code>.
     * @param output The output to set.
     */
    public void setOutput( String output )
    {
        this.output = output;
    }

    /**
     * Getter for <code>path</code>.
     * @return Returns the path.
     */
    public String getPath()
    {
        return this.path;
    }

    /**
     * Setter for <code>path</code>.
     * @param path The path to set.
     */
    public void setPath( String path )
    {
        this.path = path;
    }

    /**
     * Getter for <code>test</code>.
     * @return Returns the test.
     */
    public boolean isTest()
    {
        return this.test;
    }

    /**
     * Setter for <code>test</code>.
     * @param test The test to set.
     */
    public void setTest( boolean test )
    {
        this.test = test;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals( Object obj )
    {
        return this.path.equals( ( (EclipseSourceDir) obj ).path );
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode()
    {
        return this.path.hashCode();
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo( Object obj )
    {
        return this.path.compareTo( ( (EclipseSourceDir) obj ).path );
    }

}