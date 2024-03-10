import React, { FunctionComponent } from 'react'
import { useSelector } from 'react-redux'
import { ArchiveSource, GitSource, ProjectStatus, ShareIdSource } from '../store/backend'
import { getSelectedProject } from '../store/selectors'

import BuildingProject from './BuildingProject'
import ShareCreator from './creation/ShareCreator'
import ArchiveCreator from './creation/ArchiveCreator'
import GitCreator from './creation/GitCreator'
import Footer from './Footer'
import Header from './Header'
import styles from './Home.scss'
import ProjectDetails from './project/ProjectDetails'
import ProjectList from './ProjectList'
import Welcome from './Welcome'

const Home: FunctionComponent<{}> = () => {
  const project = useSelector(getSelectedProject)
  const status = project?.status
  const source = project?.source
  const definingStates = [ProjectStatus.Cloning, ProjectStatus.CloneFailed, ProjectStatus.Cloned]

  const creatingFromGit = status == ProjectStatus.Initializing || (definingStates.includes(status) && source instanceof GitSource)
  const creatingFromShareId = status == ProjectStatus.InitializingFromShare || (definingStates.includes(status) && source instanceof ShareIdSource)
  const creatingFromArchive = status == ProjectStatus.InitializingFromArchive
  const building = [ProjectStatus.Building, ProjectStatus.BuildFailed, ProjectStatus.BuildingFromArchive, ProjectStatus.BuildFromArchiveFailed].includes(status)
  const ready = status === ProjectStatus.Ready

  const component = creatingFromGit ? (
    <GitCreator project={project} />
  ) : creatingFromShareId ? (
    <ShareCreator project={project} />
  ) : creatingFromArchive ? (
    <ArchiveCreator project={project} />
  ) : building ? (
    <BuildingProject project={project} />
  ) : ready ? (
    <ProjectDetails project={project} />
  ) : (
    <Welcome />
  )

  return (
    <>
      <Header></Header>
      <div className={styles.projects}>
        <ProjectList />
        <div className={styles.scrollableRight}>{component}</div>
      </div>
      <Footer></Footer>
    </>
  )
}

export default Home
