import React, { FunctionComponent } from 'react'
import styles from './SectionHeading.scss'

const SectionHeading: FunctionComponent<{}> = props => {
  return <div className={styles.main}>{props.children}</div>
}

export default SectionHeading
