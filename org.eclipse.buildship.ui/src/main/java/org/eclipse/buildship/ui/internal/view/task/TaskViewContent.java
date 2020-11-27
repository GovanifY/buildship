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

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.gradle.tooling.model.eclipse.EclipseProject;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import org.eclipse.core.resources.IProject;

import org.eclipse.buildship.core.internal.util.gradle.HierarchicalElementUtils;

/**
 * Encapsulates the content backing the {@link TaskView}.
 */
public final class TaskViewContent {

    private final Map<File, Map<String, EclipseProject>> models;
    private final List<IProject> faultyWorkspaceProjects;

    public TaskViewContent(Map<File, Map<String, EclipseProject>> models, Map<String, IProject> allGradleWorkspaceProjects) {
        this.models = models;
        for (EclipseProject p : getAllModels()) {
            allGradleWorkspaceProjects.remove(p.getName());
        }
        this.faultyWorkspaceProjects = Lists.newArrayList(allGradleWorkspaceProjects.values());
    }

    public List<IProject> getFaultyWorkspaceProjects() {
        return ImmutableList.copyOf(this.faultyWorkspaceProjects);
    }

    List<EclipseProject> getAllModels() {
        List<EclipseProject> result = Lists.newArrayList();
        for(Map<String, EclipseProject> ep1 : this.models.values()) {
            for (EclipseProject ep2 : ep1.values()) {
                result.addAll(HierarchicalElementUtils.getAll(ep2));
            }
        }
        return result;
    }

    Multimap<String, EclipseProject> getAllTopLevelModels() {
        Multimap<String, EclipseProject> result = ArrayListMultimap.create();
        for(Map<String, EclipseProject> ep1 : this.models.values()) {
            for(Entry<String, EclipseProject> ep2 : ep1.entrySet()) {
                result.put(ep2.getKey(), ep2.getValue());
            }
        }
        return result;
    }

    public Map<File, Map<String, EclipseProject>> getModels() {
        return ImmutableMap.copyOf(this.models);
    }
}
