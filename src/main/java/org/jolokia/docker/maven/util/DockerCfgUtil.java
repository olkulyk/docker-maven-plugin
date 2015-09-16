package org.jolokia.docker.maven.util;

import java.io.File;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;

import org.bouncycastle.util.encoders.Base64;

import org.jolokia.docker.maven.access.AuthConfig;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * @author  jbellmann
 */
class DockerCfgUtil {

    private static final String EMPTY_STRING = "";
    private static final String AUTHS_NODE = "auths";
    private static Splitter AUTH_SPLITTER = Splitter.on(":").omitEmptyStrings().trimResults();

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

                JSONObject root = new JSONObject(new String(Files.readAllBytes(Paths.get(userDockerCfgFile.toURI()))));

                if (!root.has(AUTHS_NODE)) {
                    return null;
                }

                JSONObject data = root.getJSONObject(AUTHS_NODE);

                String registryName = getRegistryKey(data, registry);
                if (EMPTY_STRING.equals(registryName)) {

                    // skip here, no match
                    return null;
                }

                JSONObject registryConfig = data.getJSONObject(registryName);

                String username = getOrDefault(registryConfig, "username");
                String password = getOrDefault(registryConfig, "password");
                final String email = getOrDefault(registryConfig, "email");

                // special for pierone-cli
                final String auth = getOrDefault(registryConfig, "auth");

                // typical case for pierone-cli created entries
                if (username == null && password == null && auth != null && email.startsWith("no-mail-required")) {
                    String decodedAuth = new String(Base64.decode(auth));
                    Iterable<String> splitted = AUTH_SPLITTER.split(decodedAuth);
                    if (Iterables.size(splitted) == 2) {
                        username = Iterables.get(splitted, 0);
                        password = Iterables.get(splitted, 1);
                    }
                }

                Map<String, String> authConfigMap = Maps.newHashMap();
                authConfigMap.put("username", username);
                authConfigMap.put("password", password);
                authConfigMap.put("email", email);
                authConfigMap.put("auth", auth);
                authConfigMap.put("serveraddress", registry);

                AuthConfig authConfig = new AuthConfig(authConfigMap);
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

        return null;
    }
}
