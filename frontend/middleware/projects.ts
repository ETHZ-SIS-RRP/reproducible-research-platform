import { Middleware } from 'redux'
import { isType } from 'typescript-fsa'
import {
  createProject,
  startServer,
  stopServer,
  deleteProject,
  deleteProjectAccepted,
  apiRequest,
  projectCreated,
  startServerAccepted,
  stopServerAccepted,
  logoutSuccessful
} from '../actions/actions'

export const projectMiddleware: Middleware =
  ({ dispatch }) =>
  next =>
  action => {
    next(action)
    if (isType(action, createProject)) {
      const { repository, branch, username, password } = action.payload
      const body = username && password ? { repository, branch, username, password } : { repository, branch }

      dispatch(
        apiRequest({
          url: '/projects/',
          method: 'POST',
          body: JSON.stringify(body),
          onSuccess: response => {
            const events = response.headers.get('Location')
            dispatch(projectCreated({ repository: repository, events: events }))
          }
        })
      )
    }

    if (isType(action, deleteProject)) {
      dispatch(
        apiRequest({
          url: '/projects/' + action.payload,
          method: 'DELETE',
          onSuccess: () => null
        })
      )
    }

    if (isType(action, startServer)) {
      dispatch(
        apiRequest({
          url: '/projects/' + action.payload + '/command',
          method: 'POST',
          body: JSON.stringify({ op: 'start' }),
          onSuccess: response => (response.status === 401 ? dispatch(logoutSuccessful()) : dispatch(startServerAccepted(action.payload)))
        })
      )
    }

    if (isType(action, stopServer)) {
      dispatch(
        apiRequest({
          url: '/projects/' + action.payload + '/command',
          method: 'POST',
          body: JSON.stringify({ op: 'stop' }),
          onSuccess: response => dispatch(stopServerAccepted(action.payload))
        })
      )
    }
  }
