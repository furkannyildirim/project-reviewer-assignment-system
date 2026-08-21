import { useRef, useState } from 'react';
import axios from 'axios';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import CheckCircleOutlineOutlinedIcon from '@mui/icons-material/CheckCircleOutlineOutlined';
import CloseOutlinedIcon from '@mui/icons-material/CloseOutlined';
import InsertDriveFileOutlinedIcon from '@mui/icons-material/InsertDriveFileOutlined';
import UploadFileOutlinedIcon from '@mui/icons-material/UploadFileOutlined';
import {
    Alert,
    Box,
    Button,
    Chip,
    CircularProgress,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    IconButton,
    Paper,
    Stack,
    Typography,
} from '@mui/material';
import { getApiErrorMessage } from '../../../utils/getApiErrorMessage';
import type { ApiErrorResponse } from '../../../types/api';
import { companyQueryKeys } from '../../companies/companyQueryKeys';
import {
    createProjectFromUpload,
    previewProjectDocument,
    uploadProjectDocument,
} from '../projectApi';
import { projectQueryKeys } from '../projectQueryKeys';
import type {
    CreateProjectFromUploadRequest,
    ProjectDocumentPreviewDraft,
    ProjectDocumentPreviewResponse,
    ProjectUploadResponse,
} from '../project.types';
import {
    createProjectDocumentPreviewDraft,
    type ProjectDocumentPreviewFormErrors,
    ProjectDocumentPreviewStep,
} from './ProjectDocumentPreviewStep';

type ProjectUploadDialogProps = {
    open: boolean;
    onClose: () => void;
    onCreated: () => void;
};

const maxFileSize = 20 * 1024 * 1024;
const formFieldNames = [
    'companyId', 'name', 'projectManager', 'startDate', 'durationMonths',
    'category', 'sector', 'summary', 'researchAndDevelopmentNature',
    'innovativeAspect', 'mainTechnologyArea', 'projectDetail', 'keywords',
] as const;

function formatFileSize(size: number): string {
    if (size < 1024) return `${size} B`;
    if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
    return `${(size / (1024 * 1024)).toFixed(1)} MB`;
}

function getExtension(fileName: string): string {
    return fileName.split('.').pop()?.toLowerCase() ?? '';
}

function validateFile(file: File): string | null {
    if (!['pdf', 'docx'].includes(getExtension(file.name))) {
        return 'Yalnızca PDF veya DOCX dosyası yükleyebilirsiniz.';
    }
    if (file.size === 0) return 'Dosya boş olamaz.';
    if (file.size > maxFileSize) return 'Dosya boyutu 20 MB’ı aşamaz.';
    return null;
}

function normalizeOptional(value: string): string | null {
    const trimmed = value.trim();
    return trimmed || null;
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

function isApiErrorResponse(value: unknown): value is ApiErrorResponse {
    return Boolean(value && typeof value === 'object' && 'message' in value);
}

export function ProjectUploadDialog({ open, onClose, onCreated }: ProjectUploadDialogProps) {
    const inputRef = useRef<HTMLInputElement>(null);
    const queryClient = useQueryClient();
    const [file, setFile] = useState<File | null>(null);
    const [validationError, setValidationError] = useState<string | null>(null);
    const [uploadResponse, setUploadResponse] = useState<ProjectUploadResponse | null>(null);
    const [previewResponse, setPreviewResponse] =
        useState<ProjectDocumentPreviewResponse | null>(null);
    const [previewDraft, setPreviewDraft] = useState<ProjectDocumentPreviewDraft | null>(null);
    const [selectedCompanyId, setSelectedCompanyId] = useState<number | null>(null);
    const [formErrors, setFormErrors] = useState<ProjectDocumentPreviewFormErrors>({});
    const [createError, setCreateError] = useState<string | null>(null);
    const uploadMutation = useMutation({
        mutationFn: uploadProjectDocument,
        onSuccess: (response) => {
            setUploadResponse(response);
            setValidationError(null);
        },
    });
    const previewMutation = useMutation({
        mutationFn: previewProjectDocument,
        onSuccess: (response) => {
            setPreviewResponse(response);
            setPreviewDraft(createProjectDocumentPreviewDraft(response));
            setSelectedCompanyId(response.companyMatch.status === 'UNIQUE_MATCH'
                ? response.companyMatch.matchedCompany?.id ?? null
                : null);
        },
    });
    const isUploading = uploadMutation.isPending;
    const isPreparingPreview = previewMutation.isPending;
    const createMutation = useMutation({
        mutationFn: createProjectFromUpload,
        onSuccess: async (_project, request) => {
            await Promise.all([
                queryClient.invalidateQueries({ queryKey: projectQueryKeys.all }),
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
                const mappedErrors: ProjectDocumentPreviewFormErrors = {};
                Object.entries(fieldErrors).forEach(([field, message]) => {
                    if (formFieldNames.includes(field as typeof formFieldNames[number])) {
                        mappedErrors[field as keyof ProjectDocumentPreviewFormErrors] = message;
                    }
                });
                setFormErrors(mappedErrors);
            }
            setCreateError(axios.isAxiosError(error)
                && isApiErrorResponse(error.response?.data)
                && Object.keys(error.response.data.fieldErrors ?? {}).length > 0
                ? 'Lütfen işaretli alanları kontrol edin.'
                : getApiErrorMessage(error, 'Proje oluşturulamadı. Lütfen tekrar deneyin.'));
        },
    });
    const isCreating = createMutation.isPending;

    function resetState() {
        setFile(null);
        setValidationError(null);
        setUploadResponse(null);
        setPreviewResponse(null);
        setPreviewDraft(null);
        setSelectedCompanyId(null);
        setFormErrors({});
        setCreateError(null);
        uploadMutation.reset();
        previewMutation.reset();
        if (inputRef.current) inputRef.current.value = '';
    }

    function handleClose() {
        if (isUploading || isPreparingPreview || isCreating) return;
        resetState();
        onClose();
    }

    function handleFile(fileToValidate: File | null) {
        if (!fileToValidate || isUploading) return;
        const error = validateFile(fileToValidate);
        setUploadResponse(null);
        setPreviewResponse(null);
        setPreviewDraft(null);
        setSelectedCompanyId(null);
        setFormErrors({});
        setCreateError(null);
        uploadMutation.reset();
        previewMutation.reset();
        setValidationError(error);
        setFile(error ? null : fileToValidate);
        if (error && inputRef.current) inputRef.current.value = '';
    }

    function handleUpload() {
        if (!file || isUploading || uploadResponse) return;
        const error = validateFile(file);
        if (error) {
            setValidationError(error);
            return;
        }
        uploadMutation.mutate(file);
    }

    function handlePreview() {
        if (!uploadResponse || isPreparingPreview) return;
        previewMutation.mutate(uploadResponse.uploadId);
    }

    function validatePreviewForm(): ProjectDocumentPreviewFormErrors {
        if (!previewDraft) return {};
        const errors: ProjectDocumentPreviewFormErrors = {};
        if (selectedCompanyId === null) errors.companyId = 'Şirket seçimi zorunludur.';
        if (!previewDraft.name.trim()) {
            errors.name = 'Proje adı boş bırakılamaz.';
        } else if (previewDraft.name.trim().length > 255) {
            errors.name = 'Proje adı en fazla 255 karakter olabilir.';
        }
        if (previewDraft.projectManager.trim().length > 255) {
            errors.projectManager = 'Proje yöneticisi en fazla 255 karakter olabilir.';
        }
        if (!isValidDate(previewDraft.startDate)) {
            errors.startDate = 'Başlangıç tarihi zorunludur.';
        }
        if (!/^\d+$/.test(previewDraft.durationMonths)
            || Number(previewDraft.durationMonths) <= 0) {
            errors.durationMonths = 'Süre pozitif bir tam sayı olmalıdır.';
        }
        if (previewDraft.category.trim().length > 255) {
            errors.category = 'Kategori en fazla 255 karakter olabilir.';
        }
        if (previewDraft.sector.trim().length > 255) {
            errors.sector = 'Sektör en fazla 255 karakter olabilir.';
        }
        if (normalizeKeywords(previewDraft.keywords).some((keyword) => keyword.length > 255)) {
            errors.keywords = 'Anahtar kelime en fazla 255 karakter olabilir.';
        }
        return errors;
    }

    function handleCreateProject() {
        if (!uploadResponse || !previewDraft || isCreating) return;
        const errors = validatePreviewForm();
        setFormErrors(errors);
        setCreateError(null);
        const firstField = Object.keys(errors)[0];
        if (firstField) {
            requestAnimationFrame(() => document.getElementById(
                `project-preview-${firstField}`)?.focus());
            return;
        }
        const request: CreateProjectFromUploadRequest = {
            uploadId: uploadResponse.uploadId,
            companyId: selectedCompanyId!,
            name: previewDraft.name.trim(),
            projectManager: normalizeOptional(previewDraft.projectManager),
            startDate: previewDraft.startDate,
            durationMonths: Number(previewDraft.durationMonths),
            category: normalizeOptional(previewDraft.category),
            sector: normalizeOptional(previewDraft.sector),
            summary: normalizeOptional(previewDraft.summary),
            researchAndDevelopmentNature: normalizeOptional(previewDraft.researchAndDevelopmentNature),
            innovativeAspect: normalizeOptional(previewDraft.innovativeAspect),
            mainTechnologyArea: normalizeOptional(previewDraft.mainTechnologyArea),
            projectDetail: normalizeOptional(previewDraft.projectDetail),
            keywords: normalizeKeywords(previewDraft.keywords),
        };
        createMutation.mutate(request);
    }

    const displayedFile = uploadResponse
        ? {
            name: uploadResponse.originalFileName,
            size: uploadResponse.fileSize,
            extension: uploadResponse.contentType === 'application/pdf' ? 'PDF' : 'DOCX',
        }
        : file
            ? { name: file.name, size: file.size, extension: getExtension(file.name).toUpperCase() }
            : null;

    return (
        <Dialog
            fullWidth
            maxWidth={previewResponse ? 'lg' : 'sm'}
            open={open}
            onClose={handleClose}
            slotProps={{ paper: { sx: {
                border: '1px solid #E3EAF0',
                borderRadius: '12px',
                maxWidth: previewResponse ? 1120 : 680,
            } } }}
        >
            <DialogTitle sx={{ pb: 1 }}>Yeni Proje</DialogTitle>
            <DialogContent dividers={Boolean(previewResponse)} sx={{ maxHeight: '68vh' }}>
                <Stack spacing={2.25} sx={{ pt: 1 }}>
                    {previewResponse && previewDraft ? (
                        <>
                            {createError && <Alert severity="error">{createError}</Alert>}
                            <ProjectDocumentPreviewStep
                                disabled={isCreating}
                                draft={previewDraft}
                                errors={formErrors}
                                onDraftChange={(nextDraft) => {
                                    setPreviewDraft(nextDraft);
                                    setFormErrors({});
                                    setCreateError(null);
                                }}
                                preview={previewResponse}
                                selectedCompanyId={selectedCompanyId}
                                onCompanySelect={(companyId) => {
                                    setSelectedCompanyId(companyId);
                                    setFormErrors((currentErrors) => ({
                                        ...currentErrors,
                                        companyId: undefined,
                                    }));
                                    setCreateError(null);
                                }}
                            />
                        </>
                    ) : uploadResponse ? (
                        <Stack spacing={2}>
                            <Alert icon={<CheckCircleOutlineOutlinedIcon fontSize="inherit" />} severity="success">
                                Dosya başarıyla yüklendi.
                            </Alert>
                            <FileCard file={displayedFile!} disabled />
                            <Typography color="text.secondary" variant="body2">
                                Proje kaydı tamamlanana kadar dosya geçici olarak saklanacaktır.
                            </Typography>
                            {isPreparingPreview && (
                                <Alert icon={<CircularProgress size={18} />} severity="info">
                                    Belge bilgileri hazırlanıyor…
                                </Alert>
                            )}
                            {previewMutation.isError && (
                                <Alert severity="error">
                                    {getApiErrorMessage(
                                        previewMutation.error,
                                        'Belge bilgileri hazırlanamadı. Lütfen tekrar deneyin.',
                                    )}
                                </Alert>
                            )}
                        </Stack>
                    ) : (
                        <>
                            <Typography color="text.secondary">
                                Proje Bilgi Formunu yükleyerek proje bilgilerini otomatik olarak hazırlayabilirsiniz.
                            </Typography>
                            <input
                                ref={inputRef}
                                accept=".pdf,.docx"
                                hidden
                                type="file"
                                onChange={(event) => handleFile(event.target.files?.[0] ?? null)}
                            />
                            {!file ? (
                                <Paper elevation={0} sx={{
                                    alignItems: 'center', border: '1px dashed #B8C7D4', borderRadius: '10px',
                                    display: 'flex', flexDirection: 'column', gap: 1.25, px: 3, py: 4, textAlign: 'center',
                                }}>
                                    <UploadFileOutlinedIcon sx={{ color: '#2EA8D3', fontSize: 34 }} />
                                    <Typography sx={{ fontWeight: 600 }}>PDF veya DOCX dosyanızı seçin</Typography>
                                    <Typography color="text.secondary" variant="body2">
                                        Desteklenen formatlar: PDF, DOCX · En fazla 20 MB
                                    </Typography>
                                    <Button onClick={() => inputRef.current?.click()} variant="outlined" sx={secondaryButtonSx}>
                                        Dosya Seç
                                    </Button>
                                </Paper>
                            ) : (
                                <FileCard
                                    file={displayedFile!}
                                    disabled={isUploading}
                                    onRemove={() => {
                                        setFile(null);
                                        setValidationError(null);
                                        if (inputRef.current) inputRef.current.value = '';
                                    }}
                                    onReplace={() => inputRef.current?.click()}
                                />
                            )}
                            {validationError && <Alert severity="error">{validationError}</Alert>}
                            {uploadMutation.isError && (
                                <Alert severity="error">
                                    {getApiErrorMessage(uploadMutation.error, 'Dosya yüklenemedi. Lütfen tekrar deneyin.')}
                                </Alert>
                            )}
                        </>
                    )}
                </Stack>
            </DialogContent>
            <DialogActions sx={{
                justifyContent: previewResponse ? 'space-between' : 'flex-end',
                px: 3,
                pb: 2.5,
            }}>
                {previewResponse ? (
                    <>
                        <Button disabled={isCreating} onClick={handleClose} sx={cancelButtonSx}>Vazgeç</Button>
                        <Button
                            disabled={isCreating}
                            onClick={() => {
                                setPreviewResponse(null);
                                setPreviewDraft(null);
                                setSelectedCompanyId(null);
                                setFormErrors({});
                                setCreateError(null);
                                previewMutation.reset();
                            }}
                            sx={cancelButtonSx}
                        >
                            Geri Dön
                        </Button>
                        <Button
                            disabled={isCreating}
                            loading={isCreating}
                            onClick={handleCreateProject}
                            variant="contained"
                            sx={primaryButtonSx}
                        >
                            Projeyi Oluştur
                        </Button>
                    </>
                ) : uploadResponse ? (
                    <>
                        <Button disabled={isPreparingPreview} onClick={handleClose} sx={cancelButtonSx}>Vazgeç</Button>
                        <Button
                            disabled={isPreparingPreview}
                            onClick={handlePreview}
                            startIcon={isPreparingPreview
                                ? <CircularProgress color="inherit" size={16} />
                                : <CheckCircleOutlineOutlinedIcon />}
                            variant="contained"
                            sx={primaryButtonSx}
                        >
                            {isPreparingPreview ? 'Hazırlanıyor' : 'Bilgileri Kontrol Et'}
                        </Button>
                    </>
                ) : (
                    <>
                        <Button disabled={isUploading} onClick={handleClose} sx={cancelButtonSx}>Vazgeç</Button>
                        <Button
                            disabled={!file || isUploading}
                            onClick={handleUpload}
                            startIcon={isUploading ? <CircularProgress color="inherit" size={16} /> : <UploadFileOutlinedIcon />}
                            variant="contained"
                            sx={primaryButtonSx}
                        >
                            {isUploading ? 'Yükleniyor' : 'Dosyayı Yükle'}
                        </Button>
                    </>
                )}
            </DialogActions>
        </Dialog>
    );
}

function FileCard({
    file,
    onRemove,
    onReplace,
    disabled,
}: {
    file: { name: string; size: number; extension: string };
    onRemove?: () => void;
    onReplace?: () => void;
    disabled: boolean;
}) {
    return (
        <Paper elevation={0} sx={{ border: '1px solid #E3EAF0', borderRadius: '10px', p: 1.5 }}>
            <Stack direction="row" spacing={1.25} sx={{ alignItems: 'center' }}>
                <InsertDriveFileOutlinedIcon sx={{ color: '#2EA8D3' }} />
                <Box sx={{ flex: 1, minWidth: 0 }}>
                    <Typography noWrap sx={{ fontWeight: 600 }}>{file.name}</Typography>
                    <Typography color="text.secondary" variant="body2">{formatFileSize(file.size)}</Typography>
                </Box>
                <Chip label={file.extension} size="small" sx={{ bgcolor: '#F2F7FA', color: '#526273' }} />
                {onReplace && <Button disabled={disabled} onClick={onReplace} size="small" sx={cancelButtonSx}>Değiştir</Button>}
                {onRemove && (
                    <IconButton aria-label="Dosyayı kaldır" disabled={disabled} onClick={onRemove} size="small">
                        <CloseOutlinedIcon fontSize="small" />
                    </IconButton>
                )}
            </Stack>
        </Paper>
    );
}

const primaryButtonSx = {
    bgcolor: '#2A2B61',
    color: '#FFFFFF',
    minHeight: 40,
    '&:hover': { bgcolor: '#202153' },
};

const secondaryButtonSx = {
    bgcolor: '#FFFFFF',
    borderColor: '#D9E2EA',
    color: '#2A2B61',
    minHeight: 40,
    '&:hover': { bgcolor: '#FFFFFF', borderColor: '#AEBCC9' },
    '&:focus-visible': {
        borderColor: '#2EA8D3',
        boxShadow: '0 0 0 3px rgba(46, 168, 211, 0.18)',
        outline: 'none',
    },
};

const cancelButtonSx = {
    bgcolor: '#F2F4F7',
    color: '#344054',
    minHeight: 40,
    '&:hover': { bgcolor: '#EAECF0', color: '#1D2939' },
    '&:focus-visible': {
        outline: '2px solid #2EA8D3',
        outlineOffset: 2,
        boxShadow: '0 0 0 3px rgba(46, 168, 211, 0.18)',
    },
};
