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
package com.mooltiverse.oss.nyx.data;

import java.util.Objects;

/**
 * This object is a Git action value holder independent from the underlying Git implementation.
 */
public class Action {
    /**
     * The identity.
     */
    private final Identity identity;

    /**
     * The time stamp.
     */
    private final TimeStamp timeStamp;

    /**
     * Constructor.
     * 
     * @param identity the identity. Cannot be {@code null}
     * @param timeStamp the time stamp. May be {@code null}
     */
    public Action(Identity identity, TimeStamp timeStamp) {
        super();
        Objects.requireNonNull(identity);
        this.identity = identity;
        this.timeStamp = timeStamp;
    }

    /**
     * Returns the identity.
     * 
     * @return the identity. Never {@code null}.
     */
    public Identity getIdentity() {
        return identity;
    }

    /**
     * Returns the time stamp.
     * 
     * @return the time stamp. May be {@code null}.
     */
    public TimeStamp getTimeStamp() {
        return timeStamp;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return identity.toString().concat(Objects.isNull(timeStamp) ? "" : " ".concat(timeStamp.toString()));
    }
}