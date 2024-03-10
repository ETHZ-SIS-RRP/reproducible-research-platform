import { Action, isType } from 'typescript-fsa'
import { LoggedIn, LoginInProgress, NotLoggedIn, SystemState } from './types'
import { loginClicked, loginFailed, loginSuccessful, logoutSuccessful } from '../actions/actions'

export const loginReducer: { (state: SystemState, action: Action<any>): void } = (state, action) => {
  if (isType(action, loginClicked)) {
    state.login = new LoginInProgress()
  }

  if (isType(action, loginFailed)) {
    state.login = new NotLoggedIn()
  }

  if (isType(action, loginSuccessful)) {
    state.login = new LoggedIn(action.payload.username, action.payload.validTo)
  }

  if (isType(action, logoutSuccessful)) {
    state.login = new NotLoggedIn()
  }
}
