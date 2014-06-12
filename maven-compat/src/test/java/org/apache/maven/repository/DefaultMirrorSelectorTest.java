package org.apache.maven.repository;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.codehaus.plexus.PlexusTestCase;

public class DefaultMirrorSelectorTest extends PlexusTestCase {

  public void testMirrorWithMirroOfPatternContainingANegationIsNotSelected() {
    ArtifactRepository repository = new DefaultArtifactRepository("snapshots.repo", "http://whatever", null);
    String pattern = "external:*, !snapshots.repo";
    boolean matches = DefaultMirrorSelector.matchPattern(repository, pattern);
    System.out.println(matches);
    assertFalse(matches);
  }
}
