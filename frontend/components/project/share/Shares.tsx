import React, { FunctionComponent } from 'react'
import { BackendProject } from '../../../store/backend'
import CreatePlayerBundle from './CreatePlayerBundle'
import CreateShareId from './CreateShareId'
import PlayerBundleContextProvider from './PlayerBundleContextProvider'
import PlayerBundleList from './PlayerBundleList'
import ProjectContextProvider from './ProjectContextProvider'
import RepositoryStatus from './RepositoryStatus'
import ShareIdContextProvider from './ShareIdContextProvider'
import ShareIdList from './ShareIdList'
import styles from './Shares.scss'
import PlayerScript from './PlayerScript'
import ExportContextProvider from './ExportContextProvider'

type ComponentProps = {
  project: BackendProject
}

const Shares: FunctionComponent<ComponentProps> = props => {
  const { project } = props

  return (
    <div className={styles.main}>
      <ProjectContextProvider project={project}>
        <ShareIdContextProvider>
          <PlayerBundleContextProvider>
            <ExportContextProvider>
              <RepositoryStatus />
              <ShareIdList />
              <CreateShareId />
              <PlayerBundleList />
              <CreatePlayerBundle />
              <PlayerScript />
            </ExportContextProvider>
          </PlayerBundleContextProvider>
        </ShareIdContextProvider>
      </ProjectContextProvider>
    </div>
  )
}

export default Shares
