import { useState } from 'react';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import AddBusinessOutlinedIcon from '@mui/icons-material/AddBusinessOutlined';
import UploadFileOutlinedIcon from '@mui/icons-material/UploadFileOutlined';
import {
    Alert,
    Box,
    Button,
    MenuItem,
    Snackbar,
    Stack,
    TextField,
    Typography,
} from '@mui/material';
import { ConfirmDialog } from '../../components/common/ConfirmDialog';
import { EmptyState } from '../../components/common/EmptyState';
import { PageHeader } from '../../components/common/PageHeader';
import { useAuth } from '../auth/useAuth';
import { getApiErrorMessage } from '../../utils/getApiErrorMessage';
import { useDebouncedValue } from '../../hooks/useDebouncedValue';
import {
    createCompany,
    deleteCompany,
    getCompanyById,
    getCompanies,
    updateCompany,
} from './companyApi';
import { companyQueryKeys } from './companyQueryKeys';
import type {
    Company,
    CompanyType,
    CreateCompanyRequest,
    UpdateCompanyRequest,
} from './company.types';
import { companyPageSizes } from './company.types';
import { CompanyFormDialog } from './components/CompanyFormDialog';
import { CompanyImportPreviewDialog } from './components/CompanyImportPreviewDialog';
import { CompanyTable } from './components/CompanyTable';

type FormMode = {
    type: 'create' | 'edit';
    company: Company | null;
} | null;

type SnackbarState = {
    message: string;
    severity: 'success' | 'error';
} | null;

type CompaniesPageProps = {
    companyType: CompanyType;
    title: string;
    description: string;
    emptyTitle: string;
};

export function CompaniesPage({
    companyType,
    title,
    description,
    emptyTitle,
}: CompaniesPageProps) {
    const { role } = useAuth();
    const queryClient = useQueryClient();
    const [search, setSearch] = useState('');
    const [page, setPage] = useState(0);
    const [size, setSize] = useState(10);
    const [formMode, setFormMode] = useState<FormMode>(null);
    const [companyToDelete, setCompanyToDelete] = useState<Company | null>(null);
    const [isImportDialogOpen, setImportDialogOpen] = useState(false);
    const [deleteError, setDeleteError] = useState<string | null>(null);
    const [snackbar, setSnackbar] = useState<SnackbarState>(null);
    const debouncedSearch = useDebouncedValue(search.trim(), 400);
    const isAdmin = role === 'ADMIN';

    const queryParams = {
        companyType,
        search: debouncedSearch,
        page,
        size,
    };

    const companiesQuery = useQuery({
        queryKey: companyQueryKeys.list(queryParams),
        queryFn: () => getCompanies(queryParams),
        placeholderData: keepPreviousData,
    });

    const editCompanyId =
        formMode?.type === 'edit' ? formMode.company?.id : undefined;

    const companyDetailQuery = useQuery({
        queryKey: companyQueryKeys.detail(editCompanyId ?? 0),
        queryFn: () => getCompanyById(editCompanyId ?? 0),
        enabled: Boolean(formMode) && formMode?.type === 'edit' && Boolean(editCompanyId),
    });

    const invalidateCompanies = () =>
        queryClient.invalidateQueries({
            queryKey: companyQueryKeys.all,
        });

    const createMutation = useMutation({
        mutationFn: createCompany,
        onSuccess: async () => {
            setFormMode(null);
            setSnackbar({
                message: 'Şirket başarıyla eklendi.',
                severity: 'success',
            });
            setPage(0);
            await invalidateCompanies();
        },
    });

    const updateMutation = useMutation({
        mutationFn: ({
            id,
            request,
        }: {
            id: number;
            request: UpdateCompanyRequest;
        }) => updateCompany(id, request),
        onSuccess: async () => {
            if (formMode?.company) {
                queryClient.removeQueries({
                    queryKey: companyQueryKeys.detail(formMode.company.id),
                });
            }

            setFormMode(null);
            setSnackbar({
                message: 'Şirket başarıyla güncellendi.',
                severity: 'success',
            });
            await invalidateCompanies();
        },
    });

    const deleteMutation = useMutation({
        mutationFn: deleteCompany,
        onSuccess: async () => {
            const currentContentLength = companiesQuery.data?.content.length ?? 0;

            setCompanyToDelete(null);
            setDeleteError(null);
            setSnackbar({
                message: 'Şirket başarıyla silindi.',
                severity: 'success',
            });

            if (currentContentLength === 1 && page > 0) {
                setPage((currentPage) => currentPage - 1);
            }

            await invalidateCompanies();
        },
        onError: (error) => {
            const message = getApiErrorMessage(error, 'Şirket silinemedi');
            setDeleteError(message);
            setSnackbar({
                message,
                severity: 'error',
            });
        },
    });

    const isSaving = createMutation.isPending || updateMutation.isPending;
    const isMutating = isSaving || deleteMutation.isPending;
    const companies = companiesQuery.data?.content ?? [];
    const showEmptyState =
        !companiesQuery.isLoading
        && !companiesQuery.isError
        && companies.length === 0;

    async function handleFormSubmit(
        request: CreateCompanyRequest | UpdateCompanyRequest,
    ) {
        if (!formMode || isSaving) {
            return;
        }

        if (formMode.type === 'create') {
            await createMutation.mutateAsync(request as CreateCompanyRequest);
            return;
        }

        if (formMode.company) {
            await updateMutation.mutateAsync({
                id: formMode.company.id,
                request: request as UpdateCompanyRequest,
            });
        }
    }

    function handleSearchChange(value: string) {
        setSearch(value);
        setPage(0);
    }

    function handleSizeChange(nextSize: number) {
        setSize(nextSize);
        setPage(0);
    }

    function handleDeleteConfirm() {
        if (!companyToDelete || deleteMutation.isPending) {
            return;
        }

        deleteMutation.mutate(companyToDelete.id);
    }

    return (
        <Stack spacing={3}>
            <PageHeader
                title={title}
                description={description}
                action={(
                    <Stack
                        direction={{ xs: 'column', sm: 'row' }}
                        spacing={1}
                        sx={{ alignItems: { xs: 'stretch', sm: 'center' } }}
                    >
                        {companyType === 'TGB' && (
                            <Button
                                variant="outlined"
                                startIcon={<UploadFileOutlinedIcon />}
                                onClick={() => setImportDialogOpen(true)}
                            >
                                Excel'den Aktar
                            </Button>
                        )}

                        <Button
                            variant="contained"
                            startIcon={<AddBusinessOutlinedIcon />}
                            onClick={() => setFormMode({ type: 'create', company: null })}
                        >
                        Şirket Ekle
                        </Button>
                    </Stack>
                )}
            />

            <Stack
                direction={{ xs: 'column', md: 'row' }}
                spacing={2}
                sx={{ alignItems: { xs: 'stretch', md: 'center' } }}
            >
                <TextField
                    label="Arama"
                    placeholder="Şirket adı, e-posta, telefon veya faaliyet alanı"
                    value={search}
                    onChange={(event) => handleSearchChange(event.target.value)}
                    sx={{ flexGrow: 1 }}
                />

                <TextField
                    select
                    label="Satır sayısı"
                    value={size}
                    onChange={(event) => handleSizeChange(Number(event.target.value))}
                    sx={{ minWidth: { xs: '100%', md: 160 } }}
                >
                    {companyPageSizes.map((pageSize) => (
                        <MenuItem key={pageSize} value={pageSize}>
                            {pageSize}
                        </MenuItem>
                    ))}
                </TextField>

                {companiesQuery.data && (
                    <Box
                        sx={{
                            alignItems: 'center',
                            bgcolor: 'background.paper',
                            border: '1px solid',
                            borderColor: 'divider',
                            borderRadius: 2.5,
                            color: 'text.secondary',
                            display: 'flex',
                            minHeight: 40,
                            px: 1.75,
                            whiteSpace: 'nowrap',
                        }}
                    >
                        <Typography variant="body2" sx={{ fontWeight: 600 }}>
                            Toplam {companiesQuery.data.totalElements} kayıt
                        </Typography>
                    </Box>
                )}
            </Stack>

            {companiesQuery.isError && (
                <Alert severity="error">
                    {getApiErrorMessage(
                        companiesQuery.error,
                        'Şirket listesi alınamadı',
                    )}
                </Alert>
            )}


            {showEmptyState ? (
                <EmptyState
                    title={emptyTitle}
                    description={
                        debouncedSearch
                            ? 'Arama ölçütlerini değiştirerek tekrar deneyin.'
                            : 'Yeni şirket ekleyerek listeyi oluşturmaya başlayın.'
                    }
                />
            ) : (
                <Box sx={{ opacity: companiesQuery.isFetching ? 0.82 : 1 }}>
                    <CompanyTable
                        data={companiesQuery.data}
                        page={page}
                        size={size}
                        isLoading={companiesQuery.isLoading}
                        isAdmin={isAdmin}
                        isMutating={isMutating}
                        onPageChange={setPage}
                        onEdit={(company) => setFormMode({ type: 'edit', company })}
                        onDelete={(company) => {
                            setDeleteError(null);
                            setCompanyToDelete(company);
                        }}
                    />
                </Box>
            )}

            <CompanyFormDialog
                key={
                    formMode
                        ? `${formMode.type}-${formMode.company?.id ?? 'new'}-${companyDetailQuery.data?.id ?? 'pending'}`
                        : 'closed'
                }
                open={Boolean(formMode)}
                company={formMode?.company ?? null}
                companyDetail={
                    formMode?.type === 'edit' ? companyDetailQuery.data ?? null : null
                }
                isDetailLoading={
                    formMode?.type === 'edit' && companyDetailQuery.isLoading
                }
                detailError={
                    formMode?.type === 'edit' && companyDetailQuery.isError
                        ? getApiErrorMessage(
                            companyDetailQuery.error,
                            'Şirket detayı alınamadı',
                        )
                        : null
                }
                isSaving={isSaving}
                companyType={companyType}
                onClose={() => {
                    if (!isSaving) {
                        setFormMode(null);
                    }
                }}
                onSubmit={handleFormSubmit}
            />

            {companyType === 'TGB' && (
                <CompanyImportPreviewDialog
                    open={isImportDialogOpen}
                    onClose={() => setImportDialogOpen(false)}
                />
            )}

            <ConfirmDialog
                open={Boolean(companyToDelete)}
                title="Şirketi sil"
                description={
                    companyToDelete
                        ? `"${companyToDelete.name}" şirketi silinecek. Bu işlem geri alınamaz.`
                        : ''
                }
                confirmLabel="Sil"
                isConfirming={deleteMutation.isPending}
                errorMessage={deleteError}
                onClose={() => {
                    if (!deleteMutation.isPending) {
                        setCompanyToDelete(null);
                        setDeleteError(null);
                    }
                }}
                onConfirm={handleDeleteConfirm}
            />

            <Snackbar
                open={Boolean(snackbar)}
                autoHideDuration={4000}
                onClose={() => setSnackbar(null)}
                anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
            >
                <Alert
                    severity={snackbar?.severity ?? 'success'}
                    variant="filled"
                    onClose={() => setSnackbar(null)}
                >
                    {snackbar?.message}
                </Alert>
            </Snackbar>
        </Stack>
    );
}
