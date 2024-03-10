import classNames from 'classnames'
import React, { FunctionComponent, useState, useRef } from 'react'

import { useOutsideAlerter } from '../common/utils'

import DownIcon from '../svg/arrow-down.svg'
import UpIcon from '../svg/arrow-up.svg'

type ComponentProps<T> = {
  styles: { [className: string]: string }
  label: string
  value: T
  onSelect: (t: T) => void
  items: T[]
  toName: (t: T) => string
}

const Dropdown: FunctionComponent<ComponentProps<any>> = props => {
  const [open, setOpen] = useState(false)

  const wrapperRef = useRef(null)
  useOutsideAlerter(wrapperRef, () => setOpen(false))

  const itemsClasses = classNames(props.styles.dropdownItems, { [props.styles.invisible]: !open })

  return (
    <div ref={wrapperRef} className={props.styles.dropdown}>
      <div className={props.styles.label}>{props.label}</div>
      <div className={props.styles.valueWrapper} onClick={() => setOpen(!open)}>
        <div className={props.styles.currentValue}>{props.toName(props.value)}</div>
        {open ? <UpIcon className={props.styles.arrow} /> : <DownIcon className={props.styles.arrow} />}
      </div>
      <div className={props.styles.relative}>
        <div className={itemsClasses}>
          {props.items.map((item, index) => (
            <div
              key={index}
              className={props.styles.dropdownItem}
              onClick={() => {
                setOpen(false)
                props.onSelect(item)
              }}>
              {props.toName(item)}
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

export default Dropdown
