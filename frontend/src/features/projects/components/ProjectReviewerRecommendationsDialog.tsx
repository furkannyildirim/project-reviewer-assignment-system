import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
    Alert,
    Box,
    Button,
    Chip,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    Paper,
    Skeleton,
    Stack,
    Typography,
} from '@mui/material';
import { useState } from 'react';
import { getApiErrorMessage } from '../../../utils/getApiErrorMessage';
import {
    assignProjectReviewer,
    getActiveProjectReviewerAssignments,
    getProjectReviewerRecommendations,
} from '../projectApi';
import { projectQueryKeys } from '../projectQueryKeys';
import type {
    ReviewerRecommendation,
    ReviewerRecommendationLevel,
} from '../project.types';

type ProjectReviewerRecommendationsDialogProps = {
    open: boolean;
    projectId: number;
    projectName: string;
    onClose: () => void;
    onAssigned: () => void;
};

const levelPresentation: Record<ReviewerRecommendationLevel, {
    label: string;
    backgroundColor: string;
    color: string;
    borderColor: string;
}> = {
    HIGH: {
        label: 'Yüksek uyum',
        backgroundColor: '#EAF7FB',
        color: '#176B8A',
        borderColor: '#B9DFEC',
    },
    MEDIUM: {
        label: 'Orta uyum',
        backgroundColor: '#F7F4E8',
        color: '#735C12',
        borderColor: '#E8DDAF',
    },
    LOW: {
        label: 'Düşük uyum',
        backgroundColor: '#F5F7F9',
        color: '#526273',
        borderColor: '#D9E2EA',
    },
};

function RecommendationCard({
    recommendation,
    assignmentDisabled,
    isAssigning,
    onAssign,
}: {
    recommendation: ReviewerRecommendation;
    assignmentDisabled: boolean;
    isAssigning: boolean;
    onAssign: () => void;
}) {
    const presentation = levelPresentation[recommendation.level];
    return (
        <Paper
            elevation={0}
            sx={{ border: '1px solid #D9E2EA', borderRadius: '10px', bgcolor: '#FFFFFF', p: 2 }}
        >
            <Stack spacing={1.5}>
                <Stack
                    direction={{ xs: 'column', sm: 'row' }}
                    spacing={1}
                    sx={{ alignItems: { sm: 'flex-start' }, justifyContent: 'space-between' }}
                >
                    <Box sx={{ minWidth: 0 }}>
                        <Typography sx={{ color: '#2A2B61', fontSize: 16, fontWeight: 700, lineHeight: 1.4 }}>
                            {recommendation.firstName} {recommendation.lastName}
                        </Typography>
                        {recommendation.title?.trim() && (
                            <Typography color="text.secondary" sx={{ fontSize: 14, mt: 0.25 }}>
                                {recommendation.title}
                            </Typography>
                        )}
                    </Box>
                    <Chip
                        label={presentation.label}
                        size="small"
                        variant="outlined"
                        sx={{
                            alignSelf: { xs: 'flex-start', sm: 'center' },
                            bgcolor: presentation.backgroundColor,
                            borderColor: presentation.borderColor,
                            color: presentation.color,
                            fontWeight: 700,
                        }}
                    />
                </Stack>

                <Stack direction="row" spacing={0.75} useFlexGap sx={{ flexWrap: 'wrap' }}>
                    {recommendation.matchedKeywords.map((keyword) => (
                        <Chip
                            key={keyword}
                            label={keyword}
                            size="small"
                            sx={{ bgcolor: '#F2F7FA', color: '#526273', height: 25 }}
                        />
                    ))}
                </Stack>

                <Stack direction="row" spacing={2} useFlexGap sx={{ flexWrap: 'wrap' }}>
                    <Typography color="text.secondary" sx={{ fontSize: 14 }}>
                        Aktif proje: {recommendation.activeProjectCount}
                    </Typography>
                    <Typography color="text.secondary" sx={{ fontSize: 14 }}>
                        Toplam proje: {recommendation.totalProjectCount}
                    </Typography>
                </Stack>
                <Button
                    disabled={assignmentDisabled}
                    loading={isAssigning}
                    onClick={onAssign}
                    variant="contained"
                    sx={{
                        alignSelf: { xs: 'stretch', sm: 'flex-start' },
                        bgcolor: '#2A2B61',
                        color: '#FFFFFF',
                        minHeight: 40,
                        '&:hover': { bgcolor: '#202153' },
                        '&:focus-visible': {
                            outline: '2px solid #2EA8D3',
                            outlineOffset: 2,
                        },
                    }}
                >
                    Hakem Olarak Ata
                </Button>
            </Stack>
        </Paper>
    );
}

function LoadingState() {
    return (
        <Stack spacing={1.5}>
            {[0, 1, 2].map((item) => (
                <Skeleton key={item} height={150} variant="rounded" />
            ))}
        </Stack>
    );
}

export function ProjectReviewerRecommendationsDialog({
    open,
    projectId,
    projectName,
    onClose,
    onAssigned,
}: ProjectReviewerRecommendationsDialogProps) {
    const queryClient = useQueryClient();
    const [selectedRecommendation, setSelectedRecommendation] =
        useState<ReviewerRecommendation | null>(null);
    const [assignmentError, setAssignmentError] = useState<string | null>(null);
    const query = useQuery({
        queryKey: projectQueryKeys.reviewerRecommendations(projectId),
        queryFn: () => getProjectReviewerRecommendations(projectId),
        enabled: open,
    });
    const assignmentsQuery = useQuery({
        queryKey: projectQueryKeys.activeReviewerAssignments(projectId),
        queryFn: () => getActiveProjectReviewerAssignments(projectId),
        enabled: open,
    });
    const assignmentMutation = useMutation({
        mutationFn: (academicianId: number) => assignProjectReviewer(
            projectId,
            academicianId,
        ),
        onSuccess: async () => {
            await Promise.all([
                queryClient.invalidateQueries({
                    queryKey: projectQueryKeys.activeReviewerAssignments(projectId),
                }),
                queryClient.invalidateQueries({
                    queryKey: projectQueryKeys.reviewerRecommendations(projectId),
                }),
                queryClient.invalidateQueries({
                    queryKey: projectQueryKeys.detail(projectId),
                }),
                queryClient.invalidateQueries({
                    queryKey: projectQueryKeys.lists(),
                }),
            ]);
            setSelectedRecommendation(null);
            setAssignmentError(null);
            onAssigned();
        },
        onError: (error) => {
            setAssignmentError(getApiErrorMessage(
                error,
                'Hakem atanamadı. Lütfen tekrar deneyin.',
            ));
        },
    });
    const recommendations = query.data?.recommendations ?? [];
    const activeAssignmentCount = assignmentsQuery.data?.length ?? 0;
    const assignmentLimitReached = activeAssignmentCount >= 2;
    const assignmentAvailable = assignmentsQuery.isSuccess && !assignmentLimitReached;

    function handleClose() {
        if (assignmentMutation.isPending) return;
        setSelectedRecommendation(null);
        setAssignmentError(null);
        assignmentMutation.reset();
        onClose();
    }

    function selectRecommendation(recommendation: ReviewerRecommendation) {
        if (!assignmentAvailable || assignmentMutation.isPending) return;
        setSelectedRecommendation(recommendation);
        setAssignmentError(null);
        assignmentMutation.reset();
    }

    return (
        <>
        <Dialog fullWidth maxWidth="md" open={open} onClose={handleClose}>
            <DialogTitle sx={{ borderBottom: '1px solid #E3EAF0', pb: 2 }}>
                <Typography component="h2" sx={{ color: '#2A2B61', fontSize: 21, fontWeight: 700 }}>
                    Hakem Önerileri
                </Typography>
                <Typography color="text.secondary" sx={{ fontSize: 14, mt: 0.5 }}>
                    Proje içeriği ile akademisyen anahtar kelimelerine göre hazırlanmıştır.
                </Typography>
            </DialogTitle>

            <DialogContent dividers sx={{ bgcolor: '#F7FAFC', overflowY: 'auto', py: 2.5 }}>
                <Stack spacing={1.5}>
                {assignmentLimitReached && (
                    <Alert severity="info" variant="outlined" sx={{ bgcolor: '#F7FAFC' }}>
                        Bu projeye en fazla iki aktif hakem atanabilir.
                    </Alert>
                )}
                {assignmentsQuery.isError && (
                    <Alert
                        severity="error"
                        action={(
                            <Button
                                color="inherit"
                                size="small"
                                onClick={() => void assignmentsQuery.refetch()}
                            >
                                Tekrar Dene
                            </Button>
                        )}
                    >
                        Aktif hakem durumu alınamadığı için atama yapılamıyor.
                    </Alert>
                )}
                {query.isLoading ? (
                    <LoadingState />
                ) : query.isError ? (
                    <Alert
                        severity="error"
                        action={(
                            <Button color="inherit" size="small" onClick={() => void query.refetch()}>
                                Tekrar Dene
                            </Button>
                        )}
                    >
                        Hakem önerileri şu anda yüklenemedi.
                    </Alert>
                ) : recommendations.length === 0 ? (
                    <Paper elevation={0} sx={{ border: '1px solid #D9E2EA', borderRadius: '10px', p: 2.5 }}>
                        <Typography sx={{ color: '#2A2B61', fontWeight: 700 }}>
                            Bu proje için anahtar kelimelere dayalı bir hakem önerisi bulunamadı.
                        </Typography>
                        <Typography color="text.secondary" sx={{ fontSize: 14, lineHeight: 1.5, mt: 0.75 }}>
                            Proje ve akademisyen anahtar kelimelerinin güncelliğini kontrol edebilirsiniz.
                        </Typography>
                    </Paper>
                ) : (
                    <Stack spacing={1.5}>
                        {!query.data?.strongRecommendationAvailable && (
                            <Alert severity="warning" variant="outlined" sx={{ bgcolor: '#FFFCF5' }}>
                                Güçlü bir hakem önerisi bulunamadı. Mevcut eşleşmeleri inceleyebilirsiniz.
                            </Alert>
                        )}
                        {recommendations.map((recommendation) => (
                            <RecommendationCard
                                key={recommendation.academicianId}
                                recommendation={recommendation}
                                assignmentDisabled={!assignmentAvailable
                                    || assignmentMutation.isPending}
                                isAssigning={assignmentMutation.isPending
                                    && selectedRecommendation?.academicianId
                                    === recommendation.academicianId}
                                onAssign={() => selectRecommendation(recommendation)}
                            />
                        ))}
                    </Stack>
                )}
                </Stack>
            </DialogContent>

            <DialogActions sx={{ borderTop: '1px solid #E3EAF0', px: 3, py: 1.5 }}>
                <Button
                    disabled={assignmentMutation.isPending}
                    onClick={handleClose}
                    sx={{
                        bgcolor: '#F2F4F7',
                        color: '#344054',
                        '&:hover': { bgcolor: '#E4E7EC' },
                        '&:focus-visible': { outline: '2px solid #2EA8D3', outlineOffset: 2 },
                    }}
                >
                    Kapat
                </Button>
            </DialogActions>
        </Dialog>
        <Dialog
            fullWidth
            maxWidth="xs"
            open={selectedRecommendation !== null}
            onClose={assignmentMutation.isPending ? undefined : () => {
                setSelectedRecommendation(null);
                setAssignmentError(null);
                assignmentMutation.reset();
            }}
        >
            <DialogTitle sx={{ color: '#2A2B61', fontWeight: 700 }}>
                Hakem atamasını onayla
            </DialogTitle>
            <DialogContent>
                <Stack spacing={2} sx={{ pt: 0.5 }}>
                    {assignmentError && <Alert severity="error">{assignmentError}</Alert>}
                    <Box>
                        <Typography sx={{ color: '#2A2B61', fontWeight: 700 }}>
                            {selectedRecommendation?.firstName}{' '}
                            {selectedRecommendation?.lastName}
                        </Typography>
                        <Typography color="text.secondary" sx={{ fontSize: 14, mt: 0.25 }}>
                            {selectedRecommendation?.title?.trim() || 'Ünvan bilgisi bulunmuyor'}
                        </Typography>
                    </Box>
                    <Typography color="text.secondary" sx={{ lineHeight: 1.6 }}>
                        Bu akademisyen “{projectName}” projesine hakem olarak atanacaktır.
                        Devam etmek için işlemi onaylayın.
                    </Typography>
                </Stack>
            </DialogContent>
            <DialogActions sx={{ px: 3, pb: 2.5 }}>
                <Button
                    disabled={assignmentMutation.isPending}
                    onClick={() => {
                        setSelectedRecommendation(null);
                        setAssignmentError(null);
                        assignmentMutation.reset();
                    }}
                    sx={{
                        bgcolor: '#F2F4F7',
                        color: '#344054',
                        '&:hover': { bgcolor: '#E4E7EC' },
                    }}
                >
                    Vazgeç
                </Button>
                <Button
                    disabled={!selectedRecommendation || assignmentLimitReached}
                    loading={assignmentMutation.isPending}
                    onClick={() => {
                        if (selectedRecommendation) {
                            assignmentMutation.mutate(selectedRecommendation.academicianId);
                        }
                    }}
                    variant="contained"
                    sx={{
                        bgcolor: '#2A2B61',
                        color: '#FFFFFF',
                        '&:hover': { bgcolor: '#202153' },
                    }}
                >
                    Atamayı Onayla
                </Button>
            </DialogActions>
        </Dialog>
        </>
    );
}
