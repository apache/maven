/**
 *
 * Copyright 2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package compile;

import util.IsolatedClassLoader;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

public class JavacCompiler
    extends AbstractCompiler
{
    static final int OUTPUT_BUFFER_SIZE = 1024;

    public JavacCompiler()
    {
    }

    public List compile( CompilerConfiguration config ) throws Exception
    {
        File destinationDir = new File( config.getOutputLocation() );

        if ( !destinationDir.exists() )
        {
            destinationDir.mkdirs();
        }

        String[] sources = getSourceFiles( config );

        if ( sources.length == 0 )
        {
            return Collections.EMPTY_LIST;
        }

        System.out.println( "Compiling " + sources.length + " source file" + ( sources.length == 1 ? "" : "s" )
            + " to " + destinationDir.getAbsolutePath() );

        Map compilerOptions = config.getCompilerOptions();

        List args = new ArrayList( sources.length + 5 + compilerOptions.size() * 2 );

        args.add( "-d" );

        args.add( destinationDir.getAbsolutePath() );

        args.add( "-nowarn" );

        args.add( "-classpath" );

        args.add( getClasspathString( config.getClasspathEntries() ) );

        if ( config.isDebug() )
        {
            args.add( "-g" );
        }

        Iterator it = compilerOptions.entrySet().iterator();

        while ( it.hasNext() )
        {
            Map.Entry entry = (Map.Entry) it.next();
            args.add( entry.getKey() );
            if ( (entry.getValue() != null) )
                args.add( entry.getValue() );
        }

        for ( int i = 0; i < sources.length; i++ )
        {
            args.add( sources[i] );
        }

        IsolatedClassLoader cl = new IsolatedClassLoader();

        File toolsJar = new File( System.getProperty( "java.home" ), "../lib/tools.jar" );

        cl.addURL( toolsJar.toURL() );

        Class c = cl.loadClass( "sun.tools.javac.Main" );

        Constructor cons = c.getConstructor( new Class[] { OutputStream.class, String.class } );

        ByteArrayOutputStream err = new ByteArrayOutputStream();

        Object compiler = cons.newInstance( new Object[] { err, "javac" } );

        Method compile = c.getMethod( "compile", new Class[] { String[].class } );

        Boolean ok = (Boolean) compile.invoke( compiler, new Object[] { args.toArray( new String[0] ) } );

        List messages = parseModernStream( new BufferedReader( new InputStreamReader( new ByteArrayInputStream( err.toByteArray() ) ) ) );

        if ( !ok.booleanValue() && messages.isEmpty() )
        {
            // TODO: don't throw exception
            throw new Exception( "Failure executing javac, but could not parse the error:\n\n" + err.toString() );
        }

        return messages;
    }

    protected List parseModernStream( BufferedReader input ) throws IOException
    {
        List errors = new ArrayList();

        String line = null;

        StringBuffer buffer = null;

        while ( true )
        {
            // cleanup the buffer
            buffer = new StringBuffer(); // this is quicker than clearing it

            // most errors terminate with the '^' char
            do
            {
                if ( (line = input.readLine()) == null )
                {
                    return errors;
                }

                if ( buffer.length() == 0 && line.startsWith( "error: " ) )
                {
                    errors.add( new CompilerError( line ) );
                }
                else
                {
                    buffer.append( line );

                    buffer.append( '\n' );
                }
            }
            while ( !line.endsWith( "^" ) );

            // add the error bean
            errors.add( parseModernError( buffer.toString() ) );
        }
    }

    private CompilerError parseModernError( String error )
    {
        StringTokenizer tokens = new StringTokenizer( error, ":" );

        try
        {
            String file = tokens.nextToken();

            if ( file.length() == 1 )
            {
                file = new StringBuffer( file ).append( ":" ).append( tokens.nextToken() ).toString();
            }

            int line = Integer.parseInt( tokens.nextToken() );

            String message = tokens.nextToken( "\n" ).substring( 1 );

            String context = tokens.nextToken( "\n" );

            String pointer = tokens.nextToken( "\n" );

            int startcolumn = pointer.indexOf( "^" );

            int endcolumn = context.indexOf( " ", startcolumn );

            if ( endcolumn == -1 )
            {
                endcolumn = context.length();
            }

            return new CompilerError( file, true, line, startcolumn, line, endcolumn, message );
        }
        catch ( NoSuchElementException nse )
        {
            return new CompilerError( "no more tokens - could not parse error message: " + error );
        }
        catch ( Exception nse )
        {
            return new CompilerError( "could not parse error message: " + error );
        }
    }

    public String toString()
    {
        return "Sun Javac Compiler";
    }
}