import React, { FunctionComponent } from 'react'
import styles from './Button.scss'

type Props = {
  disabled?: boolean
  action: () => void
  message: string
}

const Button: FunctionComponent<Props> = props => {
  return (
    <div className={styles.main}>
      <div className={styles.message}>{props.message}</div>
      <div className={props.disabled ? styles.buttonDisabled : styles.button} onClick={props.action}>
        {props.children}
      </div>
    </div>
  )
}

export default Button
