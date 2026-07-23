import { useCallback, useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import { jwtDecode } from 'jwt-decode';
import { AUTH_UNAUTHORIZED_EVENT } from '../../api/httpClient';
import { authStorage } from './authStorage';
import { loginRequest } from './authApi';
import { AuthContext } from './authContext';
import type {
    AuthContextValue,
    AuthUser,
    JwtAccessTokenClaims,
    LoginCredentials,
    Role,
} from './auth.types';

type AuthState = {
    accessToken: string;
    currentUser: AuthUser;
    role: Role;
};

type RawJwtClaims = {
    sub?: unknown;
    roles?: unknown;
    exp?: unknown;
};

function isRole(value: unknown): value is Role {
    return value === 'ADMIN' || value === 'USER';
}

function getAuthStateFromToken(accessToken: string): AuthState | null {
    try {
        const claims = jwtDecode<RawJwtClaims>(accessToken);
        const role = Array.isArray(claims.roles)
            ? claims.roles.find(isRole)
            : null;

        if (
            typeof claims.sub !== 'string'
            || typeof claims.exp !== 'number'
            || !role
        ) {
            return null;
        }

        if (claims.exp * 1000 <= Date.now()) {
            return null;
        }

        const typedClaims: JwtAccessTokenClaims = {
            sub: claims.sub,
            roles: [role],
            exp: claims.exp,
        };

        return {
            accessToken,
            currentUser: {
                email: typedClaims.sub,
            },
            role: typedClaims.roles[0],
        };
    } catch {
        return null;
    }
}

export function AuthProvider({ children }: { children: ReactNode }) {
    const [authState, setAuthState] = useState<AuthState | null>(() => {
        const storedToken = authStorage.getAccessToken();

        if (!storedToken) {
            return null;
        }

        const restoredState = getAuthStateFromToken(storedToken);

        if (!restoredState) {
            authStorage.clearAccessToken();
        }

        return restoredState;
    });

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

    const login = useCallback(async (credentials: LoginCredentials) => {
        const loginResponse = await loginRequest(credentials);
        const nextState = getAuthStateFromToken(loginResponse.accessToken);

        if (!nextState) {
            authStorage.clearAccessToken();
            throw new Error('Oturum bilgisi doğrulanamadı');
        }

        authStorage.setAccessToken(loginResponse.accessToken);
        setAuthState(nextState);
    }, []);

    const value = useMemo<AuthContextValue>(() => ({
        accessToken: authState?.accessToken ?? null,
        isAuthenticated: Boolean(authState),
        currentUser: authState?.currentUser ?? null,
        role: authState?.role ?? null,
        login,
        logout,
    }), [authState, login, logout]);

    return (
        <AuthContext.Provider value={value}>
            {children}
        </AuthContext.Provider>
    );
}
