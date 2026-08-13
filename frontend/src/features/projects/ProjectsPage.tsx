import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { Alert, Box, Button, Skeleton, Snackbar, Stack } from '@mui/material';
import { useState } from 'react';
import NoteAddOutlinedIcon from '@mui/icons-material/NoteAddOutlined';
import { EmptyState } from '../../components/common/EmptyState';
import { ResultSummary } from '../../components/common/ResultSummary';
import { getApiErrorMessage } from '../../utils/getApiErrorMessage';
import { getProjects } from './projectApi';
import { projectQueryKeys } from './projectQueryKeys';
import { ProjectTable } from './components/ProjectTable';
import { ProjectUploadDialog } from './components/ProjectUploadDialog';

export function ProjectsPage() {
    const [page, setPage] = useState(0);
    const [size, setSize] = useState(10);
    const [isUploadDialogOpen, setUploadDialogOpen] = useState(false);
    const [creationSuccessOpen, setCreationSuccessOpen] = useState(false);
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
        </Stack>
    );
}
