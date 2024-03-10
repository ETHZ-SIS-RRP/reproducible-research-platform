import classNames from 'classnames'
import React, { FunctionComponent, useEffect, useState } from 'react'
import { useDispatch, useSelector } from 'react-redux'

import { setNewPath, updateUiData } from '../../actions/actions'
import { DataSetType, Message } from '../../store/types'

import { BackendProject } from '../../store/backend'
import { getDataSetTypes, getUiDataOf } from '../../store/selectors'
import { ActiveProjectData, DataSetCreationSelection } from '../../store/ui'
import SectionHeading from '../SectionHeading'
import Dropdown from './../Dropdown'
import Input from './../Input'
import styles from './DataSetCreator.scss'

type Props = {
  project: BackendProject
}

const DataSetCreator: FunctionComponent<Props> = props => {
  const p = props.project
  const uiData = useSelector(getUiDataOf(p)) as ActiveProjectData
  const dataSetTypes = useSelector(getDataSetTypes)

  const dispatch = useDispatch()

  const [creationOngoing, setCreationOngoing] = useState(false)
  const [message, setMessage] = useState(new Message(false, ''))

  const dataSetType = dataSetTypes.find(dst => dst.name === uiData.dataSetCreationSelection.dataSetTypeName)
  const hasProperties = dataSetType.propertyAssignments.length > 0
  const properties = uiData.dataSetCreationSelection.properties[uiData.dataSetCreationSelection.dataSetTypeName]

  useEffect(() => {
    setCreationOngoing(false)
    setMessage(new Message(false, ''))
  }, [props.project])

  function createDataSet(projectArchive: boolean) {
    setCreationOngoing(true)
    setMessage(new Message(false, ''))

    const body = {
      dataSetType: dataSetType.name,
      properties: properties,
      entityId: uiData.dataSetCreationSelection.entityId,
      projectArchive: projectArchive
    }

    fetch('/projects/' + p.id + '/dataset', {
      method: 'POST',
      body: JSON.stringify(body)
    })
      .then(response => {
        setCreationOngoing(false)
        if (response.status === 201) {
          dispatch(setNewPath(response.headers.get('Location').replace('/projects/', '/')))
        } else {
          response.json().then(json => setMessage(new Message(true, (json as any)['error'])))
        }
      })
      .catch(error => {
        console.log(error.toString())
        setCreationOngoing(false)
        setMessage(new Message(true, error.toString()))
      })
  }

  const messageStyle = classNames(styles.message, { [styles.red]: message.isError })

  const buttonStyle = creationOngoing ? styles.createDatasetButtonDisabled : styles.createDatasetButton

  function updateData(f: (value: string) => DataSetCreationSelection): (event: React.ChangeEvent<HTMLInputElement>) => void {
    return e => dispatch(updateUiData({ projectId: p.id, data: uiData.setDataSetCreationSelection(f(e.target.value)) }))
  }

  function updateDropdown(f: (value: string) => DataSetCreationSelection): (dst: DataSetType) => void {
    return dst => dispatch(updateUiData({ projectId: p.id, data: uiData.setDataSetCreationSelection(f(dst.name)) }))
  }

  const propertyStyles = classNames(styles.section, { [styles.invisible]: !hasProperties })

  return (
    <div className={styles.datasetCreator}>
      <div className={styles.section}>
        <SectionHeading>Dataset selection</SectionHeading>
        <Input
          label='Destination entity identifier / permID'
          placeHolder="e.g. '/SPACE/PROJECT/SAMPLE' or '20220124055533058-120'"
          vertical
          value={uiData.dataSetCreationSelection.entityId}
          type=''
          onChange={updateData(value => uiData.dataSetCreationSelection.setEntityId(value))}
        />

        <Dropdown
          styles={styles}
          value={dataSetType}
          onSelect={updateDropdown(value => uiData.dataSetCreationSelection.setDataSetTypeName(value))}
          label='Dataset type'
          items={dataSetTypes}
          toName={(dst: DataSetType) => dst.name}
        />
      </div>

      <div className={propertyStyles}>
        <SectionHeading> Dataset properties </SectionHeading>
        <div className={styles.instructional}>Mandatory properties are marked with an asterisk (*)</div>

        {dataSetType.propertyAssignments.map((assignment, index) => (
          <Input
            key={index}
            label={assignment.name + ' (' + assignment.dataType + ')' + (assignment.mandatory ? ' (*)' : '')}
            vertical
            placeHolder=''
            value={properties[assignment.name]}
            type=''
            onChange={updateData(value => uiData.dataSetCreationSelection.setProperty(dataSetType.name, assignment.name, value))}
          />
        ))}
      </div>

      <div className={styles.buttonWrapper}>
        <div className={messageStyle}>{message.value}</div>

        <div className={buttonStyle} onClick={() => createDataSet(false)}>
          Upload results
        </div>

        <div className={buttonStyle} onClick={() => createDataSet(true)}>
          Upload project
        </div>
      </div>
    </div>
  )
}

export default DataSetCreator
