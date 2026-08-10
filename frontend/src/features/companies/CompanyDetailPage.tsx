import { useQuery } from '@tanstack/react-query';
import ArrowBackOutlinedIcon from '@mui/icons-material/ArrowBackOutlined';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import EmailOutlinedIcon from '@mui/icons-material/EmailOutlined';
import LocationOnOutlinedIcon from '@mui/icons-material/LocationOnOutlined';
import PhoneOutlinedIcon from '@mui/icons-material/PhoneOutlined';
import BusinessOutlinedIcon from '@mui/icons-material/BusinessOutlined';
import {
    Alert,
    Box,
    Button,
    Chip,
    CircularProgress,
    Divider,
    Paper,
    Stack,
    Typography,
} from '@mui/material';
import axios from 'axios';
import type { ReactNode } from 'react';
import { useNavigate, useParams } from 'react-router';
import { useAuth } from '../auth/useAuth';
import { getApiErrorMessage } from '../../utils/getApiErrorMessage';
import { getCompanyById } from './companyApi';
import { companyQueryKeys } from './companyQueryKeys';
import type { CompanyContact, CompanyContactMethod, CompanyType } from './company.types';

const missingValue = 'Bilgi girilmemiş';

function displayOptional(value: string | null) {
    return value?.trim() || missingValue;
}

function companyTypeLabel(companyType: CompanyType) {
    return companyType === 'TGB' ? 'TGB Şirketi' : 'Dış Şirket';
}

function companyListPath(companyType: CompanyType) {
    return companyType === 'TGB' ? '/companies/tgb' : '/companies/external';
}

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
                border: '1px solid #D9E2EA',
                borderRadius: '12px',
                bgcolor: '#FFFFFF',
                p: { xs: 2, sm: 2.5 },
            }}
        >
            <Stack direction="row" spacing={1} sx={{ alignItems: 'center', mb: 2 }}>
                <Box sx={{ color: '#2A2B61', display: 'flex' }}>{icon}</Box>
                <Typography variant="h6" sx={{ fontSize: 17, fontWeight: 700 }}>
                    {title}
                </Typography>
            </Stack>
            {children}
        </Paper>
    );
}

function DetailField({ label, value }: { label: string; value: string | null }) {
    return (
        <Stack spacing={0.45}>
            <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 600 }}>
                {label}
            </Typography>
            <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
                {displayOptional(value)}
            </Typography>
        </Stack>
    );
}

function ContactMethods({ methods }: { methods: CompanyContactMethod[] }) {
    const phones = methods.filter((method) => method.type === 'PHONE');
    const emails = methods.filter((method) => method.type === 'EMAIL');

    return (
        <Stack spacing={2.25}>
            <Stack spacing={1}>
                <Typography variant="body2" sx={{ fontWeight: 700 }}>Telefonlar</Typography>
                {phones.length === 0 ? (
                    <Typography variant="body2" color="text.secondary">{missingValue}</Typography>
                ) : phones.map((phone) => (
                    <Stack key={phone.id} direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                        <PhoneOutlinedIcon fontSize="small" color="action" />
                        <Typography component="a" href={`tel:${phone.value}`} variant="body2" color="primary">
                            {phone.label?.trim() ? `${phone.label}: ${phone.value}` : phone.value}
                        </Typography>
                    </Stack>
                ))}
            </Stack>
            <Divider />
            <Stack spacing={1}>
                <Typography variant="body2" sx={{ fontWeight: 700 }}>E-postalar</Typography>
                {emails.length === 0 ? (
                    <Typography variant="body2" color="text.secondary">{missingValue}</Typography>
                ) : emails.map((email) => (
                    <Stack key={email.id} direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                        <EmailOutlinedIcon fontSize="small" color="action" />
                        <Typography component="a" href={`mailto:${email.value}`} variant="body2" color="primary" sx={{ overflowWrap: 'anywhere' }}>
                            {email.label?.trim() ? `${email.label}: ${email.value}` : email.value}
                        </Typography>
                    </Stack>
                ))}
            </Stack>
        </Stack>
    );
}

function Contacts({ contacts }: { contacts: CompanyContact[] }) {
    const sortedContacts = [...contacts].sort((first, second) => Number(second.primary) - Number(first.primary));

    if (sortedContacts.length === 0) {
        return <Typography variant="body2" color="text.secondary">{missingValue}</Typography>;
    }

    return (
        <Stack spacing={1.5}>
            {sortedContacts.map((contact, index) => (
                <Box key={contact.id}>
                    {index > 0 && <Divider sx={{ mb: 1.5 }} />}
                    <Stack direction="row" spacing={1} useFlexGap sx={{ alignItems: 'center', flexWrap: 'wrap', mb: 0.5 }}>
                        <Typography variant="body1" sx={{ fontWeight: 700 }}>{contact.fullName}</Typography>
                        {contact.primary && (
                            <Chip label="Ana yetkili" size="small" sx={{ bgcolor: 'rgba(46, 168, 211, 0.14)', color: '#176B8A', fontWeight: 700 }} />
                        )}
                    </Stack>
                    <Typography variant="body2" color="text.secondary" sx={{ mb: 1.25 }}>
                        {displayOptional(contact.position)}
                    </Typography>
                    <Stack spacing={0.75}>
                        <Typography variant="body2">
                            Telefon: {contact.phone?.trim() ? <Box component="a" href={`tel:${contact.phone}`} color="primary">{contact.phone}</Box> : missingValue}
                        </Typography>
                        <Typography variant="body2" sx={{ overflowWrap: 'anywhere' }}>
                            E-posta: {contact.email?.trim() ? <Box component="a" href={`mailto:${contact.email}`} color="primary">{contact.email}</Box> : missingValue}
                        </Typography>
                    </Stack>
                </Box>
            ))}
        </Stack>
    );
}

export function CompanyDetailPage() {
    const { companyId } = useParams();
    const navigate = useNavigate();
    const { role } = useAuth();
    const parsedCompanyId = Number(companyId);
    const isValidCompanyId = Number.isInteger(parsedCompanyId) && parsedCompanyId > 0;
    const companyQuery = useQuery({
        queryKey: companyQueryKeys.detail(isValidCompanyId ? parsedCompanyId : 0),
        queryFn: () => getCompanyById(parsedCompanyId),
        enabled: isValidCompanyId,
    });
    const company = companyQuery.data;
    const isNotFound = !isValidCompanyId || (axios.isAxiosError(companyQuery.error) && companyQuery.error.response?.status === 404);
    const backPath = company ? companyListPath(company.companyType) : '/companies/tgb';
    const canEdit = role === 'ADMIN' || role === 'USER';

    if (companyQuery.isLoading) {
        return (
            <Box sx={{ display: 'grid', minHeight: '60vh', placeItems: 'center' }}>
                <CircularProgress aria-label="Şirket detayları yükleniyor" />
            </Box>
        );
    }

    if (isNotFound || companyQuery.isError) {
        return (
            <Stack spacing={2.5} sx={{ maxWidth: 760, mx: 'auto', pt: { xs: 2, sm: 3 } }}>
                <Alert severity={isNotFound ? 'info' : 'error'}>
                    {isNotFound ? 'Şirket kaydı bulunamadı.' : getApiErrorMessage(companyQuery.error, 'Şirket detayları alınamadı')}
                </Alert>
                <Stack direction="row" spacing={1} useFlexGap sx={{ flexWrap: 'wrap' }}>
                    {!isNotFound && <Button variant="outlined" onClick={() => void companyQuery.refetch()}>Tekrar dene</Button>}
                    <Button startIcon={<ArrowBackOutlinedIcon />} onClick={() => navigate(backPath)}>Şirket listesine dön</Button>
                </Stack>
            </Stack>
        );
    }

    if (!company) {
        return null;
    }

    return (
        <Stack spacing={2.5} sx={{ maxWidth: 1280, mx: 'auto', pb: 3 }}>
            <Paper elevation={0} sx={{ borderBottom: '1px solid #D9E2EA', borderRadius: 0, bgcolor: '#FFFFFF', mx: { xs: -2, sm: -3, lg: -4 }, px: { xs: 2, sm: 3, lg: 4 }, py: { xs: 2, sm: 2.5 } }}>
                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5} sx={{ alignItems: { sm: 'center' }, justifyContent: 'space-between' }}>
                    <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center', minWidth: 0 }}>
                        <Button aria-label="Şirket listesine dön" onClick={() => navigate(backPath)} sx={{ minWidth: 0, px: 1 }}><ArrowBackOutlinedIcon /></Button>
                        <Box sx={{ bgcolor: '#2EA8D3', borderRadius: 999, flex: '0 0 auto', height: 42, width: 3 }} />
                        <Stack spacing={0.65} sx={{ minWidth: 0 }}>
                            <Typography variant="h5" noWrap sx={{ fontSize: { xs: 20, sm: 24 }, fontWeight: 700 }}>{company.name}</Typography>
                            <Chip label={companyTypeLabel(company.companyType)} size="small" sx={{ alignSelf: 'flex-start', bgcolor: 'rgba(46, 168, 211, 0.14)', color: '#176B8A', fontWeight: 700 }} />
                        </Stack>
                    </Stack>
                    {canEdit && (
                        <Button variant="outlined" startIcon={<EditOutlinedIcon />} onClick={() => navigate(backPath, { state: { editCompanyId: company.id } })}>
                            Düzenle
                        </Button>
                    )}
                </Stack>
            </Paper>

            <Box sx={{ display: 'grid', gap: 2.5, gridTemplateColumns: { xs: '1fr', md: 'repeat(2, minmax(0, 1fr))' } }}>
                <DetailCard title="Şirket bilgileri" icon={<BusinessOutlinedIcon />}>
                    <Stack spacing={2}><DetailField label="Şirket adı" value={company.name} /><DetailField label="Şirket türü" value={companyTypeLabel(company.companyType)} /><DetailField label="Faaliyet alanı" value={company.activityArea} /></Stack>
                </DetailCard>
                <DetailCard title="Konum" icon={<LocationOnOutlinedIcon />}>
                    <Box sx={{ display: 'grid', gap: 2, gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, minmax(0, 1fr))' } }}><DetailField label="İl" value={company.city} /><DetailField label="İlçe" value={company.district} /><DetailField label="OSB" value={company.osb} /><DetailField label="Bina" value={company.building} /><Box sx={{ gridColumn: { sm: '1 / -1' } }}><DetailField label="Açık adres" value={company.address} /></Box></Box>
                </DetailCard>
                <DetailCard title="Şirket iletişimi" icon={<PhoneOutlinedIcon />}><ContactMethods methods={company.contactMethods} /></DetailCard>
                <DetailCard title="Yetkili kişiler" icon={<BusinessOutlinedIcon />}><Contacts contacts={company.contacts} /></DetailCard>
            </Box>
        </Stack>
    );
}
