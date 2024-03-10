import React, { FunctionComponent } from 'react'
import { connect, ConnectedProps, useDispatch, useSelector } from 'react-redux'

import { logoutClicked, openInfoBoxClicked, usernameClicked } from '../actions/actions'
import { LoggedIn, SystemState } from '../store/types'

import styles from './Header.scss'
import InfoIcon from '../svg/info.svg'
import LogoutIcon from '../svg/logout.svg'
import MenuIcon from '../svg/menu.svg'
import { getConfig, getUsername } from '../store/selectors'
import classNames from 'classnames'

const Header: FunctionComponent<{}> = () => {
  const dispatch = useDispatch()
  const config = useSelector(getConfig)
  const username = useSelector(getUsername)

  const patEnabled = config.pat

  const usernameStyle = classNames(styles.loginName, { [styles.loginNameClickable]: patEnabled })

  if (username === null || username === '') {
    return <div className='header'></div>
  } else {
    return (
      <div className={styles.header}>
        <MenuIcon className={styles.menuButton} />
        <InfoIcon className={styles.infoButton} onClick={() => dispatch(openInfoBoxClicked())} />

        <div className={usernameStyle} onClick={() => dispatch(usernameClicked())}>
          Logged in as '{username}'
        </div>

        <LogoutIcon className={styles.logoutButton} onClick={() => dispatch(logoutClicked())}></LogoutIcon>
      </div>
    )
  }
}

export default Header
