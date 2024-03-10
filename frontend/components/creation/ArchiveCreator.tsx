import React, { FunctionComponent, useEffect, useState } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { buildFromArchiveClicked, updateUiData } from '../../actions/actions'
import { BackendProject } from '../../store/backend'
import { getUiDataOf } from '../../store/selectors'
import { ArchiveCreatorData } from '../../store/ui'
import GuideBox from '../GuideBox'
import Input from '../Input'
import styles from './ArchiveCreator.scss'

type Props = {
  project: BackendProject
}

const ArchiveCreator: FunctionComponent<Props> = props => {
  const { project } = props
  const uiData = useSelector(getUiDataOf(project)) as ArchiveCreatorData
  const dispatch = useDispatch()

  const update: (data: ArchiveCreatorData) => void = data => dispatch(updateUiData({ projectId: project.id, data: data }))
  const updateInput: (f: (value: string) => ArchiveCreatorData) => (event: React.ChangeEvent<HTMLInputElement>) => void = f => e => update(f(e.target.value))

  const [createInProgress, setCreateInProgress] = useState(false)
  useEffect(() => setCreateInProgress(false), [project, uiData])

  const createAction = buildFromArchiveClicked({ projectId: project.id, archiveId: uiData.archiveId })

  function executeCreate() {
    dispatch(createAction)
    setCreateInProgress(true)
  }

  return (
    <div className={styles.main}>
      <span className={styles.heading}> Create a project from openBIS dataset </span>
      <GuideBox>
        <span>Here you can create a personal copy of a project that has been archived to openBIS.</span>
        <span>Please enter the identifier of the openBIS dataset below. This will start the process of retrieving the required data from openBIS.</span>
        <span>After a successful retrieval you'll be ready to use the new project.</span>
      </GuideBox>

      <Input
        label='openBIS dataset id'
        vertical
        placeHolder='e.g. 20220119091234650-6677'
        value={uiData.archiveId}
        onChange={updateInput(uiData.setArchiveId)}
      />

      <div className={styles.createWrapper}>
        <div className={styles.error}> {uiData.createError} </div>
        <div className={createInProgress ? styles.createButtonDisabled : styles.createButton} onClick={executeCreate}>
          Create
        </div>
      </div>
    </div>
  )
}

export default ArchiveCreator
