import { isType } from 'typescript-fsa'
import { apiRequest } from '../actions/actions'
import { RRPMiddleware } from '../store/types'

export const apiMiddleware: RRPMiddleware = store => next => action => {
  next(action)
  const dispatch = store.dispatch

  if (isType(action, apiRequest)) {
    const { url, onSuccess, onFailure } = action.payload
    const method = action.payload.method ? action.payload.method : 'GET'
    const body = action.payload.body ? action.payload.body : null

    fetch(url, {
      method: method,
      body: body
    })
      .then(response => {
        const followUp = onSuccess(response)
        const actions = Array.isArray(followUp) ? followUp : followUp instanceof Object ? [followUp] : []
        actions.forEach(action => dispatch(action))
      })
      .catch(error => {
        console.log('api request error', error, action)
        const followUp = onFailure(error)
        const actions = Array.isArray(followUp) ? followUp : followUp instanceof Object ? [followUp] : []
        actions.forEach(action => dispatch(action))
      })
  }
}
