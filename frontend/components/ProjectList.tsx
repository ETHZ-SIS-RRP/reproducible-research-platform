import React, { FunctionComponent, useRef, useState } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { createProjectFromArchiveClicked, createProjectFromGitClicked, createProjectFromShareClicked } from '../actions/actions'
import { randomId, useOutsideAlerter } from '../common/utils'
import { getProjects, getSelectedProject } from '../store/selectors'
import PlusIcon from '../svg/plus.svg'
import Project from './Project'
import styles from './ProjectList.scss'

const ProjectList: FunctionComponent<{}> = () => {
  const projects = useSelector(getProjects)
  const selected = useSelector(getSelectedProject)?.id ?? ''

  const empty = <div className={styles.noProjects}>You haven't created any projects</div>

  const [dropdownOpen, setDropdownOpen] = useState(false)
  const dropdownStyle = dropdownOpen ? styles.dropdown : styles.invisible
  const buttonStyle = dropdownOpen ? styles.buttonOpen : styles.buttonClosed

  const wrapperRef = useRef(null)
  useOutsideAlerter(wrapperRef, () => setDropdownOpen(false))

  const dispatch = useDispatch()

  return (
    <div className={styles.projectList}>
      <div className={styles.header}>
        <div className={styles.yourProjects}>Your projects</div>
        <div className={styles.relative}>
          <div ref={wrapperRef} className={buttonStyle} onClick={() => setDropdownOpen(!dropdownOpen)}>
            <PlusIcon className={styles.plus} />
            <div className={dropdownStyle}>
              <div className={styles.dropdownHeader}>Create a new project from</div>
              <div className={styles.dropdownItem} onClick={() => dispatch(createProjectFromGitClicked(randomId()))}>
                Git repository
              </div>
              <div className={styles.dropdownItem} onClick={() => dispatch(createProjectFromArchiveClicked(randomId()))}>
                openBIS
              </div>
              <div className={styles.dropdownItem} onClick={() => dispatch(createProjectFromShareClicked(randomId()))}>
                Share identifier
              </div>
            </div>
          </div>
        </div>
      </div>
      <div className={styles.listWrapper}>
        {projects.length === 0 ? empty : projects.map(project => <Project key={project.id} project={project} selected={project.id === selected} />)}
      </div>
    </div>
  )
}

export default ProjectList
