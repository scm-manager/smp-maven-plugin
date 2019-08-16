package sonia.scm.maven;

public interface ArtifactItem {
  String getArtifactId();
  String getGroupId();
  String getVersion();
  String getType();
}
