import React, { FunctionComponent, KeyboardEvent, useState } from 'react'
import { useDispatch } from 'react-redux'
import { apiRequest } from '../../actions/actions'
import { BackendProject } from '../../store/backend'
import Input from '../Input'
import SectionHeading from '../SectionHeading'
import styles from './ProjectInformation.scss'

type ComponentProps = {
  project: BackendProject
}

const ProjectInformation: FunctionComponent<ComponentProps> = props => {
  const p = props.project
  const labelWidth = 200
  const dispatch = useDispatch()

  const [projectName, setProjectName] = useState(p.metadata['name'])
  const [memoryRequest, setMemoryRequest] = useState(p.metadata['memory-request'])
  const [cpuRequest, setCpuRequest] = useState(p.metadata['cpu-request'])
  const [updating, setUpdating] = useState(false)

  function handleKeyDown(
    attribute: string,
    initial: string,
    current: string,
    setter: (value: string) => void
  ): (event: KeyboardEvent<HTMLInputElement>) => void {
    return event => {
      if (event.key === 'Enter') {
        modifyProject(attribute, current, initial, setter)
      } else if (event.key === 'Escape') {
        setter(initial)
      }
    }
  }

  function modifyProject(key: string, value: string, initial: string, setter: (value: string) => void) {
    setUpdating(true)
    dispatch(
      apiRequest({
        url: `/projects/${p.id}`,
        method: 'POST',
        body: JSON.stringify({ key: key, value: value }),
        onSuccess: (response: Response) => {
          setUpdating(false)
          if (response.status !== 200) {
            setter(initial)
          }
        },
        onFailure: (_: any) => {
          setUpdating(false)
          setter(initial)
        }
      })
    )
  }

  return (
    <div className={styles.info}>
      <div className={styles.section}>
        <SectionHeading>Project</SectionHeading>
        <Input
          label='Name'
          onKeyDown={handleKeyDown('name', p.metadata['name'], projectName, setProjectName)}
          value={projectName}
          onChange={e => setProjectName(e.target.value)}
          onBlur={e => setProjectName(p.metadata['name'])}
          labelWidth={labelWidth}
          updating={updating && p.metadata['name'] !== projectName}
        />
        <Input label='Identifier' value={p.id} disabled={true} labelWidth={labelWidth} />
        <Input label='Created at' value={p.metadata['created']} disabled={true} labelWidth={labelWidth} />
        <Input label='Source' value={p.source.toString()} disabled={true} labelWidth={labelWidth} />
      </div>

      <div className={styles.section}>
        <SectionHeading>Repository</SectionHeading>
        <Input label='Remote' value={p.cloneStatus.remote} disabled={true} labelWidth={labelWidth} />
        <Input label='Commit message' value={p.cloneStatus.commit.message} disabled={true} labelWidth={labelWidth} />
        <Input label='Commit hash' value={p.cloneStatus.commit.hash.substring(0, 7)} disabled={true} labelWidth={labelWidth} />
        <Input label='Clean' value={p.cloneStatus.clean ? 'Yes' : 'No'} disabled={true} labelWidth={labelWidth} />
        <Input label='Local branch' value={p.cloneStatus.branch} disabled={true} labelWidth={labelWidth} />
        <Input label='Remote tracking' value={p.cloneStatus.remoteTracking.toString()} disabled={true} labelWidth={labelWidth} />
      </div>

      <div className={styles.section}>
        <SectionHeading>Resources</SectionHeading>
        <Input
          label='Memory in Gb'
          onKeyDown={handleKeyDown('memory-request', p.metadata['memory-request'], memoryRequest, setMemoryRequest)}
          value={memoryRequest}
          onChange={e => setMemoryRequest(e.target.value)}
          onBlur={e => setMemoryRequest(p.metadata['memory-request'])}
          labelWidth={labelWidth}
          updating={updating && p.metadata['memory-request'] !== memoryRequest}
        />
        <Input
          label='Number of CPUs'
          onKeyDown={handleKeyDown('cpu-request', p.metadata['cpu-request'], cpuRequest, setCpuRequest)}
          value={cpuRequest}
          onChange={e => setCpuRequest(e.target.value)}
          onBlur={e => setCpuRequest(p.metadata['cpu-request'])}
          labelWidth={labelWidth}
          updating={updating && p.metadata['cpu-request'] !== cpuRequest}
        />
      </div>
    </div>
  )
}

export default ProjectInformation
