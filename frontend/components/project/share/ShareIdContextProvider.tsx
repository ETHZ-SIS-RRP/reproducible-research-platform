import React, { FunctionComponent, useContext, useEffect, useState } from 'react'
import { useDispatch } from 'react-redux'
import { apiRequest } from '../../../actions/actions'
import { parseShare } from '../../../common/utils'
import { Share } from '../../../store/types'
import { ProjectContext } from './ProjectContextProvider'

type Context = {
  shares: Share[]
  updateShares: () => void
}

export const ShareIdContext = React.createContext(null as Context)

const ShareIdContextProvider: FunctionComponent<{}> = props => {
  const dispatch = useDispatch()

  const { project } = useContext(ProjectContext)

  const [shares, setShares] = useState(null as Share[])

  function updateShares() {
    dispatch(
      apiRequest({
        url: `/projects/${project.id}/shares`,
        onSuccess: response => {
          response.json().then(result => setShares((result as any[]).map(parseShare)))
        }
      })
    )
  }

  useEffect(updateShares, [project.id])

  const contextValue = {
    shares: shares,
    updateShares: updateShares
  }

  return <ShareIdContext.Provider value={contextValue}>{props.children}</ShareIdContext.Provider>
}

export default ShareIdContextProvider
