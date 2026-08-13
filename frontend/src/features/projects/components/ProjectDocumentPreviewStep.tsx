import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
import WarningAmberOutlinedIcon from '@mui/icons-material/WarningAmberOutlined';
import {
    Alert,
    Box,
    Chip,
    Divider,
    Paper,
    Stack,
    TextField,
    Typography,
} from '@mui/material';
import type {
    ProjectCompanyCandidate,
    ProjectCompanyMatchStatus,
    ProjectDocumentPreviewDraft,
    ProjectDocumentPreviewResponse,
    ProjectFormField,
} from '../project.types';
import { ProjectCompanySelection } from './ProjectCompanySelection';

type ProjectDocumentPreviewStepProps = {
    preview: ProjectDocumentPreviewResponse;
    draft: ProjectDocumentPreviewDraft;
    onDraftChange: (draft: ProjectDocumentPreviewDraft) => void;
    selectedCompanyId: number | null;
    onCompanySelect: (companyId: number | null) => void;
};

const fieldLabels: Record<ProjectFormField, string> = {
    COMPANY_NAME: 'Firma adı',
    NAME: 'Proje adı',
    PROJECT_MANAGER: 'Proje yöneticisi',
    START_DATE: 'Başlangıç tarihi',
    DURATION_MONTHS: 'Süre',
    CATEGORY: 'Kategori',
    SECTOR: 'Sektör',
    SUMMARY: 'Proje özeti',
    RESEARCH_AND_DEVELOPMENT_NATURE: 'Ar-Ge niteliği',
    INNOVATIVE_ASPECT: 'Yenilikçi yönü',
    MAIN_TECHNOLOGY_AREA: 'Ana teknoloji alanı',
    PROJECT_DETAIL: 'Proje detayı',
    KEYWORDS: 'Anahtar kelimeler',
};

const matchLabels: Record<ProjectCompanyMatchStatus, string> = {
    UNIQUE_MATCH: 'Şirket eşleşti',
    AMBIGUOUS: 'Birden fazla eşleşme bulundu',
    NOT_FOUND: 'Şirket bulunamadı',
    COMPANY_NAME_MISSING: 'Firma adı bulunamadı',
};

const previewTextFieldSx = {
    '& .MuiInputLabel-root.Mui-focused:not(.Mui-error)': {
        color: '#2A2B61',
    },
    '& .MuiOutlinedInput-root.Mui-focused:not(.Mui-error) .MuiOutlinedInput-notchedOutline': {
        borderColor: '#2A2B61',
        borderWidth: '1.5px',
    },
};

function formatFileSize(size: number): string {
    if (size < 1024) return `${size} B`;
    if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
    return `${(size / (1024 * 1024)).toFixed(1)} MB`;
}

function calculateEndDate(startDate: string, durationMonths: string): string {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(startDate) || !/^\d+$/.test(durationMonths)) {
        return '';
    }
    const duration = Number(durationMonths);
    const [year, month, day] = startDate.split('-').map(Number);
    if (!Number.isInteger(duration) || duration <= 0) return '';
    const source = new Date(Date.UTC(year, month - 1, day));
    if (source.getUTCFullYear() !== year || source.getUTCMonth() !== month - 1
        || source.getUTCDate() !== day) return '';
    const targetMonth = month - 1 + duration;
    const targetYear = year + Math.floor(targetMonth / 12);
    const normalizedMonth = targetMonth % 12;
    const lastDay = new Date(Date.UTC(targetYear, normalizedMonth + 1, 0)).getUTCDate();
    const target = new Date(Date.UTC(targetYear, normalizedMonth, Math.min(day, lastDay)));
    return target.toISOString().slice(0, 10);
}

function companyTypeLabel(type: ProjectCompanyCandidate['companyType']): string {
    return type === 'TGB' ? 'TGB Şirketi' : 'Dış Şirket';
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
    return (
        <Stack spacing={1.5}>
            <Typography sx={{ color: '#2A2B61', fontWeight: 700 }} variant="h6">
                {title}
            </Typography>
            {children}
        </Stack>
    );
}

export function createProjectDocumentPreviewDraft(
    preview: ProjectDocumentPreviewResponse,
): ProjectDocumentPreviewDraft {
    return {
        name: preview.name ?? '',
        projectManager: preview.projectManager ?? '',
        startDate: preview.startDate ?? '',
        durationMonths: preview.durationMonths?.toString() ?? '',
        category: preview.category ?? '',
        sector: preview.sector ?? '',
        summary: preview.summary ?? '',
        researchAndDevelopmentNature: preview.researchAndDevelopmentNature ?? '',
        innovativeAspect: preview.innovativeAspect ?? '',
        mainTechnologyArea: preview.mainTechnologyArea ?? '',
        projectDetail: preview.projectDetail ?? '',
        keywords: preview.keywords ?? [],
    };
}

export function ProjectDocumentPreviewStep({
    preview,
    draft,
    onDraftChange,
    selectedCompanyId,
    onCompanySelect,
}: ProjectDocumentPreviewStepProps) {
    const endDate = calculateEndDate(draft.startDate, draft.durationMonths);
    const missingLabels = preview.missingFields.map((field) => fieldLabels[field]);
    const reviewLabels = preview.reviewRequiredFields.map((field) => fieldLabels[field]);
    const match = preview.companyMatch;

    function setField(field: Exclude<keyof ProjectDocumentPreviewDraft, 'keywords'>, value: string) {
        onDraftChange({ ...draft, [field]: value });
    }

    return (
        <Stack spacing={3}>
            <Section title="Dosya ve genel durum">
                <Paper elevation={0} sx={{ bgcolor: '#F8FAFC', border: '1px solid #E3EAF0', borderRadius: '10px', p: 2 }}>
                    <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5} sx={{ alignItems: { sm: 'center' } }}>
                        <Box sx={{ flex: 1, minWidth: 0 }}>
                            <Typography noWrap sx={{ fontWeight: 700 }}>{preview.originalFileName}</Typography>
                            <Typography color="text.secondary" variant="body2">{formatFileSize(preview.fileSize)}</Typography>
                        </Box>
                        <Chip label={`${preview.missingFields.length} eksik alan`} size="small" sx={{ bgcolor: '#FFF7E3', color: '#8A5B00' }} />
                        <Chip label={`${preview.reviewRequiredFields.length} incelenecek alan`} size="small" sx={{ bgcolor: '#F2F7FA', color: '#526273' }} />
                    </Stack>
                </Paper>
                {(missingLabels.length > 0 || reviewLabels.length > 0 || preview.warnings.length > 0) && (
                    <Stack spacing={1}>
                        {missingLabels.length > 0 && <ReviewNotice label="Eksik bilgiler" values={missingLabels} />}
                        {reviewLabels.length > 0 && <ReviewNotice label="Kontrol edilmesi gereken bilgiler" values={reviewLabels} />}
                        {preview.warnings.map((warning) => (
                            <Alert icon={<WarningAmberOutlinedIcon fontSize="inherit" />} key={warning} severity="warning">
                                {warning}
                            </Alert>
                        ))}
                    </Stack>
                )}
            </Section>

            <Divider />
            <Section title="Şirket eşleşmesi">
                <Paper elevation={0} sx={{ border: '1px solid #E3EAF0', borderRadius: '10px', p: 2 }}>
                    <Stack spacing={1.25}>
                        <Typography color="text.secondary" variant="body2">Belgede bulunan firma adı</Typography>
                        <Typography sx={{ fontWeight: 700 }}>{preview.companyName || 'Firma adı bulunamadı'}</Typography>
                        <Chip label={matchLabels[match.status]} size="small" sx={{ alignSelf: 'flex-start', bgcolor: match.status === 'UNIQUE_MATCH' ? '#EAF7EF' : '#FFF7E3', color: match.status === 'UNIQUE_MATCH' ? '#237A47' : '#8A5B00' }} />
                        <Typography color="text.secondary" variant="body2">{match.message}</Typography>
                        {match.matchedCompany && <CompanyCandidate candidate={match.matchedCompany} />}
                        {match.status === 'AMBIGUOUS' && (
                            <Stack spacing={0.75}>
                                {match.candidates.map((candidate) => <CompanyCandidate candidate={candidate} key={candidate.id} />)}
                            </Stack>
                        )}
                        <ProjectCompanySelection
                            match={match}
                            onSelect={onCompanySelect}
                            selectedCompanyId={selectedCompanyId}
                        />
                    </Stack>
                </Paper>
            </Section>

            <Divider />
            <Section title="Temel proje bilgileri">
                <Stack spacing={2}>
                    <TextField sx={previewTextFieldSx} fullWidth label="Proje adı" value={draft.name} onChange={(event) => setField('name', event.target.value)} slotProps={{ inputLabel: { shrink: true } }} />
                    <TextField sx={previewTextFieldSx} fullWidth label="Proje yöneticisi" value={draft.projectManager} onChange={(event) => setField('projectManager', event.target.value)} slotProps={{ inputLabel: { shrink: true } }} />
                    <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                        <TextField sx={previewTextFieldSx} fullWidth label="Başlangıç tarihi" type="date" value={draft.startDate} onChange={(event) => setField('startDate', event.target.value)} slotProps={{ inputLabel: { shrink: true } }} />
                        <TextField sx={previewTextFieldSx} fullWidth label="Süre (ay)" type="number" value={draft.durationMonths} onChange={(event) => setField('durationMonths', event.target.value)} slotProps={{ htmlInput: { min: 1 }, inputLabel: { shrink: true } }} />
                        <TextField sx={previewTextFieldSx} fullWidth label="Bitiş tarihi" value={endDate} slotProps={{ inputLabel: { shrink: true }, input: { readOnly: true } }} placeholder="Hesaplanamadı" />
                    </Stack>
                    <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                        <TextField sx={previewTextFieldSx} fullWidth label="Kategori" value={draft.category} onChange={(event) => setField('category', event.target.value)} slotProps={{ inputLabel: { shrink: true } }} />
                        <TextField sx={previewTextFieldSx} fullWidth label="Sektör" value={draft.sector} onChange={(event) => setField('sector', event.target.value)} slotProps={{ inputLabel: { shrink: true } }} />
                    </Stack>
                </Stack>
            </Section>

            <Divider />
            <Section title="Teknik içerik">
                <Stack spacing={2}>
                    <TextField sx={previewTextFieldSx} fullWidth label="Proje özeti" minRows={3} multiline value={draft.summary} onChange={(event) => setField('summary', event.target.value)} slotProps={{ inputLabel: { shrink: true } }} />
                    <TextField sx={previewTextFieldSx} fullWidth label="Ar-Ge niteliği" minRows={3} multiline value={draft.researchAndDevelopmentNature} onChange={(event) => setField('researchAndDevelopmentNature', event.target.value)} slotProps={{ inputLabel: { shrink: true } }} />
                    <TextField sx={previewTextFieldSx} fullWidth label="Yenilikçi yönü" minRows={3} multiline value={draft.innovativeAspect} onChange={(event) => setField('innovativeAspect', event.target.value)} slotProps={{ inputLabel: { shrink: true } }} />
                    <TextField sx={previewTextFieldSx} fullWidth label="Ana teknoloji alanı" value={draft.mainTechnologyArea} onChange={(event) => setField('mainTechnologyArea', event.target.value)} slotProps={{ inputLabel: { shrink: true } }} />
                    <TextField sx={previewTextFieldSx} fullWidth label="Proje detayı" minRows={4} multiline value={draft.projectDetail} onChange={(event) => setField('projectDetail', event.target.value)} slotProps={{ inputLabel: { shrink: true } }} />
                    <TextField
                        sx={previewTextFieldSx}
                        fullWidth
                        helperText="Anahtar kelimeleri virgülle ayırarak düzenleyebilirsiniz."
                        label="Anahtar kelimeler"
                        slotProps={{ inputLabel: { shrink: true } }}
                        value={draft.keywords.join(', ')}
                        onChange={(event) => onDraftChange({
                            ...draft,
                            keywords: event.target.value.split(',').map((keyword) => keyword.trim()).filter(Boolean),
                        })}
                    />
                </Stack>
            </Section>
        </Stack>
    );
}

function ReviewNotice({ label, values }: { label: string; values: string[] }) {
    return (
        <Paper elevation={0} sx={{ alignItems: 'center', bgcolor: '#FFFCF5', border: '1px solid #F2D49B', borderRadius: '8px', display: 'flex', gap: 1, p: 1.25 }}>
            <InfoOutlinedIcon sx={{ color: '#B77800', fontSize: 20 }} />
            <Typography variant="body2"><Box component="span" sx={{ fontWeight: 700 }}>{label}:</Box> {values.join(', ')}</Typography>
        </Paper>
    );
}

function CompanyCandidate({ candidate }: { candidate: ProjectCompanyCandidate }) {
    return (
        <Paper elevation={0} sx={{ alignItems: 'center', bgcolor: '#F8FAFC', border: '1px solid #E3EAF0', borderRadius: '8px', display: 'flex', gap: 1, justifyContent: 'space-between', p: 1.25 }}>
            <Typography sx={{ fontWeight: 600 }}>{candidate.name}</Typography>
            <Chip label={companyTypeLabel(candidate.companyType)} size="small" sx={{ bgcolor: '#F2F7FA', color: '#526273' }} />
        </Paper>
    );
}
