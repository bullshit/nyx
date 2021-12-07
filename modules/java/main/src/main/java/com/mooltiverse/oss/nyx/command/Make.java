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
package com.mooltiverse.oss.nyx.command;

import static com.mooltiverse.oss.nyx.log.Markers.COMMAND;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mooltiverse.oss.nyx.ReleaseException;
import com.mooltiverse.oss.nyx.entities.Asset;
import com.mooltiverse.oss.nyx.entities.IllegalPropertyException;
import com.mooltiverse.oss.nyx.io.DataAccessException;
import com.mooltiverse.oss.nyx.io.TransportException;
import com.mooltiverse.oss.nyx.services.AssetService;
import com.mooltiverse.oss.nyx.services.GitException;
import com.mooltiverse.oss.nyx.services.SecurityException;
import com.mooltiverse.oss.nyx.services.git.Repository;
import com.mooltiverse.oss.nyx.state.State;

/**
 * The Make command takes care of building the release artifacts.
 * 
 * This class is not meant to be used in multi-threaded environments.
 */
public class Make extends AbstractCommand {
    /**
     * The private logger instance
     */
    private static final Logger logger = LoggerFactory.getLogger(Make.class);

    /**
     * The name used for the internal state attribute where we store current branch name.
     */
    private static final String INTERNAL_BRANCH = Mark.class.getSimpleName().concat(".").concat("repository").concat(".").concat("current").concat(".").concat("branch");

    /**
     * The name used for the internal state attribute where we store the SHA-1 of the last
     * commit in the current branch by the time this command was last executed.
     */
    private static final String INTERNAL_LAST_COMMIT = Mark.class.getSimpleName().concat(".").concat("last").concat(".").concat("commit");

    /**
     * The name used for the internal state attribute where we store the initial commit.
     */
    private static final String STATE_INITIAL_COMMIT = Mark.class.getSimpleName().concat(".").concat("state").concat(".").concat("initialCommit");

    /**
     * The flag telling if the current version is new.
     */
    private static final String STATE_NEW_VERSION = Mark.class.getSimpleName().concat(".").concat("state").concat(".").concat("newVersion");

    /**
     * The name used for the internal state attribute where we store the version.
     */
    private static final String STATE_VERSION = Mark.class.getSimpleName().concat(".").concat("state").concat(".").concat("version");

    /**
     * Standard constructor.
     * 
     * @param state the state reference
     * @param repository the repository reference
     * 
     * @throws NullPointerException if a given argument is {@code null}
     */
    public Make(State state, Repository repository) {
        super(state, repository);
        logger.debug(COMMAND, "New Make command object");
    }

    /**
     * Builds the configured assets.
     * 
     * @throws DataAccessException in case the configuration can't be loaded for some reason.
     * @throws IllegalPropertyException in case the configuration has some illegal options.
     * @throws GitException in case of unexpected issues when accessing the Git repository.
     * @throws ReleaseException if the task is unable to complete for reasons due to the release process.
     */
    private void buildAssets()
        throws DataAccessException, IllegalPropertyException, GitException, ReleaseException {
        if (state().getConfiguration().getAssets().isEmpty())
            logger.debug(COMMAND, "No assets have been configured");
        else {
            for (String assetName: state().getConfiguration().getAssets().keySet()) {
                logger.debug(COMMAND, "Evaluating asset '{}'", assetName);

                Asset asset = state().getConfiguration().getAssets().get(assetName);
                if (Objects.isNull(asset.getService()) || asset.getService().isBlank())
                    logger.debug(COMMAND, "Asset '{}' has no service configured, no build action is taken", assetName);
                else {
                    logger.debug(COMMAND, "Instantiating service '{}'", asset.getService());
                    AssetService assetService = super.resolveAssetService(asset.getService());

                    if (Objects.isNull(assetService))
                        throw new IllegalPropertyException(String.format("Asset '%s' requires service '%s' but no such service has been configured", assetName, asset.getService()));
                    
                    if (state().getConfiguration().getDryRun())
                        logger.info(COMMAND, "Asset build skipped due to dry run");
                    else {
                        logger.debug(COMMAND, "Building asset '{}' with service '{}'", assetName, asset.getService());
                        try {
                            assetService.buildAsset(asset.getPath(), state(), repository());
                        }
                        catch (SecurityException | TransportException e) {
                            throw new ReleaseException(String.format("Can't build asset '%s' using service '%s'", assetName, asset.getService()), e);
                        }
                        logger.debug(COMMAND, "Asset '{}' has been built", assetName);
                    }
                }
            }
        }
    }

    /**
     * This method stores the state internal attributes used for up-to-date checks so that subsequent invocations
     * of the {@link #isUpToDate()} method can find them and determine if the command is already up to date.
     * 
     * This method is meant to be invoked at the end of a successful {@link #run()}.
     * 
     * @throws DataAccessException in case the configuration can't be loaded for some reason.
     * @throws IllegalPropertyException in case the configuration has some illegal options.
     * @throws GitException in case of unexpected issues when accessing the Git repository.
     * 
     * @see #isUpToDate()
     * @see State#getInternals()
     */
    private void storeStatusInternalAttributes()
        throws DataAccessException, IllegalPropertyException, GitException {
        logger.debug(COMMAND, "Storing the Make command internal attributes to the State");
        if (!state().getConfiguration().getDryRun()) {
            putInternalAttribute(INTERNAL_BRANCH, getCurrentBranch());
            putInternalAttribute(INTERNAL_LAST_COMMIT, getLatestCommit());
            putInternalAttribute(STATE_VERSION, state().getVersion());
            putInternalAttribute(STATE_INITIAL_COMMIT, state().getReleaseScope().getInitialCommit());
            putInternalAttribute(STATE_NEW_VERSION, state().getNewVersion());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isUpToDate()
        throws DataAccessException, IllegalPropertyException, GitException {
        logger.debug(COMMAND, "Checking whether the Make command is up to date");
        // Never up to date if this command hasn't stored a version yet into the state
        if (Objects.isNull(state().getVersion()))
            return false;

        // The command is never considered up to date when the repository branch or last commit has changed
        if ((!isInternalAttributeUpToDate(INTERNAL_BRANCH, getCurrentBranch())) || (!isInternalAttributeUpToDate(INTERNAL_LAST_COMMIT, getLatestCommit())))
            return false;

        // Check if configuration parameters have changed
        return isInternalAttributeUpToDate(STATE_VERSION, state().getVersion()) &&
            isInternalAttributeUpToDate(STATE_INITIAL_COMMIT, state().getReleaseScope().getInitialCommit()) &&
            isInternalAttributeUpToDate(STATE_NEW_VERSION, state().getNewVersion());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public State run()
        throws DataAccessException, IllegalPropertyException, GitException, ReleaseException {
        logger.debug(COMMAND, "Running the Make command...");

        buildAssets();

        storeStatusInternalAttributes();
        return state();
    }
}