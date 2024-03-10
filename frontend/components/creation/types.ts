import { KeyboardEvent } from "react"
import { UserInterfaceData } from "../../store/ui"

export type UpdateData = (f: (value: string) => UserInterfaceData) => (event: React.ChangeEvent<HTMLInputElement>) => void
export type HandleKeyDown = (event: KeyboardEvent<HTMLInputElement>) => void
