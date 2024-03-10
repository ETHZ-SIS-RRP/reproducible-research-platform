import React, { FunctionComponent, useEffect, useState } from 'react'
import { useSelector } from 'react-redux'
import { BackendProject } from '../../../store/backend'
import { getUiDataOf } from '../../../store/selectors'
import { ActiveProjectData } from '../../../store/ui'
import LogList from './LogList'
import styles from './LogReader.scss'
import { LogEntry, ProcessStatus } from './log'
import Logs from './Logs'

type ComponentProps = {
  project: BackendProject
}

const LogReader: FunctionComponent<ComponentProps> = props => {
  const p = props.project
  const uiData = useSelector(getUiDataOf(p)) as ActiveProjectData

  const [logs, setLogs] = useState(null as LogEntry[])

  const getContent = () =>
    fetch(p.logsUrl)
      .then(response => response.json())
      .then(response =>
        response.map((log: any) => {
          const type = log['type']
          const typeLabel =
            type == 'Server'
              ? 'Server execution'
              : type == 'Clone'
              ? 'Clone external repository'
              : type == 'Build'
              ? 'Build image from clone'
              : type == 'ArchiveProject'
              ? 'Archive project'
              : type == 'ArchiveResults'
              ? 'Archive results'
              : type == 'UnarchiveProject'
              ? 'Create project from archive'
              : type == 'CreateOfflineBundle'
              ? 'Create player bundle'
              : type == 'ImageExport'
              ? 'Export image'
              : 'Unknown'

          return new LogEntry(
            log['name'],
            log['url'],
            typeLabel,
            log['timestamp'],
            'successful' in log ? (log['successful'] ? ProcessStatus.Successful : ProcessStatus.Unsuccessful) : ProcessStatus.InProgress,
            log['message']
          )
        })
      )

  useEffect(() => {
    setLogs(null)
    getContent().then(setLogs)
    const intervalId = setInterval(() => getContent().then(setLogs), 5000)
    return () => clearInterval(intervalId)
  }, [p.id])

  if (logs == null) {
    return null
  }
  if (logs.length == 0) {
    return <div>NO LOGS</div>
  }

  const selected = logs.find(entry => entry.name == uiData.logInfo.selectedLog)
  const url = selected ? selected.url : null
  return (
    <div className={styles.main}>
      <LogList selected={selected} logs={logs} />
      <Logs url={url} />
    </div>
  )
}

export default LogReader
