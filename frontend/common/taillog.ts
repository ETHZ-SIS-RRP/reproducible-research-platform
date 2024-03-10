import { useEffect, useState } from 'react'
import { Log } from '../store/types'

export class BackendLog {
  constructor(public lines: string[], public error: string, public tail: string) {}

  setError(error: string) {
    return new BackendLog(this.lines, error, this.tail)
  }

  endProcessing() {
    if (this.error != null) {
      return this
    } else {
      return new BackendLog(this.lines, null, null)
    }
  }
}

class TailLine {
  constructor(public line: string, public partial: boolean) {}
}

export const useTailLog = (logUrl: string) => {
  const [log, setLog] = useState(new BackendLog([], null, null))
  const [tail, setTail] = useState([] as TailLine[])

  useEffect(() => {
    setLog(new BackendLog([], null, null))
    setTail([])

    if (logUrl != null) {
      fetch(logUrl).then(response =>
        response.status === 200
          ? response.json().then(response => {
              setLog(new BackendLog(response['lines'], response['error'], response['tail']))
            })
          : null
      )
    }
  }, [logUrl])

  const addTailLine: (line: TailLine) => (current: TailLine[]) => TailLine[] = line => current => {
    if (current.length == 0) {
      return [line]
    } else {
      const lastIndex = current.length - 1
      const last = current[lastIndex]
      const copy = Array.from(current)
      if (last.partial) {
        copy[lastIndex] = line
        return copy
      } else {
        copy.push(line)
        return copy
      }
    }
  }

  useEffect(() => {
    if (log.tail != null) {
      const source = new EventSource(log.tail)
      source.onopen = () => {}
      source.onerror = err => {
        setLog(current => current.endProcessing())
        source.close()
      }
      source.addEventListener('complete', evt => {
        const event = evt as MessageEvent
        setTail(addTailLine(new TailLine(event.data, false)))
      })
      source.addEventListener('partial', evt => {
        const event = evt as MessageEvent
        setTail(addTailLine(new TailLine(event.data, true)))
      })
      source.addEventListener('failure', evt => {
        const event = evt as MessageEvent
        setLog(current => current.setError(event.data))
      })

      return () => {
        if (source != null) {
          source.close()
        }
      }
    }
  }, [log.tail])

  if (log == null || (log.lines.length === 0 && log.error == null)) {
    return new Log([], null, false)
  } else {
    return new Log([].concat(...log.lines.concat(tail.map(t => t.line))), log.error == null ? '' : log.error, log.error != null || log.tail == null)
  }
}
