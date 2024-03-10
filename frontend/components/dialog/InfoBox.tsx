import React, { FunctionComponent } from 'react'
import { connect, ConnectedProps } from 'react-redux'

import { closeDialogClicked } from '../../actions/actions'
import { SystemState } from '../../store/types'

import styles from './InfoBox.scss'
import Table, { Column, Row } from '../Table'

const mapState = (state: SystemState) => ({
  config: state.config
})

const mapDispatch = { closeDialogClicked }

const connector = connect(mapState, mapDispatch)

const InfoBox: FunctionComponent<ConnectedProps<typeof connector>> = props => {
  const columns: Column[] = [new Column('name', true), new Column('value', true)]

  const values: [string, string][] = [
    ['Platform version', props.config.version],
    ['Platform build', props.config.build],
    ['openBIS backend', props.config.openbis],
    ['repo2docker version', props.config.repo2docker]
  ]

  return (
    <>
      <Table withBorders withStripes columns={columns}>
        {values.map(([name, value], index) => (
          <Row key={index}>
            <div className={styles.name}>{name}</div>
            {name == 'openBIS backend' ? (
              <a className={styles.value} href={`https://${value}`} target='_blank'>
                {value}
              </a>
            ) : (
              <div className={styles.value}>{value}</div>
            )}
          </Row>
        ))}
      </Table>

      <div className={styles.createButton} onClick={() => props.closeDialogClicked()}>
        Dismiss
      </div>
    </>
  )
}

export default connector(InfoBox)
