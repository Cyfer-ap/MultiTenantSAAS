import { createAuthSession } from './authSession'
import { authStorage } from '../storage/authStorage'
import type { LoginResponse } from '../types/auth'

export function commitLoginResponse(response: LoginResponse): void {
    authStorage.write(createAuthSession(response))
}
