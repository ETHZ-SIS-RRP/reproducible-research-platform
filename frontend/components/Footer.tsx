import React, { FunctionComponent } from 'react'
import { connect, ConnectedProps } from 'react-redux'

import { openInfoBoxClicked } from '../actions/actions'
import { SystemState } from '../store/types'

import EthLogo from '../svg/ethz_logo_black.svg'
import styles from './Footer.scss'

const mapState = (state: SystemState) => ({
  config: state.config
})

const mapDispatch = { openInfoBoxClicked }

const connector = connect(mapState, mapDispatch)

const Footer: FunctionComponent<ConnectedProps<typeof connector>> = props => {
  let version = `v${props.config.version}`

  return (
    <div className={styles.footer}>
      <a className={styles.footerSis} href='https://sis.id.ethz.ch/' target='_blank'>
        Scientific IT Services - Empowering your research
      </a>

      <a className={styles.footerRrp} onClick={() => props.openInfoBoxClicked()}>
        Reproducible Research Platform {version}
      </a>

      <a className={styles.footerEth} href='https://www.ethz.ch/' target='_blank'>
        <EthLogo />
      </a>
    </div>
  )
}

export default connector(Footer)
