import React, { FunctionComponent, KeyboardEvent, useEffect, useState } from 'react'
import { useDispatch, useSelector } from 'react-redux'

import { buildFromGitClicked, cloneClicked, updateUiData } from '../../actions/actions'
import { BackendProject, ProjectStatus } from '../../store/backend'
import { getRepositoryOf, getUiDataOf, getUsername } from '../../store/selectors'
import { GitCreatorData } from '../../store/ui'

import GuideBox from '../GuideBox'
import Input from '../Input'
import Clone from './Clone'
import styles from './GitCreator.scss'
import { HandleKeyDown, UpdateData } from './types'

type Props = {
  project: BackendProject
}

const GitCreator: FunctionComponent<Props> = props => {
  const { project } = props

  const uiData = useSelector(getUiDataOf(project)) as GitCreatorData
  const username = useSelector(getUsername)
  const repository = useSelector(getRepositoryOf(project))

  const dispatch = useDispatch()

  const update: (data: GitCreatorData) => void = data => dispatch(updateUiData({ projectId: project.id, data: data }))
  const updateInput: (f: (value: string) => GitCreatorData) => (event: React.ChangeEvent<HTMLInputElement>) => void = f => e =>
    update(f(e.target.value).setBuildError(''))

  const [buildInProgress, setBuildInProgress] = useState(false)
  useEffect(() => setBuildInProgress(false), [project, uiData])

  const isCloneComplete = project.status === ProjectStatus.Cloned
  useEffect(() => (isCloneComplete ? update(uiData.setProjectName(project.metadata['name'])) : null), [isCloneComplete])

  const cloneAction = cloneClicked({
    projectId: project.id,
    repository: uiData.repository,
    username: uiData.username,
    password: uiData.password
  })

  const cloneErrorAction = updateUiData({ projectId: project.id, data: uiData.setUsername('').setPassword('') })

  const buildAction = buildFromGitClicked({ id: project.id, name: uiData.projectName, branch: uiData.branch ?? project.cloneStatus?.branch ?? 'main' })

  function executeBuild() {
    dispatch(buildAction)
    setBuildInProgress(true)
  }

  const handleKeyDown = (event: KeyboardEvent<HTMLInputElement>) => (event.key === 'Enter' ? executeBuild() : null)

  return (
    <div className={styles.main}>
      <span className={styles.heading}> Create a project from a Git repository </span>

      <GuideBox>
        <span>Here you can create a new project from any Git repository that is accessible through the web.</span>
        <span>
          The first step is to clone the repository. The only mandatory parameter is the repository URL. If the repository requires authentication, you should
          also enter your username and password.
        </span>
        <span>After a successful clone you'll be able to select a branch, tag or commit on which the new project will be based.</span>
      </GuideBox>

      <Clone project={project} cloneAction={cloneAction} errorAction={cloneErrorAction}>
        {(handleKeyDown: HandleKeyDown, updateInput: UpdateData) => (
          <>
            <Input
              label='Repository URL'
              vertical
              placeHolder='e.g. https://git.bsse.ethz.ch/rrp-examples/conda-python'
              value={repository}
              onKeyDown={handleKeyDown}
              onChange={updateInput(uiData.setRepository)}
              disabled={isCloneComplete}
              hidden={false}
            />

            <Input
              label='Username'
              placeHolder={`e.g. ${username}`}
              vertical
              value={uiData.username}
              type='username'
              onKeyDown={handleKeyDown}
              onChange={updateInput(uiData.setUsername)}
              disabled={isCloneComplete}
            />

            <Input
              label='Password'
              value={uiData.password}
              type='password'
              vertical
              onKeyDown={handleKeyDown}
              onChange={updateInput(uiData.setPassword)}
              disabled={isCloneComplete}
            />
          </>
        )}
      </Clone>

      <div className={isCloneComplete ? styles.phaseTwo : styles.invisible}>
        <Input
          label='Project name'
          placeHolder={`e.g. ${project.metadata['name']}`}
          value={uiData.projectName ?? project.metadata['name'] ?? ''}
          vertical
          onKeyDown={handleKeyDown}
          onChange={updateInput(uiData.setProjectName)}
          disabled={buildInProgress}
        />

        <Input
          label='Branch / Tag / Commit'
          placeHolder='main'
          vertical
          value={uiData.branch ?? project.cloneStatus?.branch ?? 'main'}
          onKeyDown={handleKeyDown}
          onChange={updateInput(uiData.setBranch)}
          disabled={buildInProgress}
        />

        <div className={styles.buildWrapper}>
          <div className={styles.error}> {uiData.buildError} </div>
          <div className={buildInProgress ? styles.buildButtonDisabled : styles.buildButton} onClick={executeBuild}>
            Build
          </div>
        </div>
      </div>
    </div>
  )
}

export default GitCreator
