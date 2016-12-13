/*
 * Copyright (c) 2016 the original author or authors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.buildship.core.workspace.internal;

import java.util.Collection;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import com.gradleware.tooling.toolingmodel.OmniEclipseProject;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;

import org.eclipse.buildship.core.CorePlugin;
import org.eclipse.buildship.core.GradlePluginsRuntimeException;
import org.eclipse.buildship.core.util.file.RelativePathUtils;

/**
 * Updates the markers for subproject folders.
 *
 * @author Donat Csikos
 */
final class SubprojectMarkerUpdater {

    private static final String PERSISTENT_PROP_NAME = "subprojects";

    private final IProject project;
    private final OmniEclipseProject gradleProject;

    private SubprojectMarkerUpdater(IProject project, OmniEclipseProject gradleProject) {
        this.project = Preconditions.checkNotNull(project);
        this.gradleProject = Preconditions.checkNotNull(gradleProject);
    }

    public void update(IProgressMonitor monitor) {
        SubMonitor progress = SubMonitor.convert(monitor, 2);
        try {
            List<IPath> subfolders = getNestedSubProjectFolderPaths(progress.newChild(1));
            updateSubProjectMarkers(subfolders, progress.newChild(1));
        } catch (Exception e) {
            String message = String.format("Could not update sub-project markers on project %s.", this.project.getName());
            throw new GradlePluginsRuntimeException(message, e);
        } finally {
            if (monitor != null) {
                monitor.done();
            }
        }
    }

    private List<IPath> getNestedSubProjectFolderPaths(SubMonitor progress) {
        List<IPath> subfolderPaths = Lists.newArrayList();
        final IPath parentPath = this.project.getLocation();
        for (OmniEclipseProject child : this.gradleProject.getChildren()) {
            IPath childPath = Path.fromOSString(child.getProjectDirectory().getPath());
            if (parentPath.isPrefixOf(childPath)) {
                IPath relativePath = RelativePathUtils.getRelativePath(parentPath, childPath);
                subfolderPaths.add(relativePath);
            }
        }
        return subfolderPaths;
    }

    private void updateSubProjectMarkers(List<IPath> subfolderPaths, SubMonitor progress) throws CoreException {
        List<String> knownSubfolderPaths = Lists.newArrayList();
        for (IPath subfolderPath : subfolderPaths) {
            IFolder subfolder = this.project.getFolder(subfolderPath);
            if (subfolder.exists()) {
                knownSubfolderPaths.add(subfolderPath.toPortableString());
            }
            progress.worked(1);
        }
        PersistentUpdaterUtils.setKnownItems(this.project, PERSISTENT_PROP_NAME, knownSubfolderPaths);
    }

    public static void update(IProject workspaceProject, OmniEclipseProject gradleProject, IProgressMonitor monitor) {
        new SubprojectMarkerUpdater(workspaceProject, gradleProject).update(monitor);
    }

    public static boolean isNestedSubProject(IFolder folder) {
        try {
            IPath relativePath = RelativePathUtils.getRelativePath(folder.getProject().getFullPath(), folder.getFullPath());
            Collection<String> knownPaths = PersistentUpdaterUtils.getKnownItems(folder.getProject(), PERSISTENT_PROP_NAME);
            return knownPaths.contains(relativePath.toPortableString());
        } catch (Exception e) {
            CorePlugin.logger().debug(String.format("Could not check whether folder %s is a sub project.", folder.getFullPath()), e);
            return false;
        }
    }

}
