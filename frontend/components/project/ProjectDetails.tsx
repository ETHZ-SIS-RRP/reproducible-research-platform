import React, { FunctionComponent } from 'react'
import { useSelector } from 'react-redux'

import { BackendProject } from '../../store/backend'
import { getUiDataOf } from '../../store/selectors'
import { ActiveProjectData, Tab } from '../../store/ui'
import DataSetCreator from './DataSetCreator'
import FileSystem from './FileSystem'
import LogReader from './log/LogReader'
import Mount from './Mount'
import styles from './ProjectDetails.scss'
import ProjectInformation from './ProjectInformation'
import Shares from './share/Shares'
import Tabs from './Tabs'

type Props = {
  project: BackendProject
}

function selectContent(p: BackendProject, tab: Tab): JSX.Element {
  switch (tab) {
    case Tab.Details:
      return <ProjectInformation project={p} />
    case Tab.Archive:
      return <DataSetCreator project={p} />
    case Tab.Results:
      return <FileSystem url={p.resultsUrl} />
    case Tab.Share:
      return <Shares project={p} />
    case Tab.Mount:
      return <Mount project={p} />
    case Tab.Logs:
      return <LogReader project={p} />
  }
}

const ProjectDetails: FunctionComponent<Props> = props => {
  const p = props.project
  const uiData = useSelector(getUiDataOf(p)) as ActiveProjectData
  const tab = uiData.tab

  return (
    <div className={styles.projectDetails}>
      <Tabs selected={tab} />
      {selectContent(p, tab)}
    </div>
  )
}

export default ProjectDetails
