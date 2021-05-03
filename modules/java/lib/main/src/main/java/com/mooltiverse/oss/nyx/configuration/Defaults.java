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

import java.io.File;
import java.util.List;
import java.util.Map;

import com.mooltiverse.oss.nyx.data.CommitMessageConvention;
import com.mooltiverse.oss.nyx.data.CommitMessageConventions;
import com.mooltiverse.oss.nyx.data.Scheme;
import com.mooltiverse.oss.nyx.data.Verbosity;
import com.mooltiverse.oss.nyx.version.Versions;

/**
 * A utility interface that collects default configuration values.
 */
public interface Defaults {
    /**
     * The default version identifier to bump. Value: {@code null}
     */
    public static final String BUMP = null;

    /**
     * The default commit message conventions block.
     */
    public static final CommitMessageConventions COMMIT_MESSAGE_CONVENTIONS = new CommitMessageConventions() {
        /**
         * The default enabled commit message conventions. Value: {@code null}
         */
        @Override
        public List<String> getEnabled() {
            return null;
        }

        /**
         * The default commit message conventions. Value: {@code null}
         */
        @Override
        public Map<String,CommitMessageConvention> getItems() {
            return null;
        }

        /**
         * The default commit message convention. Value: {@code null}
         */
        @Override
        public CommitMessageConvention getItem(String name) {
            return null;
        }
    };

    /**
     * The default working directory. Defaults to the current user directory returned by reading the
     * {@code user.dir} from {@link System#getProperty(String)}
     */
    public static final File DIRECTORY = new File(System.getProperty("user.dir"));

    /**
     * The flag that prevents to alter any repository state and instead just log the actions that would be taken. Value: {@code false}
     */
    public static final Boolean DRY_RUN = Boolean.FALSE;

    /**
     * The initial version to use. Value: {@link Scheme#SEMVER}
     * 
     * This strongly depends on the {@link #SCHEME} and as long as it's {@link Scheme#SEMVER}, we use that to select the initial version.
     */
    public static final String INITIAL_VERSION = Versions.defaultInitial(Scheme.SEMVER.getScheme()).toString();

    /**
     * The default prefix to add at the beginning of a version identifier to generate the release identifier. Value: {@code null}
     */
    public static final String RELEASE_PREFIX = null;

    /**
     * The flag that alows reading releases from the history tolerating arbitrary prefixes and extra non critical characters. Value: {@code true}
     */
    public static final Boolean RELEASE_LENIENT = Boolean.TRUE;

    /**
     * The flag that enables loading a previously stored State file and resume operations from there. Value: {@code false}
     */
    public static final Boolean RESUME = Boolean.FALSE;

    /**
     * The versioning scheme to use. Value: {@link Scheme#SEMVER}
     */
    public static final Scheme SCHEME = Scheme.SEMVER;

    /**
     * The path to the local state file. Value: {@code null}
     */
    public static final String STATE_FILE = null;

    /**
     * The logging level. Value: {@link Verbosity#WARNING}.
     * 
     * Please note that the verbosity option is actually ignored in this library implementation as the event filtering based
     * on the verbosity needs to be configured outside this library, depending on the logging framework deployed along with SLF4J.
     * See <a href="http://www.slf4j.org/manual.html#swapping">here</a> for more.
     */
    public static final Verbosity VERBOSITY = Verbosity.WARNING;

    /**
     * The release version. Value: {@code null}
     */
    public static final String VERSION = null;
}
