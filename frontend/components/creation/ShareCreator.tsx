import React, { FunctionComponent, useEffect } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { apiRequest, cloneClicked, updateUiData } from '../../actions/actions'
import { BackendProject } from '../../store/backend'

import { ShareIdCreatorData } from '../../store/ui'
import GuideBox from '../GuideBox'
import Input from '../Input'
import Clone from './Clone'
import { HandleKeyDown, UpdateData } from './types'

import { parseShare } from '../../common/utils'
import { getUiDataOf, getUsername } from '../../store/selectors'
import styles from './ShareCreator.scss'
import ShareIdentifier from './ShareIdentifier'

type Props = {
  project: BackendProject
}

const ShareCreator: FunctionComponent<Props> = props => {
  const { project } = props

  const uiData = useSelector(getUiDataOf(project)) as ShareIdCreatorData
  const username = useSelector(getUsername)
  const share = uiData.share

  const dispatch = useDispatch()

  const update: (data: ShareIdCreatorData) => void = data => dispatch(updateUiData({ projectId: project.id, data: data }))
  const updateInput: (f: (value: string) => ShareIdCreatorData) => (event: React.ChangeEvent<HTMLInputElement>) => void = f => e => update(f(e.target.value))

  function getShareDetails(url: string) {
    dispatch(
      apiRequest({
        url: url,
        onSuccess: response => {
          if (response.status === 200) {
            response.json().then(r => {
              const share = parseShare(r)
              update(uiData.setShare(share))
            })
          }
        }
      })
    )
  }

  useEffect(() => {
    const id = uiData.shareId
    try {
      const plainText = atob(id)
      if (plainText.split(' ').length === 4) {
        getShareDetails(`/shares/${id}`)
      } else {
        update(uiData.setShare(null))
      }
    } catch (notBase64) {
      update(uiData.setShare(null))
    }
  }, [uiData.shareId])

  const cloneAction = cloneClicked({
    projectId: project.id,
    repository: share?.repository ?? '',
    username: uiData.username,
    password: uiData.password,
    shareId: uiData.shareId
  })

  const cloneErrorAction = updateUiData({ projectId: project.id, data: uiData.setUsername('').setPassword('') })

  return (
    <div className={styles.main}>
      <span className={styles.heading}> Create a project from a share identifier </span>
      <GuideBox>
        <span>
          Here you can create a personal copy of a project another user has decided to share with you. To do this, you need a share identifier from the fellow
          user.
        </span>
        <span>
          The first step is to enter the share id. After entering the share id, you'll be asked for credentials to the Git repository that contains the code of
          the project.
        </span>
        <span>
          After a successful clone you can start using the new project. It uses the same container image than the original one, which makes the execution
          environment identical.
        </span>
      </GuideBox>

      <Input
        label='Share identifier'
        vertical
        placeHolder='e.g. ZDM1MDY5NTQtMmI4My00OTU0LTljNzAtZDgzZmEzYWFkYWMzIG5vYm9keSBhYmNkZWZnaCBhYmNkZWZnaA=='
        value={uiData.shareId}
        onChange={updateInput(uiData.setShareId)}
        disabled={share !== null}
      />

      {share == null ? null : (
        <div className={styles.part2}>
          <ShareIdentifier shareId={share} />

          <div className={styles.part2Heading}>
            Enter your credentials to Git repository at &nbsp;
            <a href={share.repository} target='_blank'>
              {share.repository}
            </a>
          </div>

          <Clone project={project} cloneAction={cloneAction} errorAction={cloneErrorAction}>
            {(handleKeyDown: HandleKeyDown, updateInput: UpdateData) => (
              <>
                <Input
                  label='Username'
                  placeHolder={`e.g. ${username}`}
                  vertical
                  value={uiData.username}
                  type='username'
                  onKeyDown={handleKeyDown}
                  onChange={updateInput(uiData.setUsername)}
                  disabled={false}
                />

                <Input
                  label='Password'
                  value={uiData.password}
                  type='password'
                  vertical
                  onKeyDown={handleKeyDown}
                  onChange={updateInput(uiData.setPassword)}
                  disabled={false}
                />
              </>
            )}
          </Clone>
        </div>
      )}
    </div>
  )
}

export default ShareCreator
