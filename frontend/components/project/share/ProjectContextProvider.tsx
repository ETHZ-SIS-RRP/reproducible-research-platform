import React, { FunctionComponent } from 'react'
import { BackendProject } from '../../../store/backend'

type ComponentProps = {
  project: BackendProject
}

export type Context = {
  project: BackendProject
  remoteTrackingOk: boolean
  shareIdCreationAllowed: boolean
  playerBundleCreationAllowed: boolean
}

export const ProjectContext = React.createContext(null as Context)

const ProjectContextProvider: FunctionComponent<ComponentProps> = props => {
  const { project } = props

  const clean = project.cloneStatus.clean
  const remoteTracking = project.cloneStatus.remoteTracking
  const remoteTrackingOk = remoteTracking.branch !== '' && remoteTracking.ahead === 0

  const contextValue = {
    project: project,
    remoteTrackingOk: remoteTrackingOk,
    shareIdCreationAllowed: clean && remoteTrackingOk,
    playerBundleCreationAllowed: clean && remoteTrackingOk
  }

  return <ProjectContext.Provider value={contextValue}>{props.children}</ProjectContext.Provider>
}

export default ProjectContextProvider
