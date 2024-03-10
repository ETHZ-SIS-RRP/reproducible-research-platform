import React, { FunctionComponent, useEffect, useState } from 'react'
import { BackendLog, useTailLog } from '../../../common/taillog'
import TerminalLog from '../../TerminalLog'
import styles from './Logs.scss'

type ComponentProps = {
  url: string
}

const Logs: FunctionComponent<ComponentProps> = props => {
  const log = props.url ? useTailLog(props.url) : new BackendLog([], null, null)

  return (
    <div className={styles.logs}>
      <TerminalLog lines={log.lines} />
    </div>
  )
}

export default Logs
