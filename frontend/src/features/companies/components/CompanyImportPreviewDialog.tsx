import { useRef, useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import CheckCircleOutlineOutlinedIcon from '@mui/icons-material/CheckCircleOutlineOutlined';
import ErrorOutlineOutlinedIcon from '@mui/icons-material/ErrorOutlineOutlined';
import InsertDriveFileOutlinedIcon from '@mui/icons-material/InsertDriveFileOutlined';
import UploadFileOutlinedIcon from '@mui/icons-material/UploadFileOutlined';
import WarningAmberOutlinedIcon from '@mui/icons-material/WarningAmberOutlined';
import {
    Alert,
    Box,
    Button,
    Chip,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    LinearProgress,
    Paper,
    Stack,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    Typography,
} from '@mui/material';
import { previewCompanyImport } from '../companyApi';
import type {
    CompanyImportPreviewResponse,
    CompanyImportPreviewRow,
} from '../company.types';
import { getApiErrorMessage } from '../../../utils/getApiErrorMessage';

type CompanyImportPreviewDialogProps = {
    open: boolean;
    onClose: () => void;
};

const optionalFallback = '—';

function displayOptional(value: string | null) {
    return value?.trim() ? value : optionalFallback;
}

function formatFileSize(size: number) {
    if (size < 1024) {
        return `${size} B`;
    }

    if (size < 1024 * 1024) {
        return `${(size / 1024).toFixed(1)} KB`;
    }

    return `${(size / (1024 * 1024)).toFixed(1)} MB`;
}

function SummaryMetric({
    label,
    value,
}: {
    label: string;
    value: number | string;
}) {
    return (
        <Paper
            elevation={0}
            sx={{
                border: '1px solid',
                borderColor: 'divider',
                borderRadius: '8px',
                minWidth: 128,
                px: 1.5,
                py: 1.25,
            }}
        >
            <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 700 }}>
                {label}
            </Typography>
            <Typography variant="h6" sx={{ fontWeight: 800, lineHeight: 1.2 }}>
                {value}
            </Typography>
        </Paper>
    );
}

function HeaderChips({
    title,
    headers,
}: {
    title: string;
    headers: string[];
}) {
    return (
        <Paper
            elevation={0}
            sx={{
                border: '1px solid',
                borderColor: 'divider',
                borderRadius: '8px',
                flex: 1,
                minWidth: 0,
                p: 1.5,
            }}
        >
            <Typography variant="body2" sx={{ fontWeight: 800, mb: 1 }}>
                {title}
            </Typography>
            {headers.length > 0 ? (
                <Stack direction="row" spacing={0.75} useFlexGap sx={{ flexWrap: 'wrap' }}>
                    {headers.map((header) => (
                        <Chip key={header} label={header} size="small" variant="outlined" />
                    ))}
                </Stack>
            ) : (
                <Typography variant="body2" color="text.secondary">
                    {optionalFallback}
                </Typography>
            )}
        </Paper>
    );
}

function RowStatus({ row }: { row: CompanyImportPreviewRow }) {
    if (!row.valid) {
        return (
            <Chip
                color="error"
                icon={<ErrorOutlineOutlinedIcon />}
                label="Hatalı"
                size="small"
                variant="outlined"
            />
        );
    }

    if (row.warnings.length > 0) {
        return (
            <Chip
                color="warning"
                icon={<WarningAmberOutlinedIcon />}
                label="Uyarılı"
                size="small"
                variant="outlined"
            />
        );
    }

    return (
        <Chip
            color="success"
            icon={<CheckCircleOutlineOutlinedIcon />}
            label="Geçerli"
            size="small"
            variant="outlined"
        />
    );
}

function RowMessages({ row }: { row: CompanyImportPreviewRow }) {
    const messages = [...row.errors, ...row.warnings];

    if (messages.length === 0) {
        return <Typography color="text.secondary">{optionalFallback}</Typography>;
    }

    return (
        <Stack spacing={0.25}>
            {messages.map((message) => (
                <Typography
                    key={message}
                    variant="body2"
                    color={row.valid ? 'warning.main' : 'error.main'}
                >
                    {message}
                </Typography>
            ))}
        </Stack>
    );
}

function PreviewSummary({ preview }: { preview: CompanyImportPreviewResponse }) {
    return (
        <Stack spacing={1.5}>
            <Stack direction="row" spacing={1.25} useFlexGap sx={{ flexWrap: 'wrap' }}>
                <SummaryMetric label="Toplam kayıt" value={preview.totalRowCount} />
                <SummaryMetric label="Geçerli kayıt" value={preview.validRowCount} />
                <SummaryMetric label="Hatalı kayıt" value={preview.invalidRowCount} />
                <SummaryMetric label="Uyarı" value={preview.warningCount} />
            </Stack>

            {preview.generalWarnings.map((warning) => (
                <Alert key={warning} severity="warning">{warning}</Alert>
            ))}

            {preview.generalInformationMessages.map((message) => (
                <Alert key={message} severity="info">{message}</Alert>
            ))}

            <Stack direction={{ xs: 'column', md: 'row' }} spacing={1.5}>
                <HeaderChips
                    title="Algılanan sütunlar"
                    headers={preview.detectedHeaders}
                />
                <HeaderChips
                    title="Kullanılmayan sütunlar"
                    headers={preview.ignoredHeaders}
                />
            </Stack>
        </Stack>
    );
}

function PreviewTable({ preview }: { preview: CompanyImportPreviewResponse }) {
    return (
        <TableContainer sx={{ maxWidth: '100%', overflowX: 'auto' }}>
            <Table size="small" sx={{ minWidth: 1040 }} aria-label="Şirket Excel önizleme tablosu">
                <TableHead>
                    <TableRow>
                        <TableCell>Durum</TableCell>
                        <TableCell>Kaynak</TableCell>
                        <TableCell>Şirket adı</TableCell>
                        <TableCell>Faaliyet alanı</TableCell>
                        <TableCell>Telefon</TableCell>
                        <TableCell>Telefon 2</TableCell>
                        <TableCell>E-posta</TableCell>
                        <TableCell>Uyarı/Hata</TableCell>
                    </TableRow>
                </TableHead>
                <TableBody>
                    {preview.rows.map((row, index) => (
                        <TableRow key={`${row.sheetName}-${row.rowNumber}-${index}`} hover>
                            <TableCell><RowStatus row={row} /></TableCell>
                            <TableCell>
                                <Stack spacing={0.25}>
                                    <Typography sx={{ fontWeight: 600 }}>
                                        {displayOptional(row.sheetName)}
                                    </Typography>
                                    <Typography variant="caption" color="text.secondary">
                                        Excel satırı {row.rowNumber}
                                    </Typography>
                                </Stack>
                            </TableCell>
                            <TableCell>{displayOptional(row.name)}</TableCell>
                            <TableCell>{displayOptional(row.activityArea)}</TableCell>
                            <TableCell>{displayOptional(row.phone)}</TableCell>
                            <TableCell>{displayOptional(row.secondaryPhone)}</TableCell>
                            <TableCell>{displayOptional(row.email)}</TableCell>
                            <TableCell><RowMessages row={row} /></TableCell>
                        </TableRow>
                    ))}
                </TableBody>
            </Table>
        </TableContainer>
    );
}

export function CompanyImportPreviewDialog({
    open,
    onClose,
}: CompanyImportPreviewDialogProps) {
    const inputRef = useRef<HTMLInputElement | null>(null);
    const [selectedFile, setSelectedFile] = useState<File | null>(null);
    const [preview, setPreview] = useState<CompanyImportPreviewResponse | null>(null);
    const [fileError, setFileError] = useState<string | null>(null);

    const previewMutation = useMutation({
        mutationFn: previewCompanyImport,
        onSuccess: (response) => {
            setPreview(response);
            setFileError(null);
        },
        onError: (error) => {
            setPreview(null);
            setFileError(getApiErrorMessage(error, 'Excel dosyası kontrol edilemedi'));
        },
    });

    const isBusy = previewMutation.isPending;

    function resetState() {
        setSelectedFile(null);
        setPreview(null);
        setFileError(null);
        previewMutation.reset();
        if (inputRef.current) {
            inputRef.current.value = '';
        }
    }

    function handleClose() {
        if (isBusy) {
            return;
        }

        resetState();
        onClose();
    }

    function handleFileChange(file: File | undefined) {
        setPreview(null);
        previewMutation.reset();

        if (!file) {
            setSelectedFile(null);
            setFileError(null);
            return;
        }

        if (!file.name.toLocaleLowerCase('tr-TR').endsWith('.xlsx')) {
            setSelectedFile(null);
            setFileError('Yalnızca .xlsx uzantılı Excel dosyası seçebilirsiniz.');
            if (inputRef.current) {
                inputRef.current.value = '';
            }
            return;
        }

        setSelectedFile(file);
        setFileError(null);
    }

    function handlePreview() {
        if (!selectedFile || isBusy) {
            return;
        }

        previewMutation.mutate(selectedFile);
    }

    return (
        <Dialog
            open={open}
            fullWidth
            maxWidth="xl"
            onClose={isBusy ? undefined : handleClose}
        >
            <DialogTitle>TGB şirketlerini Excel'den önizle</DialogTitle>
            <DialogContent dividers>
                <Stack spacing={2.5}>
                    <Typography color="text.secondary">
                        Excel dosyasını kontrol ederek kayıtları veritabanına aktarmadan önce önizleyebilirsiniz.
                    </Typography>

                    <Stack
                        direction={{ xs: 'column', md: 'row' }}
                        spacing={1.5}
                        sx={{ alignItems: { xs: 'stretch', md: 'center' } }}
                    >
                        <Button
                            component="label"
                            variant="outlined"
                            startIcon={<UploadFileOutlinedIcon />}
                            disabled={isBusy}
                        >
                            Dosya Seç
                            <input
                                ref={inputRef}
                                hidden
                                type="file"
                                accept=".xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                onChange={(event) =>
                                    handleFileChange(event.target.files?.[0])}
                            />
                        </Button>

                        {selectedFile && (
                            <Paper
                                elevation={0}
                                sx={{
                                    alignItems: 'center',
                                    border: '1px solid',
                                    borderColor: 'divider',
                                    borderRadius: '8px',
                                    display: 'flex',
                                    gap: 1,
                                    minWidth: 0,
                                    px: 1.5,
                                    py: 1,
                                }}
                            >
                                <InsertDriveFileOutlinedIcon color="action" />
                                <Box sx={{ minWidth: 0 }}>
                                    <Typography sx={{ fontWeight: 700 }} noWrap>
                                        {selectedFile.name}
                                    </Typography>
                                    <Typography variant="caption" color="text.secondary">
                                        {formatFileSize(selectedFile.size)}
                                    </Typography>
                                </Box>
                            </Paper>
                        )}
                    </Stack>

                    {fileError && <Alert severity="error">{fileError}</Alert>}

                    {previewMutation.isPending && (
                        <Stack spacing={1}>
                            <LinearProgress />
                            <Typography variant="body2" color="text.secondary">
                                Dosya kontrol ediliyor...
                            </Typography>
                        </Stack>
                    )}

                    {preview && (
                        <>
                            <PreviewSummary preview={preview} />
                            <PreviewTable preview={preview} />
                        </>
                    )}
                </Stack>
            </DialogContent>
            <DialogActions>
                <Button onClick={handleClose} disabled={isBusy}>
                    Kapat
                </Button>
                <Button
                    variant="contained"
                    loading={previewMutation.isPending}
                    disabled={!selectedFile || isBusy}
                    onClick={handlePreview}
                >
                    {preview ? 'Yeniden Kontrol Et' : 'Dosyayı Kontrol Et'}
                </Button>
            </DialogActions>
        </Dialog>
    );
}
