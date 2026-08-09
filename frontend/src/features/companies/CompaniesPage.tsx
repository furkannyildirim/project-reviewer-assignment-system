import { useMemo, useState } from 'react';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import AddBusinessOutlinedIcon from '@mui/icons-material/AddBusinessOutlined';
import FileDownloadOutlinedIcon from '@mui/icons-material/FileDownloadOutlined';
import SearchOutlinedIcon from '@mui/icons-material/SearchOutlined';
import UploadFileOutlinedIcon from '@mui/icons-material/UploadFileOutlined';
import {
    Alert,
    Box,
    Button,
    InputAdornment,
    MenuItem,
    Snackbar,
    Stack,
    TextField,
} from '@mui/material';
import { ConfirmDialog } from '../../components/common/ConfirmDialog';
import { EmptyState } from '../../components/common/EmptyState';
import { PageHeader } from '../../components/common/PageHeader';
import { ResultSummary } from '../../components/common/ResultSummary';
import { useAuth } from '../auth/useAuth';
import { getApiErrorMessage } from '../../utils/getApiErrorMessage';
import { useDebouncedValue } from '../../hooks/useDebouncedValue';
import {
    createCompany,
    deleteCompany,
    exportCompanies,
    getCompanyCount,
    getCompanyById,
    getCompanyIds,
    getCompanies,
    getExternalCompanyNameWarning,
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
    severity: 'success' | 'error' | 'warning';
} | null;

type SelectionState = {
    filterKey: string;
    ids: Set<number>;
};

type CompaniesPageProps = {
    companyType: CompanyType;
    title: string;
    description: string;
    emptyTitle: string;
};

const companyInitialOptions = [
    'A',
    'B',
    'C',
    'Ç',
    'D',
    'E',
    'F',
    'G',
    'Ğ',
    'H',
    'I',
    'İ',
    'J',
    'K',
    'L',
    'M',
    'N',
    'O',
    'Ö',
    'P',
    'R',
    'S',
    'Ş',
    'T',
    'U',
    'Ü',
    'V',
    'Y',
    'Z',
] as const;

export function CompaniesPage({
    companyType,
    title,
    description,
    emptyTitle,
}: CompaniesPageProps) {
    const { role } = useAuth();
    const queryClient = useQueryClient();
    const [search, setSearch] = useState('');
    const [initial, setInitial] = useState('');
    const [page, setPage] = useState(0);
    const [size, setSize] = useState(10);
    const [formMode, setFormMode] = useState<FormMode>(null);
    const [companyToDelete, setCompanyToDelete] = useState<Company | null>(null);
    const [isImportDialogOpen, setImportDialogOpen] = useState(false);
    const [isCheckingExternalName, setIsCheckingExternalName] = useState(false);
    const [deleteError, setDeleteError] = useState<string | null>(null);
    const [snackbar, setSnackbar] = useState<SnackbarState>(null);
    const debouncedSearch = useDebouncedValue(search.trim(), 400);
    const hasActiveFilter = Boolean(debouncedSearch || initial);
    const isAdmin = role === 'ADMIN';
    const selectionFilterKey = `${companyType}|${debouncedSearch}|${initial}`;
    const [selection, setSelection] = useState<SelectionState>(() => ({
        filterKey: selectionFilterKey,
        ids: new Set(),
    }));
    const selectedIds = selection.filterKey === selectionFilterKey
        ? selection.ids
        : new Set<number>();

    const queryParams = {
        companyType,
        search: debouncedSearch,
        initial: initial || undefined,
        page,
        size,
    };

    const companiesQuery = useQuery({
        queryKey: companyQueryKeys.list(queryParams),
        queryFn: () => getCompanies(queryParams),
        placeholderData: keepPreviousData,
    });

    const companyCountQuery = useQuery({
        queryKey: companyQueryKeys.count(companyType),
        queryFn: () => getCompanyCount(companyType),
        staleTime: 5 * 60 * 1000,
    });

    const idQueryParams = {
        companyType,
        search: debouncedSearch,
        initial: initial || undefined,
    };

    const companyIdsQuery = useQuery({
        queryKey: companyQueryKeys.ids(idQueryParams),
        queryFn: () => getCompanyIds(idQueryParams),
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

    const exportMutation = useMutation({
        mutationFn: exportCompanies,
        onSuccess: (blob) => {
            const fileName = companyType === 'EXTERNAL'
                ? 'dis-sirketler.xlsx'
                : 'tgb-sirketleri.xlsx';
            const url = URL.createObjectURL(blob);
            const anchor = document.createElement('a');
            anchor.href = url;
            anchor.download = fileName;
            document.body.appendChild(anchor);
            anchor.click();
            anchor.remove();
            URL.revokeObjectURL(url);
        },
        onError: (error) => {
            setSnackbar({
                message: getApiErrorMessage(
                    error,
                    'Şirketler Excel dosyasına aktarılamadı',
                ),
                severity: 'error',
            });
        },
    });

    const isSaving = createMutation.isPending
        || updateMutation.isPending
        || isCheckingExternalName;
    const isMutating = isSaving || deleteMutation.isPending;
    const companies = companiesQuery.data?.content ?? [];
    const filteredIds = useMemo(
        () => companyIdsQuery.data ?? [],
        [companyIdsQuery.data],
    );
    const selectedFilteredCount = filteredIds.filter((id) =>
        selectedIds.has(id)).length;
    const isAllSelected =
        filteredIds.length > 0 && selectedFilteredCount === filteredIds.length;
    const isSelectionIndeterminate =
        selectedFilteredCount > 0 && selectedFilteredCount < filteredIds.length;
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
            const createRequest = request as CreateCompanyRequest;

            if (!await confirmExternalName(createRequest.name)) {
                return;
            }

            await createMutation.mutateAsync(createRequest);
            return;
        }

        if (formMode.company) {
            const updateRequest = request as UpdateCompanyRequest;
            if (!await confirmExternalName(
                updateRequest.name,
                formMode.company.id,
            )) {
                return;
            }

            await updateMutation.mutateAsync({
                id: formMode.company.id,
                request: updateRequest,
            });
        }
    }

    async function confirmExternalName(
        name: string,
        excludeCompanyId?: number,
    ): Promise<boolean> {
        if (companyType !== 'EXTERNAL') {
            return true;
        }

        setIsCheckingExternalName(true);
        try {
            const warning = await getExternalCompanyNameWarning(
                name,
                excludeCompanyId,
            );
            return !warning.warning
                || !warning.message
                || window.confirm(warning.message);
        } finally {
            setIsCheckingExternalName(false);
        }
    }

    function handleSearchChange(value: string) {
        setSearch(value);
        setPage(0);
    }

    function handleInitialChange(value: string) {
        setInitial(value);
        setPage(0);
    }

    function handleSizeChange(nextSize: number) {
        setSize(nextSize);
        setPage(0);
    }

    function handleToggleOne(id: number) {
        setSelection((currentSelection) => {
            const currentIds = currentSelection.filterKey === selectionFilterKey
                ? currentSelection.ids
                : new Set<number>();
            const nextIds = new Set(currentIds);
            if (nextIds.has(id)) {
                nextIds.delete(id);
            } else {
                nextIds.add(id);
            }
            return { filterKey: selectionFilterKey, ids: nextIds };
        });
    }

    async function handleToggleAll() {
        if (companyIdsQuery.isFetching) {
            return;
        }

        try {
            const result = companyIdsQuery.data
                ? { data: companyIdsQuery.data, error: null }
                : await companyIdsQuery.refetch();
            if (result.error) {
                throw result.error;
            }
            const nextFilteredIds = result.data ?? [];

            setSelection((currentSelection) => {
                const currentIds = currentSelection.filterKey === selectionFilterKey
                    ? currentSelection.ids
                    : new Set<number>();
                const nextIds = new Set(currentIds);
                const allSelected = nextFilteredIds.length > 0
                    && nextFilteredIds.every((id) => nextIds.has(id));

                if (allSelected) {
                    nextFilteredIds.forEach((id) => nextIds.delete(id));
                    return { filterKey: selectionFilterKey, ids: nextIds };
                }

                nextFilteredIds.forEach((id) => nextIds.add(id));
                return { filterKey: selectionFilterKey, ids: nextIds };
            });
        } catch {
            setSnackbar({
                message: 'Secilecek firma listesi alinamadi',
                severity: 'error',
            });
        }
    }

    function handleDeleteConfirm() {
        if (!companyToDelete || deleteMutation.isPending) {
            return;
        }

        deleteMutation.mutate(companyToDelete.id);
    }

    function handleExport() {
        if (exportMutation.isPending) {
            return;
        }

        if (selectedIds.size === 0) {
            setSnackbar({
                message: 'Excel’e aktarmak için en az bir kayıt seçmelisiniz.',
                severity: 'warning',
            });
            return;
        }

        exportMutation.mutate({
            companyType,
            ids: [...selectedIds],
        });
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
                        <Button
                            variant="outlined"
                            startIcon={<UploadFileOutlinedIcon />}
                            onClick={() => setImportDialogOpen(true)}
                            sx={{
                                bgcolor: '#FFFFFF',
                                borderColor: '#D9E2EA',
                                color: '#2A2B61',
                                '&:hover': {
                                    bgcolor: 'rgba(46, 168, 211, 0.06)',
                                    borderColor: 'rgba(46, 168, 211, 0.45)',
                                },
                            }}
                        >
                            Excel'den Aktar
                        </Button>

                        <Button
                            variant="outlined"
                            startIcon={<FileDownloadOutlinedIcon />}
                            disabled={exportMutation.isPending}
                            onClick={handleExport}
                            sx={{
                                bgcolor: '#FFFFFF',
                                borderColor: '#D9E2EA',
                                color: '#2A2B61',
                                '&:hover': {
                                    bgcolor: 'rgba(46, 168, 211, 0.06)',
                                    borderColor: 'rgba(46, 168, 211, 0.45)',
                                },
                            }}
                        >
                            {exportMutation.isPending
                                ? 'Aktarılıyor'
                                : `Excel'e Aktar${selectedIds.size > 0
                                    ? ` (${selectedIds.size})`
                                    : ''}`}
                        </Button>

                        <Button
                            variant="contained"
                            startIcon={<AddBusinessOutlinedIcon />}
                            onClick={() => setFormMode({ type: 'create', company: null })}
                            sx={{
                                bgcolor: '#2A2B61',
                                color: '#FFFFFF',
                                '&:hover': {
                                    bgcolor: '#202153',
                                },
                            }}
                        >
                        Şirket Ekle
                        </Button>
                    </Stack>
                )}
            />

            <Stack
                direction={{ xs: 'column', md: 'row' }}
                spacing={2}
                sx={{
                    alignItems: { xs: 'stretch', md: 'center' },
                    '& .MuiInputLabel-root.Mui-focused': {
                        color: '#2A2B61',
                    },
                    '& .MuiOutlinedInput-root': {
                        bgcolor: '#FFFFFF',
                        borderRadius: '10px',
                        minHeight: 44,
                        '& .MuiOutlinedInput-notchedOutline': {
                            borderColor: '#D9E2EA',
                        },
                        '&:hover .MuiOutlinedInput-notchedOutline': {
                            borderColor: '#B8C7D4',
                        },
                        '&.Mui-focused': {
                            boxShadow: '0 0 0 3px rgba(42, 43, 97, 0.09)',
                        },
                        '&.Mui-focused .MuiInputAdornment-root .MuiSvgIcon-root': {
                            color: '#2A2B61',
                        },
                        '&.Mui-focused .MuiOutlinedInput-notchedOutline': {
                            borderColor: '#2A2B61',
                            borderWidth: 1,
                        },
                    },
                }}
            >
                <TextField
                    label="Arama"
                    placeholder="Şirket adı, e-posta, telefon veya faaliyet alanı"
                    value={search}
                    onChange={(event) => handleSearchChange(event.target.value)}
                    slotProps={{
                        input: {
                            startAdornment: (
                                <InputAdornment position="start">
                                    <SearchOutlinedIcon
                                        sx={{ color: '#728196', fontSize: 20 }}
                                    />
                                </InputAdornment>
                            ),
                        },
                    }}
                    sx={{ flexGrow: 1 }}
                />

                <TextField
                    select
                    label="Baş harf"
                    value={initial}
                    onChange={(event) => handleInitialChange(event.target.value)}
                    sx={{ minWidth: { xs: '100%', md: 150 } }}
                >
                    <MenuItem value="">Tümü</MenuItem>
                    {companyInitialOptions.map((option) => (
                        <MenuItem key={option} value={option}>
                            {option}
                        </MenuItem>
                    ))}
                </TextField>

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

                {companiesQuery.data
                    && companyCountQuery.data !== undefined && (
                    <ResultSummary
                        filteredCount={companiesQuery.data.totalElements}
                        totalCount={companyCountQuery.data}
                        hasActiveFilter={hasActiveFilter}
                        entityLabel={companyType === 'TGB'
                            ? 'TGB şirketi'
                            : 'dış şirket'}
                    />
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
                        selectedIds={selectedIds}
                        isAllSelected={isAllSelected}
                        isSelectionIndeterminate={isSelectionIndeterminate}
                        isSelectionLoading={companyIdsQuery.isFetching}
                        onPageChange={setPage}
                        onToggleAll={() => void handleToggleAll()}
                        onToggleOne={handleToggleOne}
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

            <CompanyImportPreviewDialog
                open={isImportDialogOpen}
                companyType={companyType}
                onClose={() => setImportDialogOpen(false)}
                onImported={invalidateCompanies}
            />

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
