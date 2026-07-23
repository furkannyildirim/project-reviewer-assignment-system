import { Navigate, Route, Routes } from 'react-router';
import { AppLayout } from '../components/layout/AppLayout';
import { AcademiciansPage } from '../features/academicians/AcademiciansPage';
import { LoginPage } from '../features/auth/LoginPage';
import { useAuth } from '../features/auth/useAuth';
import { CompaniesPage } from '../features/companies/CompaniesPage';
import { UsersPage } from '../features/users/UsersPage';
import { AdminRoute } from './AdminRoute';
import { ProtectedRoute } from './ProtectedRoute';

function RootRedirect() {
    const { isAuthenticated } = useAuth();

    return (
        <Navigate
            to={isAuthenticated ? '/academicians' : '/login'}
            replace
        />
    );
}

function NotFoundRedirect() {
    const { isAuthenticated } = useAuth();

    return (
        <Navigate
            to={isAuthenticated ? '/academicians' : '/login'}
            replace
        />
    );
}

export function AppRoutes() {
    return (
        <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/" element={<RootRedirect />} />

            <Route element={<ProtectedRoute />}>
                <Route element={<AppLayout />}>
                    <Route
                        path="/academicians"
                        element={<AcademiciansPage />}
                    />
                    <Route path="/companies" element={<CompaniesPage />} />
                    <Route element={<AdminRoute />}>
                        <Route path="/users" element={<UsersPage />} />
                    </Route>
                </Route>
            </Route>

            <Route path="*" element={<NotFoundRedirect />} />
        </Routes>
    );
}
