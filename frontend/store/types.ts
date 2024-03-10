import { immerable } from 'immer'
import { AnyAction, Dispatch, Middleware } from 'redux'
import { BackendProject } from './backend'
import { UserInterfaceData } from './ui'

export type RRPMiddleware = Middleware<{}, SystemState, Dispatch<AnyAction>>

export class Log {
  [immerable] = true
  constructor(public lines: string[], public error: string, public complete: boolean) {}
}

export class SystemInfo {
  [immerable] = true
  constructor(public version: string, public commit: string, public repo2Docker: string) {}
}

export class NotLoggedIn {
  [immerable] = true
}
export class LoginInProgress {
  [immerable] = true
}
export class LoggedIn {
  [immerable] = true
  constructor(public readonly username: string, public readonly validTo: string) {}
}

type LoginStatus = NotLoggedIn | LoginInProgress | LoggedIn

export class PropertyAssignment {
  [immerable] = true
  constructor(public name: string, public dataType: string, public mandatory: boolean) {}
}
export class DataSetType {
  [immerable] = true
  constructor(public name: string, public propertyAssignments: PropertyAssignment[]) {}
}

export class Share {
  [immerable] = true
  constructor(
    public id: string,
    public author: string,
    public projectName: string,
    public createdAt: string,
    public repository: string,
    public commitMessage: string,
    public commitHash: string,
    public commitTimestamp: string,
    public commitAuthorName: string,
    public commitAuthorEmail: string,
    public description: string,
    public error: boolean
  ) {}
}

export class Configuration {
  [immerable] = true
  constructor(
    public readonly version: string,
    public readonly build: string,
    public readonly openbis: string,
    public readonly repo2docker: string,
    public readonly pat: boolean,
    public readonly downloader: string
  ) {}
}

export class SystemState {
  [immerable] = true
  constructor(
    public login: LoginStatus,
    public selectedProject: string,
    public dialogHeader: string,
    public dialogContent: string,
    public deletionCandidate: BackendProject,
    public dataSetTypes: DataSetType[],
    public backendProjects: BackendProject[],
    public uiData: { [projectId: string]: UserInterfaceData },
    public routingInitialized: boolean,
    public config: Configuration
  ) {}
}

export class Message {
  constructor(public isError: boolean, public value: string) {}
}
