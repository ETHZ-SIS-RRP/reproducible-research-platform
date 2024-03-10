import React, { Children, cloneElement, FunctionComponent, isValidElement, ReactChild, ReactNode, useState } from 'react'
import defaultStyles from './Table.scss'
import classNames from 'classnames'

export class Column {
  constructor(public name: String, public grow: boolean) {}
}

export const Row: FunctionComponent<{ rowIndex?: number; numRows?: number; striped?: boolean; borders?: boolean; highlight?: boolean }> = props => {
  const [active, setActive] = useState(false)

  const children = Children.toArray(props.children)

  const cellClasses: (index: number) => string = index =>
    classNames(defaultStyles.cell, {
      [defaultStyles.activeCell]: active && props.highlight,
      [defaultStyles.cellStripe]: props.rowIndex % 2 === 0 && props.striped,
      [defaultStyles.defaultBorder]: props.borders,
      [defaultStyles.rightBorder]: props.borders && index === children.length - 1,
      [defaultStyles.bottomBorder]: props.borders && props.rowIndex === props.numRows - 1
    })

  const cells = children.map((child, index) => (
    <div key={index} className={cellClasses(index)} onMouseEnter={() => setActive(true)} onMouseLeave={() => setActive(false)}>
      {child}
    </div>
  ))

  return <>{cells}</>
}

type ComponentProps = {
  styles?: { [className: string]: string }
  withHeaders?: boolean
  withBorders?: boolean
  withStripes?: boolean
  withHighlight?: boolean
  columns: Column[]
}

const Table: FunctionComponent<ComponentProps> = props => {
  const styles = props.styles ? props.styles : defaultStyles
  const templateColumns = props.columns.map(column => (column.grow ? 'auto' : 'max-content')).join(' ')
  const children = Children.toArray(props.children)

  return (
    <div style={{ gridTemplateColumns: templateColumns }} className={styles.table}>
      <>
        {props.withHeaders
          ? props.columns.map((column, index) => (
              <div key={index} className={styles.header}>
                {column.name}
              </div>
            ))
          : null}
        {children.map((child, index) =>
          isValidElement(child)
            ? cloneElement(child, {
                rowIndex: index,
                numRows: children.length,
                striped: props.withStripes && children.length > 1,
                borders: props.withBorders,
                highlight: props.withHighlight
              })
            : child
        )}
      </>
    </div>
  )
}

export default Table
