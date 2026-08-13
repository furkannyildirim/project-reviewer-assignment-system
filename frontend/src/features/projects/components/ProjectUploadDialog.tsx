import { useRef, useState } from 'react';
import { useMutation } from '@tanstack/react-query';
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
import { previewProjectDocument, uploadProjectDocument } from '../projectApi';
import type {
    ProjectDocumentPreviewDraft,
    ProjectDocumentPreviewResponse,
    ProjectUploadResponse,
} from '../project.types';
import {
    createProjectDocumentPreviewDraft,
    ProjectDocumentPreviewStep,
} from './ProjectDocumentPreviewStep';

type ProjectUploadDialogProps = {
    open: boolean;
    onClose: () => void;
};

const maxFileSize = 20 * 1024 * 1024;

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

export function ProjectUploadDialog({ open, onClose }: ProjectUploadDialogProps) {
    const inputRef = useRef<HTMLInputElement>(null);
    const [file, setFile] = useState<File | null>(null);
    const [validationError, setValidationError] = useState<string | null>(null);
    const [uploadResponse, setUploadResponse] = useState<ProjectUploadResponse | null>(null);
    const [previewResponse, setPreviewResponse] =
        useState<ProjectDocumentPreviewResponse | null>(null);
    const [previewDraft, setPreviewDraft] = useState<ProjectDocumentPreviewDraft | null>(null);
    const [selectedCompanyId, setSelectedCompanyId] = useState<number | null>(null);
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

    function resetState() {
        setFile(null);
        setValidationError(null);
        setUploadResponse(null);
        setPreviewResponse(null);
        setPreviewDraft(null);
        setSelectedCompanyId(null);
        uploadMutation.reset();
        previewMutation.reset();
        if (inputRef.current) inputRef.current.value = '';
    }

    function handleClose() {
        if (isUploading || isPreparingPreview) return;
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
                        <ProjectDocumentPreviewStep
                            draft={previewDraft}
                            onDraftChange={setPreviewDraft}
                            preview={previewResponse}
                            selectedCompanyId={selectedCompanyId}
                            onCompanySelect={setSelectedCompanyId}
                        />
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
                        <Button onClick={handleClose} sx={cancelButtonSx}>Vazgeç</Button>
                        <Button
                            onClick={() => {
                                setPreviewResponse(null);
                                setPreviewDraft(null);
                                setSelectedCompanyId(null);
                                previewMutation.reset();
                            }}
                            sx={cancelButtonSx}
                        >
                            Geri Dön
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
