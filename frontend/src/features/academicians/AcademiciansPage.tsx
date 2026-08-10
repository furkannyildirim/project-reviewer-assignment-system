import { useMemo, useState } from 'react';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import FileDownloadOutlinedIcon from '@mui/icons-material/FileDownloadOutlined';
import PersonAddAltOutlinedIcon from '@mui/icons-material/PersonAddAltOutlined';
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
    Typography,
} from '@mui/material';
import { ConfirmDialog } from '../../components/common/ConfirmDialog';
import { EmptyState } from '../../components/common/EmptyState';
import { ResultSummary } from '../../components/common/ResultSummary';
import { useDebouncedValue } from '../../hooks/useDebouncedValue';
import { getApiErrorMessage } from '../../utils/getApiErrorMessage';
import { useAuth } from '../auth/useAuth';
import {
    createAcademician,
    deleteAcademician,
    exportAcademicians,
    getAcademicianCount,
    getAcademicianIds,
    getAcademicianIdentityWarning,
    getAcademicians,
    updateAcademician,
} from './academicianApi';
import { academicianQueryKeys } from './academicianQueryKeys';
import type {
    Academician,
    CreateAcademicianRequest,
    UpdateAcademicianRequest,
} from './academician.types';
import { academicianPageSizes } from './academician.types';
import { AcademicianFormDialog } from './components/AcademicianFormDialog';
import { AcademicianImportDialog } from './components/AcademicianImportDialog';
import { AcademicianTable } from './components/AcademicianTable';

type FormMode = {
    type: 'create' | 'edit';
    academician: Academician | null;
} | null;

type SnackbarState = {
    message: string;
    severity: 'success' | 'error' | 'warning';
} | null;

type SelectionState = {
    filterKey: string;
    ids: Set<number>;
};

const academicianInitialOptions = [
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

function getFullName(academician: Academician) {
    return `${academician.firstName} ${academician.lastName}`.trim();
}

export function AcademiciansPage() {
    const { role } = useAuth();
    const queryClient = useQueryClient();
    const [search, setSearch] = useState('');
    const [initial, setInitial] = useState('');
    const [page, setPage] = useState(0);
    const [size, setSize] = useState(10);
    const [formMode, setFormMode] = useState<FormMode>(null);
    const [isImportDialogOpen, setImportDialogOpen] = useState(false);
    const [academicianToDelete, setAcademicianToDelete] =
        useState<Academician | null>(null);
    const [deleteError, setDeleteError] = useState<string | null>(null);
    const [snackbar, setSnackbar] = useState<SnackbarState>(null);
    const [isCheckingIdentity, setIsCheckingIdentity] = useState(false);
    const debouncedSearch = useDebouncedValue(search.trim(), 400);
    const hasActiveFilter = Boolean(debouncedSearch || initial);
    const isAdmin = role === 'ADMIN';
    const selectionFilterKey = `${debouncedSearch}|${initial}`;
    const [selection, setSelection] = useState<SelectionState>(() => ({
        filterKey: selectionFilterKey,
        ids: new Set(),
    }));
    const selectedIds = selection.filterKey === selectionFilterKey
        ? selection.ids
        : new Set<number>();

    const queryParams = {
        search: debouncedSearch,
        initial: initial || undefined,
        page,
        size,
    };

    const academiciansQuery = useQuery({
        queryKey: academicianQueryKeys.list(queryParams),
        queryFn: () => getAcademicians(queryParams),
        placeholderData: keepPreviousData,
    });

    const academicianCountQuery = useQuery({
        queryKey: academicianQueryKeys.count(),
        queryFn: getAcademicianCount,
        staleTime: 5 * 60 * 1000,
    });

    const idQueryParams = {
        search: debouncedSearch,
        initial: initial || undefined,
    };

    const academicianIdsQuery = useQuery({
        queryKey: academicianQueryKeys.ids(idQueryParams),
        queryFn: () => getAcademicianIds(idQueryParams),
        placeholderData: keepPreviousData,
    });

    const invalidateAcademicians = () =>
        queryClient.invalidateQueries({
            queryKey: academicianQueryKeys.all,
        });

    const createMutation = useMutation({
        mutationFn: createAcademician,
        onSuccess: async () => {
            setFormMode(null);
            setSnackbar({
                message: 'Akademisyen başarıyla eklendi',
                severity: 'success',
            });
            setPage(0);
            await invalidateAcademicians();
        },
    });

    const updateMutation = useMutation({
        mutationFn: ({
            id,
            request,
        }: {
            id: number;
            request: UpdateAcademicianRequest;
        }) => updateAcademician(id, request),
        onSuccess: async () => {
            setFormMode(null);
            setSnackbar({
                message: 'Akademisyen başarıyla güncellendi',
                severity: 'success',
            });
            await invalidateAcademicians();
        },
    });

    const deleteMutation = useMutation({
        mutationFn: deleteAcademician,
        onSuccess: async () => {
            const currentContentLength =
                academiciansQuery.data?.content.length ?? 0;

            setAcademicianToDelete(null);
            setDeleteError(null);
            setSnackbar({
                message: 'Akademisyen başarıyla silindi',
                severity: 'success',
            });

            if (currentContentLength === 1 && page > 0) {
                setPage((currentPage) => currentPage - 1);
            }

            await invalidateAcademicians();
        },
        onError: (error) => {
            const message = getApiErrorMessage(error, 'Akademisyen silinemedi');
            setDeleteError(message);
            setSnackbar({
                message,
                severity: 'error',
            });
        },
    });

    const exportMutation = useMutation({
        mutationFn: exportAcademicians,
        onSuccess: (blob) => {
            const url = URL.createObjectURL(blob);
            const anchor = document.createElement('a');
            anchor.href = url;
            anchor.download = 'akademisyenler.xlsx';
            document.body.appendChild(anchor);
            anchor.click();
            anchor.remove();
            URL.revokeObjectURL(url);
        },
        onError: (error) => {
            setSnackbar({
                message: getApiErrorMessage(
                    error,
                    'Akademisyenler Excel dosyasına aktarılamadı',
                ),
                severity: 'error',
            });
        },
    });

    const isSaving = createMutation.isPending
        || updateMutation.isPending
        || isCheckingIdentity;
    const isMutating = isSaving || deleteMutation.isPending;
    const academicians = academiciansQuery.data?.content ?? [];
    const filteredIds = useMemo(
        () => academicianIdsQuery.data ?? [],
        [academicianIdsQuery.data],
    );
    const selectedFilteredCount = filteredIds.filter((id) =>
        selectedIds.has(id)).length;
    const isAllSelected =
        filteredIds.length > 0 && selectedFilteredCount === filteredIds.length;
    const isSelectionIndeterminate =
        selectedFilteredCount > 0 && selectedFilteredCount < filteredIds.length;
    const showEmptyState =
        !academiciansQuery.isLoading
        && !academiciansQuery.isError
        && academicians.length === 0;

    async function handleFormSubmit(
        request: CreateAcademicianRequest | UpdateAcademicianRequest,
    ) {
        if (!formMode || isSaving) {
            return;
        }

        if (formMode.type === 'create') {
            const createRequest = request as CreateAcademicianRequest;

            if (!await confirmIdentity(createRequest)) {
                return;
            }

            await createMutation.mutateAsync(createRequest);
            return;
        }

        if (formMode.academician) {
            const updateRequest = request as UpdateAcademicianRequest;

            if (!await confirmIdentity(
                updateRequest,
                formMode.academician.id,
            )) {
                return;
            }

            await updateMutation.mutateAsync({
                id: formMode.academician.id,
                request: updateRequest,
            });
        }
    }

    async function confirmIdentity(
        request: CreateAcademicianRequest | UpdateAcademicianRequest,
        excludeAcademicianId?: number,
    ): Promise<boolean> {
        setIsCheckingIdentity(true);
        try {
            const warning = await getAcademicianIdentityWarning(
                request,
                excludeAcademicianId,
            );

            return !warning.warning
                || !warning.message
                || window.confirm(warning.message);
        } finally {
            setIsCheckingIdentity(false);
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
        if (academicianIdsQuery.isFetching) {
            return;
        }

        try {
            const result = academicianIdsQuery.data
                ? { data: academicianIdsQuery.data, error: null }
                : await academicianIdsQuery.refetch();
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
                message: 'Secilecek akademisyen listesi alinamadi',
                severity: 'error',
            });
        }
    }

    function handleDeleteConfirm() {
        if (!academicianToDelete || deleteMutation.isPending) {
            return;
        }

        deleteMutation.mutate(academicianToDelete.id);
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
            ids: [...selectedIds],
        });
    }

    return (
        <Stack spacing={3}>
            <Box
                sx={{
                    bgcolor: 'background.default',
                    borderBottom: '1px solid #E3EAF0',
                    display: 'flex',
                    flexDirection: 'column',
                    gap: 3,
                    position: 'sticky',
                    pt: { xs: 2.5, sm: 3, lg: 4 },
                    top: 0,
                    zIndex: (theme) => theme.zIndex.appBar - 1,
                }}
            >
            <Stack
                direction={{ xs: 'column', sm: 'row' }}
                spacing={2}
                sx={{
                    alignItems: { xs: 'stretch', sm: 'flex-start' },
                    justifyContent: 'space-between',
                }}
            >
                <Stack
                    direction="row"
                    spacing={1.75}
                    sx={{ alignItems: 'center', minWidth: 0 }}
                >
                    <Box
                        aria-hidden="true"
                        sx={{
                            bgcolor: '#2EA8D3',
                            borderRadius: '4px',
                            flex: '0 0 auto',
                            height: 52,
                            width: 4,
                        }}
                    />
                    <Box sx={{ minWidth: 0 }}>
                        <Typography variant="h4" component="h1">
                            Akademisyen Yönetimi
                        </Typography>
                        <Typography color="text.secondary" sx={{ mt: 0.75 }}>
                            Akademisyen kayıtlarını arayın, sayfalayın, oluşturun ve güncelleyin.
                        </Typography>
                    </Box>
                </Stack>
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
                            startIcon={<PersonAddAltOutlinedIcon />}
                            onClick={() => setFormMode({
                                type: 'create',
                                academician: null,
                            })}
                            sx={{
                                bgcolor: '#2A2B61',
                                color: '#FFFFFF',
                                '&:hover': {
                                    bgcolor: '#202153',
                                },
                            }}
                        >
                            Yeni Akademisyen
                        </Button>
                    </Stack>
            </Stack>

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
                    placeholder="Ad, soyad, e-posta, telefon, ünvan veya anahtar kelime"
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
                    {academicianInitialOptions.map((option) => (
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
                    {academicianPageSizes.map((pageSize) => (
                        <MenuItem key={pageSize} value={pageSize}>
                            {pageSize}
                        </MenuItem>
                    ))}
                </TextField>

                {academiciansQuery.data
                    && academicianCountQuery.data !== undefined && (
                    <ResultSummary
                        filteredCount={academiciansQuery.data.totalElements}
                        totalCount={academicianCountQuery.data}
                        hasActiveFilter={hasActiveFilter}
                        entityLabel="akademisyen"
                    />
                )}
            </Stack>
            </Box>

            {academiciansQuery.isError && (
                <Alert
                    severity="error"
                    action={(
                        <Button
                            color="inherit"
                            size="small"
                            onClick={() => void academiciansQuery.refetch()}
                        >
                            Tekrar dene
                        </Button>
                    )}
                >
                    {getApiErrorMessage(
                        academiciansQuery.error,
                        'Akademisyen listesi alınamadı',
                    )}
                </Alert>
            )}


            {showEmptyState ? (
                <EmptyState
                    title="Akademisyen kaydı bulunamadı"
                    description={
                        debouncedSearch
                            ? 'Arama ölçütlerini değiştirerek tekrar deneyin.'
                            : 'Yeni akademisyen ekleyerek listeyi oluşturmaya başlayın.'
                    }
                />
            ) : (
                <Box sx={{ opacity: academiciansQuery.isFetching ? 0.82 : 1 }}>
                    <AcademicianTable
                        data={academiciansQuery.data}
                        page={page}
                        size={size}
                        isLoading={academiciansQuery.isLoading}
                        isAdmin={isAdmin}
                        isMutating={isMutating}
                        selectedIds={selectedIds}
                        isAllSelected={isAllSelected}
                        isSelectionIndeterminate={isSelectionIndeterminate}
                        isSelectionLoading={academicianIdsQuery.isFetching}
                        onPageChange={setPage}
                        onToggleAll={() => void handleToggleAll()}
                        onToggleOne={handleToggleOne}
                        onEdit={(academician) => setFormMode({
                            type: 'edit',
                            academician,
                        })}
                        onDelete={(academician) => {
                            setDeleteError(null);
                            setAcademicianToDelete(academician);
                        }}
                    />
                </Box>
            )}

            <AcademicianFormDialog
                key={
                    formMode
                        ? `${formMode.type}-${formMode.academician?.id ?? 'new'}`
                        : 'closed'
                }
                open={Boolean(formMode)}
                academician={formMode?.academician ?? null}
                isSaving={isSaving}
                onClose={() => {
                    if (!isSaving) {
                        setFormMode(null);
                    }
                }}
                onSubmit={handleFormSubmit}
            />

            <AcademicianImportDialog
                open={isImportDialogOpen}
                onClose={() => setImportDialogOpen(false)}
            />

            <ConfirmDialog
                open={Boolean(academicianToDelete)}
                title="Akademisyeni sil"
                description={
                    academicianToDelete
                        ? `"${getFullName(academicianToDelete)}" kaydı silinecek. Bu işlem geri alınamaz.`
                        : ''
                }
                confirmLabel="Sil"
                isConfirming={deleteMutation.isPending}
                errorMessage={deleteError}
                onClose={() => {
                    if (!deleteMutation.isPending) {
                        setAcademicianToDelete(null);
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
