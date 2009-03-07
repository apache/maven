package org.apache.maven.mercury;

import java.util.List;
import java.util.Map;

import org.apache.maven.mercury.artifact.ArtifactMetadata;
import org.apache.maven.mercury.builder.api.MetadataReader;
import org.apache.maven.mercury.builder.api.MetadataReaderException;
import org.apache.maven.shared.model.ModelProperty;

public interface PomProcessor
{
    List<ModelProperty> getRawPom(ArtifactMetadata bmd, MetadataReader mdReader, Map<String, String>  env,
                                  Map<String, String>  sysProps)
            throws MetadataReaderException, PomProcessorException;
}
