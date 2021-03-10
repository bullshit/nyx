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

import static com.mooltiverse.oss.nyx.log.Markers.DEFAULT;

import java.io.File;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mooltiverse.oss.nyx.data.DataAccessException;
import com.mooltiverse.oss.nyx.data.IllegalPropertyException;
import com.mooltiverse.oss.nyx.data.Scheme;
import com.mooltiverse.oss.nyx.data.Verbosity;
import com.mooltiverse.oss.nyx.version.Version;

/**
 * The default configuration layer. This is a singleton class so instances are to be retrieved via the static {@link #getInstance()} method.
 * 
 * The default configuration layer is used with the least priority when evaluating configuration options so it's queried
 * only if and when a certain ption doesn't appear in any other layer with higher priority.
 */
class DefaultLayer implements ConfigurationLayer, Defaults {
    /**
     * The private logger instance
     */
    private static final Logger logger = LoggerFactory.getLogger(Configuration.class);

    /**
     * The single instance for this class.
     */
    private static DefaultLayer instance = null;

    /**
     * Default constructor is private on purpose.
     */
    private DefaultLayer() {
        super();
    }

    /**
     * Returns the singleton instance of this class.
     * 
     * @return the singleton instance of this class
     */
    static DefaultLayer getInstance() {
        if (Objects.isNull(instance))
            instance = new DefaultLayer();
        return instance;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getBump()
        throws DataAccessException, IllegalPropertyException {
        logger.trace(DEFAULT, "Retrieving the default {} configuration option: {}", "bump", BUMP);
        return BUMP;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public File getDirectory()
        throws DataAccessException, IllegalPropertyException {
        logger.trace(DEFAULT, "Retrieving the default {} configuration option: {}", "directory", DIRECTORY);
        return DIRECTORY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean getDryRun()
        throws DataAccessException, IllegalPropertyException {
        logger.trace(DEFAULT, "Retrieving the default {} configuration option: {}", "dryRun", DRY_RUN);
        return DRY_RUN;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Version getInitialVersion()
        throws DataAccessException, IllegalPropertyException {
        logger.trace(DEFAULT, "Retrieving the default {} configuration option: {}", "initialVersion", INITIAL_VERSION);
        return INITIAL_VERSION;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getReleasePrefix()
        throws DataAccessException, IllegalPropertyException {
        logger.trace(DEFAULT, "Retrieving the default {} configuration option: {}", "releasePrefix", RELEASE_PREFIX);
        return RELEASE_PREFIX;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean getReleaseLenient()
        throws DataAccessException, IllegalPropertyException {
        logger.trace(DEFAULT, "Retrieving the default {} configuration option: {}", "releaseLenient", RELEASE_LENIENT);
        return RELEASE_LENIENT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Scheme getScheme()
        throws DataAccessException, IllegalPropertyException {
        logger.trace(DEFAULT, "Retrieving the default {} configuration option: {}", "scheme", SCHEME);
        return SCHEME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Verbosity getVerbosity()
        throws DataAccessException, IllegalPropertyException {
        logger.trace(DEFAULT, "Retrieving the default {} configuration option: {}", "verbosity", VERBOSITY);
        return VERBOSITY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Version getVersion()
        throws DataAccessException, IllegalPropertyException {
        logger.trace(DEFAULT, "Retrieving the default {} configuration option: {}", "version", VERSION);
        return VERSION;
    }
}
