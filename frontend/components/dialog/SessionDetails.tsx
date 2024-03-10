import React, { FunctionComponent } from 'react'
import { useDispatch, useSelector } from 'react-redux'

import { closeDialogClicked } from '../../actions/actions'
import { getSessionValidTo, getUsername } from '../../store/selectors'
import Table, { Column, Row } from '../Table'

import styles from './SessionDetails.scss'

const SessionDetails: FunctionComponent<{}> = () => {
  const dispatch = useDispatch()

  const username = useSelector(getUsername)
  const validTo = useSelector(getSessionValidTo)

  const columns: Column[] = [new Column('name', true), new Column('value', true)]

  const values: [string, string][] = [
    ['User name', username],
    ['Session type', 'Personal access token'],
    ['Access token valid until', validTo]
  ]

  return (
    <div className={styles.main}>
      <Table withBorders withStripes columns={columns}>
        {values.map(([name, value], index) => (
          <Row key={index}>
            <div className={styles.name}>{name}</div>
            <div className={styles.value}>{value}</div>
          </Row>
        ))}
      </Table>

      <div className={styles.dismissButton} onClick={() => dispatch(closeDialogClicked())}>
        Dismiss
      </div>
    </div>
  )
}

export default SessionDetails
