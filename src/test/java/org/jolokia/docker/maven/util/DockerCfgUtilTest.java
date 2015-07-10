package org.jolokia.docker.maven.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;

import org.apache.maven.plugin.MojoExecutionException;

import org.jolokia.docker.maven.access.AuthConfig;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.junit.rules.TemporaryFolder;

public class DockerCfgUtilTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File validDockerCfgFile;

    @Before
    public void setUp() throws FileNotFoundException, IOException {

        // set up an '.dockercfg' file
        validDockerCfgFile = temporaryFolder.newFile("testDockerCfg");
        IOUtils.copy(getClass().getResourceAsStream("/dockercfg/valid.json"), new FileOutputStream(validDockerCfgFile));
    }

    @Test
    public void testNotExistentFile() throws MojoExecutionException {
        DockerCfgUtil util = new DockerCfgUtil(new File("/notexistent/.dockercfg"));
        AuthConfig authConfig = util.fromDockerCfg("example.org");
        Assert.assertNull(authConfig);
    }

    @Test
    public void testRegistryParameterNull() throws FileNotFoundException, IOException, MojoExecutionException {

        DockerCfgUtil util = new DockerCfgUtil(validDockerCfgFile);
        AuthConfig authConfig = util.fromDockerCfg(null);
        Assert.assertNull(authConfig);
    }

    @Test
    public void testRegistryParameterWhitespace() throws FileNotFoundException, IOException, MojoExecutionException {

        DockerCfgUtil util = new DockerCfgUtil(validDockerCfgFile);
        AuthConfig authConfig = util.fromDockerCfg("  ");
        Assert.assertNull(authConfig);
    }

    @Test
    public void testRegistryParameterEmpty() throws FileNotFoundException, IOException, MojoExecutionException {

        DockerCfgUtil util = new DockerCfgUtil(validDockerCfgFile);
        AuthConfig authConfig = util.fromDockerCfg("");
        Assert.assertNull(authConfig);
    }

}
