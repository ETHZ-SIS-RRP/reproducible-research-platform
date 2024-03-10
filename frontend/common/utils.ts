import React, { useRef, useEffect } from 'react'
import { Share } from '../store/types'

export const randomId = () =>
  Math.floor((1 + Math.random()) * 0x1000000000)
    .toString(36)
    .substring(1)

export function usePrevious<T>(value: T) {
  const ref = useRef<T>()
  useEffect(() => {
    ref.current = value
  })
  return ref.current
}

export function useOutsideAlerter(ref: React.MutableRefObject<any>, f: () => void) {
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (ref.current && !ref.current.contains(event.target)) {
        f()
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => {
      document.removeEventListener('mousedown', handleClickOutside)
    }
  }, [ref])
}

export function flatten<A>(as: A[][]): A[] {
  return as.reduce((acc, a) => acc.concat(a), [])
}

export function parseShare(raw: any): Share {
  const error = 'error' in raw
  const commitMessage = error ? raw['error'] : raw['commit']['message']
  return new Share(
    raw['id'],
    raw['author'],
    raw['projectName'],
    raw['createdAt'],
    raw['repository'],
    commitMessage,
    raw['commit']['hash'],
    raw['commit']['timestamp'],
    raw['commit']['authorName'],
    raw['commit']['authorEmail'],
    raw['description'],
    error
  )
}
