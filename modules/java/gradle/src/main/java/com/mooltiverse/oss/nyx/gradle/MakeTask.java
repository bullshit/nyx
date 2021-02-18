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
package com.mooltiverse.oss.nyx.gradle;

import javax.inject.Inject;

import org.gradle.api.Project;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskProvider;

/**
 * The task running the Make command.
 */
public abstract class MakeTask extends CoreTask {
    /**
     * The decription of the task. This also appears in Gradle help.
     */
    public static final String DESCRIPTION = "Builds the configured release artifacts";

    /**
     * The name of the task. This is the name of the task to use inside Gradle scripts.
     */
    public static final String NAME = "nyxMake";

    /**
     * Default constructor
     */
    @Inject
    public MakeTask() {
        super();
    }

    /**
     * Registers the task into the given project. The task is lazily registered, for deferred creation.
     * 
     * @param project the project to define the task for
     * 
     * @return the task provider used for the task definition
     * 
     * @see #define(Project, String, Class, Action)
     */
    public static TaskProvider<MakeTask> define(Project project) {
        return define(project, NAME, MakeTask.class, task -> configure(task));
    }

    /**
     * Configures the task (group, description, dependencies, properties).
     * 
     * This method is invoked upon configuration as it's passed in the register(...) phase (see {@link #define(Project)}).
     * 
     * @param task the task to configure
     */
    protected static void configure(MakeTask task) {
        CoreTask.configure(task);
        task.setDescription(DESCRIPTION);

        // Configure dependencies
        task.dependsOn(InferTask.NAME);
    }

    /**
     * The actual business method for this task.
     * 
     * Gradle knows this is the method to run upon task execution thanks to the {@link TaskAction} annotation.
     */
    @TaskAction
    public void make() {
        // TODO: replace this method body with actual business logic, invoking the Nyx backing class
        getLogger().info("Running MakeTask: {}", NAME);
    }
}
