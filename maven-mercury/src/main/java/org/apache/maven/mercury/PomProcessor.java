package org.apache.maven.mercury;

import org.apache.maven.mercury.builder.api.MetadataReaderException;
import org.apache.maven.mercury.builder.api.MetadataReader;
import org.apache.maven.mercury.artifact.ArtifactBasicMetadata;
import org.apache.maven.shared.model.ModelProperty;

import java.util.Map;
import java.util.List;

public interface PomProcessor
{
    List<ModelProperty> getRawPom(ArtifactBasicMetadata bmd, MetadataReader mdReader, Map env, Map sysProps)
            throws MetadataReaderException, PomProcessorException;
}
