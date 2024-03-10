import React, { FunctionComponent } from 'react'
import DownloadIcon from '../../svg/download.svg'
import styles from './FileSystemItem.scss'

type ComponentProps = {
  path?: string
  name: string
  downloadable: boolean
  modification?: string
  size?: number
  indent: number
}

function padding(indent: number): string {
  return (indent + 2) * 15 + 'px'
}

const FileSystemItem: FunctionComponent<ComponentProps> = props => {
  return (
    <div className={props.downloadable ? styles.fileItem : styles.directoryItem}>
      <DownloadIcon />
      <div style={{ paddingLeft: padding(props.indent) }} className={styles.name}>
        {props.name}
      </div>
      <div className={styles.modificationTime}>{props.modification}</div>
      <div className={styles.size}>{props.size}</div>
    </div>
  )
}

export default FileSystemItem
