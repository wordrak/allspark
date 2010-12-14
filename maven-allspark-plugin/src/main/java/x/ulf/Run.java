package x.ulf;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

/**
 * Goal which touches a timestamp file.
 *
 * @goal run
 * @phase install
 * @aggregator true
 * @requiresDirectInvocation true
 */
public class Run extends AbstractMojo {

    /**
     * The Maven Project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project = null;

    /**
     * @parameter expression="${project.build.directory}"
     */
    private String target;
    /**
     * @component
     */
    private org.apache.maven.artifact.factory.ArtifactFactory artifactFactory;

    /**
     * @component
     */
    private org.apache.maven.artifact.resolver.ArtifactResolver resolver;

    /**
     * @parameter default-value="${localRepository}"
     */
    private org.apache.maven.artifact.repository.ArtifactRepository localRepository;

    /**
     * @parameter default-value="${project.remoteArtifactRepositories}"
     */
    private java.util.List remoteRepositories;

    /**
     * The artifact metadata source to use.
     *
     * @component
     * @required
     * @readonly
     */
    private ArtifactMetadataSource artifactMetadataSource;

    /**
     * The artifact collector to use.
     *
     * @component
     * @required
     * @readonly
     */
    private ArtifactCollector artifactCollector;

    /**
     * The dependency tree builder to use.
     *
     * @component
     * @required
     * @readonly
     */
    private DependencyTreeBuilder dependencyTreeBuilder;

    /**
     * @parameter default-value="${project.distributionManagementArtifactRepository}"
     */
    private ArtifactRepository deploymentRepository;

    private File bundle = null;
    private File lib = null;
    private Set<Artifact> done = new HashSet<Artifact>();

    public void execute() throws MojoExecutionException {
        try {
            bundle = mkdir(target + "/bundle");
            lib = mkdir(target + "/lib");

            DependencyNode root = dependencyTreeBuilder.buildDependencyTree(project, localRepository, artifactFactory, artifactMetadataSource, null, artifactCollector);

            this.getLog().info("");
            copyDependency(root);

            this.getLog().info("");
            runCorpus();

        } catch (Exception ex) {
            ex.printStackTrace();
            throw new MojoExecutionException("", ex);
        }

    }

    private void copyDependency(DependencyNode node) throws ArtifactResolutionException, IOException {
        Artifact artifact = node.getArtifact();

        if (this.done.contains(artifact)) {
            return;
        }

        if (artifact.getType() == "jar") {
            try {
                resolver.resolve(artifact, remoteRepositories, localRepository);
                File jar = artifact.getFile();
                this.getLog().info("Copy: " + artifact.toString());
                if (isBundle(jar)) {
                    copy(jar, new File(bundle, jar.getName()));
                } else {
                    copy(jar, new File(lib, jar.getName()));
                }
            } catch (ArtifactNotFoundException e) {
                getLog().info("Artifact not found: " + artifact.toString());
            }
        } else {
            getLog().info("Artifact ignored: " + artifact.toString());
        }

        if (node.hasChildren()) {
            for (DependencyNode child : (List<DependencyNode>) node.getChildren()) {
                copyDependency(child);
            }
        }

        this.done.add(artifact);

    }

    private void runCorpus() throws IOException, InterruptedException {
        Process corpus = new ProcessBuilder("java", "-jar", "lib/corpus-1.0-SNAPSHOT.jar").
                directory(new File(target)).
                start();

        StreamForwarder outputForwarder = new StreamForwarder("stdout", corpus.getInputStream(), System.out);
        StreamForwarder errorForwarder = new StreamForwarder("stderr", corpus.getErrorStream(), System.err);
        StreamForwarder inputForwarder = new StreamForwarder("stdin", System.in, corpus.getOutputStream());
        errorForwarder.start();
        outputForwarder.start();
        inputForwarder.start();

        Thread.sleep(10);

        this.getLog().info("");
        this.getLog().info("Corpus is started, waiting for termination.");
        this.getLog().info("");
        this.getLog().info("");
        corpus.waitFor();

        Thread.sleep(10);
    }

    private boolean isRunning(Process process) {
        try {
            process.exitValue();
            return false;
        } catch (IllegalThreadStateException ex) {
            return true;
        }
    }

    private boolean isBundle(File file) throws IOException {
        JarFile jar = new JarFile(file);

//        for (Map.Entry<Object, Object> entry : jar.getManifest().getMainAttributes().entrySet()) {
//            this.getLog().info(entry.getKey().getClass().getName());
//            this.getLog().info(entry.toString());
//        }
//        this.getLog().info(String.valueOf(jar.getManifest().getMainAttributes().get("Bundle-SymbolicName")));

        return jar.getManifest().getMainAttributes().containsKey(new Attributes.Name("Bundle-SymbolicName"));
    }

    private void copy(File source, File target) throws IOException {
        FileChannel inChannel = new FileInputStream(source).getChannel();
        FileChannel outChannel = new FileOutputStream(target).getChannel();
        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } finally {
            if (inChannel != null) inChannel.close();
            if (outChannel != null) outChannel.close();
        }
    }


    private File mkdir(String name) {
        File dir = new File(name);
        if (dir.exists()) {
//            dir.delete();
        }
        dir.mkdirs();
        return dir;
    }

    private void prepareBundleFolder(Set<Artifact> artifacts) {

    }

    class StreamForwarder extends Thread {
        private InputStream in;
        private OutputStream out;

        StreamForwarder(String name, InputStream in, OutputStream out) {
            super(name);
            this.in = in;
            this.out = out;
        }

        public void run() {
            Run.this.getLog().info("Start StreamForwarder " + this.getName());
            try {
                byte[] bytes = new byte[1024];
                int cnt = 0;
                while ((cnt = this.in.read(bytes)) != -1) {
                    this.out.write(bytes, 0, cnt);
                    this.out.flush();
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            } finally {
                Run.this.getLog().info("Done StreamForwarder " + this.getName());
            }
        }
    }
}
