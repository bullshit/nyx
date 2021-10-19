/*
 * Copyright 2020 Mooltiverse
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mooltiverse.oss.nyx.configuration;

import static com.mooltiverse.oss.nyx.log.Markers.CONFIGURATION;

import java.io.File;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mooltiverse.oss.nyx.Nyx;
import com.mooltiverse.oss.nyx.configuration.presets.Presets;
import com.mooltiverse.oss.nyx.data.CommitMessageConvention;
import com.mooltiverse.oss.nyx.data.CommitMessageConventions;
import com.mooltiverse.oss.nyx.data.DataAccessException;
import com.mooltiverse.oss.nyx.data.FileMapper;
import com.mooltiverse.oss.nyx.data.IllegalPropertyException;
import com.mooltiverse.oss.nyx.data.LayeredMapConfigurationBlock;
import com.mooltiverse.oss.nyx.data.ReleaseType;
import com.mooltiverse.oss.nyx.data.ReleaseTypes;
import com.mooltiverse.oss.nyx.data.Verbosity;
import com.mooltiverse.oss.nyx.version.Scheme;

/**
 * The Nyx configuration. The configuration is a live object that resolves each option lazily, only when required.
 * This not only improves the overall performances but is also safer as in case of malformed configuration options, only
 * those actually needed are resolved.
 * 
 * This means that even if the configuration sources don't change throughout a release process, the state of the
 * configuration may change every time a not yet resolved oprion is requested and evaluated.
 * 
 * The configuration is layered, where each layer represents a source of configuration options. There is a clear definition
 * of priorities among different layers so there is a clear precedence of options coming from one layer or another.
 * Thanks to this, each option can be overridden by other layer with higher priority.
 * 
 * There must be only one instance of this class for every execution and it's retrieved by {@link Nyx#configuration()}.
 */
public class Configuration implements Root {
    /**
     * The private logger instance
     */
    private static final Logger logger = LoggerFactory.getLogger(Configuration.class);

    /**
     * The private instance of the commit message convention configuration block.
     */
    private CommitMessageConventionsBlock commitMessageConventionsBlock = null;

    /**
     * The private instance of the release types configuration block.
     */
    private ReleaseTypesBlock releaseTypesBlock = null;

    /**
     * The internal representation of the configuration layers and their priorities.
     * 
     * Map entries are ordered by the natural order of the {@link LayerPriority} keys so by iterating over the
     * entries in this map we are assured that the evaluation order is safe.
     * 
     * This instance is initialized with the {@link DefaultLayer} (which has the least priority) as a last resort
     * when looking up configuration options.
     */
    private final EnumMap<LayerPriority, ConfigurationLayer> layers = new EnumMap<LayerPriority, ConfigurationLayer>(LayerPriority.class){
        // The default serial version UID
        private static final long serialVersionUID = 1L;
        {
            // Initialize the layers with the default values, which have the least priority
            put(LayerPriority.DEFAULT, DefaultLayer.getInstance());
        }
    };

    /**
     * Default constructor. Returns a new configuration object at its initial state.
     * 
     * Instances if this class are created using the {@link Nyx#configuration()} method so this constructor should never be used alone.
     * 
     * See {@link Nyx#configuration()}
     * 
     * @throws DataAccessException in case data cannot be read or accessed.
     * @throws IllegalPropertyException in case some option has been defined but has incorrect values or it can't be resolved.
     */
    public Configuration()
        throws DataAccessException, IllegalPropertyException {
        super();
        logger.debug(CONFIGURATION, "New configuration object");
        loadStandardConfigurationFileLayers();
    }

    /**
     * Returns a file built on the given path if it's already an absolute file path, otherwise make it absolute by resolving it with the
     * configured (or default) directory.
     * 
     * @param path the file to make absolute
     * 
     * @return the absolute representation of the file
     * 
     * @see #getDirectory()
     * 
     * @throws DataAccessException in case data cannot be read or accessed.
     * @throws IllegalPropertyException in case some option has been defined but has incorrect values or it can't be resolved.
     */
    private File getAbsoluteFilePath(String path)
        throws DataAccessException, IllegalPropertyException {
        File res = new File(path);
        return res.isAbsolute() ? res : new File(new File(getDirectory()), path);
    }

    /**
     * Resets the cache of resolved options.
     */
    private synchronized void resetCache() {
        logger.trace(CONFIGURATION, "Clearing the configuration cache");
        if (!Objects.isNull(commitMessageConventionsBlock))
            commitMessageConventionsBlock.resetCache();
        if (!Objects.isNull(releaseTypesBlock))
            releaseTypesBlock.resetCache();
    }

    /**
     * Makes sure that the configuration layers that can in turn be configured (custom configuration file, custom shared
     * configuration file, preset) are added or removed into the internal layers representation, according to the actual
     * configuration parameters.
     * 
     * This method should be invoked after any core layer (not among the configured ones) changes, or after a batch of
     * those changes.
     * 
     * @throws DataAccessException in case data cannot be read or accessed.
     * @throws IllegalPropertyException in case some option has been defined but has incorrect values or it can't be resolved.
     */
    private synchronized void updateConfiguredConfigurationLayers()
        throws DataAccessException, IllegalPropertyException {
        
        // follow the evaluation order from top to bottom to make sure that any change applied here does not
        // require further changes to the layers that we already checked

        // start with the local custom configuration file
        if (Objects.isNull(getConfigurationFile())) {
            logger.debug(CONFIGURATION, "Clearing the custom local configuration file, if any");
            layers.remove(LayerPriority.CUSTOM_LOCAL_FILE);
        }
        else if (getConfigurationFile().isBlank()) {
            logger.error(CONFIGURATION, "An empty path has been defined for the local custom configuration file and it will be ignored");
            layers.remove(LayerPriority.CUSTOM_LOCAL_FILE);
        }
        else {
            File customLocalConfigurationFile = getAbsoluteFilePath(getConfigurationFile());
            logger.debug(CONFIGURATION, "Loading custom local configuration file at {}", customLocalConfigurationFile.getAbsolutePath());
            layers.put(LayerPriority.CUSTOM_LOCAL_FILE, FileMapper.load(customLocalConfigurationFile, SimpleConfigurationLayer.class));
            logger.debug(CONFIGURATION, "Custom local configuration file {} loaded", customLocalConfigurationFile.getAbsolutePath());
        }

        // now the local shared configuration file
        if (Objects.isNull(getSharedConfigurationFile())) {
            logger.debug(CONFIGURATION, "Clearing the custom shared configuration file, if any");
            layers.remove(LayerPriority.CUSTOM_SHARED_FILE);
        }
        else if (getSharedConfigurationFile().isBlank()) {
            logger.error(CONFIGURATION, "An empty path has been defined for the local shared configuration file and it will be ignored");
            layers.remove(LayerPriority.CUSTOM_SHARED_FILE);
        }
        else {
            File customSharedConfigurationFile = getAbsoluteFilePath(getSharedConfigurationFile());
            logger.debug(CONFIGURATION, "Loading custom shared configuration file at {}", customSharedConfigurationFile.getAbsolutePath());
            layers.put(LayerPriority.CUSTOM_SHARED_FILE, FileMapper.load(customSharedConfigurationFile, SimpleConfigurationLayer.class));
            logger.debug(CONFIGURATION, "Custom shared configuration file {} loaded", customSharedConfigurationFile.getAbsolutePath());
        }

        // now the preset
        if (Objects.isNull(getPreset())) {
            logger.debug(CONFIGURATION, "Clearing the preset configuration, if any");
            layers.remove(LayerPriority.PRESET);
        }
        else if (getPreset().isBlank()) {
            logger.error(CONFIGURATION, "An empty name has been defined for the preset configuration and it will be ignored");
            layers.remove(LayerPriority.PRESET);
        }
        else {
            logger.debug(CONFIGURATION, "Loading preset configuration {}", getPreset());
            layers.put(LayerPriority.PRESET, Presets.byName(getPreset()));
            logger.debug(CONFIGURATION, "Preset configuration {} loaded", getPreset());
        }
    }

    /**
     * Loads the various standard configuration file layers, if found, searched at their default locations.
     * 
     * @throws DataAccessException in case data cannot be read or accessed.
     * @throws IllegalPropertyException in case some option has been defined but has incorrect values or it can't be resolved.
     */
    private void loadStandardConfigurationFileLayers()
        throws DataAccessException, IllegalPropertyException {
        logger.debug(CONFIGURATION, "Searching for standard configuration files...");
        // load standard local configuration files first, if any
        for (String fileName: List.<String>of(".nyx.json", ".nyx.yaml", ".nyx.yml")) {
            File file = getAbsoluteFilePath(fileName);
            if (file.exists() && file.isFile()) {
                logger.debug(CONFIGURATION, "Standard local configuration file found at {}. Loading...", file.getAbsolutePath());
                layers.put(LayerPriority.STANDARD_LOCAL_FILE, FileMapper.load(file, SimpleConfigurationLayer.class));
                logger.debug(CONFIGURATION, "Standard local configuration file {} loaded", file.getAbsolutePath());
                break;
            }
            else logger.debug(CONFIGURATION, "Standard local configuration file {} not found.", file.getAbsolutePath());
        }
        // then load standard shared configuration files, if any
        for (String fileName: List.<String>of(".nyx-shared.json", ".nyx-shared.yaml", ".nyx-shared.yml")) {
            File file = getAbsoluteFilePath(fileName);
            if (file.exists() && file.isFile()) {
                logger.debug(CONFIGURATION, "Standard shared configuration file found at {}. Loading...", file.getAbsolutePath());
                layers.put(LayerPriority.STANDARD_SHARED_FILE, FileMapper.load(file, SimpleConfigurationLayer.class));
                logger.debug(CONFIGURATION, "Standard shared configuration file {} loaded", file.getAbsolutePath());
                break;
            }
            else logger.debug(CONFIGURATION, "Standard shared configuration file {} not found.", file.getAbsolutePath());
        }
        if (layers.containsKey(LayerPriority.STANDARD_LOCAL_FILE) || layers.containsKey(LayerPriority.STANDARD_SHARED_FILE))
            updateConfiguredConfigurationLayers();
    }

    /**
     * Adds, replaces or removes the layer at the {@link LayerPriority#COMMAND_LINE} level.
     * 
     * @param layer the configuration layer to set at the {@link LayerPriority#COMMAND_LINE} level.
     * If {@code null} any existing configuration layer at the same level is removed (if any).
     * 
     * @return a reference to this same object.
     * 
     * @throws DataAccessException in case data cannot be read or accessed.
     * @throws IllegalPropertyException in case some option has been defined but has incorrect values or it can't be resolved.
     */
    public Configuration withCommandLineConfiguration(ConfigurationLayer layer)
        throws DataAccessException, IllegalPropertyException {
        if (Objects.isNull(layer)) {
            logger.debug(CONFIGURATION, "Removing the existing {} configuration layer, if any", LayerPriority.COMMAND_LINE);
            layers.remove(LayerPriority.COMMAND_LINE);
        }
        else {
            logger.debug(CONFIGURATION, "Adding or replacing the {} configuration layer", LayerPriority.COMMAND_LINE);
            layers.put(LayerPriority.COMMAND_LINE, layer);
        }
        updateConfiguredConfigurationLayers();
        resetCache();
        return this;
    }

    /**
     * Adds, replaces or removes the layer at the {@link LayerPriority#PLUGIN} level.
     * 
     * @param layer the configuration layer to set at the {@link LayerPriority#PLUGIN} level.
     * If {@code null} any existing configuration layer at the same level is removed (if any).
     * 
     * @return a reference to this same object.
     * 
     * @throws DataAccessException in case data cannot be read or accessed.
     * @throws IllegalPropertyException in case some option has been defined but has incorrect values or it can't be resolved.
     */
    public Configuration withPluginConfiguration(ConfigurationLayer layer)
        throws DataAccessException, IllegalPropertyException {
        if (Objects.isNull(layer)) {
            logger.debug(CONFIGURATION, "Removing the existing {} configuration layer, if any", LayerPriority.PLUGIN);
            layers.remove(LayerPriority.PLUGIN);
        }
        else {
            logger.debug(CONFIGURATION, "Adding or replacing the {} configuration layer", LayerPriority.PLUGIN);
            layers.put(LayerPriority.PLUGIN, layer);
        }
        updateConfiguredConfigurationLayers();
        resetCache();
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getBump()
        throws DataAccessException, IllegalPropertyException {
        logger.trace(CONFIGURATION, "Retrieving the {} configuration option", "bump");
        for (ConfigurationLayer layer: layers.values()) {
            String bump = layer.getBump();
            if (!Objects.isNull(bump)) {
                logger.trace(CONFIGURATION, "The {} configuration option value is: {}", "bump", bump);
                return bump;
            }
        }
        return DefaultLayer.getInstance().getBump();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CommitMessageConventions getCommitMessageConventions()
        throws DataAccessException, IllegalPropertyException {
        logger.trace(CONFIGURATION, "Retrieving the commit message conventions");
        if (Objects.isNull(commitMessageConventionsBlock)) {
            commitMessageConventionsBlock = new CommitMessageConventionsBlock();
        }
        return commitMessageConventionsBlock;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getConfigurationFile()
        throws DataAccessException, IllegalPropertyException {
        logger.trace(CONFIGURATION, "Retrieving the {} configuration option", "configurationFile");
        for (Map.Entry<LayerPriority, ConfigurationLayer> layerEntry: layers.entrySet()) {
            // custom configuration file configuration is ignored on custom configuration file layers to avoid chaining
            if (!LayerPriority.CUSTOM_LOCAL_FILE.equals(layerEntry.getKey())) {
                String configurationFile = layerEntry.getValue().getConfigurationFile();
                if (!Objects.isNull(configurationFile)) {
                    logger.trace(CONFIGURATION, "The {} configuration option value is: {}", "configurationFile", configurationFile);
                    return configurationFile;
                }
            }
        }
        return DefaultLayer.getInstance().getConfigurationFile();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDirectory()
        throws DataAccessException, IllegalPropertyException {
        logger.trace(CONFIGURATION, "Retrieving the {} configuration option", "directory");
        for (ConfigurationLayer layer: layers.values()) {
            String directory = layer.getDirectory();
            if (!Objects.isNull(directory)) {
                logger.trace(CONFIGURATION, "The {} configuration option value is: {}", "directory", directory);
                return directory;
            }
        }
        return DefaultLayer.getInstance().getDirectory();
    }

    /**
     * This method allows to override the default directory that will be returned by {@link #getDirectory()}.
     * This method must be invoked before instances of {@link Nyx} or other classes are created or the given value may be ignored.
     * 
     * @param directory the new default directory. If {@code null} then the standard default directory will be used.
     */
    public static void setDefaultDirectory(File directory) {
        logger.trace(CONFIGURATION, "Setting the default directory {}", Objects.isNull(directory) ? "null" : directory.getAbsolutePath());
        DefaultLayer.getInstance().setDirectory(Objects.isNull(directory) ? null : directory.getAbsolutePath());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean getDryRun()
        throws DataAccessException, IllegalPropertyException {
        logger.trace(CONFIGURATION, "Retrieving the {} configuration option", "dryRun");
        for (ConfigurationLayer layer: layers.values()) {
            Boolean dryRun = layer.getDryRun();
            if (!Objects.isNull(dryRun)) {
                logger.trace(CONFIGURATION, "The {} configuration option value is: {}", "dryRun", dryRun);
                return dryRun;
            }
        }
        return DefaultLayer.getInstance().getDryRun();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getInitialVersion()
        throws DataAccessException, IllegalPropertyException {
        logger.trace(CONFIGURATION, "Retrieving the {} configuration option", "initialVersion");
        for (ConfigurationLayer layer: layers.values()) {
            String initialVersion = layer.getInitialVersion();
            if (!Objects.isNull(initialVersion)) {
                logger.trace(CONFIGURATION, "The {} configuration option value is: {}", "initialVersion", initialVersion);
                return initialVersion;
            }
        }
        return DefaultLayer.getInstance().getInitialVersion();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPreset()
        throws DataAccessException, IllegalPropertyException {
        logger.trace(CONFIGURATION, "Retrieving the {} configuration option", "preset");
        for (Map.Entry<LayerPriority, ConfigurationLayer> layerEntry: layers.entrySet()) {
            // preset configuration is ignored on preset layers to avoid chaining
            if (!LayerPriority.PRESET.equals(layerEntry.getKey())) {
                String preset = layerEntry.getValue().getPreset();
                if (!Objects.isNull(preset)) {
                    logger.trace(CONFIGURATION, "The {} configuration option value is: {}", "preset", preset);
                    return preset;
                }
            }
        }
        return DefaultLayer.getInstance().getPreset();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getReleasePrefix()
        throws DataAccessException, IllegalPropertyException {
        logger.trace(CONFIGURATION, "Retrieving the {} configuration option", "releasePrefix");
        for (ConfigurationLayer layer: layers.values()) {
            String releasePrefix = layer.getReleasePrefix();
            if (!Objects.isNull(releasePrefix)) {
                logger.trace(CONFIGURATION, "The {} configuration option value is: {}", "releasePrefix", releasePrefix);
                return releasePrefix;
            }
        }
        return DefaultLayer.getInstance().getReleasePrefix();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean getReleaseLenient()
        throws DataAccessException, IllegalPropertyException {
        logger.trace(CONFIGURATION, "Retrieving the {} configuration option", "releaseLenient");
        for (ConfigurationLayer layer: layers.values()) {
            Boolean releaseLenient = layer.getReleaseLenient();
            if (!Objects.isNull(releaseLenient)) {
                logger.trace(CONFIGURATION, "The {} configuration option value is: {}", "releaseLenient", releaseLenient);
                return releaseLenient;
            }
        }
        return DefaultLayer.getInstance().getReleaseLenient();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReleaseTypes getReleaseTypes()
        throws DataAccessException, IllegalPropertyException {
        logger.trace(CONFIGURATION, "Retrieving the release types");

        if (Objects.isNull(releaseTypesBlock)) {
            releaseTypesBlock = new ReleaseTypesBlock();
        }
        return releaseTypesBlock;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean getResume()
        throws DataAccessException, IllegalPropertyException {
        logger.trace(CONFIGURATION, "Retrieving the {} configuration option", "resume");
        for (ConfigurationLayer layer: layers.values()) {
            Boolean resume = layer.getResume();
            if (!Objects.isNull(resume)) {
                logger.trace(CONFIGURATION, "The {} configuration option value is: {}", "resume", resume);
                return resume;
            }
        }
        return DefaultLayer.getInstance().getResume();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Scheme getScheme()
        throws DataAccessException, IllegalPropertyException {
        logger.trace(CONFIGURATION, "Retrieving the {} configuration option", "scheme");
        for (ConfigurationLayer layer: layers.values()) {
            Scheme scheme = layer.getScheme();
            if (!Objects.isNull(scheme)) {
                logger.trace(CONFIGURATION, "The {} configuration option value is: {}", "scheme", scheme);
                return scheme;
            }
        }
        return DefaultLayer.getInstance().getScheme();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSharedConfigurationFile()
        throws DataAccessException, IllegalPropertyException {
        logger.trace(CONFIGURATION, "Retrieving the {} configuration option", "sharedConfigurationFile");
        for (Map.Entry<LayerPriority, ConfigurationLayer> layerEntry: layers.entrySet()) {
            // custom shared configuration file configuration is ignored on custom shared configuration file layers to avoid chaining
            if (!LayerPriority.CUSTOM_SHARED_FILE.equals(layerEntry.getKey())) {
                String sharedConfigurationFile = layerEntry.getValue().getSharedConfigurationFile();
                if (!Objects.isNull(sharedConfigurationFile)) {
                    logger.trace(CONFIGURATION, "The {} configuration option value is: {}", "sharedConfigurationFile", sharedConfigurationFile);
                    return sharedConfigurationFile;
                }
            }
        }
        return DefaultLayer.getInstance().getSharedConfigurationFile();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getStateFile()
        throws DataAccessException, IllegalPropertyException {
        logger.trace(CONFIGURATION, "Retrieving the {} configuration option", "stateFile");
        for (ConfigurationLayer layer: layers.values()) {
            String stateFile = layer.getStateFile();
            if (!Objects.isNull(stateFile)) {
                logger.trace(CONFIGURATION, "The {} configuration option value is: {}", "stateFile", stateFile);
                return stateFile;
            }
        }
        return DefaultLayer.getInstance().getStateFile();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Verbosity getVerbosity()
        throws DataAccessException, IllegalPropertyException {
        logger.trace(CONFIGURATION, "Retrieving the {} configuration option", "verbosity");
        for (ConfigurationLayer layer: layers.values()) {
            Verbosity verbosity = layer.getVerbosity();
            if (!Objects.isNull(verbosity)) {
                logger.trace(CONFIGURATION, "The {} configuration option value is: {}", "verbosity", verbosity);
                return verbosity;
            }
        }
        return DefaultLayer.getInstance().getVerbosity();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getVersion()
        throws DataAccessException, IllegalPropertyException {
        logger.trace(CONFIGURATION, "Retrieving the {} configuration option", "version");
        for (ConfigurationLayer layer: layers.values()) {
            String version = layer.getVersion();
            if (!Objects.isNull(version)) {
                logger.trace(CONFIGURATION, "The {} configuration option value is: {}", "version", version);
                return version;
            }
        }
        return DefaultLayer.getInstance().getVersion();
    }

    /**
     * The class implementing the {@link CommitMessageConventions} configuration block.
     */
    private class CommitMessageConventionsBlock extends LayeredMapConfigurationBlock<CommitMessageConvention> implements CommitMessageConventions {
        /**
         * Default constructor is private on purpose.
         */
        private CommitMessageConventionsBlock() {
            super("commitMessageConventions");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<String> getEnabled()
            throws DataAccessException, IllegalPropertyException {
            logger.trace(CONFIGURATION, "Retrieving the {}.{} configuration option", configurationBlockName, "enabled");
            for (ConfigurationLayer layer: layers.values()) {
                List<String> enabled = layer.getCommitMessageConventions().getEnabled();
                if (!Objects.isNull(enabled)) {
                    logger.trace(CONFIGURATION, "The {}.{} configuration option value is: {}", configurationBlockName, "enabled", String.join(", ", enabled));
                    return enabled;
                }
            }
            return DefaultLayer.getInstance().getCommitMessageConventions().getEnabled();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected CommitMessageConvention getResolvedItem(String name)
            throws DataAccessException, IllegalPropertyException {
            logger.trace(CONFIGURATION, "Resolving the {}.{}[{}] configuration option", configurationBlockName, "items", name);
            for (ConfigurationLayer layer: layers.values()) {
                CommitMessageConvention item = layer.getCommitMessageConventions().getItem(name);
                if (!Objects.isNull(item)) {
                    logger.trace(CONFIGURATION, "The {}.{}[{}] configuration option has been resolved", configurationBlockName, "items", name);
                    return item;
                }
            }
            logger.error(CONFIGURATION, "Unable to resolve the {}.{}[{}] configuration option", configurationBlockName, "items", name);
            throw new IllegalPropertyException(String.format("Unable to resolve the %s.%s[%s] configuration option", configurationBlockName, "items", name));
        }
    }

    /**
     * The class implementing the {@link ReleaseTypes} configuration block.
     */
    private class ReleaseTypesBlock extends LayeredMapConfigurationBlock<ReleaseType> implements ReleaseTypes {
        /**
         * Default constructor is private on purpose.
         */
        private ReleaseTypesBlock() {
            super("releaseTypes");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<String> getEnabled()
            throws DataAccessException, IllegalPropertyException {
            logger.trace(CONFIGURATION, "Retrieving the {}.{} configuration option", configurationBlockName, "enabled");
            for (ConfigurationLayer layer: layers.values()) {
                List<String> enabled = layer.getReleaseTypes().getEnabled();
                if (!Objects.isNull(enabled)) {
                    logger.trace(CONFIGURATION, "The {}.{} configuration option value is: {}", configurationBlockName, "enabled", String.join(", ", enabled));
                    return enabled;
                }
            }
            return DefaultLayer.getInstance().getReleaseTypes().getEnabled();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected ReleaseType getResolvedItem(String name)
            throws DataAccessException, IllegalPropertyException {
            logger.trace(CONFIGURATION, "Resolving the {}.{}[{}] configuration option", configurationBlockName, "items", name);
            for (ConfigurationLayer layer: layers.values()) {
                ReleaseType item = layer.getReleaseTypes().getItem(name);
                if (!Objects.isNull(item)) {
                    logger.trace(CONFIGURATION, "The {}.{}[{}] configuration option has been resolved", configurationBlockName, "items", name);
                    return item;
                }
            }
            logger.error(CONFIGURATION, "Unable to resolve the {}.{}[{}] configuration option", configurationBlockName, "items", name);
            throw new IllegalPropertyException(String.format("Unable to resolve the %s.%s[%s] configuration option", configurationBlockName, "items", name));
        }
    }
}
