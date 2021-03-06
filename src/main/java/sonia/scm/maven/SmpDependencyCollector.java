/**
 * Copyright (c) 2014, Sebastian Sdorra
 * All rights reserved.
 * <p>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * <p>
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 3. Neither the name of SCM-Manager; nor the names of its
 * contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * <p>
 * https://bitbucket.org/sdorra/smp-maven-plugin
 */


package sonia.scm.maven;

//~--- non-JDK imports --------------------------------------------------------

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

//~--- JDK imports ------------------------------------------------------------

/**
 * @author Sebastian Sdorra
 */
public final class SmpDependencyCollector {

  private static final Logger LOG = LoggerFactory.getLogger(SmpDependencyCollector.class);

  private static final String PLUGIN_DESCRIPTOR = "META-INF/scm/plugin.xml";
  private final MavenProject project;

  private SmpDependencyCollector(MavenProject project) {
    this.project = project;
  }

  public static Set<SmpArtifact> collect(MavenProject project) throws MojoExecutionException {
    return new SmpDependencyCollector(project).collectSmpDependencies();
  }

  private Set<SmpArtifact> collectSmpDependencies() throws MojoExecutionException {
    Set<Artifact> artifacts = findRuntimeArtifacts();
    return filterAndMapSmpArtifacts(artifacts);
  }

  private Set<Artifact> findRuntimeArtifacts() {
    return project.getArtifacts()
      .stream()
      .filter(a -> !isSelf(a))
      .filter(a -> a.getArtifactHandler().isAddedToClasspath())
      .filter(a -> Artifact.SCOPE_COMPILE.equals(a.getScope()) || Artifact.SCOPE_RUNTIME.equals(a.getScope()))
      .filter(a -> a.getFile() != null)
      .collect(Collectors.toSet());
  }

  private boolean isSelf(Artifact a) {
    return a.getArtifactId().equals(project.getArtifactId())
      && a.getGroupId().equals(project.getGroupId());
  }

  private Set<SmpArtifact> filterAndMapSmpArtifacts(Set<Artifact> artifacts) throws MojoExecutionException {
    Set<SmpArtifact> smps = new HashSet<>();
    for (Artifact artifact : artifacts) {

      PluginDescriptor descriptor = findDescriptor(artifact);
      if (descriptor != null) {
        LOG.debug("found smp dependency {}:{}:{}", artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());
        smps.add(create(artifact, descriptor));
      }
    }
    return smps;
  }

  private SmpArtifact create(Artifact artifact, PluginDescriptor descriptor) {
    return new SmpArtifact(
      artifact.getGroupId(),
      artifact.getArtifactId(),
      artifact.getVersion(),
      artifact.isOptional(),
      descriptor
    );
  }


  private PluginDescriptor findDescriptor(Artifact artifact) throws MojoExecutionException {
    File file = artifact.getFile();
    byte[] bytes = ZipUtil.unpackEntry(file, PLUGIN_DESCRIPTOR);
    if (bytes != null) {
      return PluginDescriptor.from(bytes);
    }
    return null;
  }
}
