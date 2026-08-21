import { useState } from 'react';
import axios from 'axios';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import {
    Alert,
    Box,
    Button,
    Chip,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    Divider,
    Paper,
    Stack,
    TextField,
    Typography,
} from '@mui/material';
import type { ApiErrorResponse } from '../../../types/api';
import { getApiErrorMessage } from '../../../utils/getApiErrorMessage';
import { companyQueryKeys } from '../../companies/companyQueryKeys';
import { createManualProject } from '../projectApi';
import { projectQueryKeys } from '../projectQueryKeys';
import type { CreateManualProjectRequest, ProjectCompanyMatch } from '../project.types';
import { ProjectCompanySelection } from './ProjectCompanySelection';

type ManualProjectDialogProps = {
    open: boolean;
    onClose: () => void;
    onCreated: () => void;
};

type ManualProjectFormErrors = Partial<Record<
    'companyId' | 'name' | 'startDate' | 'durationMonths' | 'keywords',
    string
>>;

const manualCompanyMatch: ProjectCompanyMatch = {
    status: 'COMPANY_NAME_MISSING',
    matchedCompany: null,
    candidates: [],
    message: 'Projeyi ilişkilendireceğiniz şirketi arayıp seçin.',
};

const textFieldSx = {
    '& .MuiInputLabel-root.Mui-focused:not(.Mui-error)': { color: '#2A2B61' },
    '& .MuiOutlinedInput-root.Mui-focused:not(.Mui-error) .MuiOutlinedInput-notchedOutline': {
        borderColor: '#2A2B61',
        borderWidth: '1.5px',
    },
};

function normalizeOptional(value: string): string | undefined {
    const trimmed = value.trim();
    return trimmed || undefined;
}

function normalizeKeywords(keywords: string[]): string[] {
    const canonical = new Set<string>();
    return keywords.reduce<string[]>((normalized, keyword) => {
        const trimmed = keyword.trim();
        const key = trimmed.toLocaleLowerCase('tr-TR');
        if (trimmed && !canonical.has(key)) {
            canonical.add(key);
            normalized.push(trimmed);
        }
        return normalized;
    }, []);
}

function isValidDate(value: string): boolean {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return false;
    const [year, month, day] = value.split('-').map(Number);
    const date = new Date(Date.UTC(year, month - 1, day));
    return date.getUTCFullYear() === year
        && date.getUTCMonth() === month - 1
        && date.getUTCDate() === day;
}

function previewEndDate(startDate: string, durationMonths: string): string {
    if (!isValidDate(startDate) || !/^\d+$/.test(durationMonths)
        || Number(durationMonths) <= 0) {
        return '';
    }

    const [year, month, day] = startDate.split('-').map(Number);
    const targetMonthIndex = month - 1 + Number(durationMonths);
    const targetYear = year + Math.floor(targetMonthIndex / 12);
    const targetMonth = ((targetMonthIndex % 12) + 12) % 12;
    const lastDayOfTargetMonth = new Date(Date.UTC(targetYear, targetMonth + 1, 0))
        .getUTCDate();
    const targetDay = Math.min(day, lastDayOfTargetMonth);

    return `${targetYear}-${String(targetMonth + 1).padStart(2, '0')}-${String(targetDay).padStart(2, '0')}`;
}

function isApiErrorResponse(value: unknown): value is ApiErrorResponse {
    return Boolean(value && typeof value === 'object' && 'message' in value);
}

function Section({ children, title }: { children: React.ReactNode; title: string }) {
    return (
        <Stack spacing={1.5}>
            <Typography sx={{ color: '#2A2B61', fontWeight: 700 }} variant="subtitle1">
                {title}
            </Typography>
            {children}
        </Stack>
    );
}

export function ManualProjectDialog({ open, onClose, onCreated }: ManualProjectDialogProps) {
    const queryClient = useQueryClient();
    const [companyId, setCompanyId] = useState<number | null>(null);
    const [name, setName] = useState('');
    const [projectManager, setProjectManager] = useState('');
    const [startDate, setStartDate] = useState('');
    const [durationMonths, setDurationMonths] = useState('');
    const [category, setCategory] = useState('');
    const [sector, setSector] = useState('');
    const [summary, setSummary] = useState('');
    const [researchAndDevelopmentNature, setResearchAndDevelopmentNature] = useState('');
    const [innovativeAspect, setInnovativeAspect] = useState('');
    const [mainTechnologyArea, setMainTechnologyArea] = useState('');
    const [projectDetail, setProjectDetail] = useState('');
    const [keywords, setKeywords] = useState<string[]>([]);
    const [keywordInput, setKeywordInput] = useState('');
    const [formErrors, setFormErrors] = useState<ManualProjectFormErrors>({});
    const [createError, setCreateError] = useState<string | null>(null);

    function resetState() {
        setCompanyId(null);
        setName('');
        setProjectManager('');
        setStartDate('');
        setDurationMonths('');
        setCategory('');
        setSector('');
        setSummary('');
        setResearchAndDevelopmentNature('');
        setInnovativeAspect('');
        setMainTechnologyArea('');
        setProjectDetail('');
        setKeywords([]);
        setKeywordInput('');
        setFormErrors({});
        setCreateError(null);
    }

    const createMutation = useMutation({
        mutationFn: createManualProject,
        onSuccess: async (_project, request) => {
            await Promise.all([
                queryClient.invalidateQueries({ queryKey: projectQueryKeys.lists() }),
                queryClient.invalidateQueries({
                    queryKey: companyQueryKeys.projectSummary(request.companyId),
                }),
            ]);
            resetState();
            onClose();
            onCreated();
        },
        onError: (error) => {
            if (axios.isAxiosError(error) && isApiErrorResponse(error.response?.data)) {
                const fieldErrors = error.response.data.fieldErrors ?? {};
                const mappedErrors: ManualProjectFormErrors = {};
                Object.entries(fieldErrors).forEach(([field, message]) => {
                    if (field in mappedErrors || [
                        'companyId', 'name', 'startDate', 'durationMonths', 'keywords',
                    ].includes(field)) {
                        mappedErrors[field as keyof ManualProjectFormErrors] = message;
                    }
                });
                setFormErrors(mappedErrors);
            }
            setCreateError(getApiErrorMessage(
                error,
                'Manuel proje oluşturulamadı. Lütfen tekrar deneyin.',
            ));
        },
    });

    function validateForm(): ManualProjectFormErrors {
        const errors: ManualProjectFormErrors = {};
        if (companyId === null) errors.companyId = 'Şirket seçimi zorunludur.';
        if (!name.trim()) errors.name = 'Proje adı boş bırakılamaz.';
        if (!isValidDate(startDate)) errors.startDate = 'Başlangıç tarihi zorunludur.';
        if (!/^\d+$/.test(durationMonths) || Number(durationMonths) <= 0) {
            errors.durationMonths = 'Süre pozitif bir tam sayı olmalıdır.';
        }
        return errors;
    }

    function addKeywords(value: string) {
        const additions = value.split(',');
        setKeywords((current) => normalizeKeywords([...current, ...additions]));
        setKeywordInput('');
    }

    function handleKeywordKeyDown(event: React.KeyboardEvent<HTMLInputElement>) {
        if ((event.key === 'Enter' || event.key === ',') && keywordInput.trim()) {
            event.preventDefault();
            addKeywords(keywordInput);
        }
    }

    function handleClose() {
        if (createMutation.isPending) return;
        resetState();
        onClose();
    }

    function handleSubmit() {
        if (createMutation.isPending) return;
        const errors = validateForm();
        setFormErrors(errors);
        setCreateError(null);
        const firstField = Object.keys(errors)[0];
        if (firstField) {
            requestAnimationFrame(() => document.getElementById(`manual-project-${firstField}`)?.focus());
            return;
        }

        const request: CreateManualProjectRequest = {
            companyId: companyId!,
            name: name.trim(),
            startDate,
            durationMonths: Number(durationMonths),
            projectManager: normalizeOptional(projectManager),
            category: normalizeOptional(category),
            sector: normalizeOptional(sector),
            summary: normalizeOptional(summary),
            researchAndDevelopmentNature: normalizeOptional(researchAndDevelopmentNature),
            innovativeAspect: normalizeOptional(innovativeAspect),
            mainTechnologyArea: normalizeOptional(mainTechnologyArea),
            projectDetail: normalizeOptional(projectDetail),
            keywords: normalizeKeywords(keywords),
        };
        createMutation.mutate(request);
    }

    const displayedEndDate = previewEndDate(startDate, durationMonths);
    const fieldProps = (field: keyof ManualProjectFormErrors) => ({
        error: Boolean(formErrors[field]),
        helperText: formErrors[field],
        id: `manual-project-${field}`,
    });

    return (
        <Dialog fullWidth maxWidth="md" onClose={createMutation.isPending ? undefined : handleClose} open={open}>
            <DialogTitle>Manuel Proje Ekle</DialogTitle>
            <DialogContent dividers>
                <Stack spacing={3} sx={{ pt: 0.5 }}>
                    {createError && <Alert severity="error">{createError}</Alert>}
                    <Section title="Temel Bilgiler">
                        <Box>
                            <Typography component="label" htmlFor="manual-project-companyId" sx={{ display: 'block', fontWeight: 600, mb: 0.75 }} variant="body2">
                                Şirket *
                            </Typography>
                            <ProjectCompanySelection
                                disabled={createMutation.isPending}
                                error={formErrors.companyId}
                                key={open ? 'manual-project-company-selection-open' : 'manual-project-company-selection-closed'}
                                match={manualCompanyMatch}
                                onSelect={setCompanyId}
                                selectedCompanyId={companyId}
                                variant="autocomplete"
                            />
                        </Box>
                        <TextField {...fieldProps('name')} disabled={createMutation.isPending} fullWidth label="Proje Adı *" onChange={(event) => setName(event.target.value)} sx={textFieldSx} value={name} />
                        <TextField disabled={createMutation.isPending} fullWidth label="Proje Yöneticisi" onChange={(event) => setProjectManager(event.target.value)} sx={textFieldSx} value={projectManager} />
                    </Section>

                    <Divider />
                    <Section title="Takvim">
                        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                            <TextField {...fieldProps('startDate')} disabled={createMutation.isPending} fullWidth label="Başlangıç Tarihi *" onChange={(event) => setStartDate(event.target.value)} slotProps={{ inputLabel: { shrink: true } }} sx={textFieldSx} type="date" value={startDate} />
                            <TextField {...fieldProps('durationMonths')} disabled={createMutation.isPending} fullWidth label="Süre (Ay) *" onChange={(event) => setDurationMonths(event.target.value)} slotProps={{ htmlInput: { min: 1, step: 1 }, inputLabel: { shrink: true } }} sx={textFieldSx} type="number" value={durationMonths} />
                            <TextField fullWidth label="Bitiş Tarihi" placeholder="Hesaplanacak" slotProps={{ input: { readOnly: true }, inputLabel: { shrink: true } }} sx={textFieldSx} value={displayedEndDate} />
                        </Stack>
                    </Section>

                    <Divider />
                    <Section title="Sınıflandırma">
                        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                            <TextField disabled={createMutation.isPending} fullWidth label="Kategori" onChange={(event) => setCategory(event.target.value)} sx={textFieldSx} value={category} />
                            <TextField disabled={createMutation.isPending} fullWidth label="Sektör" onChange={(event) => setSector(event.target.value)} sx={textFieldSx} value={sector} />
                        </Stack>
                    </Section>

                    <Divider />
                    <Section title="Teknik İçerik">
                        <TextField disabled={createMutation.isPending} fullWidth label="Özet" minRows={3} multiline onChange={(event) => setSummary(event.target.value)} sx={textFieldSx} value={summary} />
                        <TextField disabled={createMutation.isPending} fullWidth label="Ar-Ge Niteliği" minRows={3} multiline onChange={(event) => setResearchAndDevelopmentNature(event.target.value)} sx={textFieldSx} value={researchAndDevelopmentNature} />
                        <TextField disabled={createMutation.isPending} fullWidth label="Yenilikçi Yön" minRows={3} multiline onChange={(event) => setInnovativeAspect(event.target.value)} sx={textFieldSx} value={innovativeAspect} />
                        <TextField disabled={createMutation.isPending} fullWidth label="Ana Teknoloji Alanı" onChange={(event) => setMainTechnologyArea(event.target.value)} sx={textFieldSx} value={mainTechnologyArea} />
                        <TextField disabled={createMutation.isPending} fullWidth label="Proje Detayı" minRows={4} multiline onChange={(event) => setProjectDetail(event.target.value)} sx={textFieldSx} value={projectDetail} />
                    </Section>

                    <Divider />
                    <Section title="Anahtar Kelimeler">
                        <Paper elevation={0} sx={{ border: '1px solid #D0D5DD', borderRadius: 1, p: 1.25 }}>
                            <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.75, mb: keywords.length > 0 ? 1 : 0 }}>
                                {keywords.map((keyword) => (
                                    <Chip
                                        key={keyword.toLocaleLowerCase('tr-TR')}
                                        label={keyword}
                                        onDelete={createMutation.isPending ? undefined : () => setKeywords((current) => current.filter((item) => item !== keyword))}
                                        size="small"
                                    />
                                ))}
                            </Box>
                            <TextField
                                {...fieldProps('keywords')}
                                disabled={createMutation.isPending}
                                fullWidth
                                helperText={formErrors.keywords ?? 'Anahtar kelimeyi yazıp Enter veya virgülle ekleyin.'}
                                onChange={(event) => setKeywordInput(event.target.value)}
                                onKeyDown={handleKeywordKeyDown}
                                placeholder="Örn. yapay zeka"
                                sx={textFieldSx}
                                value={keywordInput}
                                variant="standard"
                            />
                        </Paper>
                    </Section>
                </Stack>
            </DialogContent>
            <DialogActions sx={{ px: 3, py: 2 }}>
                <Button disabled={createMutation.isPending} onClick={handleClose} sx={{ color: '#526273' }}>
                    Vazgeç
                </Button>
                <Button disabled={createMutation.isPending} loading={createMutation.isPending} onClick={handleSubmit} sx={{ bgcolor: '#2A2B61', '&:hover': { bgcolor: '#202153' } }} variant="contained">
                    Projeyi Oluştur
                </Button>
            </DialogActions>
        </Dialog>
    );
}
