import { useContext } from 'react';
import { AuthContext } from './authContext';

export function useAuth() {
    const authContext = useContext(AuthContext);

    if (!authContext) {
        throw new Error('useAuth AuthProvider içinde kullanılmalıdır');
    }

    return authContext;
}
