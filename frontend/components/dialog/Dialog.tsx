import React, { FunctionComponent } from 'react'
import { connect, ConnectedProps } from 'react-redux'

import { SystemState } from '../../store/types'

import DeleteConfirmation from './DeleteConfirmation'
import styles from './Dialog.scss'
import InfoBox from './InfoBox'
import SessionDetails from './SessionDetails'

const mapState = (state: SystemState) => ({
  dialogHeader: state.dialogHeader,
  dialogContent: state.dialogContent
})

const connector = connect(mapState)

const Dialog: FunctionComponent<ConnectedProps<typeof connector>> = props => {
  if (props.dialogContent === '') {
    return null
  } else {
    let content
    if (props.dialogContent === 'infobox') {
      content = <InfoBox />
    } else if (props.dialogContent == 'delete-confirmation') {
      content = <DeleteConfirmation />
    } else if (props.dialogContent == 'session-details') {
      content = <SessionDetails />
    } else {
      content = null
    }

    return (
      <div className={styles.dialog}>
        <div className={styles.dialogContainer}>
          <div className={styles.dialogContent}>
            <div className={styles.dialogHeader}>{props.dialogHeader}</div>
            {content}
          </div>
        </div>
      </div>
    )
  }
}

export default connector(Dialog)
