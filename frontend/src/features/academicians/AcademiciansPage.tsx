import { useState } from 'react';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import PersonAddAltOutlinedIcon from '@mui/icons-material/PersonAddAltOutlined';
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
import { useDebouncedValue } from '../../hooks/useDebouncedValue';
import { getApiErrorMessage } from '../../utils/getApiErrorMessage';
import { useAuth } from '../auth/useAuth';
import {
    createAcademician,
    deleteAcademician,
    getAcademicianCount,
    getAcademicians,
    updateAcademician,
} from './academicianApi';
import { academicianQueryKeys } from './academicianQueryKeys';
import type { Academician, CreateAcademicianRequest } from './academician.types';
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
    severity: 'success' | 'error';
} | null;

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
    const debouncedSearch = useDebouncedValue(search.trim(), 400);
    const isAdmin = role === 'ADMIN';

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
            request: CreateAcademicianRequest;
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

    const isSaving = createMutation.isPending || updateMutation.isPending;
    const isMutating = isSaving || deleteMutation.isPending;
    const academicians = academiciansQuery.data?.content ?? [];
    const showEmptyState =
        !academiciansQuery.isLoading
        && !academiciansQuery.isError
        && academicians.length === 0;

    async function handleFormSubmit(request: CreateAcademicianRequest) {
        if (!formMode || isSaving) {
            return;
        }

        if (formMode.type === 'create') {
            await createMutation.mutateAsync(request);
            return;
        }

        if (formMode.academician) {
            await updateMutation.mutateAsync({
                id: formMode.academician.id,
                request,
            });
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

    function handleDeleteConfirm() {
        if (!academicianToDelete || deleteMutation.isPending) {
            return;
        }

        deleteMutation.mutate(academicianToDelete.id);
    }

    return (
        <Stack spacing={3}>
            <PageHeader
                title="Akademisyen Yönetimi"
                description="Akademisyen kayıtlarını arayın, sayfalayın, oluşturun ve güncelleyin."
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
                        >
                            Excel'den Aktar
                        </Button>
                        <Button
                            variant="contained"
                            startIcon={<PersonAddAltOutlinedIcon />}
                            onClick={() => setFormMode({
                                type: 'create',
                                academician: null,
                            })}
                        >
                            Yeni Akademisyen
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
                    placeholder="Ad, soyad, e-posta, telefon, ünvan veya anahtar kelime"
                    value={search}
                    onChange={(event) => handleSearchChange(event.target.value)}
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

                {academiciansQuery.data && (
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
                            {academiciansQuery.data.totalElements} akademisyen bulundu
                        </Typography>
                    </Box>
                )}

                {academicianCountQuery.data !== undefined && (
                    <Box
                        sx={{
                            alignItems: 'center',
                            color: 'text.secondary',
                            display: 'flex',
                            minHeight: 40,
                            ml: { md: 'auto' },
                            whiteSpace: 'nowrap',
                        }}
                    >
                        <Typography variant="body2" sx={{ fontWeight: 500 }}>
                            Toplam: {academicianCountQuery.data} akademisyen
                        </Typography>
                    </Box>
                )}
            </Stack>

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
                        onPageChange={setPage}
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
