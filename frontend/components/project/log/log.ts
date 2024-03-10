export enum ProcessStatus {
  InProgress = "In progress",
  Successful = "Completed successfully",
  Unsuccessful = "Completed unsuccessfully"
}

export class LogEntry {
  constructor(
    public readonly name: string,
    public readonly url: string,
    public readonly type: string,
    public readonly timestamp: string,
    public readonly status: ProcessStatus,
    public readonly message: string
  ) {}
}
