import { Navigate, Outlet } from 'react-router';
import { useAuth } from '../features/auth/useAuth';

export function AdminRoute() {
    const { role } = useAuth();

    if (role !== 'ADMIN') {
        return <Navigate to="/academicians" replace />;
    }

    return <Outlet />;
}
