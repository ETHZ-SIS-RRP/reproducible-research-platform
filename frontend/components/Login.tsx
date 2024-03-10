import React, { FunctionComponent, KeyboardEvent, useEffect, useState } from 'react'
import { connect, ConnectedProps } from 'react-redux'

import { configRetrieved, loginClicked } from '../actions/actions'
import { LoginInProgress, NotLoggedIn, SystemState } from '../store/types'
import { usePrevious } from '../common/utils'

import Input from './Input'
import styles from './Login.scss'
import openBISLogo from '../images/openBIS-logo.png'
import classNames from 'classnames'

const mapState = (state: SystemState) => ({
  login: state.login,
  config: state.config
})

const mapDispatch = { loginClicked }
const connector = connect(mapState, mapDispatch)

const Login: FunctionComponent<ConnectedProps<typeof connector>> = props => {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [pat, setPat] = useState('')
  const [loginError, setLoginError] = useState(false)

  const disabled = props.login instanceof LoginInProgress
  const previousLogin = usePrevious(props.login)

  useEffect(() => {
    if (props.login instanceof NotLoggedIn && previousLogin instanceof LoginInProgress) {
      setLoginError(true)
    }
  }, [props.login])

  const handleKeyDown = (event: KeyboardEvent<HTMLInputElement>) => {
    if (event.key === 'Enter') {
      props.loginClicked({ username, password, pat })
    } else if (loginError) {
      setLoginError(false)
    }
  }

  const msg = loginError ? 'login failed' : ''

  return (
    <div className={styles.loginWrapper}>
      <div className={styles.login}>
        <div className={styles.rrp}>Reproducible Research Platform</div>
        <div className={styles.signIn}>Sign in</div>
        <div className={styles.logos}>
          <div className={styles.poweredBy}>Powered by</div>
          <a href='https://openbis.ch/' target='_blank'>
            <img className={styles.openbisLogo} src={openBISLogo} />
          </a>
        </div>

        <form>
          <Input
            styles={styles}
            label='Username'
            value={username}
            type='username'
            placeHolder='Username'
            vertical
            onKeyDown={handleKeyDown}
            onChange={e => setUsername(e.target.value)}></Input>
          <Input
            styles={styles}
            label='Password'
            value={password}
            type='password'
            placeHolder='Password'
            vertical
            onKeyDown={handleKeyDown}
            onChange={e => setPassword(e.target.value)}></Input>
        </form>

        <div className={classNames(styles.pat, { [styles.invisible]: !props.config.pat })}>
          <div className={styles.orText}>or</div>
          <form>
            <Input
              styles={styles}
              label='Personal access token'
              value={pat}
              type='password'
              placeHolder='Personal access token'
              vertical
              onKeyDown={handleKeyDown}
              onChange={e => setPat(e.target.value)}></Input>
          </form>
        </div>

        <div className={styles.error}>{msg}</div>

        <div className={disabled ? styles.signInButtonDisabled : styles.signInButton} onClick={() => props.loginClicked({ username, password, pat })}>
          Login
        </div>
      </div>
    </div>
  )
}

export default connector(Login)
