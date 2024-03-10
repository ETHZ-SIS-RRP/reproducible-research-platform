import { Middleware } from 'redux'
import { isType } from 'typescript-fsa'
import { apiRequest, loginSuccessful, checkAuthentication, loginFailed, fetchDataSetTypes, projectUpdated } from '../actions/actions'
import { parseProjects } from './events'

export const authenticationMiddleware: Middleware = store => next => action => {
  next(action)
  const dispatch = store.dispatch

  if (isType(action, checkAuthentication)) {
    dispatch(
      apiRequest({
        url: '/authentication/',
        onSuccess: response => {
          if (response.status == 200) {
            response.json().then(json => {
              dispatch(
                loginSuccessful({
                  username: json.username,
                  validTo: json.validTo
                })
              )
              dispatch(projectUpdated({ update: parseProjects(json.updates as any[]) }))
            })

            dispatch(fetchDataSetTypes())
          } else {
            dispatch(loginFailed())
          }
        }
      })
    )
  }
}
