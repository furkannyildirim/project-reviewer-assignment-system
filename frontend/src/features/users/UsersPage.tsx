import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import PersonAddAltOutlinedIcon from '@mui/icons-material/PersonAddAltOutlined';
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
import {
    createUser,
    deleteUser,
    getUsers,
    updateUser,
} from './userApi';
import { userQueryKeys } from './userQueryKeys';
import type {
    CreateUserRequest,
    UpdateUserRequest,
    User,
} from './user.types';
import { userPageSizes } from './user.types';
import { UserFormDialog } from './components/UserFormDialog';
import { UserTable } from './components/UserTable';

type FormMode = {
    type: 'create' | 'edit';
    user: User | null;
} | null;

type SnackbarState = {
    message: string;
    severity: 'success' | 'error';
} | null;

function getFullName(user: User) {
    return `${user.firstName} ${user.lastName}`.trim();
}

function matchesSearch(user: User, search: string) {
    const normalizedSearch = search.toLocaleLowerCase('tr-TR');

    return [
        user.firstName,
        user.lastName,
        user.email,
        user.phone,
    ].some((value) =>
        value.toLocaleLowerCase('tr-TR').includes(normalizedSearch),
    );
}

export function UsersPage() {
    const queryClient = useQueryClient();
    const [search, setSearch] = useState('');
    const [page, setPage] = useState(0);
    const [size, setSize] = useState(10);
    const [formMode, setFormMode] = useState<FormMode>(null);
    const [userToDelete, setUserToDelete] = useState<User | null>(null);
    const [deleteError, setDeleteError] = useState<string | null>(null);
    const [snackbar, setSnackbar] = useState<SnackbarState>(null);
    const debouncedSearch = useDebouncedValue(search.trim(), 400);

    const usersQuery = useQuery({
        queryKey: userQueryKeys.list(),
        queryFn: getUsers,
    });

    const users = useMemo(
        () => usersQuery.data?.filter((user) => user.role !== 'ADMIN') ?? [],
        [usersQuery.data],
    );

    const filteredUsers = useMemo(() => {
        if (!debouncedSearch) {
            return users;
        }

        return users.filter((user) => matchesSearch(user, debouncedSearch));
    }, [debouncedSearch, users]);

    const totalPages = Math.max(Math.ceil(filteredUsers.length / size), 1);
    const pagedUsers = filteredUsers.slice(page * size, page * size + size);
    const showEmptyState =
        !usersQuery.isLoading
        && !usersQuery.isError
        && filteredUsers.length === 0;

    const invalidateUsers = () =>
        queryClient.invalidateQueries({
            queryKey: userQueryKeys.all,
        });

    const createMutation = useMutation({
        mutationFn: createUser,
        onSuccess: async () => {
            setFormMode(null);
            setSnackbar({
                message: 'Kullanıcı başarıyla eklendi',
                severity: 'success',
            });
            setPage(0);
            await invalidateUsers();
        },
    });

    const updateMutation = useMutation({
        mutationFn: ({
            id,
            request,
        }: {
            id: number;
            request: UpdateUserRequest;
        }) => updateUser(id, request),
        onSuccess: async () => {
            setFormMode(null);
            setSnackbar({
                message: 'Kullanıcı başarıyla güncellendi',
                severity: 'success',
            });
            await invalidateUsers();
        },
    });

    const deleteMutation = useMutation({
        mutationFn: deleteUser,
        onSuccess: async () => {
            const currentPageLength = pagedUsers.length;

            setUserToDelete(null);
            setDeleteError(null);
            setSnackbar({
                message: 'Kullanıcı başarıyla silindi',
                severity: 'success',
            });

            if (currentPageLength === 1 && page > 0) {
                setPage((currentPage) => currentPage - 1);
            }

            await invalidateUsers();
        },
        onError: (error) => {
            const message = getApiErrorMessage(error, 'Kullanıcı silinemedi');
            setDeleteError(message);
            setSnackbar({
                message,
                severity: 'error',
            });
        },
    });

    const isSaving = createMutation.isPending || updateMutation.isPending;
    const isMutating = isSaving || deleteMutation.isPending;

    async function handleFormSubmit(
        request: CreateUserRequest | UpdateUserRequest,
        mode: 'create' | 'edit',
    ) {
        if (!formMode || isSaving) {
            return;
        }

        if (mode === 'create') {
            await createMutation.mutateAsync(request as CreateUserRequest);
            return;
        }

        if (formMode.user) {
            await updateMutation.mutateAsync({
                id: formMode.user.id,
                request: request as UpdateUserRequest,
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
        if (!userToDelete || deleteMutation.isPending) {
            return;
        }

        deleteMutation.mutate(userToDelete.id);
    }

    return (
        <Stack spacing={3}>
            <PageHeader
                title="Kullanıcı Yönetimi"
                description="Sistem kullanıcılarını oluşturun, arayın, güncelleyin ve yönetin."
                action={(
                    <Button
                        variant="contained"
                        startIcon={<PersonAddAltOutlinedIcon />}
                        onClick={() => setFormMode({
                            type: 'create',
                            user: null,
                        })}
                    >
                        Yeni Kullanıcı
                    </Button>
                )}
            />

            <Stack
                direction={{ xs: 'column', md: 'row' }}
                spacing={2}
                sx={{ alignItems: { xs: 'stretch', md: 'center' } }}
            >
                <TextField
                    label="Arama"
                    placeholder="Ad, soyad, e-posta veya telefon"
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
                    {userPageSizes.map((pageSize) => (
                        <MenuItem key={pageSize} value={pageSize}>
                            {pageSize}
                        </MenuItem>
                    ))}
                </TextField>

                {!usersQuery.isError && (
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
                            Toplam {filteredUsers.length} kayıt
                        </Typography>
                    </Box>
                )}
            </Stack>

            {usersQuery.isError && (
                <Alert
                    severity="error"
                    action={(
                        <Button
                            color="inherit"
                            size="small"
                            onClick={() => void usersQuery.refetch()}
                        >
                            Tekrar dene
                        </Button>
                    )}
                >
                    {getApiErrorMessage(
                        usersQuery.error,
                        'Kullanıcı listesi alınamadı',
                    )}
                </Alert>
            )}


            {showEmptyState ? (
                <EmptyState
                    title="Kullanıcı kaydı bulunamadı"
                    description={
                        debouncedSearch
                            ? 'Arama ölçütlerini değiştirerek tekrar deneyin.'
                            : 'Yeni kullanıcı ekleyerek listeyi oluşturmaya başlayın.'
                    }
                />
            ) : (
                <Box sx={{ opacity: usersQuery.isFetching ? 0.82 : 1 }}>
                    <UserTable
                        users={pagedUsers}
                        page={page}
                        totalPages={totalPages}
                        isLoading={usersQuery.isLoading}
                        isMutating={isMutating}
                        onPageChange={setPage}
                        onEdit={(user) => setFormMode({
                            type: 'edit',
                            user,
                        })}
                        onDelete={(user) => {
                            setDeleteError(null);
                            setUserToDelete(user);
                        }}
                    />
                </Box>
            )}

            <UserFormDialog
                key={
                    formMode
                        ? `${formMode.type}-${formMode.user?.id ?? 'new'}`
                        : 'closed'
                }
                open={Boolean(formMode)}
                user={formMode?.user ?? null}
                isSaving={isSaving}
                onClose={() => {
                    if (!isSaving) {
                        setFormMode(null);
                    }
                }}
                onSubmit={handleFormSubmit}
            />

            <ConfirmDialog
                open={Boolean(userToDelete)}
                title="Kullanıcıyı sil"
                description={
                    userToDelete
                        ? `"${getFullName(userToDelete)}" kullanıcısı silinecek. Bu işlem geri alınamaz.`
                        : ''
                }
                confirmLabel="Sil"
                isConfirming={deleteMutation.isPending}
                errorMessage={deleteError}
                onClose={() => {
                    if (!deleteMutation.isPending) {
                        setUserToDelete(null);
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
