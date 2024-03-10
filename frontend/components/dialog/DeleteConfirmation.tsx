import React, { FunctionComponent } from 'react'
import { connect, ConnectedProps, useSelector } from 'react-redux'

import { deleteProject, closeDialogClicked } from '../../actions/actions'
import { getNameOf } from '../../store/selectors'
import { SystemState } from '../../store/types'

import styles from './DeleteConfirmation.scss'

const mapState = (state: SystemState) => ({
  project: state.deletionCandidate
})

const mapDispatch = { deleteProject, closeDialogClicked }

const connector = connect(mapState, mapDispatch)

const DeletionConfirmation: FunctionComponent<ConnectedProps<typeof connector>> = props => {
  const name = useSelector(getNameOf(props.project))

  return (
    <div className={styles.confirmationDialog}>
      <div className={styles.confirmationDialogText}>Do you really want to delete project '{name}'?</div>
      <div className={styles.confirmationDialogButtons}>
        <div
          className={styles.confirmButton}
          onClick={() => {
            props.deleteProject(props.project.id)
            props.closeDialogClicked()
          }}>
          Delete
        </div>
        <div className={styles.cancel} onClick={() => props.closeDialogClicked()}>
          Cancel
        </div>
      </div>
    </div>
  )
}

export default connector(DeletionConfirmation)
