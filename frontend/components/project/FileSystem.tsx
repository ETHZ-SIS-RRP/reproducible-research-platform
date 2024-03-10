import React, { FunctionComponent, useEffect, useState } from 'react'
import { DirectoryNode, FileNode, FileSystemNode, getContent } from '../../common/filesystem'
import styles from './FileSystem.scss'
import { flatten } from '../../common/utils'
import FileSystemItem from './FileSystemItem'

class FlatFileSystemNode {
  constructor(public indent: number, public path: string[], public node: FileSystemNode) {}
}

function flattenNodes(node: FileSystemNode, path: string[] = [], indent: number = 0): FlatFileSystemNode[] {
  if (node instanceof DirectoryNode) {
    const subitems = node.content.map(subnode => flattenNodes(subnode, [...path, node.name], indent + 1))
    return [new FlatFileSystemNode(indent, path, node), ...flatten(subitems)]
  } else if (node instanceof FileNode) {
    return [new FlatFileSystemNode(indent, path, node)]
  }
}

type ComponentProps = {
  url: string
}

function createRow(item: FlatFileSystemNode, key: number): JSX.Element {
  const node = item.node
  if (node instanceof DirectoryNode) {
    return <FileSystemItem key={key} name={node.name + '/'} downloadable={false} indent={item.indent} />
  } else if (node instanceof FileNode) {
    return (
      <FileSystemItem
        key={key}
        path={item.path.join('/')}
        name={node.name}
        downloadable
        modification={node.modificationTime}
        size={node.size}
        indent={item.indent}
      />
    )
  } else {
    return null
  }
}

const FileSystem: FunctionComponent<ComponentProps> = props => {
  const [results, setResults] = useState(null as FileSystemNode)

  useEffect(() => {
    if (results == null) {
      getContent(props.url).then(setResults)
    }
    const intervalId = setInterval(() => getContent(props.url).then(setResults), 5000)
    return () => clearInterval(intervalId)
  })

  useEffect(() => {
    setResults(null)
  }, [props.url])

  const content = results == null ? [] : flatten((results as DirectoryNode).content.map(node => flattenNodes(node)))
  if (content.length == 0) {
    return <div className={styles.emptyFileSystem}>There are no result files</div>
  } else {
    return <div className={styles.fileSystem}>{content.map(createRow)}</div>
  }
}

export default FileSystem
