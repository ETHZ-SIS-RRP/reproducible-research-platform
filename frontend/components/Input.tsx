import classNames from 'classnames'
import React, { FunctionComponent, KeyboardEvent } from 'react'

import defaultStyles from './Input.scss'

type ComponentProps = {
  label: string
  value: string
  placeHolder?: string
  onClick?: () => void
  type?: string
  onKeyDown?: (e: KeyboardEvent<HTMLInputElement>) => void
  onChange?: (e: React.ChangeEvent<HTMLInputElement>) => void
  onBlur?: (e: React.FocusEvent) => void
  disabled?: boolean
  vertical?: boolean
  hidden?: boolean
  labelWidth?: number
  styles?: { [className: string]: string }
  warning?: boolean
  updating?: boolean
}

const Input: FunctionComponent<ComponentProps> = props => {
  const type = props.type == null ? 'text' : props.type
  let autoComplete = ''
  switch (type) {
    case 'username':
      autoComplete = 'username'
      break
    case 'password':
      autoComplete = 'current-password'
      break
  }

  const styles = props.styles ? props.styles : defaultStyles
  const wrapperStyle = props.vertical ? styles.wrapperVertical : styles.wrapperHorizontal
  const label = props.labelWidth ? { width: props.labelWidth + 'px' } : {}
  const hideWrapper = props.hidden ? { display: 'none' } : {}

  const labelClasses = classNames(styles.label, { [styles.updating]: props.updating })
  const inputClasses = classNames(styles.input, { [styles.warn]: props.warning, [styles.updating]: props.updating })

  return (
    <div style={hideWrapper} className={wrapperStyle}>
      <div style={label} className={labelClasses}>
        {props.label}
      </div>
      <input
        className={inputClasses}
        placeholder={props.placeHolder}
        type={type}
        value={props.value}
        autoComplete={autoComplete}
        onClick={
          props.onClick == null
            ? () => {
                return
              }
            : props.onClick
        }
        onKeyDown={
          props.onKeyDown == null
            ? () => {
                return
              }
            : props.onKeyDown
        }
        onBlur={
          props.onBlur == null
            ? () => {
                return
              }
            : props.onBlur
        }
        onChange={props.onChange}
        disabled={props.disabled || props.updating}></input>
    </div>
  )
}

export default Input
