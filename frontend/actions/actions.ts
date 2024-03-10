import { AnyAction } from 'redux'
import actionCreatorFactory from 'typescript-fsa'
import { BackendProject } from '../store/backend'
import { Configuration, DataSetType } from '../store/types'
import { Tab, UserInterfaceData } from '../store/ui'

const coreActionCreator = actionCreatorFactory('CORE')
export const init = coreActionCreator<void>('Initialize')
export const fetchDataSetTypes = coreActionCreator<void>('Fetch dataset types')
export const dataSetTypesRetrieved = coreActionCreator<DataSetType[]>('Dataset types retrieved')
export const getConfig = coreActionCreator<void>('Get configuration')
export const configRetrieved = coreActionCreator<Configuration>('Configuration retrieved')
export const setSelectedProject = coreActionCreator<string>('Set selected project')
export const setSelectedTab = coreActionCreator<Tab>('Set selected tab')

const apiActionCreator = actionCreatorFactory('API')
export const apiRequest = apiActionCreator<{
  url: string
  method?: string
  body?: string
  onSuccess: (response: Response) => Array<AnyAction> | AnyAction | void
  onFailure?: (error: any) => Array<AnyAction> | AnyAction | void
}>('API request')

const routingActionCreator = actionCreatorFactory('ROUTING')
export const initRouting = routingActionCreator<void>('Initialize routing')
export const setNewPath = routingActionCreator<string>('Set new path')
export const pathChanged = routingActionCreator<string>('Path changed')

const sseActionCreator = actionCreatorFactory('SSE')
export const terminalOutput = sseActionCreator<string>('Terminal output')

const authenticationActionCreator = actionCreatorFactory('AUTHENTICATION')
export const checkAuthentication = authenticationActionCreator<void>('Check authentication')
export const loginFailed = authenticationActionCreator<void>('Login failed')
export const loginSuccessful = authenticationActionCreator<{ username: string; validTo: string }>('Login successful')
export const logoutSuccessful = authenticationActionCreator<void>('Logout successful')
export const removeLoginError = authenticationActionCreator<void>('Remove login error')

const projectActionCreator = actionCreatorFactory('PROJECT')
export const createProject = projectActionCreator<{ repository: string; branch: string; username: string; password: string }>('Create project')
export const createProjectAccepted = projectActionCreator<{ name: string }>('Create project accepted')
export const deleteProject = projectActionCreator<string>('Delete project')
export const deleteProjectAccepted = projectActionCreator<string>('Delete project accepted')
export const startServer = projectActionCreator<string>('Start server')
export const startServerAccepted = projectActionCreator<string>('Start server accepted')
export const stopServer = projectActionCreator<string>('Stop server')
export const stopServerAccepted = projectActionCreator<string>('Stop server accepted')
export const cloneLogAvailable = projectActionCreator<{ url: string; projectId: string }>('Clone log available')
export const cloneFailed = projectActionCreator<string>('Clone failed')
export const buildFromGitFailedToStart = projectActionCreator<{ projectId: string; message: string }>('Build from git failed to start')
export const buildFromArchiveFailedToStart = projectActionCreator<{ projectId: string; message: string }>('Build from archive failed to start')

const eventActionCreator = actionCreatorFactory('EVENTS')
export const startEvents = eventActionCreator<void>('Start listening to events')
export const stopEvents = eventActionCreator<void>('Stop listening to events')
export const projectCreated = eventActionCreator<{ repository: string; events: string }>('Project created')
export const projectUpdated = eventActionCreator<{ update: BackendProject[] }>('Project updated')
export const projectDeleted = eventActionCreator<string>('Project deleted')

const userInterfaceActionCreator = actionCreatorFactory('USER INTERFACE')
export const loginClicked = userInterfaceActionCreator<{ username: string; password: string; pat: string }>('Login clicked')
export const logoutClicked = userInterfaceActionCreator<void>('Logout clicked')
export const projectClicked = userInterfaceActionCreator<string>('Project clicked')
export const tabClicked = userInterfaceActionCreator<string>('Tab clicked')
export const createProjectFromGitClicked = userInterfaceActionCreator<string>('Create project from Git clicked')
export const createProjectFromShareClicked = userInterfaceActionCreator<string>('Create project from share clicked')
export const createProjectFromArchiveClicked = userInterfaceActionCreator<string>('Create project from archive clicked')
export const openInfoBoxClicked = userInterfaceActionCreator<void>('Open infobox clicked')
export const closeDialogClicked = userInterfaceActionCreator<void>('Close dialog clicked')
export const deleteClicked = userInterfaceActionCreator<BackendProject>('Delete project clicked')
export const cloneClicked = userInterfaceActionCreator<{ projectId: string; repository: string; username: string; password: string; shareId?: string }>(
  'Clone repository clicked'
)
export const buildFromGitClicked = userInterfaceActionCreator<{ id: string; name: string; branch: string }>('Build project from Git clicked')
export const buildFromArchiveClicked = userInterfaceActionCreator<{ projectId: string; archiveId: string }>('Build project from archive clicked')
export const logClicked = userInterfaceActionCreator<string>('Log clicked')
export const usernameClicked = userInterfaceActionCreator<void>('Username clicked')

export const updateUiData = userInterfaceActionCreator<{ projectId: string; data: UserInterfaceData }>('Update UI data')
