import { useQuery } from '@tanstack/react-query';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import BadgeOutlinedIcon from '@mui/icons-material/BadgeOutlined';
import EmailOutlinedIcon from '@mui/icons-material/EmailOutlined';
import LocalOfferOutlinedIcon from '@mui/icons-material/LocalOfferOutlined';
import PhoneOutlinedIcon from '@mui/icons-material/PhoneOutlined';
import WorkOutlineOutlinedIcon from '@mui/icons-material/WorkOutlineOutlined';
import {
    Alert,
    Box,
    Button,
    Chip,
    Paper,
    Skeleton,
    Stack,
    Typography,
} from '@mui/material';
import axios from 'axios';
import type { ReactNode } from 'react';
import { useNavigate, useParams } from 'react-router';
import { getApiErrorMessage } from '../../utils/getApiErrorMessage';
import { getAcademicianById, getAcademicianProjectSummary } from './academicianApi';
import { academicianQueryKeys } from './academicianQueryKeys';

const missingValue = 'Bilgi girilmemiş';

function DetailCard({
    title,
    icon,
    children,
}: {
    title: string;
    icon: ReactNode;
    children: ReactNode;
}) {
    return (
        <Paper
            elevation={0}
            sx={{
                bgcolor: '#FFFFFF',
                border: '1px solid #D9E2EA',
                borderRadius: '12px',
                minWidth: 0,
                p: { xs: 2, sm: 2.5 },
            }}
        >
            <Stack direction="row" spacing={1} sx={{ alignItems: 'center', mb: 2 }}>
                <Box sx={{ color: '#2A2B61', display: 'flex' }}>{icon}</Box>
                <Typography sx={{ color: '#0A1026', fontSize: 19, fontWeight: 700 }}>
                    {title}
                </Typography>
            </Stack>
            {children}
        </Paper>
    );
}

function DetailField({ label, value }: { label: string; value: string | null }) {
    const displayValue = value?.trim();

    return (
        <Stack spacing={0.45} sx={{ minWidth: 0 }}>
            <Typography color="text.secondary" sx={{ fontSize: 14, fontWeight: 600 }}>
                {label}
            </Typography>
            <Typography
                color={displayValue ? 'text.primary' : 'text.secondary'}
                sx={{ fontSize: { xs: 15, sm: 16 }, overflowWrap: 'anywhere' }}
            >
                {displayValue || missingValue}
            </Typography>
        </Stack>
    );
}

function ContactField({
    label,
    value,
    type,
}: {
    label: string;
    value: string | null;
    type: 'email' | 'phone';
}) {
    const displayValue = value?.trim();
    const Icon = type === 'email' ? EmailOutlinedIcon : PhoneOutlinedIcon;

    return (
        <Stack spacing={0.6} sx={{ minWidth: 0 }}>
            <Typography color="text.secondary" sx={{ fontSize: 14, fontWeight: 600 }}>
                {label}
            </Typography>
            {displayValue ? (
                <Stack direction="row" spacing={1} sx={{ alignItems: 'flex-start', minWidth: 0 }}>
                    <Icon sx={{ color: '#728196', flex: '0 0 auto', fontSize: 20, mt: 0.15 }} />
                    <Typography
                        component="a"
                        href={`${type === 'email' ? 'mailto' : 'tel'}:${displayValue}`}
                        sx={{ color: '#176B8A', fontSize: { xs: 15, sm: 16 }, overflowWrap: 'anywhere' }}
                    >
                        {displayValue}
                    </Typography>
                </Stack>
            ) : (
                <Typography color="text.secondary" sx={{ fontSize: 15 }}>
                    {missingValue}
                </Typography>
            )}
        </Stack>
    );
}

function PageSkeleton() {
    return (
        <Stack spacing={2.5} sx={{ maxWidth: 1280, mx: 'auto', pt: { xs: 2.5, sm: 3 } }}>
            <Skeleton height={112} variant="rounded" />
            <Box sx={{ display: 'grid', gap: 2.5, gridTemplateColumns: { xs: '1fr', md: 'repeat(2, minmax(0, 1fr))' } }}>
                {[0, 1, 2, 3].map((item) => <Skeleton key={item} height={220} variant="rounded" />)}
            </Box>
        </Stack>
    );
}

export function AcademicianDetailPage() {
    const { academicianId } = useParams();
    const navigate = useNavigate();
    const parsedAcademicianId = Number(academicianId);
    const isValidAcademicianId = Number.isInteger(parsedAcademicianId)
        && parsedAcademicianId > 0;
    const academicianQuery = useQuery({
        queryKey: academicianQueryKeys.detail(
            isValidAcademicianId ? parsedAcademicianId : 0,
        ),
        queryFn: () => getAcademicianById(parsedAcademicianId),
        enabled: isValidAcademicianId,
    });
    const summaryQuery = useQuery({
        queryKey: academicianQueryKeys.projectSummary(
            isValidAcademicianId ? parsedAcademicianId : 0,
        ),
        queryFn: () => getAcademicianProjectSummary(parsedAcademicianId),
        enabled: isValidAcademicianId,
    });
    const academician = academicianQuery.data;
    const isNotFound = !isValidAcademicianId
        || (axios.isAxiosError(academicianQuery.error)
            && academicianQuery.error.response?.status === 404);

    if (academicianQuery.isLoading) {
        return <PageSkeleton />;
    }

    if (isNotFound || academicianQuery.isError) {
        return (
            <Stack spacing={2.5} sx={{ maxWidth: 760, mx: 'auto', pt: { xs: 2.5, sm: 3 } }}>
                <Alert severity={isNotFound ? 'info' : 'error'}>
                    {isNotFound
                        ? 'Akademisyen bulunamadı.'
                        : getApiErrorMessage(
                            academicianQuery.error,
                            'Akademisyen bilgileri şu anda yüklenemedi.',
                        )}
                </Alert>
                <Stack direction="row" spacing={1} useFlexGap sx={{ flexWrap: 'wrap' }}>
                    {!isNotFound && (
                        <Button variant="outlined" onClick={() => void academicianQuery.refetch()}>
                            Tekrar Dene
                        </Button>
                    )}
                    <Button startIcon={<ArrowBackIcon />} onClick={() => navigate('/academicians')}>
                        Akademisyenlere Dön
                    </Button>
                </Stack>
            </Stack>
        );
    }

    if (!academician) {
        return null;
    }

    const fullName = `${academician.firstName} ${academician.lastName}`.trim();

    return (
        <Stack spacing={2.5} sx={{ maxWidth: 1280, mx: 'auto', pb: 3, pt: { xs: 2.5, sm: 3 } }}>
            <Paper
                elevation={0}
                sx={{
                    bgcolor: '#FFFFFF',
                    border: '1px solid #D9E2EA',
                    borderRadius: '13px',
                    p: { xs: 2, sm: 2.5 },
                }}
            >
                <Stack
                    direction={{ xs: 'column', sm: 'row' }}
                    spacing={2}
                    sx={{ alignItems: { sm: 'center' }, justifyContent: 'space-between' }}
                >
                    <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center', minWidth: 0 }}>
                        <Box sx={{ bgcolor: '#2EA8D3', borderRadius: 999, flex: '0 0 auto', height: 48, width: 4 }} />
                        <Box sx={{ minWidth: 0 }}>
                            <Typography
                                component="h1"
                                sx={{ color: '#0A1026', fontSize: { xs: 22, sm: 26 }, fontWeight: 700, overflowWrap: 'anywhere' }}
                            >
                                {fullName}
                            </Typography>
                            <Typography color="text.secondary" sx={{ mt: 0.5, overflowWrap: 'anywhere' }}>
                                {academician.title?.trim() || missingValue}
                            </Typography>
                        </Box>
                    </Stack>
                    <Button
                        variant="contained"
                        startIcon={<ArrowBackIcon />}
                        onClick={() => navigate('/academicians')}
                        sx={{ alignSelf: { xs: 'stretch', sm: 'center' }, bgcolor: '#2A2B61', color: '#FFFFFF', '&:hover': { bgcolor: '#202153' } }}
                    >
                        Akademisyenlere Dön
                    </Button>
                </Stack>
            </Paper>

            <Box sx={{ display: 'grid', gap: 2.5, gridTemplateColumns: { xs: 'minmax(0, 1fr)', md: 'repeat(2, minmax(0, 1fr))' } }}>
                <DetailCard title="Akademisyen bilgileri" icon={<BadgeOutlinedIcon />}>
                    <Stack spacing={2}>
                        <DetailField label="Ad" value={academician.firstName} />
                        <DetailField label="Soyad" value={academician.lastName} />
                        <DetailField label="Ünvan" value={academician.title} />
                    </Stack>
                </DetailCard>

                <DetailCard title="İletişim" icon={<EmailOutlinedIcon />}>
                    <Stack spacing={2.25}>
                        <ContactField label="E-posta" value={academician.email} type="email" />
                        <ContactField label="Telefon" value={academician.phone} type="phone" />
                    </Stack>
                </DetailCard>

                <DetailCard title="Proje özeti" icon={<WorkOutlineOutlinedIcon />}>
                    {summaryQuery.isLoading ? (
                        <Box sx={{ display: 'grid', gap: 1.5, gridTemplateColumns: { xs: '1fr', sm: 'repeat(3, minmax(0, 1fr))' } }}>
                            {[0, 1, 2].map((item) => <Skeleton key={item} height={102} variant="rounded" />)}
                        </Box>
                    ) : summaryQuery.isError ? (
                        <Alert
                            severity="error"
                            action={(
                                <Button color="inherit" size="small" onClick={() => void summaryQuery.refetch()}>
                                    Tekrar Dene
                                </Button>
                            )}
                        >
                            Proje özeti şu anda yüklenemedi.
                        </Alert>
                    ) : (
                        <Box sx={{ display: 'grid', gap: 1.5, gridTemplateColumns: { xs: '1fr', sm: 'repeat(3, minmax(0, 1fr))' } }}>
                            {[
                                ['Aktif proje', summaryQuery.data?.activeProjectCount ?? 0, 'Devam eden aktif görevlendirmeler'],
                                ['Tamamlanan proje', summaryQuery.data?.completedProjectCount ?? 0, 'Tamamlanmış görevlendirmeler'],
                                ['Toplam proje', summaryQuery.data?.totalProjectCount ?? 0, 'Aktif + tamamlanan farklı projeler'],
                            ].map(([label, value, description]) => (
                                <Box key={label} sx={{ bgcolor: '#F7FAFC', border: '1px solid #E3EAF0', borderRadius: '10px', minWidth: 0, p: 1.5 }}>
                                    <Typography sx={{ color: '#2A2B61', fontSize: 25, fontWeight: 750, lineHeight: 1.2 }}>{value}</Typography>
                                    <Typography sx={{ color: '#0A1026', fontSize: 14, fontWeight: 700, mt: 0.5 }}>{label}</Typography>
                                    <Typography color="text.secondary" sx={{ fontSize: 12.5, lineHeight: 1.4, mt: 0.35 }}>{description}</Typography>
                                </Box>
                            ))}
                        </Box>
                    )}
                    <Typography color="text.secondary" sx={{ fontSize: 12.5, mt: 1.5 }}>
                        Kaldırılmış atamalar proje sayılarına dahil edilmez.
                    </Typography>
                </DetailCard>

                <DetailCard title="Anahtar kelimeler" icon={<LocalOfferOutlinedIcon />}>
                    {academician.keywords.length > 0 ? (
                        <Stack direction="row" spacing={0.75} useFlexGap sx={{ flexWrap: 'wrap', minWidth: 0 }}>
                            {academician.keywords.map((keyword) => (
                                <Chip
                                    key={keyword}
                                    label={keyword}
                                    size="small"
                                    sx={{ bgcolor: 'rgba(46, 168, 211, 0.10)', color: '#176B8A', maxWidth: '100%', '& .MuiChip-label': { overflow: 'hidden', textOverflow: 'ellipsis' } }}
                                />
                            ))}
                        </Stack>
                    ) : (
                        <Typography color="text.secondary" sx={{ fontSize: 15 }}>
                            Anahtar kelime bilgisi girilmemiş.
                        </Typography>
                    )}
                </DetailCard>
            </Box>
        </Stack>
    );
}
