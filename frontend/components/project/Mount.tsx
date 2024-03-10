import classNames from 'classnames'
import React, { FunctionComponent, useEffect } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { apiRequest, updateUiData } from '../../actions/actions'
import { BackendProject } from '../../store/backend'
import { getUiDataOf } from '../../store/selectors'
import { Message } from '../../store/types'
import { ActiveProjectData, MountInfo } from '../../store/ui'
import styles from './Mount.scss'

type ComponentProps = {
  project: BackendProject
}

const Mount: FunctionComponent<ComponentProps> = props => {
  const p = props.project
  const uiData = useSelector(getUiDataOf(p)) as ActiveProjectData
  const mountInfo = uiData.mountInfo

  const dispatch = useDispatch()

  function updateData(mountInfo: MountInfo): void {
    dispatch(updateUiData({ projectId: p.id, data: uiData.setMountInfo(mountInfo) }))
  }

  function getCurrentData() {
    fetch('/projects/' + p.id + '/mounts')
      .then(response => response.json())
      .then(response => updateData(new MountInfo(response, false, new Message(false, ''))))
  }

  function updateMounts() {
    dispatch(
      apiRequest({
        url: `/projects/${p.id}/mounts`,
        method: 'POST',
        body: JSON.stringify(mountInfo.data),
        onSuccess: response => {
          if (response.status === 200) {
            updateData(mountInfo.setDirty(false).setMessage(new Message(false, 'Dataset mounts updated')))
          } else if (response.status === 400) {
            response.json().then(json => updateData(mountInfo.setMessage(new Message(true, json['error']))))
          } else {
            updateData(mountInfo.setMessage(new Message(true, 'Internal error')))
          }
        }
      })
    )
  }

  useEffect(() => {
    if (!mountInfo.dirty) {
      getCurrentData()
    }
  }, [props.project.id])

  const message = mountInfo.message.value

  const messageStyle = classNames(styles.message, { [styles.red]: mountInfo.message.isError })
  const updateButtonStyle = styles.updateMountsButton
  const cancelButtonStyle = mountInfo.dirty ? styles.cancelButton : styles.cancelButtonDisabled

  return (
    <div className={styles.main}>
      <textarea className={styles.textarea} value={mountInfo.data} onChange={e => updateData(new MountInfo(e.target.value, true, new Message(false, '')))} />
      <div className={styles.buttonWrapper}>
        <div className={messageStyle}>{message}</div>
        <div className={updateButtonStyle} onClick={updateMounts}>
          Update mounts
        </div>
        <div className={cancelButtonStyle} onClick={getCurrentData}>
          Revert
        </div>
      </div>
    </div>
  )
}

export default Mount
