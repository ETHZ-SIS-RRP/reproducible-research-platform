import React, { FunctionComponent } from 'react'
import { LogEntry } from './log'
import styles from './LogListItem.scss'
import classNames from 'classnames'
import { useDispatch } from 'react-redux'
import { logClicked } from '../../../actions/actions'
import { BackendProject } from '../../../store/backend'

type Props = {
  entry: LogEntry
  isSelected: boolean
}

const LogListItem: FunctionComponent<Props> = props => {
  const { name, url, type, timestamp, status, message } = props.entry
  const isSelected = props.isSelected
  const mainClasses = classNames(styles.main, { [styles.selected]: isSelected })
  const statusStyle = styles.status

  const dispatch = useDispatch()

  return (
    <div className={mainClasses} onClick={() => dispatch(logClicked(name))}>
      <div className={styles.header}>{type}</div>
      <div className={styles.timestamp}>{timestamp}</div>
      <div className={statusStyle}>{status}</div>
    </div>
  )
}

export default LogListItem
