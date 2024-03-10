import * as React from 'react'
import ReactDOM from 'react-dom'
import App from './components/App'
import './styles/index.scss'

import { Reducer, applyMiddleware, createStore } from 'redux'
import { Action } from 'typescript-fsa'
import { loginReducer } from './store/login-reducer'
import { initialState, reducer } from './store/reducer'
import { uiDataReducer } from './store/ui-data-reducer'

import { composeWithDevTools } from '@redux-devtools/extension'
import { apiMiddleware } from './middleware/api'
import { authenticationMiddleware } from './middleware/authentication'
import { coreMiddleware } from './middleware/core'
import { eventMiddleware } from './middleware/events'
import { projectMiddleware } from './middleware/projects'
import { history, routingMiddleware } from './middleware/routing'
import { uiMiddleware } from './middleware/ui'

import { enableMapSet, produce } from 'immer'
import { init } from './actions/actions'
import { SystemState } from './store/types'

enableMapSet()

function combineReducers(...sliceReducers: { (state: SystemState, action: Action<any>): void }[]): Reducer<SystemState, Action<any>> {
  return produce((state: SystemState, action: Action<any>) => {
    sliceReducers.forEach(s => s(state, action))
  })
}

const middleware = applyMiddleware(coreMiddleware, apiMiddleware, routingMiddleware, authenticationMiddleware, projectMiddleware, eventMiddleware, uiMiddleware)
const rootReducer = combineReducers(reducer, loginReducer, uiDataReducer)
const store = createStore(rootReducer, initialState, composeWithDevTools(middleware))
store.dispatch(init())

ReactDOM.render(React.createElement(App, { store: store, history: history }), document.getElementById('root'))
