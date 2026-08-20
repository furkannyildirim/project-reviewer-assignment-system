import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
    Alert,
    Box,
    Button,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    Skeleton,
    Snackbar,
    Stack,
    Typography,
} from '@mui/material';
import { useState } from 'react';
import { useNavigate } from 'react-router';
import NoteAddOutlinedIcon from '@mui/icons-material/NoteAddOutlined';
import { EmptyState } from '../../components/common/EmptyState';
import { ResultSummary } from '../../components/common/ResultSummary';
import type { PageResponse } from '../../types/api';
import { getApiErrorMessage } from '../../utils/getApiErrorMessage';
import { academicianQueryKeys } from '../academicians/academicianQueryKeys';
import { companyQueryKeys } from '../companies/companyQueryKeys';
import { deleteProject, getProjects } from './projectApi';
import { projectQueryKeys } from './projectQueryKeys';
import type { ProjectListItem } from './project.types';
import { ProjectTable } from './components/ProjectTable';
import { ProjectUploadDialog } from './components/ProjectUploadDialog';

export function ProjectsPage() {
    const navigate = useNavigate();
    const queryClient = useQueryClient();
    const [page, setPage] = useState(0);
    const [size, setSize] = useState(10);
    const [isUploadDialogOpen, setUploadDialogOpen] = useState(false);
    const [creationSuccessOpen, setCreationSuccessOpen] = useState(false);
    const [projectToDelete, setProjectToDelete] = useState<ProjectListItem | null>(null);
    const [deleteError, setDeleteError] = useState<string | null>(null);
    const [deletionSuccessOpen, setDeletionSuccessOpen] = useState(false);
    const queryParams = { page, size };
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
                        {projectsQuery.isLoading && !projectsQuery.data ? (
                            <Skeleton height={40} variant="rounded" width={110} />
                        ) : (
                            <ResultSummary
                                filteredCount={projectsQuery.data?.totalElements ?? 0}
                                totalCount={projectsQuery.data?.totalElements ?? 0}
                                hasActiveFilter={false}
                                entityLabel="proje"
                            />
                        )}
                    </Stack>
                </Stack>
            </Box>

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
                    onSizeChange={(nextSize) => {
                        setSize(nextSize);
                        setPage(0);
                    }}
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
                                ? `“${projectToDelete.name}” projesi, proje anahtar kelimeleri, varsa hakem atama geçmişi ve saklanan kaynak belge kalıcı olarak kaldırılacaktır. Bu işlem geri alınamaz.`
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
