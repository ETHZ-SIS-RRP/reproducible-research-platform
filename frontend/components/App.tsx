import { History } from 'history'
import React from 'react'
import { Provider } from 'react-redux'
import { Router } from 'react-router'
import { Store } from 'redux'
import { Action } from 'typescript-fsa'

import { SystemState } from '../store/types'

import Main from './Main'

interface AppProps {
  store: Store<SystemState, Action<any>>
  history: History
}

const App = (props: AppProps) => {
  return (
    <Router history={props.history}>
      <Provider store={props.store}>
        <Main />
      </Provider>
    </Router>
  )
}

export default App
