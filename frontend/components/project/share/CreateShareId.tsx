import React, { FunctionComponent, KeyboardEvent, useContext, useEffect, useState } from 'react'
import { useDispatch } from 'react-redux'
import { apiRequest } from '../../../actions/actions'
import Input from '../../Input'
import Button from './Button'
import styles from './CreateShareId.scss'
import SectionHeading from '../../SectionHeading'
import { ProjectContext } from './ProjectContextProvider'
import { ShareIdContext } from './ShareIdContextProvider'

const CreateShareId: FunctionComponent<{}> = () => {
  const dispatch = useDispatch()

  const { project, shareIdCreationAllowed } = useContext(ProjectContext)
  const { updateShares } = useContext(ShareIdContext)

  const [message, setMessage] = useState('')
  const [description, setDescription] = useState('')

  useEffect(() => setDescription(''), [project.id])

  const creationDisabled = description === '' || !shareIdCreationAllowed

  function createShareId(description: string) {
    dispatch(
      apiRequest({
        url: `/projects/${project.id}/shares`,
        method: 'POST',
        body: JSON.stringify(description),
        onSuccess: response => {
          if (response.status === 201) {
            updateShares()
            setDescription('')
          } else if (response.status === 400) {
            response.json().then(json => {
              setMessage(json['error'])
            })
          } else {
            setMessage('Internal error')
          }
        }
      })
    )
  }

  const handleKeyDown = (event: KeyboardEvent<HTMLInputElement>) => {
    if (event.key === 'Enter' && !creationDisabled) {
      createShareId(description)
    }
  }

  const labelWidth = 200

  return (
    <div className={styles.main}>
      <SectionHeading>Create a new share identifier</SectionHeading>

      <Input
        label='Description'
        placeHolder='Description of the new share identifier'
        value={description}
        labelWidth={labelWidth}
        onChange={e => {
          setDescription(e.target.value)
          setMessage('')
        }}
        onKeyDown={handleKeyDown}
      />

      <Button disabled={creationDisabled} action={() => createShareId(description)} message={message}>
        Create share identifier
      </Button>
    </div>
  )
}

export default CreateShareId
