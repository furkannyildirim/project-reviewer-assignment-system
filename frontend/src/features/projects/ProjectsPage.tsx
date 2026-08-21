import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
    Alert,
    Box,
    Button,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    InputAdornment,
    MenuItem,
    Skeleton,
    Snackbar,
    Stack,
    TextField,
    Typography,
} from '@mui/material';
import { useState } from 'react';
import { useNavigate } from 'react-router';
import NoteAddOutlinedIcon from '@mui/icons-material/NoteAddOutlined';
import SearchOutlinedIcon from '@mui/icons-material/SearchOutlined';
import { EmptyState } from '../../components/common/EmptyState';
import { ResultSummary } from '../../components/common/ResultSummary';
import { useDebouncedValue } from '../../hooks/useDebouncedValue';
import type { PageResponse } from '../../types/api';
import { getApiErrorMessage } from '../../utils/getApiErrorMessage';
import { academicianQueryKeys } from '../academicians/academicianQueryKeys';
import { companyQueryKeys } from '../companies/companyQueryKeys';
import { deleteProject, getProjects } from './projectApi';
import { projectQueryKeys } from './projectQueryKeys';
import { projectPageSizes, type ProjectListItem } from './project.types';
import { ProjectTable } from './components/ProjectTable';
import { ProjectUploadDialog } from './components/ProjectUploadDialog';
import { ManualProjectDialog } from './components/ManualProjectDialog';

export function ProjectsPage() {
    const navigate = useNavigate();
    const queryClient = useQueryClient();
    const [search, setSearch] = useState('');
    const [page, setPage] = useState(0);
    const [size, setSize] = useState(10);
    const [isUploadDialogOpen, setUploadDialogOpen] = useState(false);
    const [isManualDialogOpen, setManualDialogOpen] = useState(false);
    const [creationSuccessOpen, setCreationSuccessOpen] = useState(false);
    const [projectToDelete, setProjectToDelete] = useState<ProjectListItem | null>(null);
    const [deleteError, setDeleteError] = useState<string | null>(null);
    const [deletionSuccessOpen, setDeletionSuccessOpen] = useState(false);
    const debouncedSearch = useDebouncedValue(search.trim(), 400);
    const queryParams = { page, size, search: debouncedSearch || undefined };
    const projectsQuery = useQuery({
        queryKey: projectQueryKeys.list(queryParams),
        queryFn: () => getProjects(queryParams),
        placeholderData: keepPreviousData,
    });
    const projects = projectsQuery.data?.content ?? [];
    const showEmptyState = !projectsQuery.isLoading
        && !projectsQuery.isError
        && projects.length === 0;
    const deleteMutation = useMutation({
        mutationFn: deleteProject,
        onMutate: async (projectId) => {
            await queryClient.cancelQueries({
                queryKey: projectQueryKeys.list(queryParams),
            });
            const previousList = queryClient.getQueryData<PageResponse<ProjectListItem>>(
                projectQueryKeys.list(queryParams),
            );
            const wasLastItemOnPage = previousList?.content.length === 1;

            queryClient.setQueryData<PageResponse<ProjectListItem>>(
                projectQueryKeys.list(queryParams),
                (currentList) => {
                    if (!currentList || !currentList.content.some((project) => project.id === projectId)) {
                        return currentList;
                    }

                    const totalElements = Math.max(currentList.totalElements - 1, 0);
                    return {
                        ...currentList,
                        content: currentList.content.filter((project) => project.id !== projectId),
                        totalElements,
                        totalPages: Math.ceil(totalElements / currentList.size),
                    };
                },
            );

            return { previousList, wasLastItemOnPage };
        },
        onError: (error, _projectId, context) => {
            queryClient.setQueryData(
                projectQueryKeys.list(queryParams),
                context?.previousList,
            );
            setDeleteError(getApiErrorMessage(
                error,
                'Proje silinemedi. Lütfen tekrar deneyin.',
            ));
        },
        onSuccess: async (_response, projectId, context) => {
            setProjectToDelete(null);
            setDeleteError(null);
            setDeletionSuccessOpen(true);

            if (context?.wasLastItemOnPage && page > 0) {
                setPage((currentPage) => currentPage - 1);
            }

            queryClient.removeQueries({ queryKey: projectQueryKeys.detail(projectId), exact: true });
            queryClient.removeQueries({ queryKey: projectQueryKeys.reviewerRecommendations(projectId), exact: true });
            queryClient.removeQueries({ queryKey: projectQueryKeys.activeReviewerAssignments(projectId), exact: true });
            queryClient.removeQueries({ queryKey: projectQueryKeys.reviewerAssignmentHistory(projectId), exact: true });

            await Promise.all([
                queryClient.invalidateQueries({ queryKey: projectQueryKeys.lists() }),
                queryClient.invalidateQueries({
                    queryKey: companyQueryKeys.projectSummaries(),
                }),
                queryClient.invalidateQueries({
                    queryKey: academicianQueryKeys.all,
                }),
            ]);
        },
    });

    function handleDeleteConfirm() {
        if (!projectToDelete || deleteMutation.isPending) {
            return;
        }

        deleteMutation.mutate(projectToDelete.id);
    }

    function handleSearchChange(value: string) {
        setSearch(value);
        setPage(0);
    }

    function handleSizeChange(nextSize: number) {
        setSize(nextSize);
        setPage(0);
    }

    return (
        <Stack spacing={3}>
            <Box
                sx={{
                    bgcolor: 'background.default',
                    borderBottom: '1px solid #E3EAF0',
                    position: 'sticky',
                    pt: { xs: 2.5, sm: 3, lg: 4 },
                    top: 0,
                    zIndex: (theme) => theme.zIndex.appBar - 1,
                }}
            >
                <Stack
                    direction={{ xs: 'column', sm: 'row' }}
                    spacing={2}
                    sx={{ alignItems: { xs: 'stretch', sm: 'center' }, justifyContent: 'space-between', pb: 3 }}
                >
                    <Stack direction="row" spacing={1.75} sx={{ alignItems: 'center', minWidth: 0 }}>
                        <Box aria-hidden="true" sx={{
                            bgcolor: '#2EA8D3', borderRadius: '4px', flex: '0 0 auto', height: 52, width: 4,
                        }} />
                        <Box sx={{ minWidth: 0 }}>
                            <Box component="h1" sx={{ fontSize: { xs: 26, sm: 30 }, fontWeight: 700, lineHeight: 1.2, m: 0 }}>
                                Proje Yönetimi
                            </Box>
                            <Box component="p" sx={{ color: 'text.secondary', m: 0, mt: 0.75 }}>
                                Projeleri görüntüleyin, durumlarını ve hakem atamalarını takip edin.
                            </Box>
                        </Box>
                    </Stack>
                    <Stack
                        direction={{ xs: 'column', sm: 'row' }}
                        spacing={1}
                        sx={{ alignItems: { xs: 'stretch', sm: 'center' } }}
                    >
                        <Button
                            onClick={() => setUploadDialogOpen(true)}
                            startIcon={<NoteAddOutlinedIcon />}
                            variant="outlined"
                            sx={{
                                borderColor: '#2A2B61',
                                color: '#2A2B61',
                                minHeight: 40,
                                '&:hover': { bgcolor: '#F2F4F7', borderColor: '#202153' },
                            }}
                        >
                            PDF'den Yükle
                        </Button>
                        <Button
                            onClick={() => setManualDialogOpen(true)}
                            startIcon={<NoteAddOutlinedIcon />}
                            variant="contained"
                            sx={{
                                bgcolor: '#2A2B61',
                                color: '#FFFFFF',
                                minHeight: 40,
                                '&:hover': { bgcolor: '#202153' },
                                '&:focus-visible': {
                                    outline: '2px solid #2EA8D3',
                                    outlineOffset: 2,
                                    boxShadow: '0 0 0 3px rgba(46, 168, 211, 0.18)',
                                },
                            }}
                        >
                            Yeni Proje
                        </Button>
                    </Stack>
                </Stack>
            </Box>

            <Stack
                direction={{ xs: 'column', md: 'row' }}
                spacing={2}
                sx={{
                    alignItems: { xs: 'stretch', md: 'center' },
                    '& .MuiInputLabel-root.Mui-focused': { color: '#2A2B61' },
                    '& .MuiOutlinedInput-root': {
                        bgcolor: '#FFFFFF',
                        borderRadius: '10px',
                        minHeight: 44,
                        '& .MuiOutlinedInput-notchedOutline': { borderColor: '#D9E2EA' },
                        '&:hover .MuiOutlinedInput-notchedOutline': { borderColor: '#B8C7D4' },
                        '&.Mui-focused': { boxShadow: '0 0 0 3px rgba(42, 43, 97, 0.09)' },
                        '&.Mui-focused .MuiInputAdornment-root .MuiSvgIcon-root': { color: '#2A2B61' },
                        '&.Mui-focused .MuiOutlinedInput-notchedOutline': {
                            borderColor: '#2A2B61',
                            borderWidth: 1,
                        },
                    },
                }}
            >
                <TextField
                    label="Arama"
                    onChange={(event) => handleSearchChange(event.target.value)}
                    placeholder="Proje ara..."
                    slotProps={{
                        input: {
                            startAdornment: (
                                <InputAdornment position="start">
                                    <SearchOutlinedIcon sx={{ color: '#728196', fontSize: 20 }} />
                                </InputAdornment>
                            ),
                        },
                    }}
                    sx={{ flexGrow: 1 }}
                    value={search}
                />
                <TextField
                    select
                    label="Satır sayısı"
                    onChange={(event) => handleSizeChange(Number(event.target.value))}
                    sx={{ minWidth: { xs: '100%', md: 160 } }}
                    value={size}
                >
                    {projectPageSizes.map((pageSize) => (
                        <MenuItem key={pageSize} value={pageSize}>
                            {pageSize}
                        </MenuItem>
                    ))}
                </TextField>
                {projectsQuery.isLoading && !projectsQuery.data ? (
                    <Skeleton height={48} variant="rounded" width={120} />
                ) : (
                    <Box sx={{ '& > div': { fontSize: 16, justifyContent: 'center', minHeight: 46, minWidth: 132, px: 2.5 } }}>
                        <ResultSummary
                            filteredCount={projectsQuery.data?.totalElements ?? 0}
                            totalCount={projectsQuery.data?.totalElements ?? 0}
                            hasActiveFilter={false}
                            entityLabel="proje"
                        />
                    </Box>
                )}
            </Stack>

            {projectsQuery.isError ? (
                <Alert
                    action={<Button color="inherit" size="small" onClick={() => projectsQuery.refetch()}>Tekrar Dene</Button>}
                    severity="error"
                >
                    {getApiErrorMessage(projectsQuery.error, 'Projeler şu anda yüklenemedi.')}
                </Alert>
            ) : showEmptyState ? (
                <EmptyState
                    title="Henüz proje kaydı bulunmuyor."
                    description="Proje kayıtları burada listelenecek."
                />
            ) : (
                <ProjectTable
                    data={projectsQuery.data}
                    isLoading={projectsQuery.isLoading}
                    page={page}
                    size={size}
                    onPageChange={setPage}
                    onView={(project) => navigate(`/projects/${project.id}`)}
                    onDelete={(project) => {
                        setDeleteError(null);
                        setProjectToDelete(project);
                    }}
                    isDeleting={deleteMutation.isPending}
                />
            )}
            <ProjectUploadDialog
                open={isUploadDialogOpen}
                onClose={() => setUploadDialogOpen(false)}
                onCreated={() => setCreationSuccessOpen(true)}
            />
            <ManualProjectDialog
                open={isManualDialogOpen}
                onClose={() => setManualDialogOpen(false)}
                onCreated={() => setCreationSuccessOpen(true)}
            />
            <Snackbar
                autoHideDuration={4000}
                onClose={() => setCreationSuccessOpen(false)}
                open={creationSuccessOpen}
            >
                <Alert severity="success" variant="filled" onClose={() => setCreationSuccessOpen(false)}>
                    Proje başarıyla oluşturuldu.
                </Alert>
            </Snackbar>
            <Dialog
                open={Boolean(projectToDelete)}
                onClose={deleteMutation.isPending ? undefined : () => {
                    if (!deleteMutation.isPending) {
                        setProjectToDelete(null);
                        setDeleteError(null);
                    }
                }}
                fullWidth
                maxWidth="xs"
            >
                <DialogTitle>Projeyi sil</DialogTitle>
                <DialogContent>
                    <Stack spacing={2}>
                        {deleteError && <Alert severity="error">{deleteError}</Alert>}
                        <Typography color="text.secondary">
                            {projectToDelete
                                ? `“${projectToDelete.name}” projesi, proje anahtar kelimeleri ve varsa hakem atama geçmişi kalıcı olarak kaldırılacaktır. Bu işlem geri alınamaz.`
                                : ''}
                        </Typography>
                    </Stack>
                </DialogContent>
                <DialogActions>
                    <Button
                        disabled={deleteMutation.isPending}
                        onClick={() => {
                            if (!deleteMutation.isPending) {
                                setProjectToDelete(null);
                                setDeleteError(null);
                            }
                        }}
                        sx={{
                            color: '#526273',
                            '&:hover': { bgcolor: '#F2F4F7', color: '#344054' },
                        }}
                    >
                        Vazgeç
                    </Button>
                    <Button
                        color="error"
                        disabled={deleteMutation.isPending}
                        loading={deleteMutation.isPending}
                        onClick={handleDeleteConfirm}
                        variant="contained"
                    >
                        Projeyi Sil
                    </Button>
                </DialogActions>
            </Dialog>
            <Snackbar
                autoHideDuration={4000}
                onClose={() => setDeletionSuccessOpen(false)}
                open={deletionSuccessOpen}
            >
                <Alert severity="success" variant="filled" onClose={() => setDeletionSuccessOpen(false)}>
                    Proje başarıyla silindi.
                </Alert>
            </Snackbar>
        </Stack>
    );
}
