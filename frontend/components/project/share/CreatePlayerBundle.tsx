import React, { FunctionComponent, KeyboardEvent, useContext, useEffect, useState } from 'react'
import { useDispatch } from 'react-redux'
import { apiRequest } from '../../../actions/actions'
import Input from '../../Input'
import Button from './Button'
import styles from './CreateShareId.scss'
import SectionHeading from '../../SectionHeading'
import { PlayerBundleContext } from './PlayerBundleContextProvider'
import { ProjectContext } from './ProjectContextProvider'

const CreatePlayerBundle: FunctionComponent<{}> = () => {
  const dispatch = useDispatch()

  const { project, playerBundleCreationAllowed } = useContext(ProjectContext)
  const { bundles, updateBundles } = useContext(PlayerBundleContext)

  const [message, setMessage] = useState('')
  const [description, setDescription] = useState('')
  const [requestInProgress, setRequestInProgress] = useState(false)

  useEffect(() => setDescription(''), [project.id])

  const creationInProgress = bundles ? bundles.findIndex(bundle => !bundle.finished) != -1 : false
  const creationDisabled = description === '' || creationInProgress || requestInProgress || !playerBundleCreationAllowed

  function createPlayerBundle(description: string) {
    setRequestInProgress(true)
    setDescription('')
    dispatch(
      apiRequest({
        url: `/projects/${project.id}/bundles`,
        method: 'POST',
        body: JSON.stringify(description),
        onSuccess: response => {
          updateBundles()
          setRequestInProgress(false)
          if (response.status !== 201) {
            response.json().then(json => setMessage((json as any)['error']))
          }
        }
      })
    )
  }

  const handleKeyDown = (event: KeyboardEvent<HTMLInputElement>) => {
    if (event.key === 'Enter' && !creationDisabled) {
      createPlayerBundle(description)
    }
  }

  const labelWidth = 200

  return (
    <div className={styles.main}>
      <SectionHeading>Create a new player bundle</SectionHeading>

      <Input
        label='Description'
        placeHolder='Description of the new player bundle'
        value={description}
        labelWidth={labelWidth}
        onChange={e => {
          setDescription(e.target.value)
          setMessage('')
        }}
        onKeyDown={handleKeyDown}
      />

      <Button disabled={creationDisabled} action={() => createPlayerBundle(description)} message={message}>
        Create player bundle
      </Button>
    </div>
  )
}

export default CreatePlayerBundle
