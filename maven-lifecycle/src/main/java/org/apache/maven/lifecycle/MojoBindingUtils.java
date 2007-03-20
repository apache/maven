package org.apache.maven.lifecycle;

import org.apache.maven.lifecycle.model.MojoBinding;

public final class MojoBindingUtils
{

    private MojoBindingUtils()
    {
    }

    public static String toString( MojoBinding mojoBinding )
    {
        return mojoBinding.getGroupId() + ":" + mojoBinding.getArtifactId() + ":"
            + ( mojoBinding.getVersion() == null ? "" : mojoBinding.getVersion() + ":" ) + mojoBinding.getGoal();
    }

}
