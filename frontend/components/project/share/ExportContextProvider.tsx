import React, { FunctionComponent, useContext, useEffect, useState } from 'react'
import { useDispatch } from 'react-redux'
import { apiRequest } from '../../../actions/actions'
import { ProjectContext } from './ProjectContextProvider'

type ExportStatus = {
  image: string
  command: string
  remote: string
  commit: string
  dataSetExports: { [key: string]: [string, string] }
}

type Context = {
  exportStatus: ExportStatus
  updateExportStatus: () => void
}

export const ExportContext = React.createContext(null as Context)

const ExportContextProvider: FunctionComponent<{}> = props => {
  const dispatch = useDispatch()

  const { project } = useContext(ProjectContext)

  const [exportStatus, setExportStatus] = useState(null as ExportStatus)

  function updateExportStatus() {
    dispatch(
      apiRequest({
        url: `/projects/${project.id}/exports`,
        onSuccess: response => {
          response.json().then(result => {
            setExportStatus(result)
          })
        }
      })
    )
  }

  useEffect(updateExportStatus, [project.id])

  const contextValue = {
    exportStatus: exportStatus,
    updateExportStatus: updateExportStatus
  }

  return <ExportContext.Provider value={contextValue}>{props.children}</ExportContext.Provider>
}

export default ExportContextProvider
