import React, { FunctionComponent } from 'react'
import styles from './GuideBox.scss'

const GuideBox: FunctionComponent<{}> = props => {
  return <div className={styles.main}>{props.children}</div>
}

export default GuideBox
