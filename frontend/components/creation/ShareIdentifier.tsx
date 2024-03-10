import React, { FunctionComponent } from 'react'

import { Share } from '../../store/types'
import Table, { Column, Row } from '../Table'

import styles from './ShareIdentifier.scss'

type ComponentProps = {
  shareId: Share
}

const columns: Column[] = [new Column('name', true), new Column('value', true)]

const ShareIdentifier: FunctionComponent<ComponentProps> = props => {
  const s = props.shareId
  return (
    <div className={styles.main}>
      <div className={styles.header}>Share identifier</div>
      <Table withBorders withStripes columns={columns}>
        <Row>
          <div className={styles.cell}>Author</div>
          <div className={styles.cell}>{s.author}</div>
        </Row>
        <Row>
          <div className={styles.cell}>Created at </div>
          <div className={styles.cell}>{s.createdAt}</div>
        </Row>
        <Row>
          <div className={styles.cell}>Project name</div>
          <div className={styles.cell}>{s.projectName}</div>
        </Row>
        <Row>
          <div className={styles.cell}>Description</div>
          <div className={styles.cell}>{s.description}</div>
        </Row>
        <Row>
          <div className={styles.cell}>Repository</div>
          <a className={styles.link} href={s.repository} target='_blank'>
            {s.repository}
          </a>
        </Row>
        <Row>
          <div className={styles.cell}>Commit message</div>
          <div className={styles.cell}>{s.commitMessage}</div>
        </Row>
        <Row>
          <div className={styles.cell}>Commit hash</div>
          <div className={styles.cell}>{s.commitHash.substring(0, 7)}</div>
        </Row>
        <Row>
          <div className={styles.cell}>Commit author</div>
          <div className={styles.cell}>{s.commitAuthorName + ' (' + s.commitAuthorEmail + ')'}</div>
        </Row>
        <Row>
          <div className={styles.cell}>Commit time</div>
          <div className={styles.cell}>{s.commitTimestamp}</div>
        </Row>
      </Table>
    </div>
  )
}

export default ShareIdentifier
