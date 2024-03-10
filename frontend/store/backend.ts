import { immerable } from 'immer'
import { Share } from './types'

export class BackendProject {
  [immerable] = true

  constructor(
    public readonly id: string,
    public readonly status: ProjectStatus,
    public readonly statusMessage: string,
    public readonly source: ProjectSource,
    public readonly metadata: { [key: string]: string },
    public readonly serverUrl: string,
    public readonly cloneLogUrl: string,
    public readonly buildLogUrl: string,
    public readonly resultsUrl: string,
    public readonly logsUrl: string,
    public readonly cloneStatus?: CloneDetails
  ) {}
}

export enum ProjectStatus {
  Initializing = 'Initializing',
  InitializingFromShare = 'InitializingFromShare',
  InitializingFromArchive = 'InitializingFromArchive',
  Cloning = 'Cloning',
  CloneFailed = 'CloneFailed',
  Cloned = 'Cloned',
  Building = 'Building',
  BuildFailed = 'BuildFailed',
  BuildingFromArchive = 'BuildingFromArchive',
  BuildFromArchiveFailed = 'BuildFromArchiveFailed',
  Ready = 'Ready'
}

export class GitSource {
  [immerable] = true
  constructor(public readonly remote: string, public readonly commitHash: string) {}
  toString() {
    return 'Git remote ' + this.remote
  }
}

export class ShareIdSource {
  [immerable] = true
  constructor(public readonly share: Share) {}
  toString() {
    return `Share identifier from user '${this.share.author}'`
  }
}

export class ArchiveSource {
  [immerable] = true
  constructor(public readonly archiveId: string) {}
  toString() {
    return `Archive '${this.archiveId}'`
  }
}

export const SourceNotYetDefined = 'SourceNotYetDefined'
export type ProjectSource = GitSource | ShareIdSource | ArchiveSource | typeof SourceNotYetDefined

export class CommitDetails {
  [immerable] = true
  constructor(
    public readonly hash: string,
    public readonly message: string,
    public readonly timestamp: string,
    public readonly authorName: string,
    public readonly authorEmail: string
  ) {}
}

export class RemoteTracking {
  [immerable] = true
  constructor(public readonly branch: string, public readonly ahead: number, public readonly behind: number) {}
  toString(): string {
    if (this.branch === '') {
      return 'Branch not tracked'
    } else if (this.ahead > 0 && this.behind > 0) {
      return `Diverged from ${this.branch} (${this.ahead} commits ahead, ${this.behind} commits behind)`
    } else if (this.behind > 0) {
      return `${this.behind} commits behind of ${this.branch}`
    } else if (this.ahead > 0) {
      return `${this.ahead} commits ahead of ${this.branch}`
    } else {
      return `Up-to-date with ${this.branch}`
    }
  }
}

export class CloneDetails {
  [immerable] = true
  public constructor(
    public clean: boolean,
    public readonly branch: string,
    public readonly commit: CommitDetails,
    public readonly remote?: string,
    public readonly remoteTracking?: RemoteTracking
  ) {}
}
