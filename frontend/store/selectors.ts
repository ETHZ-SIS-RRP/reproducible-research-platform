import { ArchiveSource, BackendProject, GitSource, ProjectStatus, ShareIdSource } from './backend'
import { Configuration, DataSetType, LoggedIn, Message, SystemState } from './types'
import {
  ActiveProjectData,
  ArchiveCreatorData,
  DataSetCreationSelection,
  DataSetProperties,
  GitCreatorData,
  LogInfo,
  MountInfo,
  ShareIdCreatorData,
  Tab,
  UserInterfaceData
} from './ui'

type Selector<A> = (state: SystemState) => A

export const getProject: (id: String) => Selector<BackendProject> = id => state => state.backendProjects.find(p => p.id === id)

export const getSelectedProject: Selector<BackendProject> = state => getProject(state.selectedProject)(state)

const initialGitCreatorData = new GitCreatorData('', '', '', 'Untitled project', null, '')

function defaultCreationSelection(dataSetTypes: DataSetType[]): DataSetCreationSelection {
  const selections: DataSetProperties = {}

  dataSetTypes.forEach(dst => {
    const values: { [key: string]: string } = {}
    dst.propertyAssignments.forEach(ass => {
      values[ass.name] = ''
    })
    selections[dst.name] = values
  })

  return new DataSetCreationSelection(dataSetTypes[0].name, '', selections)
}

export function defaultActiveProjectData(dataSetTypes: DataSetType[]): ActiveProjectData {
  return new ActiveProjectData(Tab.Details, defaultCreationSelection(dataSetTypes), new MountInfo('', false, new Message(false, '')), new LogInfo(''))
}

export const getUiDataOf: (_: BackendProject) => Selector<UserInterfaceData> = project => state => {
  const uiData = state.uiData[project.id]
  switch (project.status) {
    case ProjectStatus.Initializing:
      return uiData instanceof GitCreatorData ? uiData : initialGitCreatorData
    case ProjectStatus.InitializingFromShare:
      return uiData instanceof ShareIdCreatorData ? uiData : new ShareIdCreatorData('', '', '', null)
    case ProjectStatus.InitializingFromArchive:
      return uiData instanceof ArchiveCreatorData ? uiData : new ArchiveCreatorData('', '')
    case ProjectStatus.Cloning:
    case ProjectStatus.Cloned:
    case ProjectStatus.CloneFailed:
      if (project.source instanceof GitSource) {
        return uiData instanceof GitCreatorData ? uiData : initialGitCreatorData
      } else if (project.source instanceof ShareIdSource) {
        return uiData instanceof ShareIdCreatorData ? uiData : new ShareIdCreatorData(project.source.share.id, '', '', project.source.share)
      } else {
        return null
      }
    case ProjectStatus.Ready:
      return uiData instanceof ActiveProjectData ? uiData : defaultActiveProjectData(state.dataSetTypes)
    default:
      return null
  }
}

export const getRepositoryOf: (_: BackendProject) => Selector<string> = project => state => {
  const uiData = getUiDataOf(project)(state)
  if (uiData instanceof GitCreatorData && !project.cloneStatus) {
    return uiData.repository
  } else if (uiData instanceof ShareIdCreatorData && !project.cloneStatus) {
    return uiData.share?.repository ?? ''
  } else if (uiData instanceof ArchiveCreatorData) {
    return uiData.archiveId ? 'openBIS dataset ' + uiData.archiveId : ''
  } else {
    let source = ''
    if (project.source instanceof ArchiveSource) {
      source = 'openBIS dataset ' + project.source.archiveId
    }
    return project.cloneStatus?.remote ?? source
  }
}

export const getNameOf: (_: BackendProject) => Selector<string> = project => state => {
  const uiData = getUiDataOf(project)(state)
  if (uiData instanceof GitCreatorData) {
    return uiData.projectName
  } else if (uiData instanceof ShareIdCreatorData) {
    return uiData.share?.projectName ?? 'Untitled project'
  } else {
    return project.metadata['name'] ?? 'Untitled project'
  }
}

export const getUsername: Selector<string> = state => (state.login instanceof LoggedIn ? state.login.username : '')

export const getSessionValidTo: Selector<string> = state => (state.login instanceof LoggedIn ? state.login.validTo : '')

export const getConfig: Selector<Configuration> = state => state.config

export const getProjects: Selector<BackendProject[]> = state => state.backendProjects ?? []

export const getDataSetTypes: Selector<DataSetType[]> = state => state.dataSetTypes ?? []
