import { createBrowserHistory } from 'history'
import { isType } from 'typescript-fsa'
import {
  createProjectFromArchiveClicked,
  createProjectFromGitClicked,
  createProjectFromShareClicked,
  deleteProject,
  initRouting,
  logClicked,
  pathChanged,
  projectClicked,
  projectDeleted,
  projectUpdated,
  setNewPath,
  setSelectedProject,
  setSelectedTab,
  tabClicked,
  updateUiData
} from '../actions/actions'
import { getUiDataOf } from '../store/selectors'
import { RRPMiddleware } from '../store/types'
import { ActiveProjectData, LogInfo, Tab } from '../store/ui'

export const history = createBrowserHistory()

export const routingMiddleware: RRPMiddleware = store => next => action => {
  next(action)
  const state = store.getState()
  const dispatch = store.dispatch

  if (isType(action, initRouting)) {
    history.listen((location, _) => dispatch(pathChanged(location.pathname)))
    setTimeout(() => history.push(history.location.pathname), 0)
  }

  if (isType(action, setNewPath)) {
    let path = action.payload
    try {
      const url = new URL(action.payload)
      path = url.pathname
    } catch (error) {}
    setTimeout(() => history.push(path), 0)
  }

  if (isType(action, pathChanged)) {
    const parts = action.payload.split('/').filter(part => part.trim().length > 0)
    if (parts.length == 0) {
      if (state.backendProjects.length > 0) {
        dispatch(setNewPath('/' + state.backendProjects[0].id))
      } else {
        dispatch(setSelectedProject(''))
      }
    } else if (parts.length == 1) {
      const project = state.backendProjects.find(p => p.id === parts[0])
      if (project != null) {
        const uiData = getUiDataOf(project)(state)
        if (uiData instanceof ActiveProjectData) {
          if (uiData.tab === Tab.Logs) {
            dispatch(setNewPath(project.id + '/' + uiData.tab + '/' + uiData.logInfo.selectedLog))
          } else {
            dispatch(setNewPath(project.id + '/' + uiData.tab))
          }
        } else {
          dispatch(setSelectedProject(project.id))
        }
      } else {
        dispatch(setNewPath('/'))
      }
    } else {
      const project = state.backendProjects.find(p => p.id === parts[0])
      const tab = Object.values(Tab).includes(parts[1] as Tab) ? (parts[1] as Tab) : Tab.Details

      if (project != null) {
        dispatch(setSelectedProject(project.id))
        dispatch(setSelectedTab(tab))
        if (tab == Tab.Logs && parts.length >= 3) {
          dispatch(
            updateUiData({ projectId: project.id, data: (getUiDataOf(project)(state) as ActiveProjectData).setTab(tab).setLogInfo(new LogInfo(parts[2])) })
          )
        }
      } else {
        dispatch(setNewPath('/'))
      }
    }
  }

  if (isType(action, projectClicked)) {
    dispatch(setNewPath('/' + action.payload))
  }

  if (isType(action, tabClicked)) {
    dispatch(setNewPath('/' + state.selectedProject + '/' + action.payload))
  }

  if (isType(action, createProjectFromGitClicked)) {
    dispatch(setNewPath('/' + action.payload))
  }

  if (isType(action, createProjectFromShareClicked)) {
    dispatch(setNewPath('/' + action.payload))
  }

  if (isType(action, createProjectFromArchiveClicked)) {
    dispatch(setNewPath('/' + action.payload))
  }

  if (isType(action, projectUpdated)) {
    if (!state.selectedProject && state.routingInitialized) {
      dispatch(setNewPath('/'))
    }
  }

  if (isType(action, deleteProject) || isType(action, projectDeleted)) {
    if (state.selectedProject === action.payload) {
      dispatch(setNewPath('/'))
    }
  }

  if (isType(action, logClicked)) {
    dispatch(setNewPath(`/${state.selectedProject}/logs/${action.payload}`))
  }
}
