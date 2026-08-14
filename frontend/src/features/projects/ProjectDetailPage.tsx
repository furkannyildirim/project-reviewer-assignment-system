import { useQuery } from '@tanstack/react-query';
import ArrowBackOutlinedIcon from '@mui/icons-material/ArrowBackOutlined';
import BusinessOutlinedIcon from '@mui/icons-material/BusinessOutlined';
import CalendarMonthOutlinedIcon from '@mui/icons-material/CalendarMonthOutlined';
import DescriptionOutlinedIcon from '@mui/icons-material/DescriptionOutlined';
import DownloadOutlinedIcon from '@mui/icons-material/DownloadOutlined';
import EngineeringOutlinedIcon from '@mui/icons-material/EngineeringOutlined';
import VisibilityOutlinedIcon from '@mui/icons-material/VisibilityOutlined';
import {
    Alert,
    Box,
    Button,
    Chip,
    CircularProgress,
    Paper,
    Skeleton,
    Snackbar,
    Stack,
    Typography,
} from '@mui/material';
import axios from 'axios';
import { useState, type ReactNode } from 'react';
import { useNavigate, useParams } from 'react-router';
import { getApiErrorMessage } from '../../utils/getApiErrorMessage';
import {
    downloadProjectDocument,
    getActiveProjectReviewerAssignments,
    getProjectById,
} from './projectApi';
import { projectQueryKeys } from './projectQueryKeys';
import type { ProjectCompanyType, ProjectResponse, ProjectStatus } from './project.types';
import { ProjectReviewerRecommendationsDialog } from './components/ProjectReviewerRecommendationsDialog';

const missingValue = 'Bilgi girilmemiş';
const backButtonSx = {
    bgcolor: '#2A2B61',
    color: '#FFFFFF',
    borderRadius: '8px',
    minHeight: 40,
    px: 2,
    '&:hover': { bgcolor: '#202153' },
    '&:focus-visible': {
        outline: '2px solid #2EA8D3',
        outlineOffset: 2,
        boxShadow: '0 0 0 3px rgba(46, 168, 211, 0.18)',
    },
};
const statusLabels: Record<ProjectStatus, string> = {
    DRAFT: 'Taslak',
    AWAITING_REVIEWER: 'Hakem bekliyor',
    ACTIVE: 'Aktif',
    COMPLETED: 'Tamamlandı',
    CANCELLED: 'İptal edildi',
};

function companyTypeLabel(type: ProjectCompanyType) {
    return type === 'TGB' ? 'TGB Şirketi' : 'Dış Şirket';
}

function formatDate(value: string | null | undefined) {
    if (!value) return missingValue;
    const date = new Date(`${value.slice(0, 10)}T00:00:00`);
    return Number.isNaN(date.getTime()) ? missingValue : new Intl.DateTimeFormat('tr-TR', {
        day: '2-digit', month: 'short', year: 'numeric',
    }).format(date);
}

function formatDateTime(value: string | null | undefined) {
    if (!value) return missingValue;
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? missingValue : new Intl.DateTimeFormat('tr-TR', {
        dateStyle: 'medium',
        timeStyle: 'short',
    }).format(date);
}

function display(value: string | null | undefined) {
    return value?.trim() || missingValue;
}

function fileTypeLabel(contentType: string | null) {
    if (contentType === 'application/pdf') return 'PDF';
    if (contentType === 'application/vnd.openxmlformats-officedocument.wordprocessingml.document') return 'DOCX';
    return 'Dosya';
}

function formatFileSize(bytes: number | null) {
    if (bytes === null || !Number.isFinite(bytes) || bytes < 0) return missingValue;
    if (bytes < 1024) return `${bytes.toLocaleString('tr-TR')} B`;
    const units = ['KB', 'MB'];
    let value = bytes / 1024;
    let unitIndex = 0;
    while (value >= 1024 && unitIndex < units.length - 1) {
        value /= 1024;
        unitIndex += 1;
    }
    return `${value.toLocaleString('tr-TR', { maximumFractionDigits: 1 })} ${units[unitIndex]}`;
}

function DetailCard({ title, icon, children }: { title: string; icon: ReactNode; children: ReactNode }) {
    return (
        <Paper elevation={0} sx={{ border: '1px solid #D9E2EA', borderRadius: '12px', bgcolor: '#FFFFFF', p: { xs: 2, sm: 2.5 } }}>
            <Stack direction="row" spacing={1} sx={{ alignItems: 'center', mb: 2 }}>
                <Box sx={{ color: '#2A2B61', display: 'flex' }}>{icon}</Box>
                <Typography sx={{ color: '#2A2B61', fontSize: { xs: 18, sm: 19 }, fontWeight: 700 }}>{title}</Typography>
            </Stack>
            {children}
        </Paper>
    );
}

function Field({ label, value }: { label: string; value: string | null | undefined }) {
    const hasValue = Boolean(value?.trim());
    return (
        <Stack spacing={0.45} sx={{ minWidth: 0 }}>
            <Typography sx={{ color: '#526273', fontSize: 14, fontWeight: 600 }}>{label}</Typography>
            <Typography sx={{ color: hasValue ? '#1F2937' : '#7B8794', fontSize: { xs: 15, sm: 16 }, lineHeight: 1.5, overflowWrap: 'anywhere', whiteSpace: 'pre-wrap' }}>
                {display(value)}
            </Typography>
        </Stack>
    );
}

function LoadingState() {
    return <Stack spacing={2.5} sx={{ pt: { xs: 2, sm: 3 } }}><Skeleton height={110} variant="rounded" /><Skeleton height={240} variant="rounded" /><Skeleton height={240} variant="rounded" /></Stack>;
}

export function ProjectDetailPage() {
    const navigate = useNavigate();
    const [isDownloading, setIsDownloading] = useState(false);
    const [downloadErrorOpen, setDownloadErrorOpen] = useState(false);
    const [assignmentSuccessOpen, setAssignmentSuccessOpen] = useState(false);
    const [recommendationsOpen, setRecommendationsOpen] = useState(false);
    const { projectId } = useParams();
    const id = Number(projectId);
    const validId = Number.isInteger(id) && id > 0;
    const query = useQuery({
        queryKey: projectQueryKeys.detail(validId ? id : 0),
        queryFn: () => getProjectById(id),
        enabled: validId,
    });
    const assignmentsQuery = useQuery({
        queryKey: projectQueryKeys.activeReviewerAssignments(validId ? id : 0),
        queryFn: () => getActiveProjectReviewerAssignments(id),
        enabled: validId && query.isSuccess,
    });
    const project = query.data as ProjectResponse | undefined;
    const notFound = !validId || (axios.isAxiosError(query.error) && query.error.response?.status === 404);

    async function handleDocumentDownload() {
        if (!project || isDownloading) return;
        setIsDownloading(true);
        setDownloadErrorOpen(false);
        let objectUrl: string | null = null;
        try {
            const download = await downloadProjectDocument(project.id);
            objectUrl = URL.createObjectURL(download.blob);
            const anchor = document.createElement('a');
            anchor.href = objectUrl;
            anchor.download = download.fileName || project.originalFileName || 'proje-belgesi';
            document.body.appendChild(anchor);
            anchor.click();
            anchor.remove();
        } catch {
            setDownloadErrorOpen(true);
        } finally {
            if (objectUrl) URL.revokeObjectURL(objectUrl);
            setIsDownloading(false);
        }
    }

    if (query.isLoading) return <LoadingState />;
    if (notFound || query.isError) {
        return <Stack spacing={2.5} sx={{ maxWidth: 760, mx: 'auto', pt: { xs: 2, sm: 3 } }}>
            <Alert severity={notFound ? 'info' : 'error'}>{notFound ? 'Proje kaydı bulunamadı.' : getApiErrorMessage(query.error, 'Proje detayları yüklenemedi.')}</Alert>
            <Stack direction="row" spacing={1} useFlexGap sx={{ flexWrap: 'wrap' }}>
                {!notFound && <Button onClick={() => void query.refetch()}>Tekrar Dene</Button>}
                <Button variant="contained" startIcon={<ArrowBackOutlinedIcon />} onClick={() => navigate('/projects')} sx={backButtonSx}>Projelere Dön</Button>
            </Stack>
        </Stack>;
    }
    if (!project) return null;

    const companyPath = project.companyType === 'TGB' ? 'tgb' : 'external';
    const hasDownloadableDocument = Boolean(
        project.sourceFileId
        && project.originalFileName
        && project.contentType
        && project.fileSize !== null,
    );
    const activeAssignments = assignmentsQuery.data ?? [];
    const assignmentLimitReached = activeAssignments.length >= 2;
    const assignmentEligibleStatus = project.status === 'AWAITING_REVIEWER'
        || project.status === 'ACTIVE';
    const showRecommendationAction = assignmentEligibleStatus
        && assignmentsQuery.isSuccess
        && !assignmentLimitReached;
    return <Stack spacing={2.5} sx={{ maxWidth: 1280, mx: 'auto', pb: 3, pt: { xs: 2.5, sm: 3 } }}>
        <Paper elevation={0} sx={{ border: '1px solid #D9E2EA', borderRadius: '13px', bgcolor: '#FFFFFF', p: { xs: 2, sm: 3 } }}>
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5} sx={{ alignItems: { sm: 'center' }, justifyContent: 'space-between' }}>
                <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center', minWidth: 0 }}>
                    <Box aria-hidden="true" sx={{ bgcolor: '#2EA8D3', borderRadius: '4px', flex: '0 0 auto', height: 58, width: 4 }} />
                    <Box sx={{ minWidth: 0 }}>
                        <Typography component="h1" sx={{ color: '#2A2B61', fontSize: { xs: 24, sm: 30 }, fontWeight: 700, lineHeight: 1.2, overflowWrap: 'anywhere' }}>{project.name}</Typography>
                        <Stack direction="row" spacing={1} useFlexGap sx={{ alignItems: 'center', flexWrap: 'wrap', mt: 1 }}>
                            <Typography sx={{ color: '#526273', fontSize: { xs: 15, sm: 16 } }}>{project.companyName}</Typography>
                            <Chip label={companyTypeLabel(project.companyType)} size="small" sx={{ bgcolor: '#F2F7FA', color: '#526273', fontWeight: 600 }} />
                            <Chip label={statusLabels[project.status]} size="small" sx={{ bgcolor: '#EAF7FB', color: '#176B8A', fontWeight: 700 }} />
                        </Stack>
                    </Box>
                </Stack>
                <Button variant="contained" startIcon={<ArrowBackOutlinedIcon />} onClick={() => navigate('/projects')} sx={{ ...backButtonSx, alignSelf: { xs: 'stretch', sm: 'center' } }}>Projelere Dön</Button>
            </Stack>
        </Paper>

        <Box sx={{ display: 'grid', gap: 2.5, gridTemplateColumns: { xs: '1fr', md: 'repeat(2, minmax(0, 1fr))' } }}>
            <DetailCard title="Genel bilgiler" icon={<DescriptionOutlinedIcon />}>
                <Box sx={{ display: 'grid', gap: 2, gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, minmax(0, 1fr))' } }}>
                    <Field label="Proje adı" value={project.name} /><Field label="Proje yöneticisi" value={project.projectManager} />
                    <Field label="Kategori" value={project.category} /><Field label="Sektör" value={project.sector} /><Field label="Durum" value={statusLabels[project.status]} /><Field label="Oluşturan" value={project.createdByName} />
                </Box>
            </DetailCard>
            <DetailCard title="Şirket" icon={<BusinessOutlinedIcon />}>
                <Stack spacing={1.5}><Field label="Şirket adı" value={project.companyName} /><Field label="Şirket türü" value={companyTypeLabel(project.companyType)} />
                    <Button size="small" startIcon={<VisibilityOutlinedIcon />} onClick={() => navigate(`/companies/${companyPath}/${project.companyId}`)} sx={{ alignSelf: 'flex-start', color: '#2A2B61' }}>Şirketi Görüntüle</Button>
                </Stack>
            </DetailCard>
            <DetailCard title="Takvim" icon={<CalendarMonthOutlinedIcon />}>
                <Box sx={{ display: 'grid', gap: 2, gridTemplateColumns: { xs: '1fr', sm: 'repeat(3, minmax(0, 1fr))' } }}>
                    <Field label="Başlangıç tarihi" value={formatDate(project.startDate)} /><Field label="Süre" value={`${project.durationMonths} ay`} /><Field label="Bitiş tarihi" value={formatDate(project.endDate)} />
                </Box>
            </DetailCard>
            <DetailCard title="Hakem durumu" icon={<EngineeringOutlinedIcon />}>
                <Stack spacing={1.75}>
                    {assignmentsQuery.isLoading ? (
                        <Stack spacing={1.25}>
                            <Skeleton height={72} variant="rounded" />
                            <Skeleton height={72} variant="rounded" />
                        </Stack>
                    ) : assignmentsQuery.isError ? (
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
                            Aktif hakemler yüklenemedi.
                        </Alert>
                    ) : activeAssignments.length === 0 ? (
                        <Typography color="text.secondary" sx={{ fontSize: 15, lineHeight: 1.5 }}>
                            Henüz hakem atanmadı
                        </Typography>
                    ) : (
                        <Stack spacing={1.25}>
                            {activeAssignments.slice(0, 2).map((assignment) => (
                                <Paper
                                    key={assignment.assignmentId}
                                    elevation={0}
                                    sx={{
                                        border: '1px solid #D9E2EA',
                                        borderRadius: '10px',
                                        bgcolor: '#F9FBFC',
                                        p: 1.75,
                                    }}
                                >
                                    <Stack spacing={1} sx={{ minWidth: 0 }}>
                                        <Stack
                                            direction="row"
                                            spacing={1}
                                            useFlexGap
                                            sx={{
                                                alignItems: 'flex-start',
                                                justifyContent: 'space-between',
                                                flexWrap: 'wrap',
                                            }}
                                        >
                                            <Box sx={{ minWidth: 0 }}>
                                                <Typography sx={{
                                                    color: '#2A2B61',
                                                    fontWeight: 700,
                                                    overflowWrap: 'anywhere',
                                                }}>
                                                    {assignment.academicianName}
                                                </Typography>
                                                <Typography color="text.secondary" sx={{ fontSize: 14, mt: 0.2 }}>
                                                    {assignment.academicianTitle?.trim() || missingValue}
                                                </Typography>
                                            </Box>
                                            <Chip
                                                label="Aktif"
                                                size="small"
                                                sx={{
                                                    bgcolor: '#EAF7FB',
                                                    color: '#176B8A',
                                                    fontWeight: 700,
                                                }}
                                            />
                                        </Stack>
                                        <Box sx={{
                                            display: 'grid',
                                            gap: 1,
                                            gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, minmax(0, 1fr))' },
                                        }}>
                                            <Field
                                                label="Atanma tarihi"
                                                value={formatDateTime(assignment.assignedAt)}
                                            />
                                            <Field label="Atayan" value={assignment.assignedByName} />
                                        </Box>
                                    </Stack>
                                </Paper>
                            ))}
                        </Stack>
                    )}
                    {assignmentLimitReached && assignmentEligibleStatus && (
                        <Typography color="text.secondary" sx={{ fontSize: 14, lineHeight: 1.5 }}>
                            Bu projeye en fazla iki aktif hakem atanabilir.
                        </Typography>
                    )}
                    {showRecommendationAction && (
                        <Button
                            variant="contained"
                            onClick={() => setRecommendationsOpen(true)}
                            sx={{ ...backButtonSx, alignSelf: 'flex-start', maxWidth: '100%' }}
                        >
                            Hakem Önerilerini Gör
                        </Button>
                    )}
                </Stack>
            </DetailCard>
            <Box sx={{ gridColumn: { md: '1 / -1' } }}><DetailCard title="Teknik içerik" icon={<EngineeringOutlinedIcon />}>
                <Stack spacing={2}><Field label="Proje özeti" value={project.summary} /><Field label="Ar-Ge niteliği" value={project.researchAndDevelopmentNature} /><Field label="Yenilikçi yönü" value={project.innovativeAspect} /><Field label="Ana teknoloji alanı" value={project.mainTechnologyArea} /><Field label="Proje detayı" value={project.projectDetail} /></Stack>
            </DetailCard></Box>
            <Box sx={{ gridColumn: { md: '1 / -1' } }}><DetailCard title="Anahtar kelimeler" icon={<DescriptionOutlinedIcon />}>
                {project.keywords.length ? <Stack direction="row" spacing={1} useFlexGap sx={{ flexWrap: 'wrap' }}>{project.keywords.map((keyword) => <Chip key={keyword} label={keyword} size="small" sx={{ bgcolor: '#F2F7FA', color: '#526273' }} />)}</Stack> : <Typography color="text.secondary">{missingValue}</Typography>}
            </DetailCard></Box>
            {(project.originalFileName || project.contentType || project.fileSize !== null) && <Box sx={{ gridColumn: { md: '1 / -1' } }}><DetailCard title="Kaynak belge" icon={<DescriptionOutlinedIcon />}>
                <Box sx={{ display: 'grid', gap: 2, gridTemplateColumns: { xs: '1fr', sm: 'repeat(3, minmax(0, 1fr))' } }}><Field label="Dosya adı" value={project.originalFileName} /><Field label="Tür" value={project.contentType ? fileTypeLabel(project.contentType) : null} /><Field label="Boyut" value={formatFileSize(project.fileSize)} /></Box>
                {hasDownloadableDocument && <Button
                    variant="contained"
                    disabled={isDownloading}
                    onClick={() => void handleDocumentDownload()}
                    startIcon={isDownloading ? <CircularProgress color="inherit" size={18} /> : <DownloadOutlinedIcon />}
                    sx={{ ...backButtonSx, alignSelf: 'flex-start', mt: 2 }}
                >
                    {isDownloading ? 'İndiriliyor…' : 'Belgeyi İndir'}
                </Button>}
            </DetailCard></Box>}
        </Box>
        <Snackbar autoHideDuration={4000} open={downloadErrorOpen} onClose={() => setDownloadErrorOpen(false)}>
            <Alert severity="error" variant="filled" onClose={() => setDownloadErrorOpen(false)}>
                Belge indirilemedi. Lütfen tekrar deneyin.
            </Alert>
        </Snackbar>
        <Snackbar
            autoHideDuration={4000}
            open={assignmentSuccessOpen}
            onClose={() => setAssignmentSuccessOpen(false)}
        >
            <Alert
                severity="success"
                variant="filled"
                onClose={() => setAssignmentSuccessOpen(false)}
            >
                Hakem başarıyla atandı.
            </Alert>
        </Snackbar>
        <ProjectReviewerRecommendationsDialog
            open={recommendationsOpen}
            projectId={project.id}
            projectName={project.name}
            onClose={() => setRecommendationsOpen(false)}
            onAssigned={() => setAssignmentSuccessOpen(true)}
        />
    </Stack>;
}
