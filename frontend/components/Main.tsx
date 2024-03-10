import React, { FunctionComponent } from 'react'
import { connect, ConnectedProps, useDispatch } from 'react-redux'
import { initRouting } from '../actions/actions'

import { LoggedIn, SystemState } from '../store/types'

import Dialog from './dialog/Dialog'
import Home from './Home'
import Login from './Login'
import styles from './Main.scss'

const mapState = (state: SystemState) => ({
  loginStatus: state.login,
  projects: state.backendProjects,
  dataSetTypes: state.dataSetTypes,
  config: state.config,
  selected: state.selectedProject,
  routingInitialized: state.routingInitialized
})

const connector = connect(mapState)

const Main: FunctionComponent<ConnectedProps<typeof connector>> = props => {
  const initialized = props.loginStatus != null && props.config != null
  const loggedIn = props.loginStatus instanceof LoggedIn
  const projectsInitialized = props.projects != null
  const dataSetTypesInitialized = props.dataSetTypes.length > 0
  const routingInitialized = props.routingInitialized

  const dispatch = useDispatch()

  let content: JSX.Element
  if (initialized) {
    if (loggedIn) {
      if (projectsInitialized && dataSetTypesInitialized) {
        if (routingInitialized) {
          content = <Home />
        } else {
          content = <div />
          dispatch(initRouting())
        }
      } else {
        content = <div />
      }
    } else {
      content = <Login />
    }
  } else {
    content = <div />
  }

  return (
    <>
      <Dialog />
      <div className={styles.main}>{content}</div>
    </>
  )
}

export default connector(Main)
