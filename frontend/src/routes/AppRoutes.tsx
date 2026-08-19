import { Navigate, Route, Routes } from 'react-router';
import { AppLayout } from '../components/layout/AppLayout';
import { AcademiciansPage } from '../features/academicians/AcademiciansPage';
import { AcademicianDetailPage } from '../features/academicians/AcademicianDetailPage';
import { LoginPage } from '../features/auth/LoginPage';
import { useAuth } from '../features/auth/useAuth';
import { CompaniesPage } from '../features/companies/CompaniesPage';
import { CompanyDetailPage } from '../features/companies/CompanyDetailPage';
import { AdminProfilePage } from '../features/profile/AdminProfilePage';
import { ProjectsPage } from '../features/projects/ProjectsPage';
import { ProjectDetailPage } from '../features/projects/ProjectDetailPage';
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
                    <Route
                        path="/academicians/:academicianId"
                        element={<AcademicianDetailPage />}
                    />
                    <Route path="/projects" element={<ProjectsPage />} />
                    <Route path="/projects/:projectId" element={<ProjectDetailPage />} />
                    <Route
                        path="/companies"
                        element={<Navigate to="/companies/tgb" replace />}
                    />
                    <Route
                        path="/companies/tgb"
                        element={(
                            <CompaniesPage
                                key="tgb-companies"
                                companyType="TGB"
                                title="TGB Şirketleri"
                                description="TGB şirket kayıtlarını arayın, sayfalayın, oluşturun ve güncelleyin."
                                emptyTitle="Henüz TGB şirketi bulunmuyor."
                            />
                        )}
                    />
                    <Route
                        path="/companies/external"
                        element={(
                            <CompaniesPage
                                key="external-companies"
                                companyType="EXTERNAL"
                                title="Dış Şirketler"
                                description="Dış şirket kayıtlarını arayın, sayfalayın, oluşturun ve güncelleyin."
                                emptyTitle="Henüz dış şirket bulunmuyor."
                            />
                        )}
                    />
                    <Route
                        path="/companies/:companyContext/:companyId"
                        element={<CompanyDetailPage />}
                    />
                    <Route element={<AdminRoute />}>
                        <Route path="/profile" element={<AdminProfilePage />} />
                        <Route path="/users" element={<UsersPage />} />
                    </Route>
                </Route>
            </Route>

            <Route path="*" element={<NotFoundRedirect />} />
        </Routes>
    );
}
