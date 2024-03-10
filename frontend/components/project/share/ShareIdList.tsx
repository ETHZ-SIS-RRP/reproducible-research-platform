import classNames from 'classnames'
import React, { FunctionComponent, useContext } from 'react'
import { useDispatch } from 'react-redux'
import { apiRequest } from '../../../actions/actions'
import DeleteIcon from '../../../svg/delete.svg'
import Table, { Column, Row } from '../../Table'
import SectionHeading from '../../SectionHeading'
import { ProjectContext } from './ProjectContextProvider'
import { ShareIdContext } from './ShareIdContextProvider'
import styles from './ShareIdList.scss'

const ShareIdList: FunctionComponent<{}> = () => {
  const dispatch = useDispatch()

  const { project } = useContext(ProjectContext)
  const { shares, updateShares } = useContext(ShareIdContext)

  const data = shares == null ? [] : shares.sort((a, b) => a.createdAt.localeCompare(b.createdAt) * -1)

  const columns: Column[] = [
    new Column('Share identifier', false),
    new Column('Created at', false),
    new Column('Description', true),
    new Column('Commit', true),
    new Column('Delete', false)
  ]

  function deleteShareId(shareId: string) {
    dispatch(
      apiRequest({
        url: `/projects/${project.id}/shares/${shareId}`,
        method: 'DELETE',
        onSuccess: response => {
          updateShares()
        }
      })
    )
  }

  const mainStyles = classNames(styles.main, { [styles.inactive]: data.length === 0 })

  return (
    <div className={mainStyles}>
      <SectionHeading>Share identifiers</SectionHeading>
      <div className={styles.empty} />

      <Table withHeaders withHighlight withStripes columns={columns}>
        {data.map((shareId, index) => (
          <Row key={index}>
            <div className={styles.shareIdValue}>{shareId.id}</div>
            <div className={styles.timestamp} style={{ whiteSpace: 'pre' }}>
              {shareId.createdAt}
            </div>
            <div className={styles.tableValue}>{shareId.description}</div>
            <div className={shareId.error ? styles.tableValueError : styles.tableValue}>
              {shareId.commitMessage + (shareId.error ? '' : ' (' + shareId.commitHash.substring(0, 7) + ')')}
            </div>
            <DeleteIcon className={styles.icon} onClick={() => deleteShareId(shareId.id)} />
          </Row>
        ))}
      </Table>
    </div>
  )
}

export default ShareIdList
