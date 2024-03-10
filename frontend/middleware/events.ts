import { Middleware } from 'redux'
import { isType } from 'typescript-fsa'
import { projectDeleted, projectUpdated, startEvents, stopEvents } from '../actions/actions'
import { parseShare } from '../common/utils'
import {
  ArchiveSource,
  BackendProject,
  CloneDetails,
  CommitDetails,
  GitSource,
  ProjectSource,
  ProjectStatus,
  RemoteTracking,
  ShareIdSource,
  SourceNotYetDefined
} from '../store/backend'
import { RRPMiddleware } from '../store/types'

let source: EventSource = null

function parseProject(raw: any): BackendProject {
  const id = raw['id']
  const rawStatus = raw['status']

  let status: ProjectStatus
  let statusMessage = ''

  Object.values(ProjectStatus).forEach(s => (s.toString() in rawStatus ? (status = s) : null))

  if ('CloneFailed' in rawStatus) {
    statusMessage = rawStatus['CloneFailed']['reason']
  }

  const rawSource = raw['source']
  let source: ProjectSource
  if ('SourceNotYetDefined' in rawSource) {
    source = SourceNotYetDefined
  } else if ('GitSource' in rawSource) {
    source = new GitSource(rawSource['GitSource']['remote'], rawSource['GitSource']['commitHash'])
  } else if ('ShareIdSource' in rawSource) {
    source = new ShareIdSource(parseShare(rawSource['ShareIdSource']['share']))
  } else if ('ArchiveSource' in rawSource) {
    source = new ArchiveSource(rawSource['ArchiveSource']['archiveId'])
  }

  let rawCloneStatus = raw['cloneStatus']
  let cloneStatus: CloneDetails = null

  if (rawCloneStatus != null) {
    const commit = rawCloneStatus['commit']
    const rawRemoteTracking = rawCloneStatus['remoteTracking']
    let remoteTracking: RemoteTracking = null
    if (rawRemoteTracking != null) {
      remoteTracking = new RemoteTracking(rawRemoteTracking['branch'], rawRemoteTracking['ahead'], rawRemoteTracking['behind'])
    }
    cloneStatus = new CloneDetails(
      rawCloneStatus['clean'],
      rawCloneStatus['branch'],
      new CommitDetails(commit['hash'], commit['message'], commit['timestamp'], commit['authorName'], commit['authorEmail']),
      rawCloneStatus['remote'],
      remoteTracking
    )
  }

  return new BackendProject(
    id,
    status,
    statusMessage,
    source,
    raw['metadata'],
    raw['serverUrl'],
    raw['cloneLogUrl'],
    raw['buildLogUrl'],
    raw['resultsUrl'],
    raw['logsUrl'],
    cloneStatus
  )
}

export function parseProjects(rawProjects: any[]): BackendProject[] {
  return rawProjects.map(parseProject).filter(project => project.status != ProjectStatus.Initializing)
}

export const eventMiddleware: RRPMiddleware = store => next => action => {
  next(action)

  const state = store.getState()
  const dispatch = store.dispatch

  if (isType(action, startEvents)) {
    source = new EventSource('/events')
    source.onopen = () => {}
    source.onerror = err => {
      console.log('error in event stream')
    }

    source.addEventListener('project-update', evt => {
      const event = evt as MessageEvent
      if (event.lastEventId === 'deletion') {
        const ids: any[] = JSON.parse(event.data)
        ids.forEach(id => dispatch(projectDeleted(id)))
      } else {
        const rawProjects: any[] = JSON.parse(event.data)
        const projects = parseProjects(rawProjects)

        if (projects.length > 0) {
          dispatch(projectUpdated({ update: projects }))
        }
      }
    })
  }

  if (isType(action, stopEvents)) {
    if (source != null) {
      source.close()
      source = null
    }
  }
}
