package org.jolokia.docker.maven.util;

import java.io.File;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.maven.plugin.MojoExecutionException;

import org.jolokia.docker.maven.access.AuthConfig;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * @author  jbellmann
 */
class DockerCfgUtil {

    private static final String EMPTY_STRING = "";

    private final File userDockerCfgFile;

    public DockerCfgUtil(final File userDockerCfgFile) {
        this.userDockerCfgFile = userDockerCfgFile;
    }

    AuthConfig fromDockerCfg(final String registry) throws MojoExecutionException {
        if (registry == null || registry.trim().isEmpty()) {
            return null;
        }

        if (userDockerCfgFile.exists()) {
            try {

                JSONObject data = new JSONObject(new String(Files.readAllBytes(Paths.get(userDockerCfgFile.toURI()))));
                String registryName = getRegistryKey(data, registry);
                if (EMPTY_STRING.equals(registryName)) {

                    // skip here, no match
                    return null;
                }

                JSONObject registryConfig = data.getJSONObject(registryName);

                final String username = getOrDefault(registryConfig, "username");
                final String password = getOrDefault(registryConfig, "password");
                final String email = getOrDefault(registryConfig, "email");
                final String auth = getOrDefault(registryConfig, "auth");
                AuthConfig authConfig = new AuthConfig(username, password, email, auth);
                return authConfig;

            } catch (IOException e) {
                throw new MojoExecutionException("Unable to build 'AuthConfig' from '${user.home}/.dockercfg'", e);
            }
        }

        return null;
    }

    private String getRegistryKey(final JSONObject jsonObject, final String registry) {
        String[] registries = JSONObject.getNames(jsonObject);
        Iterable<String> registryKeys = Iterables.filter(Lists.newArrayList(registries), new Predicate<String>() {
                    @Override
                    public boolean apply(final String input) {
                        return input.contains(registry);
                    }
                });

        // we only get the first one, can be improved
        return Iterables.get(registryKeys, 0, EMPTY_STRING);
    }

    private String getOrDefault(final JSONObject jsonObject, final String key) {
        try {

            final String value = jsonObject.getString(key);
            return value;
        } catch (JSONException e) { }

        return EMPTY_STRING;
    }
}
