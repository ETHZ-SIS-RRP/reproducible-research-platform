import React, { FunctionComponent, useContext, useEffect, useState } from 'react'
import { useDispatch } from 'react-redux'
import { apiRequest } from '../../../actions/actions'
import { ProjectContext } from './ProjectContextProvider'

export type Bundle = {
  description: string
  createdAt: string
  success: boolean
  finished: boolean
  fileSize: number
  commit: string
  link: string
}

export type Context = {
  bundles: Bundle[]
  updateBundles: () => void
}

export const PlayerBundleContext = React.createContext(null as Context)

const PlayerBundleContextProvider: FunctionComponent<{}> = props => {
  const dispatch = useDispatch()

  const { project } = useContext(ProjectContext)

  const [bundles, setBundles] = useState(null as Bundle[])

  function parseBundle(raw: any): Bundle {
    return {
      description: raw['description'],
      createdAt: raw['createdAt'],
      success: raw['success'],
      finished: raw['finished'],
      fileSize: raw['fileSize'],
      commit: raw['commitMessage'] + ' (' + raw['commitHash'].substring(0, 7) + ')',
      link: raw['link']
    }
  }

  function updateBundles() {
    dispatch(
      apiRequest({
        url: `/projects/${project.id}/bundles`,
        onSuccess: response => {
          response.json().then(result => setBundles((result as any[]).map(parseBundle)))
        }
      })
    )
  }

  useEffect(updateBundles, [project.id])

  const contextValue = {
    bundles: bundles,
    updateBundles: updateBundles
  }

  return <PlayerBundleContext.Provider value={contextValue}>{props.children}</PlayerBundleContext.Provider>
}

export default PlayerBundleContextProvider
