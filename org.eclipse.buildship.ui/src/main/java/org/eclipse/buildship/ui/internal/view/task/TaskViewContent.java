/*******************************************************************************
 * Copyright (c) 2019 Gradle Inc.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package org.eclipse.buildship.ui.internal.view.task;

import java.util.List;

import org.gradle.tooling.model.eclipse.EclipseProject;

import org.eclipse.core.resources.IProject;

/**
 * Encapsulates the content backing the {@link TaskView}.
 */
public final class TaskViewContent {

    private final List<EclipseProject> projectsFromRootBuild;
    private final List<EclipseProject> projectsFromIncludedBuild;
    private final List<IProject> faultyProjects;

    public TaskViewContent(List<EclipseProject> projectsFromRootBuild, List<EclipseProject> projectsFromIncludedBuild, List<IProject> faultyProjects) {
        this.projectsFromRootBuild = projectsFromRootBuild;
        this.projectsFromIncludedBuild = projectsFromIncludedBuild;
        this.faultyProjects = faultyProjects;
    }

    public List<EclipseProject> getProjectsFromRootBuild() {
        return this.projectsFromRootBuild;
    }

    public List<EclipseProject> getProjectsFromIncludedBuild() {
        return this.projectsFromIncludedBuild;
    }

    public List<IProject> getFaultyProjects() {
        return this.faultyProjects;
    }
}
