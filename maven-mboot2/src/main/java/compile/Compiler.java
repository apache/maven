package compile;

import java.util.List;
import java.util.Map;

/**
 *
 *
 * @author <a href="mailto:jason@plexus.org">Jason van Zyl</a>
 *
 * @version $Id$
 */
public interface Compiler
{
    static String ROLE = Compiler.class.getName();

    List compile( CompilerConfiguration configuration )
        throws Exception;
}

