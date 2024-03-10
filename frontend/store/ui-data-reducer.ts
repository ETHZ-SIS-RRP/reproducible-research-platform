import { Action, isType } from 'typescript-fsa'
import { buildFromArchiveFailedToStart, buildFromGitFailedToStart, setSelectedTab } from '../actions/actions'
import { defaultActiveProjectData } from './selectors'
import { SystemState } from './types'
import { ActiveProjectData, ArchiveCreatorData, GitCreatorData } from './ui'

export const uiDataReducer: { (state: SystemState, action: Action<any>): void } = (state, action) => {
  if (isType(action, buildFromGitFailedToStart)) {
    const uiData = state.uiData[action.payload.projectId] as GitCreatorData
    state.uiData[action.payload.projectId] = uiData.setBuildError(action.payload.message)
  }

  if (isType(action, buildFromArchiveFailedToStart)) {
    const uiData = state.uiData[action.payload.projectId] as ArchiveCreatorData
    state.uiData[action.payload.projectId] = uiData.setCreateError(action.payload.message)
  }

  if (isType(action, setSelectedTab)) {
    const current = state.uiData[state.selectedProject]
    const uiData = current instanceof ActiveProjectData ? current : defaultActiveProjectData(state.dataSetTypes)
    state.uiData[state.selectedProject] = uiData.setTab(action.payload)
  }
}
