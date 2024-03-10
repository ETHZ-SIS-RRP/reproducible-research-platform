export interface FileSystemNode {
  name: string
  modificationTime: string
}

export class DirectoryNode implements FileSystemNode {
  constructor(public name: string, public modificationTime: string, public content: FileSystemNode[]) {}
}

export class FileNode implements FileSystemNode {
  constructor(public name: string, public modificationTime: string, public size: number) {}
}

function createNode(obj: any): FileSystemNode {
  if ('content' in obj) {
    return new DirectoryNode(
      obj['name'],
      obj['modificationTime'],
      (obj['content'] as object[])
        .map(createNode)
        .sort((a, b) => (a.constructor === b.constructor ? a.name.localeCompare(b.name) : a instanceof DirectoryNode ? -1 : 1))
    )
  } else {
    return new FileNode(obj['name'], obj['modificationTime'], obj['size'])
  }
}

export const getContent = (url: string) => {
  return fetch(url)
    .then(response => response.json())
    .then(createNode)
}
