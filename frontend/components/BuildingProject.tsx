import React, { FunctionComponent } from 'react'

import { useTailLog } from '../common/taillog'
import { BackendProject } from '../store/backend'

import styles from './BuildingProject.scss'
import TerminalLog from './TerminalLog'

type Props = {
  project: BackendProject
}

const BuildingProject: FunctionComponent<Props> = props => {
  const log = useTailLog(props.project.buildLogUrl)

  return (
    <div className={styles.buildingProject}>
      <TerminalLog lines={log.lines} />
    </div>
  )
}

export default BuildingProject
