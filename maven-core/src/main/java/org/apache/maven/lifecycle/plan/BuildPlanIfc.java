package org.apache.maven.lifecycle.plan;

import org.apache.maven.lifecycle.LifecycleLoaderException;
import org.apache.maven.lifecycle.LifecycleSpecificationException;
import org.apache.maven.lifecycle.binding.LifecycleBindingManager;
import org.apache.maven.project.MavenProject;

import java.util.List;

public interface BuildPlanIfc
    extends ModifiablePlanElement
{

    List getPlanMojoBindings(MavenProject project, LifecycleBindingManager bindingManager)
        throws LifecycleSpecificationException, LifecyclePlannerException, LifecycleLoaderException;

    List getTasks();

}
