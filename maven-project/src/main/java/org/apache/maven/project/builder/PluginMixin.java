package org.apache.maven.project.builder;

import org.apache.maven.shared.model.InputStreamDomainModel;
import org.apache.maven.shared.model.ModelProperty;
import org.apache.maven.shared.model.ModelMarshaller;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Model;
import org.apache.maven.model.Build;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;

import java.io.*;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;


public class PluginMixin implements InputStreamDomainModel {


    /**
     * Bytes containing the underlying model
     */
    private byte[] inputBytes;

    /**
     * History of joins and deletes of model properties
     */
    private String eventHistory;

    private List<ModelProperty> modelProperties;    

    public PluginMixin(Plugin plugin)
        throws IOException
    {
        if(plugin == null)
        {
            throw new IllegalArgumentException("plugin: null");
        }
        Model model = new Model();
        Build build = new Build();
        build.addPlugin(plugin);
        model.setBuild(build);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Writer out = null;
        MavenXpp3Writer writer = new MavenXpp3Writer();
        try
        {
            out = WriterFactory.newXmlWriter( baos );
            writer.write( out, model );
        }
        finally
        {
            if ( out != null )
            {
                out.close();
            }
        }
        inputBytes = baos.toByteArray();
    }

    public InputStream getInputStream() {
        byte[] copy = new byte[inputBytes.length];
        System.arraycopy( inputBytes, 0, copy, 0, inputBytes.length );
        return new ByteArrayInputStream( copy );
    }

    public List<ModelProperty> getModelProperties() throws IOException {
        if(modelProperties == null)
        {
            Set<String> s = new HashSet<String>();
            //TODO: Should add all collections from ProjectUri
            s.addAll(PomTransformer.URIS);
            s.add(ProjectUri.Build.PluginManagement.Plugins.Plugin.Executions.xUri);
            s.add(ProjectUri.DependencyManagement.Dependencies.Dependency.Exclusions.xUri);
            s.add(ProjectUri.Dependencies.Dependency.Exclusions.xUri);
            s.add(ProjectUri.Build.Plugins.Plugin.Executions.xUri);
            s.add(ProjectUri.Build.Plugins.Plugin.Executions.Execution.Goals.xURI);
            s.add(ProjectUri.Reporting.Plugins.Plugin.ReportSets.xUri);
            s.add(ProjectUri.Reporting.Plugins.Plugin.ReportSets.ReportSet.configuration);
            s.add(ProjectUri.Build.Plugins.Plugin.Executions.Execution.configuration);
            modelProperties = ModelMarshaller.marshallXmlToModelProperties(
                getInputStream(), ProjectUri.baseUri, s );
        }
        return new ArrayList<ModelProperty>(modelProperties);
    }

    public String getEventHistory() {
        return eventHistory;
    }

    public void setEventHistory(String eventHistory) {
        this.eventHistory = eventHistory;
    }

    /**
     * Returns XML model as string
     *
     * @return XML model as string
     */
    public String asString()
    {
        try
        {
            return IOUtil.toString( ReaderFactory.newXmlReader( new ByteArrayInputStream( inputBytes ) ) );
        }
        catch ( IOException ioe )
        {
            // should not occur: everything is in-memory
            return "";
        }
    }
}
