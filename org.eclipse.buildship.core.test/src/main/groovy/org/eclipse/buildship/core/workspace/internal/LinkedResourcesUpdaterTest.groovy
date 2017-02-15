package org.eclipse.buildship.core.workspace.internal

import com.gradleware.tooling.toolingmodel.OmniEclipseLinkedResource

import org.eclipse.core.resources.IFolder
import org.eclipse.core.resources.IProject
import org.eclipse.core.resources.IResource
import org.eclipse.core.runtime.IPath
import org.eclipse.core.runtime.NullProgressMonitor
import org.eclipse.core.runtime.Path

import org.eclipse.buildship.core.preferences.PersistentModel
import org.eclipse.buildship.core.preferences.PersistentModelFactory
import org.eclipse.buildship.core.test.fixtures.WorkspaceSpecification

class LinkedResourcesUpdaterTest extends WorkspaceSpecification {

    def "Can define a linked resource"() {
        given:
        File externalDir = dir('another')
        IProject project = newProject('project-name')
        OmniEclipseLinkedResource linkedResource =  newFolderLinkedResource(externalDir.name, externalDir)
        PersistentModelBuilder persistentModel = builder(project)

        when:
        LinkedResourcesUpdater.update(project, [linkedResource], persistentModel, new NullProgressMonitor())
        Collection<IFolder> linkedFolders = linkedFolders(project)

        then:
        linkedFolders.size() == 1
        linkedFolders[0].name == 'another'
        linkedFolders[0].exists()
        linkedFolders[0].location.toFile().equals(externalDir)
    }

    def "Can define a linked resource even if the resource does not exist"() {
        given:
            File externalDir = getDir('another')
            IProject project = newProject('project-name')
            OmniEclipseLinkedResource linkedResource =  newFolderLinkedResource(externalDir.name, externalDir)
            PersistentModelBuilder persistentModel = builder(project)

            when:
            LinkedResourcesUpdater.update(project, [linkedResource], persistentModel, new NullProgressMonitor())
            Collection<IFolder> linkedFolders = linkedFolders(project)

            then:
            linkedFolders.size() == 1
            linkedFolders[0].name == 'another'
            linkedFolders[0].exists()
            linkedFolders[0].location.toFile().equals(externalDir)
    }

    def "Defining a linked resource is idempotent"() {
        given:
        File externalDir = dir('another')
        IProject project = newProject('project-name')
        OmniEclipseLinkedResource linkedResource =  newFolderLinkedResource(linkName, externalDir)
        PersistentModelBuilder persistentModel = builder(project)
        LinkedResourcesUpdater.update(project, [linkedResource], persistentModel, new NullProgressMonitor())
        linkedResource = newFolderLinkedResource(linkName, externalDir)

        persistentModel = builder(persistentModel.build())

        when:
        LinkedResourcesUpdater.update(project, [linkedResource], persistentModel, new NullProgressMonitor())
        Collection<IFolder> linkedFolders = linkedFolders(project)

        then:
        linkedFolders.size() == 1
        linkedFolders[0].name == expectedFolderName
        linkedFolders[0].fullPath.toPortableString() == expectedFolderPath

        where:
        linkName  | expectedFolderName | expectedFolderPath
        'another' | 'another'          | '/project-name/another'
        'a/b/c'   | 'c'                | '/project-name/a/b/c'
    }

    def "Only local folder linked resources are set on the project" () {
        given:
        File externalDir = dir('another')
        File externalFile = file('file')
        IProject project = newProject('project-name')
        OmniEclipseLinkedResource localFolder =  newFolderLinkedResource(externalDir.name, externalDir)
        OmniEclipseLinkedResource localFile =  newFileLinkedResource(externalFile.name, externalFile)
        OmniEclipseLinkedResource virtualResource =  newVirtualLinkedResource()
        PersistentModelBuilder persistentModel = builder(project)

        when:
        LinkedResourcesUpdater.update(project, [localFile, localFolder, virtualResource], persistentModel, new NullProgressMonitor())

        then:
        linkedFolders(project).size() == 1
    }

    def "If a linked resource name matches to an existing folder, then the folder is replaced" () {
        given:
        IProject project = newProject('project-name')
        project.getFolder('foldername').create(true, true, null)
        File externalDir = dir('foldername')
        OmniEclipseLinkedResource linkedResource =  newFolderLinkedResource(externalDir.name, externalDir)
        PersistentModelBuilder persistentModel = builder(project)

        when:
        LinkedResourcesUpdater.update(project, [linkedResource], persistentModel, new NullProgressMonitor())
        Collection<IFolder> linkedFolders = linkedFolders(project)

        then:
        linkedFolders.size() == 1
        linkedFolders[0].name == 'foldername'
    }

    def "A linked resource is deleted if no longer part of the Gradle model"() {
        given:
        File externalDirA = dir('another1')
        File externalDirB = dir('another2')
        IProject project = newProject('project-name')
        OmniEclipseLinkedResource linkedResourceA =  newFolderLinkedResource(linkName, externalDirA)
        OmniEclipseLinkedResource linkedResourceB =  newFolderLinkedResource(externalDirB.name, externalDirB)
        PersistentModelBuilder persistentModel = builder(project)
        LinkedResourcesUpdater.update(project, [linkedResourceA], persistentModel, new NullProgressMonitor())

        persistentModel = builder(persistentModel.build())

        when:
        LinkedResourcesUpdater.update(project, [linkedResourceB], persistentModel, new NullProgressMonitor())
        Collection<IFolder> linkedFolders = linkedFolders(project)

        then:
        !project.getFolder(linkName).exists()
        linkedFolders.size() == 1
        linkedFolders[0].name == 'another2'
        linkedFolders[0].exists()
        linkedFolders[0].location.toFile().equals(externalDirB)

        where:
        linkName << ['another', 'a/b/c']
    }

    def "Only linked resources are removed from the project"() {
        given:
        File externalDirA = dir('another1')
        File externalDirB = dir('another2')
        IProject project = newProject('project-name')
        OmniEclipseLinkedResource linkedResourceA =  newFolderLinkedResource(externalDirA.name, externalDirA)
        OmniEclipseLinkedResource linkedResourceB =  newFolderLinkedResource(externalDirB.name, externalDirB)
        PersistentModelBuilder persistentModel = builder(project)
        LinkedResourcesUpdater.update(project, [linkedResourceA], persistentModel, new NullProgressMonitor())

        persistentModel = builder(persistentModel.build())

        when:
        project.getFolder('another1').delete(false, new NullProgressMonitor())
        project.getFolder('another1').create(true, true, new NullProgressMonitor())
        LinkedResourcesUpdater.update(project, [linkedResourceB], persistentModel, new NullProgressMonitor())
        Collection<IFolder> linkedFolders = linkedFolders(project)

        then:
        project.getFolder('another1').exists()
    }

    def "Model linked resources that were previously defined manually are transformed to model linked resources"() {
        given:
        File externalDir = dir('another')
        IProject project = newProject('project-name')
        IPath linkedFolderPath = new Path(externalDir.absolutePath)
        PersistentModelBuilder persistentModel = builder(project)
        IFolder manuallyDefinedLinkedFolder = project.getFolder(externalDir.name)
        manuallyDefinedLinkedFolder.createLink(linkedFolderPath, IResource.NONE, null);
        OmniEclipseLinkedResource linkedResource = newFolderLinkedResource(externalDir.name, externalDir)
        LinkedResourcesUpdater.update(project, [linkedResource], persistentModel, new NullProgressMonitor())

        persistentModel = builder(persistentModel.build())

        when:
        LinkedResourcesUpdater.update(project, [], persistentModel, new NullProgressMonitor())

        then:
        !project.getFolder('another').isLinked()
    }

    def "Can create linked resources in the subfolders" () {
        given:
        File externalDir = dir('ext')
        IProject project = newProject('project-name')
        OmniEclipseLinkedResource linkedResource =  newFolderLinkedResource('links/link-to-ext', externalDir)
        PersistentModelBuilder persistentModel = builder(project)

        when:
        LinkedResourcesUpdater.update(project, [linkedResource], persistentModel, new NullProgressMonitor())

        then:
        linkedFolders(project).size() == 1
        project.getFolder('links/link-to-ext').isLinked()
    }

    private def newFolderLinkedResource(String name, File location) {
        OmniEclipseLinkedResource linkedResource = Mock()
        linkedResource.name >> name
        linkedResource.type >> '2'
        linkedResource.location >> location.path
        linkedResource.locationUri >> null
        linkedResource
    }

    private def newVirtualLinkedResource() {
        OmniEclipseLinkedResource linkedResource = Mock()
        linkedResource.name >> 'example'
        linkedResource.type >> '1'
        linkedResource.location >> null
        linkedResource.locationUri >> 'http://example.com'
        linkedResource
    }

    private def newFileLinkedResource(String name, File file) {
        assert file.isFile()
        OmniEclipseLinkedResource linkedResource = Mock()
        linkedResource.name >> name
        linkedResource.type >> '1'
        linkedResource.location >> file.path
        linkedResource.locationUri >> null
        linkedResource
    }

    private def linkedFolders(IProject project) {
        collectLinkedFolders(project.members() as List)
    }

    private def collectLinkedFolders(Collection resources, Collection result = []) {
        resources.each { resource ->
            if (resource instanceof IFolder) {
                if (resource.linked) {
                    result.add(resource)
                } else {
                    collectLinkedFolders(resource.members() as List, result)
                }
            }
        }
        result
    }

    private PersistentModelBuilder builder(PersistentModel model) {
        new PersistentModelBuilder(model)
    }

    private PersistentModelBuilder builder(IProject project) {
        new PersistentModelBuilder(emptyModel(project))
    }

    private PersistentModel emptyModel(IProject project) {
        PersistentModelFactory.from(project, new Path("build"), [], [], [], [])
    }
}
