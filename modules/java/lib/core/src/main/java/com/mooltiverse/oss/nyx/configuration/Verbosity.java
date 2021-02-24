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

import java.util.Objects;

import org.slf4j.event.Level;

/**
 * This class maps log {@link Level}s and the corresponding configuration options, whose string representations
 * may not always match the {@link Level} values.
 */
public enum Verbosity {
    /**
     * The fatal log level. Please note that this level has no corresponding level into SLF4J
     * so the value is the same as {@link #ERROR}.
     * 
     * @see Level#ERROR
     */
    FATAL(Level.ERROR, "fatal"),

    /**
     * The error log level.
     * 
     * @see Level#ERROR
     */
    ERROR(Level.ERROR, "error"),

    /**
     * The warning log level.
     * 
     * @see Level#WARN
     */
    WARNING(Level.WARN, "warning"),

    /**
     * The info log level.
     * 
     * @see Level#INFO
     */
    INFO(Level.INFO, "info"),

    /**
     * The debug log level.
     * 
     * @see Level#DEBUG
     */
    DEBUG(Level.DEBUG, "debug"),

    /**
     * The trace log level.
     * 
     * @see Level#TRACE
     */
    TRACE(Level.TRACE, "trace");

    /**
     * The SLF4J corresponding logging level.
     */
    private final Level level;

    /**
     * The verbosity level value, also used in configuration.
     */
    private final String value;

    /**
     * Builds the enumeration item.
     * 
     * @param level the SLF4J corresponding logging level
     * @param value the verbosity level value, also used in configuration
     */
    private Verbosity(Level level, String value) {
        this.level = level;
        this.value = value;
    }

    /**
     * Returns the SLF4J level corresponding to this verbosity level
     * 
     * @return the SLF4J level corresponding to this verbosity level
     */
    public Level getLevel() {
        return level;
    }

    /**
     * Returns the string representation of this verbosity level. This string is also used in configuration options.
     * 
     * @return the string representation of this verbosity level
     */
    public String getValue() {
        return value;
    }

    /**
     * Returns the proper verbosity level mapped from the given SLF4J level.
     * 
     * @param level the SLF4J level to parse and return the verbosity for
     * 
     * @return the proper verbosity level mapped from the given SLF4J level.
     * 
     * @throws IllegalArgumentException if the given value cannot be mapped to any existing verbosity level
     * @throws NullPointerException if the given value is {@code null}
     */
    public static Verbosity from(Level level)
        throws IllegalArgumentException, NullPointerException {
        if (Objects.isNull(level))
            throw new NullPointerException();

        switch (level) {
            case TRACE: return Verbosity.TRACE;
            case DEBUG: return Verbosity.DEBUG;
            case INFO:  return Verbosity.INFO;
            case WARN:  return Verbosity.WARNING;
            case ERROR: return Verbosity.ERROR;
            default: throw new IllegalArgumentException(level.toString());
        }
    }

    /**
     * Returns the proper verbosity level mapped from the given value.
     * 
     * @param value string value level to parse and return the verbosity for
     * 
     * @return the proper verbosity level mapped from the given value.
     * 
     * @throws IllegalArgumentException if the given value cannot be mapped to any existing verbosity level
     * @throws NullPointerException if the given value is {@code null}
     */
    public static Verbosity from(String value)
        throws IllegalArgumentException, NullPointerException {
        if (Objects.isNull(value))
            throw new NullPointerException();

        switch (value) {
            case "trace":   return Verbosity.TRACE;
            case "debug":   return Verbosity.DEBUG;
            case "info":    return Verbosity.INFO;
            case "warning": return Verbosity.WARNING;
            case "error":   return Verbosity.ERROR;
            case "fatal":   return Verbosity.FATAL;
            default: throw new IllegalArgumentException(value);
        }
    }
}
