import classNames from 'classnames'
import React, { FunctionComponent, useEffect, useState } from 'react'
import { useDispatch, useSelector } from 'react-redux'

import { deleteClicked, projectClicked, startServer, stopServer } from '../actions/actions'

import { BackendProject, GitSource, ProjectStatus } from '../store/backend'
import { getNameOf, getRepositoryOf } from '../store/selectors'
import DeleteIcon from '../svg/delete.svg'
import JoinIcon from '../svg/join.svg'
import PlayIcon from '../svg/play.svg'
import StopIcon from '../svg/stop.svg'
import styles from './Project.scss'

type Props = {
  project: BackendProject
  selected: boolean
}

const Project: FunctionComponent<Props> = props => {
  const p = props.project

  const dispatch = useDispatch()

  const [isDisabled, setDisabled] = useState(false)
  function start() {
    dispatch(startServer(p.id))
    setDisabled(true)
  }
  function stop() {
    dispatch(stopServer(p.id))
    setDisabled(true)
  }

  useEffect(() => setDisabled(false), [p])

  const isReady = p.status === ProjectStatus.Ready
  const isRunning = p.serverUrl.startsWith('http')
  const isStopped = p.serverUrl === 'not running'
  const isProcessing = isReady && !isRunning && !isStopped
  const isBuilding = p.status === ProjectStatus.Building
  const serverUrl = isRunning ? p.serverUrl : ''

  const playClasses = classNames(styles.iconWrapper, {
    [styles.invisible]: isRunning,
    [styles.disabled]: !isReady || !isStopped || isDisabled
  })

  const stopClasses = classNames(styles.iconWrapper, {
    [styles.invisible]: !isRunning,
    [styles.disabled]: !isReady || isStopped || isDisabled
  })

  const joinClasses = classNames(styles.iconWrapper, {
    [styles.disabled]: !isRunning || isDisabled
  })

  const deleteClasses = classNames(styles.iconWrapper, {
    [styles.disabled]: isBuilding || isRunning || isProcessing || isDisabled
  })

  const projectName = useSelector(getNameOf(p))
  const repository = useSelector(getRepositoryOf(p))

  let statusStyle: string
  let statusText: string

  if (
    [
      ProjectStatus.Initializing,
      ProjectStatus.InitializingFromShare,
      ProjectStatus.InitializingFromArchive,
      ProjectStatus.Cloning,
      ProjectStatus.CloneFailed,
      ProjectStatus.Cloned
    ].includes(p.status)
  ) {
    statusText = 'defining'
    statusStyle = styles.black
  } else if (p.status === ProjectStatus.BuildFailed || p.status === ProjectStatus.BuildFromArchiveFailed) {
    statusText = 'not built'
    statusStyle = styles.red
  } else if (p.status === ProjectStatus.Building || p.status === ProjectStatus.BuildingFromArchive) {
    statusText = 'building'
    statusStyle = styles.yellow
  } else if (p.status === ProjectStatus.Ready) {
    if (isProcessing || isDisabled) {
      statusText = 'processing'
      statusStyle = styles.yellow
    } else if (isStopped) {
      statusText = 'not running'
      statusStyle = styles.black
    } else if (isRunning) {
      statusText = 'running'
      statusStyle = styles.green
    }
  }

  const active = props.selected
  const classes = active ? styles.selectedProject : styles.project
  const statusClasses = styles.status + ' ' + statusStyle

  return (
    <div className={classes} onClick={() => dispatch(projectClicked(p.id))}>
      <div className={styles.name}>{projectName}</div>
      <div className={styles.repository}>
        <a href={repository} target='_blank'>
          {repository}
        </a>
      </div>

      <div className={styles.controls}>
        <div className={styles.statusLabel}>Status:</div>
        <div className={statusClasses}>{statusText}</div>
        <div className={playClasses}>
          <PlayIcon onClick={start} className={styles.icon}></PlayIcon>
        </div>
        <div className={stopClasses}>
          <StopIcon onClick={stop} className={styles.icon}></StopIcon>
        </div>
        <div className={joinClasses}>
          <JoinIcon onClick={() => window.open(serverUrl, '_blank').focus()} className={styles.icon}></JoinIcon>
        </div>
        <div className={deleteClasses}>
          <DeleteIcon onClick={() => dispatch(deleteClicked(p))} className={styles.icon}></DeleteIcon>
        </div>
      </div>
    </div>
  )
}

export default Project
