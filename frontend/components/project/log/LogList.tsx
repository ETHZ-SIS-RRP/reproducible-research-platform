import React, { FunctionComponent } from 'react'
import { LogEntry } from './log'
import styles from './LogList.scss'
import LogListItem from './LogListItem'

type Props = {
  selected: LogEntry
  logs: LogEntry[]
}

const LogList: FunctionComponent<Props> = props => {
  const selectedName = props.selected ? props.selected.name : ''
  const logs = props.logs
  logs.sort((a, b) => b.timestamp.localeCompare(a.timestamp))
  return (
    <div className={styles.main}>
      {logs.map(log => (
        <LogListItem key={log.url} isSelected={log.name === selectedName} entry={log} />
      ))}
    </div>
  )
}

export default LogList
