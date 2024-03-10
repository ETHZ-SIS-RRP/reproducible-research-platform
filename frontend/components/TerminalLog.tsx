import React, { FunctionComponent, useRef, useEffect } from 'react'
import styles from './TerminalLog.scss'

type Props = {
  lines: string[]
}

const TerminalLog: FunctionComponent<Props> = props => {
  const terminal = useRef(null)
  useEffect(() => {
    if (terminal != null && terminal.current.lastChild != null) {
      terminal.current.lastChild.scrollIntoView()
    }
  }, [props.lines.length != 0])

  // Firefox patch - https://web.dev/snap-after-layout/
  useEffect(() => {
    if (terminal != null && terminal.current != null) {
      terminal.current.scrollBy(0, 0)
    }
  }, [props.lines])

  return (
    <div ref={terminal} className={styles.terminal}>
      {props.lines.map((line, index) => (
        <div key={index} className={line == '' ? styles.emptyLine : styles.line}>
          {line}
        </div>
      ))}
    </div>
  )
}

export default React.memo(TerminalLog)
