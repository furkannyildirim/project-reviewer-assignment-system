import { useCallback, useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import axios from 'axios';
import { jwtDecode } from 'jwt-decode';
import { AUTH_UNAUTHORIZED_EVENT } from '../../api/httpClient';
import { authStorage } from './authStorage';
import { getCurrentUser, loginRequest } from './authApi';
import { AuthContext } from './authContext';
import type {
    AuthContextValue,
    CurrentUserResponse,
    LoginCredentials,
} from './auth.types';

type AuthState = {
    accessToken: string;
    currentUser: CurrentUserResponse;
};

type RawJwtClaims = {
    exp?: unknown;
};

function hasValidExpiration(accessToken: string): boolean {
    try {
        const claims = jwtDecode<RawJwtClaims>(accessToken);

        return typeof claims.exp === 'number'
            && claims.exp * 1000 > Date.now();
    } catch {
        return false;
    }
}

function isAuthorizationFailure(error: unknown): boolean {
    if (!axios.isAxiosError(error)) {
        return false;
    }

    return error.response?.status === 401 || error.response?.status === 403;
}

export function AuthProvider({ children }: { children: ReactNode }) {
    const [initialAccessToken] = useState(
        () => authStorage.getAccessToken(),
    );
    const [authState, setAuthState] = useState<AuthState | null>(null);
    const [isInitializing, setIsInitializing] = useState(
        () => Boolean(initialAccessToken),
    );

    const logout = useCallback(() => {
        authStorage.clearAccessToken();
        setAuthState(null);
    }, []);

    useEffect(() => {
        window.addEventListener(AUTH_UNAUTHORIZED_EVENT, logout);

        return () => {
            window.removeEventListener(AUTH_UNAUTHORIZED_EVENT, logout);
        };
    }, [logout]);

    useEffect(() => {
        if (!initialAccessToken) {
            return;
        }

        if (!hasValidExpiration(initialAccessToken)) {
            authStorage.clearAccessToken();
            setIsInitializing(false);
            return;
        }

        let isActive = true;

        getCurrentUser()
            .then((currentUser) => {
                if (isActive) {
                    setAuthState({
                        accessToken: initialAccessToken,
                        currentUser,
                    });
                }
            })
            .catch((error: unknown) => {
                if (isActive && isAuthorizationFailure(error)) {
                    authStorage.clearAccessToken();
                }
            })
            .finally(() => {
                if (isActive) {
                    setIsInitializing(false);
                }
            });

        return () => {
            isActive = false;
        };
    }, [initialAccessToken]);

    const login = useCallback(async (credentials: LoginCredentials) => {
        const loginResponse = await loginRequest(credentials);

        if (!hasValidExpiration(loginResponse.accessToken)) {
            authStorage.clearAccessToken();
            throw new Error('Oturum bilgisi doğrulanamadı');
        }

        authStorage.setAccessToken(loginResponse.accessToken);

        try {
            const currentUser = await getCurrentUser();

            setAuthState({
                accessToken: loginResponse.accessToken,
                currentUser,
            });
        } catch (error) {
            authStorage.clearAccessToken();
            setAuthState(null);
            throw error;
        }
    }, []);

    const value = useMemo<AuthContextValue>(() => ({
        accessToken: authState?.accessToken ?? null,
        isAuthenticated: Boolean(authState),
        currentUser: authState?.currentUser ?? null,
        role: authState?.currentUser.role ?? null,
        login,
        logout,
    }), [authState, login, logout]);

    if (isInitializing) {
        return null;
    }

    return (
        <AuthContext.Provider value={value}>
            {children}
        </AuthContext.Provider>
    );
}
