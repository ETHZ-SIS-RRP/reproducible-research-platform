import { saveAs } from 'file-saver'
import JSZip from 'jszip'
import React, { FunctionComponent, useContext, useEffect } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { apiRequest, setNewPath } from '../../../actions/actions'
import Input from '../../Input'
import SectionHeading from '../../SectionHeading'
import Button from './Button'
import { ExportContext } from './ExportContextProvider'
import styles from './PlayerScript.scss'
import { ProjectContext } from './ProjectContextProvider'
import windowsBat from './play.bat'
import windowsPs1 from './play.ps1'
import unixPlay from './play.sh'
import { getConfig } from '../../../store/selectors'

const PlayerScript: FunctionComponent<{}> = () => {
  const dispatch = useDispatch()
  const labelWidth = 200

  const { project } = useContext(ProjectContext)
  const { exportStatus, updateExportStatus } = useContext(ExportContext)
  const { downloader } = useSelector(getConfig)

  useEffect(() => {
    if (updateExportStatus != null) {
      const id = setTimeout(updateExportStatus, 10000)
      return () => clearTimeout(id)
    }
  })

  function datasetList(idsFull: string[]) {
    const ids = idsFull.map(id => id.split('/').slice(-1)[0])
    const uniq = Array.from(new Set(ids))

    if (uniq.length <= 3) {
      return uniq.join(', ')
    } else {
      return uniq.slice(0, 3).join(', ') + ', ... (total ' + uniq.length + ')'
    }
  }

  function exportImage() {
    dispatch(
      apiRequest({
        url: `/projects/${project.id}/exports`,
        method: 'POST',
        onSuccess: response => {
          if (response.status == 201) {
            dispatch(setNewPath(response.headers.get('Location')))
          } else {
            console.log('Image export failed')
          }
        }
      })
    )
  }

  function padTo2Digits(num: number) {
    return num.toString().padStart(2, '0')
  }

  function formatDate(date: Date) {
    return (
      [date.getFullYear(), padTo2Digits(date.getMonth() + 1), padTo2Digits(date.getDate())].join('-') +
      '_' +
      [padTo2Digits(date.getHours()), padTo2Digits(date.getMinutes()), padTo2Digits(date.getSeconds())].join('')
    )
  }

  function createReferences(): string {
    let references = ''
    for (const path in exportStatus.dataSetExports) {
      const [id, url] = exportStatus.dataSetExports[path]
      references += `${id};${path};${url}\n`
    }
    return references
  }

  function generateScript() {
    console.log('Generating player scripts with config', exportStatus)
    const zip = new JSZip()
    const name = formatDate(new Date()) + '_' + project.id + '-player-script'
    const folder = zip.folder(name)
    folder.file('play', unixPlay, {
      unixPermissions: '755'
    })
    folder.file('play.bat', windowsBat)
    folder.file('play.ps1', windowsPs1)
    folder.file('references.txt', createReferences())
    folder.file('image.txt', exportStatus.image)
    folder.file('remote.txt', exportStatus.remote)
    folder.file('commit.txt', exportStatus.commit)
    folder.file('command.txt', exportStatus.command)
    folder.file('download-image.txt', downloader)

    zip.generateAsync({ type: 'blob', platform: 'UNIX' }).then(function (content) {
      saveAs(content, name)
    })
  }

  const imageExported = exportStatus?.image ? 'Yes' : 'No'
  const exportedDataSets = Object.keys(exportStatus?.dataSetExports ?? []).filter(key => exportStatus.dataSetExports[key][1] !== '')
  const notExportedDataSets = Object.keys(exportStatus?.dataSetExports ?? []).filter(key => exportStatus.dataSetExports[key][1] === '')

  return (
    <div className={styles.main}>
      <SectionHeading>Player scripts</SectionHeading>
      <Input label='Image exported' value={imageExported} disabled labelWidth={labelWidth} />
      <Input label='Exported datasets' value={datasetList(exportedDataSets)} disabled labelWidth={labelWidth} />
      <Input label='Not exported datasets' value={datasetList(notExportedDataSets)} disabled labelWidth={labelWidth} />

      <Button disabled={false} action={exportImage} message={''}>
        Export image
      </Button>
      <Button disabled={!exportStatus?.image} action={generateScript} message={''}>
        Generate player script
      </Button>
    </div>
  )
}

export default PlayerScript
