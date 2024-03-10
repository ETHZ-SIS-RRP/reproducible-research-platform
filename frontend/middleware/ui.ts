import { Middleware } from 'redux'
import { isType } from 'typescript-fsa'
import {
  apiRequest,
  buildFromGitFailedToStart,
  buildFromGitClicked,
  checkAuthentication,
  cloneFailed,
  cloneClicked,
  loginFailed,
  loginClicked,
  logoutClicked,
  logoutSuccessful,
  stopEvents,
  buildFromArchiveClicked,
  buildFromArchiveFailedToStart
} from '../actions/actions'

export const uiMiddleware: Middleware =
  ({ dispatch }) =>
  next =>
  action => {
    next(action)

    if (isType(action, loginClicked)) {
      const body =
        action.payload.pat !== ''
          ? {
              token: action.payload.pat
            }
          : {
              username: action.payload.username,
              password: action.payload.password
            }

      dispatch(
        apiRequest({
          url: '/authentication/',
          method: 'POST',
          body: JSON.stringify(body),
          onSuccess: response => {
            if (response.status == 200) {
              response.json().then(json => {
                dispatch(checkAuthentication())
              })
            } else {
              dispatch(loginFailed())
            }
          }
        })
      )
    }

    if (isType(action, logoutClicked)) {
      dispatch(
        apiRequest({
          url: '/authentication/',
          method: 'DELETE',
          onSuccess: response => logoutSuccessful()
        })
      )
      dispatch(stopEvents())
    }

    if (isType(action, cloneClicked)) {
      dispatch(
        apiRequest({
          url: '/clones',
          method: 'POST',
          body: JSON.stringify(action.payload),
          onSuccess: (_: any) => null,
          onFailure: (_: any) => cloneFailed(action.payload.projectId)
        })
      )
    }

    if (isType(action, buildFromGitClicked)) {
      const body = {
        projectId: action.payload.id,
        name: action.payload.name,
        branch: action.payload.branch
      }

      dispatch(
        apiRequest({
          url: '/projects',
          method: 'POST',
          body: JSON.stringify(body),
          onSuccess: response => {
            if (response.status === 400)
              response.json().then(json => dispatch(buildFromGitFailedToStart({ projectId: action.payload.id, message: json['error'] })))
          }
        })
      )
    }

    if (isType(action, buildFromArchiveClicked)) {
      const body = {
        projectId: action.payload.projectId,
        archiveId: action.payload.archiveId
      }

      dispatch(
        apiRequest({
          url: '/projects',
          method: 'POST',
          body: JSON.stringify(body),
          onSuccess: response => {
            if (response.status === 400)
              response.json().then(json => dispatch(buildFromArchiveFailedToStart({ projectId: action.payload.projectId, message: json['error'] })))
          }
        })
      )
    }
  }
