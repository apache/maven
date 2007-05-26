package org.apache.maven.lifecycle;

import org.apache.maven.lifecycle.model.MojoBinding;

public final class MojoBindingUtils
{

    private MojoBindingUtils()
    {
    }

    public static String toString( final MojoBinding mojoBinding )
    {
        return mojoBinding.getGroupId() + ":" + mojoBinding.getArtifactId() + ":"
                        + ( mojoBinding.getVersion() == null ? "" : mojoBinding.getVersion() + ":" )
                        + mojoBinding.getGoal();
    }

    public static String createMojoBindingKey( final MojoBinding mojoBinding, final boolean considerExecutionId )
    {
        String key = mojoBinding.getGroupId() + ":" + mojoBinding.getArtifactId() + ":" + mojoBinding.getGoal();

        if ( considerExecutionId )
        {
            key += ":" + mojoBinding.getExecutionId();
        }

        return key;
    }

    public static String createPluginKey( final MojoBinding binding )
    {
        String result = binding.getGroupId() + ":" + binding.getArtifactId();

        if ( binding.getVersion() != null )
        {
            result += ":" + binding.getVersion();
        }

        return result;
    }

}
