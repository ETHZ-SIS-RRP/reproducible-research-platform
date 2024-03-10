import { Middleware } from 'redux'
import { isType } from 'typescript-fsa'
import {
  apiRequest,
  checkAuthentication,
  configRetrieved,
  dataSetTypesRetrieved,
  fetchDataSetTypes,
  getConfig,
  init,
  logoutSuccessful,
  startEvents
} from '../actions/actions'
import { DataSetType } from '../store/types'

export const coreMiddleware: Middleware =
  ({ dispatch }) =>
  next =>
  action => {
    next(action)
    if (isType(action, init)) {
      dispatch(getConfig())
      dispatch(checkAuthentication())
    }

    if (isType(action, fetchDataSetTypes)) {
      dispatch(
        apiRequest({
          url: '/dataset-types',
          onSuccess: response => {
            if (response.status == 401) {
              dispatch(logoutSuccessful())
            } else if (response.status == 200) {
              response.json().then(json => {
                dispatch(dataSetTypesRetrieved([...(json as DataSetType[])].sort((a, b) => a.name.localeCompare(b.name))))
                dispatch(startEvents())
              })
            }
          }
        })
      )
    }

    if (isType(action, getConfig)) {
      dispatch(
        apiRequest({
          url: '/config.json',
          onSuccess: response => {
            if (response.status == 200) {
              response.json().then(json => {
                dispatch(configRetrieved(json))
              })
            }
          }
        })
      )
    }
  }
