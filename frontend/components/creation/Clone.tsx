import React, { FunctionComponent, KeyboardEvent, ReactNode, useEffect, useState } from 'react'

import { useDispatch } from 'react-redux'
import { Action } from 'redux'
import { updateUiData } from '../../actions/actions'
import { useTailLog } from '../../common/taillog'
import { BackendProject, ProjectStatus } from '../../store/backend'
import { UserInterfaceData } from '../../store/ui'
import styles from './Clone.scss'
import { HandleKeyDown, UpdateData } from './types'

type Props = {
  project: BackendProject
  cloneAction: Action<any>
  errorAction: Action<any>
  children: ((handleKeyDown: HandleKeyDown,  updateData: UpdateData) => ReactNode)
}

const Clone: FunctionComponent<Props> = props => {

  const { project, cloneAction, errorAction } = props
  const status = project.status

  const dispatch = useDispatch()

  const [showError, setShowError] = useState(true)

  const isError = status === ProjectStatus.CloneFailed

  const initialCloneButtonEnabled = [ProjectStatus.Initializing, ProjectStatus.InitializingFromShare, ProjectStatus.CloneFailed].includes(status)
  const [cloneButtonEnabled, setCloneButtonEnabled] = useState(initialCloneButtonEnabled)

  useEffect(() => {
    setCloneButtonEnabled(initialCloneButtonEnabled)
    if (status == ProjectStatus.CloneFailed) {
      setShowError(true)
      dispatch(errorAction)
    }
  }, [project])

  const handleKeyDown = (event: KeyboardEvent<HTMLInputElement>) => {
    if (event.key === 'Enter') {
      executeClone()
    }
  }

  function executeClone() {
    dispatch(cloneAction)
    setCloneButtonEnabled(false)
  }

  function updateData(f: (value: string) => UserInterfaceData): (event: React.ChangeEvent<HTMLInputElement>) => void {
    return e => {
      dispatch(updateUiData({projectId: project.id, data: f(e.target.value)}))
      setShowError(false)
    }
  }

  const log = useTailLog(status == ProjectStatus.Cloning ? project.cloneLogUrl : null)

  let logLine = ""
  if (status === ProjectStatus.Cloning) {
    logLine = log.lines.length > 0 ? log.lines[log.lines.length - 1] : ''
  } else if (status === ProjectStatus.CloneFailed) { 
    logLine = showError ? project.statusMessage : ""
  } else if (![ProjectStatus.Initializing, ProjectStatus.InitializingFromShare].includes(status)) {
    logLine = "Clone complete"
  }

  return (
      <>
      <form>
        {props.children(handleKeyDown, updateData)}
      </form>
      <div className={styles.cloneWrapper}>
        <div className={isError ? styles.errorLog : styles.log}> {logLine} </div>
        <div className={!cloneButtonEnabled ? styles.cloneButtonDisabled : styles.cloneButton} onClick={executeClone}>
          Clone
        </div>
      </div>
      </>
  )
}

export default Clone
