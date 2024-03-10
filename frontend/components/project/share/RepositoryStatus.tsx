import React, { FunctionComponent, useContext } from 'react'
import Input from '../../Input'
import SectionHeading from '../../SectionHeading'
import { ProjectContext } from './ProjectContextProvider'
import styles from './RepositoryStatus.scss'

const RepositoryStatus: FunctionComponent<{}> = () => {
  const { project, remoteTrackingOk } = useContext(ProjectContext)

  const commit = project.cloneStatus.commit.message + ' (' + project.cloneStatus.commit.hash.substring(0, 7) + ')'
  const labelWidth = 200

  const cloneStatus = project.cloneStatus.clean ? 'Yes' : 'No'

  return (
    <div className={styles.main}>
      <SectionHeading>Repository status</SectionHeading>

      <Input label='Commit' value={commit} disabled labelWidth={labelWidth} />
      <Input label='Repository clean' value={cloneStatus} warning={cloneStatus !== 'Yes'} disabled labelWidth={labelWidth} />
      <Input label='Remote tracking' value={project.cloneStatus.remoteTracking.toString()} warning={!remoteTrackingOk} disabled labelWidth={labelWidth} />
    </div>
  )
}

export default RepositoryStatus
