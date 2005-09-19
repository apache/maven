package org.apache.maven.acm.text;

import org.apache.maven.acm.model.Environment;
import org.apache.maven.acm.model.Model;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Write out an application configuration management model in the
 * simple text format.
 *
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id:$
 * @todo groups of properties
 * @todo sorting
 * @todo order of environments inside a propertyKey setting
 */
public class AcmTextDescriptorWriter
{
    public void write( PrintWriter writer, Model model)
        throws Exception
    {
        writeEnvironments( writer, model );

        writeProperties( writer, model );

        writer.flush();

        writer.close();
    }

    protected void writeEnvironments( PrintWriter writer, Model model )
        throws Exception
    {
        for ( Iterator i = model.getEnvironments().values().iterator(); i.hasNext(); )
        {
            Environment e = (Environment) i.next();

            writer.print( "+" );

            writer.print( e.getId() );

            writer.print( " = " );

            writer.println( e.getDescription() );
        }

        writer.println();
    }

    protected void writeProperties( PrintWriter writer, Model model )
        throws Exception
    {
        List sortedPropertyKeys = new ArrayList( model.getPropertyKeySet() );

        Collections.sort( sortedPropertyKeys );

        for ( Iterator i = sortedPropertyKeys.iterator(); i.hasNext(); )
        {
            String propertyKey = (String) i.next();

            writer.print( "*" );

            writer.println( propertyKey );

            writer.println( "{" );

            // ----------------------------------------------------------------------
            // For each environment state the value for this property key
            // ----------------------------------------------------------------------

            for ( Iterator j = model.getEnvironments().values().iterator(); j.hasNext(); )
            {
                Environment e = (Environment) j.next();

                writer.print( "  " );

                writer.print( e.getId() );

                writer.print( " = " );

                writer.println( e.getProperty( propertyKey ) );
            }

            writer.println( "}" );

            writer.println();
        }
    }
}
