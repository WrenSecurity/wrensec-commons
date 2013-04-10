/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * If applicable, add the following below this MPL 2.0 HEADER, replacing
 * the fields enclosed by brackets "[]" replaced with your own identifying
 * information:
 *     Portions Copyright [yyyy] [name of copyright owner]
 *
 *     Copyright 2013 ForgeRock AS
 *
 */

package org.forgerock.doc.maven;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.twdata.maven.mojoexecutor.MojoExecutor;

/**
 * Implementation using <a href="http://arrenbrecht.ch/jcite/">JCite</a> to cite
 * Java code in DocBook XML. This Mojo generates source including the citations.
 * For example, if your DocBook source file includes the following
 * &lt;programlisting&gt;:
 *
 * <pre>
 * &lt;programlisting language=&quot;java&quot;
 * &gt;[jcp:org.forgerock.doc.jcite.test.Test:--- mainMethod]&lt;/programlisting&gt;
 * </pre>
 *
 * Then class replaces the citation with the code in between // --- mainMethod
 * comments, suitable for inclusion in XML, and places the new file in the
 * generated sources directory for further processing.
 *
 * @Checkstyle:ignoreFor 2
 * @goal jcite
 * @phase pre-site
 */
public class JCiteBuildMojo extends AbstractBuildMojo {

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        // The Executor is what actually calls other plugins.
        Executor exec = new Executor();

        getLog().info("Running JCite on DocBook XML sources...");
        exec.runJCite();
    }

    /**
     * When running JCite, the set of source paths where cited Java files are to
     * be found.
     *
     * @parameter
     */
    private List<File> sourcePaths;

    /**
     * Enclose methods to run plugins.
     */
    class Executor extends MojoExecutor {

        /**
         * Run JCite on the DocBook XML source files.
         * @throws MojoExecutionException
         */
        void runJCite() throws MojoExecutionException {

            String outputDir = FilenameUtils.separatorsToUnix(
                    getDocbkxGeneratedSourceDirectory().getPath());
            String sourceDir = FilenameUtils.separatorsToUnix(
                    getDocbkxSourceDirectory().getPath());

            // mojo-executor lacks fluent support for element attributes.
            // You can hack around this by including attributes in the name
            // of elements without children. But the hack does not work for
            // elements with children: SAX barfs on closing tags containing
            // a bunch of attributes.
            Xpp3Dom mkdir = new Xpp3Dom("mkdir");
            mkdir.setAttribute("dir", outputDir);

            Xpp3Dom taskdef = new Xpp3Dom("taskdef");
            taskdef.setAttribute("name", "jcite");
            taskdef.setAttribute("classname", "ch.arrenbrecht.jcite.JCiteTask");
            taskdef.setAttribute("classpathref", "maven.plugin.classpath");

            Xpp3Dom jcite = new Xpp3Dom("jcite");
            jcite.setAttribute("srcdir", sourceDir);
            jcite.setAttribute("destdir", outputDir);

            // Might have multiple paths to sources.
            Xpp3Dom sourcepath = new Xpp3Dom("sourcepath");
            if (sourcePaths != null && !sourcePaths.isEmpty()) {
                for (File sourcePath : sourcePaths) {
                    String location = FilenameUtils
                            .separatorsToSystem(sourcePath.getPath());
                    Xpp3Dom pathelement = new Xpp3Dom("pathelement");
                    pathelement.setAttribute("location", location);
                    sourcepath.addChild(pathelement);
                }
            } else { // No source path defined. Try src/main/java.
                Xpp3Dom pathelement = new Xpp3Dom("pathelement");
                pathelement.setAttribute("location", "src/main/java");
                sourcepath.addChild(pathelement);
            }
            jcite.addChild(sourcepath);

            Xpp3Dom include = new Xpp3Dom("include");
            include.setAttribute("name", "**/*.xml");
            jcite.addChild(include);

            Xpp3Dom target = new Xpp3Dom("target");
            target.addChild(mkdir);
            target.addChild(taskdef);
            target.addChild(jcite);

            Xpp3Dom configuration = new Xpp3Dom("configuration");
            configuration.addChild(target);

            // mojo-executor lacks fluent support for dependencies.
            // The antrun plugin needs JCite in its runtime classpath.
            Plugin antrun = MojoExecutor.plugin(
                    "org.apache.maven.plugins",
                    "maven-antrun-plugin",
                    "1.7");

            // See https://code.google.com/r/markcraig-jcite/.
            Dependency jCitePlugin = new Dependency();
            jCitePlugin.setGroupId("org.mcraig");
            jCitePlugin.setArtifactId("jcite");
            jCitePlugin.setVersion(getJCiteVersion());

            antrun.addDependency(jCitePlugin);

            executeMojo(
                    antrun,
                    goal("run"),
                    configuration,
                    executionEnvironment(
                            getProject(),
                            getSession(),
                            getPluginManager()));
        }
    }
}
