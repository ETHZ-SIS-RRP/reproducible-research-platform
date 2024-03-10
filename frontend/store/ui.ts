import { immerable } from 'immer'
import { LoginInProgress, Message, Share } from './types'

export interface UserInterfaceData {}

export class GitCreatorData implements UserInterfaceData {
  [immerable] = true
  constructor(
    public readonly repository: string,
    public readonly username: string,
    public readonly password: string,
    public readonly projectName: string,
    public readonly branch: string,
    public readonly buildError: string
  ) {}
  setRepository = (value: string) => new GitCreatorData(value, this.username, this.password, this.projectName, this.branch, this.buildError)
  setUsername = (value: string) => new GitCreatorData(this.repository, value, this.password, this.projectName, this.branch, this.buildError)
  setPassword = (value: string) => new GitCreatorData(this.repository, this.username, value, this.projectName, this.branch, this.buildError)
  setProjectName = (value: string) => new GitCreatorData(this.repository, this.username, this.password, value, this.branch, this.buildError)
  setBranch = (value: string) => new GitCreatorData(this.repository, this.username, this.password, this.projectName, value, this.buildError)
  setBuildError = (value: string) => new GitCreatorData(this.repository, this.username, this.password, this.projectName, this.branch, value)
}

export class ShareIdCreatorData implements UserInterfaceData {
  [immerable] = true
  constructor(public readonly shareId: string, public readonly username: string, public readonly password: string, public readonly share: Share) {}

  setShareId = (value: string) => new ShareIdCreatorData(value, this.username, this.password, this.share)
  setUsername = (value: string) => new ShareIdCreatorData(this.shareId, value, this.password, this.share)
  setPassword = (value: string) => new ShareIdCreatorData(this.shareId, this.username, value, this.share)
  setShare = (value: Share) => new ShareIdCreatorData(this.shareId, this.username, this.password, value)
}

export class ArchiveCreatorData implements UserInterfaceData {
  [immerable] = true
  constructor(public readonly archiveId: string, public readonly createError: string) {}
  setArchiveId = (value: string) => new ArchiveCreatorData(value, this.createError)
  setCreateError = (value: string) => new ArchiveCreatorData(this.archiveId, value)
}

export type DataSetProperties = { [key: string]: { [key: string]: string } }

export class DataSetCreationSelection {
  [immerable] = true
  constructor(public dataSetTypeName: string, public entityId: string, public properties: DataSetProperties) {}

  setDataSetTypeName = (value: string) => new DataSetCreationSelection(value, this.entityId, this.properties)
  setEntityId = (value: string) => new DataSetCreationSelection(this.dataSetTypeName, value, this.properties)
  setProperty = (dataSetType: string, assignment: string, value: string) => {
    const props: DataSetProperties = {}
    Object.keys(this.properties).forEach(key => (props[key] = { ...this.properties[key] }))
    props[dataSetType][assignment] = value
    return new DataSetCreationSelection(this.dataSetTypeName, this.entityId, props)
  }
}

export class MountInfo {
  [immerable] = true
  constructor(public readonly data: string, public readonly dirty: boolean, public readonly message: Message) {}

  setData = (value: string) => new MountInfo(value, this.dirty, this.message)
  setDirty = (value: boolean) => new MountInfo(this.data, value, this.message)
  setMessage = (value: Message) => new MountInfo(this.data, this.dirty, value)
}

export class LogInfo {
  [immerable] = true
  constructor(public readonly selectedLog: string) {}
  setSelectedLog = (value: string) => new LogInfo(value)
}

export enum Tab {
  Details = 'details',
  Results = 'results',
  Archive = 'upload',
  Mount = 'mount',
  Share = 'share',
  Logs = 'logs'
}

export class ActiveProjectData implements UserInterfaceData {
  [immerable] = true
  constructor(
    public readonly tab: Tab,
    public readonly dataSetCreationSelection: DataSetCreationSelection,
    public readonly mountInfo: MountInfo,
    public readonly logInfo: LogInfo
  ) {}
  setTab = (value: Tab) => new ActiveProjectData(value, this.dataSetCreationSelection, this.mountInfo, this.logInfo)
  setDataSetCreationSelection = (value: DataSetCreationSelection) => new ActiveProjectData(this.tab, value, this.mountInfo, this.logInfo)
  setMountInfo = (value: MountInfo) => new ActiveProjectData(this.tab, this.dataSetCreationSelection, value, this.logInfo)
  setLogInfo = (value: LogInfo) => new ActiveProjectData(this.tab, this.dataSetCreationSelection, this.mountInfo, value)
}
