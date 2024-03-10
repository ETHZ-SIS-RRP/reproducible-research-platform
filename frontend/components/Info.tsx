import React, { FunctionComponent } from 'react'

type ComponentProps = {
  label: string
  value: string
  onClick?: () => void
}

const Info: FunctionComponent<ComponentProps> = props => {
  return (
    <div className='info-field'>
      <div className='label'>{props.label}</div>
      <div
        className={props.onClick == null ? 'value' : 'value link'}
        onClick={
          props.onClick == null
            ? () => {
                return
              }
            : props.onClick
        }>
        {props.value}
      </div>
    </div>
  )
}

export default Info
