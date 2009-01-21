package org.apache.maven.project.builder.rules;

import org.apache.maven.shared.model.ModelProperty;
import org.apache.maven.project.builder.ProjectUri;

import java.io.IOException;
import java.util.List;
import java.util.Arrays;

public class ExecutionRuleTest {

    @org.junit.Test
    public void execute() throws IOException
    {
        List<ModelProperty> modelProperties = Arrays.asList(
                new ModelProperty(ProjectUri.Build.Plugins.Plugin.Executions.Execution.xUri, null),
                new ModelProperty(ProjectUri.Build.Plugins.Plugin.Executions.Execution.Goals.xURI, null),
                new ModelProperty(ProjectUri.Build.Plugins.Plugin.Executions.Execution.Goals.goal, "parent-a"),
                new ModelProperty(ProjectUri.Build.Plugins.Plugin.Executions.Execution.Goals.goal, "merged"),
                new ModelProperty(ProjectUri.Build.Plugins.Plugin.Executions.Execution.Goals.goal, "parent-b"),
                new ModelProperty(ProjectUri.Build.Plugins.Plugin.Executions.Execution.Goals.xURI, null),
                new ModelProperty(ProjectUri.Build.Plugins.Plugin.Executions.Execution.Goals.goal, "child-b"),
                new ModelProperty(ProjectUri.Build.Plugins.Plugin.Executions.Execution.Goals.goal, "merged"),
                new ModelProperty(ProjectUri.Build.Plugins.Plugin.Executions.Execution.Goals.goal, "child-a"));

        List<ModelProperty> mps = new ExecutionRule().execute(modelProperties);
        for(ModelProperty mp : mps) {
            //System.out.println(mp);
        }
    }
}
