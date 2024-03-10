import React, { FunctionComponent } from 'react'
import styles from './Welcome.scss'

const Welcome: FunctionComponent = () => {
  return (
    <div className={styles.welcome}>
      <span className={styles.welcomeHeader}>Welcome to Reproducible Research Platform!</span>
      <span>To get started, create your first project by clicking the plus icon next to "Your projects"</span>
    </div>
  )
}

export default Welcome
