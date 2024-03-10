import React, { FunctionComponent } from 'react'
import styles from './Tabs.scss'
import classNames from 'classnames'
import { useDispatch } from 'react-redux'
import { tabClicked } from '../../actions/actions'
import { Tab } from '../../store/ui'

type ComponentProps = {
  selected: string
}

const Tabs: FunctionComponent<ComponentProps> = props => {
  const dispatch = useDispatch()

  const classesOf: (item: Tab) => string = item => {
    const myIndex = Object.values(Tab).findIndex(value => value === item)
    const selectedIndex = Object.values(Tab).findIndex(value => value === props.selected)

    return classNames(styles.tab, {
      [styles.selected]: myIndex === selectedIndex,
      [styles.previous]: myIndex == selectedIndex - 1,
      [styles.next]: myIndex === selectedIndex + 1,
      [styles.first]: myIndex === 0,
      [styles.last]: myIndex === Object.values(Tab).length - 1
    })
  }

  return (
    <div className={styles.tabs}>
      {Object.values(Tab).map(tab => (
        <div key={tab} className={classesOf(tab)} onClick={() => dispatch(tabClicked(tab))}>
          {tab.charAt(0).toUpperCase() + tab.slice(1)}
        </div>
      ))}
    </div>
  )
}

export default Tabs
