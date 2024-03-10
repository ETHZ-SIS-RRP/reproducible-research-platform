import classNames from 'classnames'
import React, { FunctionComponent, useContext, useEffect } from 'react'
import { useDispatch } from 'react-redux'
import { apiRequest } from '../../../actions/actions'
import DeleteIcon from '../../../svg/delete.svg'
import DownloadIcon from '../../../svg/download.svg'
import Table, { Column, Row } from '../../Table'
import SectionHeading from '../../SectionHeading'
import { Bundle, PlayerBundleContext } from './PlayerBundleContextProvider'
import styles from './PlayerBundleList.scss'

const PlayerBundleList: FunctionComponent<{}> = () => {
  const dispatch = useDispatch()

  const { bundles, updateBundles } = useContext(PlayerBundleContext)

  const data = bundles == null ? [] : bundles.sort((a, b) => a.createdAt.localeCompare(b.createdAt) * -1)

  const columns: Column[] = [
    new Column('Description', true),
    new Column('Commit', true),
    new Column('Created at', false),
    new Column('Size', false),
    new Column('Status', false),
    new Column('Download', false),
    new Column('Delete', false)
  ]

  const inProgress = data.findIndex(bundle => !bundle.finished) != -1

  useEffect(() => {
    if (inProgress) {
      const id = setTimeout(updateBundles, 1000)
      return () => clearTimeout(id)
    }
  })

  function deleteBundle(url: string) {
    dispatch(
      apiRequest({
        url: url,
        method: 'DELETE',
        onSuccess: response => {
          updateBundles()
        }
      })
    )
  }

  const mainStyles = classNames(styles.main, { [styles.inactive]: data.length === 0 })

  function humanFileSize(size: number): string {
    var i = size == 0 ? 0 : Math.floor(Math.log(size) / Math.log(1024))
    return (size / Math.pow(1024, i)).toFixed(2) + ' ' + ['B', 'kB', 'MB', 'GB', 'TB'][i]
  }

  function status(bundle: Bundle): string {
    if (!bundle.finished) {
      return 'In progress'
    } else if (bundle.success) {
      return 'Complete'
    } else {
      return 'Failed'
    }
  }

  return (
    <div className={mainStyles}>
      <SectionHeading>Player bundles</SectionHeading>
      <div className={styles.empty} />

      <Table withHeaders withHighlight withStripes columns={columns}>
        {data.map((bundle, index) => (
          <Row key={index}>
            <div className={styles.tableValue}>{bundle.description}</div>
            <div className={styles.tableValue}>{bundle.commit}</div>
            <div className={styles.timestamp}>{bundle.createdAt}</div>
            <div className={styles.tableValue}>{humanFileSize(bundle.fileSize)}</div>
            <div className={styles.tableValue}>{status(bundle)}</div>
            {bundle.success ? (
              <a href={bundle.link} download={bundle.link.split('/').slice(-1)}>
                <DownloadIcon className={styles.icon} />
              </a>
            ) : (
              <div />
            )}
            {bundle.finished ? <DeleteIcon className={styles.icon} onClick={() => deleteBundle(bundle.link)} /> : <div />}
          </Row>
        ))}
      </Table>
    </div>
  )
}

export default PlayerBundleList
