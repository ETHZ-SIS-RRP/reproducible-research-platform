import { Action, isType } from 'typescript-fsa'
import {
  closeDialogClicked,
  configRetrieved,
  createProjectFromArchiveClicked,
  createProjectFromGitClicked,
  createProjectFromShareClicked,
  dataSetTypesRetrieved,
  deleteClicked,
  deleteProject,
  logoutSuccessful,
  openInfoBoxClicked,
  pathChanged,
  projectDeleted,
  projectUpdated,
  setSelectedProject,
  updateUiData,
  usernameClicked
} from '../actions/actions'
import { BackendProject, ProjectStatus, SourceNotYetDefined } from './backend'
import { SystemState } from './types'

export const initialState: SystemState = new SystemState(null, null, '', '', null, [], null, {}, false, null)

export const reducer: { (state: SystemState, action: Action<any>): void } = (state: SystemState, action: Action<any>) => {
  if (isType(action, pathChanged)) {
    state.routingInitialized = true
  }

  if (isType(action, setSelectedProject)) {
    state.selectedProject = action.payload
  }

  if (isType(action, projectUpdated)) {
    if (state.backendProjects == null) {
      state.backendProjects = action.payload.update
    } else {
      action.payload.update.forEach(update => {
        const i = state.backendProjects.findIndex(p => p.id === update.id)
        if (i === -1) {
          state.backendProjects = state.backendProjects.concat(update)
        } else {
          state.backendProjects[i] = update
        }
      })
    }
  }

  if (isType(action, deleteProject)) {
    state.backendProjects = state.backendProjects.filter(p => p.id != action.payload)
  }

  if (isType(action, projectDeleted)) {
    state.backendProjects = state.backendProjects.filter(p => p.id != action.payload)
  }

  if (isType(action, createProjectFromGitClicked)) {
    const project = new BackendProject(action.payload, ProjectStatus.Initializing, '', SourceNotYetDefined, {}, '', '', '', '', '')
    state.backendProjects = state.backendProjects.concat([project])
  }

  if (isType(action, createProjectFromShareClicked)) {
    const project = new BackendProject(action.payload, ProjectStatus.InitializingFromShare, '', SourceNotYetDefined, {}, '', '', '', '', '')
    state.backendProjects = state.backendProjects.concat([project])
  }

  if (isType(action, createProjectFromArchiveClicked)) {
    const project = new BackendProject(action.payload, ProjectStatus.InitializingFromArchive, '', SourceNotYetDefined, {}, '', '', '', '', '')
    state.backendProjects = state.backendProjects.concat([project])
  }

  if (isType(action, openInfoBoxClicked)) {
    state.dialogHeader = 'Reproducible Research Platform'
    state.dialogContent = 'infobox'
  }

  if (isType(action, usernameClicked)) {
    state.dialogHeader = 'Session details'
    state.dialogContent = 'session-details'
  }

  if (isType(action, deleteClicked)) {
    state.dialogHeader = 'Are you sure?'
    state.dialogContent = 'delete-confirmation'
    state.deletionCandidate = action.payload
  }

  if (isType(action, closeDialogClicked)) {
    state.dialogContent = ''
  }

  if (isType(action, dataSetTypesRetrieved)) {
    state.dataSetTypes = action.payload
  }

  if (isType(action, logoutSuccessful)) {
    state.dataSetTypes = initialState.dataSetTypes
    state.deletionCandidate = initialState.deletionCandidate
    state.dialogContent = initialState.dialogContent
    state.dialogHeader = initialState.dialogHeader
    state.login = initialState.login
    state.backendProjects = initialState.backendProjects
    state.selectedProject = initialState.selectedProject
  }

  if (isType(action, configRetrieved)) {
    state.config = action.payload
  }

  if (isType(action, updateUiData)) {
    state.uiData[action.payload.projectId] = action.payload.data
  }
}
