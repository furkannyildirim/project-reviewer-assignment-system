import { useEffect, useMemo, useRef, useState, type RefObject } from 'react';
import axios from 'axios';
import { useMutation, useQueryClient } from '@tanstack/react-query';
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
    Divider,
    LinearProgress,
    Paper,
    Stack,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TablePagination,
    TableRow,
    ToggleButton,
    ToggleButtonGroup,
    Tooltip,
    Typography,
    alpha,
    useMediaQuery,
    useTheme,
} from '@mui/material';
import { importAcademicians, previewAcademicianImport } from '../academicianApi';
import { academicianQueryKeys } from '../academicianQueryKeys';
import type {
    AcademicianImportPreviewResponse,
    AcademicianImportPreviewRow,
    AcademicianImportResultResponse,
    AcademicianImportResultRow,
} from '../academician.types';
import { getApiErrorMessage } from '../../../utils/getApiErrorMessage';

type AcademicianImportDialogProps = {
    open: boolean;
    onClose: () => void;
};

type RowFilter = 'all' | 'invalid' | 'warning';

const defaultRowsPerPage = 25;
const optionalFallback = '—';
const revalidationErrorMessage =
    'Dosya yeniden doğrulanırken hata bulundu. Lütfen önizlemeyi tekrar kontrol edin.';

function formatFileSize(size: number) {
    if (size < 1024) {
        return `${size} B`;
    }

    if (size < 1024 * 1024) {
        return `${(size / 1024).toFixed(1)} KB`;
    }

    return `${(size / (1024 * 1024)).toFixed(1)} MB`;
}

function displayOptional(value: string | null) {
    return value?.trim() ? value : optionalFallback;
}

function hasProjectsHeader(headers: string[]) {
    return headers.some((header) =>
        header
            .toLocaleUpperCase('tr-TR')
            .replace(/\s+/g, ' ')
            .includes('PROJELER'),
    );
}

function SummaryMetric({
    label,
    value,
    color = 'text.primary',
}: {
    label: string;
    value: number | string;
    color?: string;
}) {
    return (
        <Paper
            elevation={0}
            sx={{
                border: '1px solid',
                borderColor: 'divider',
                borderRadius: '8px',
                minWidth: 132,
                px: 1.5,
                py: 1.25,
            }}
        >
            <Typography variant="caption" color="text.secondary">
                {label}
            </Typography>
            <Typography
                variant="h6"
                sx={{ color, fontWeight: 700, lineHeight: 1.25, mt: 0.25 }}
            >
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
        <Stack spacing={0.75} sx={{ minWidth: 0 }}>
            <Typography variant="body2" sx={{ fontWeight: 700 }}>
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
        </Stack>
    );
}

function KeywordPreview({ keywords }: { keywords: string[] }) {
    const visibleKeywords = keywords.slice(0, 3);
    const hiddenKeywords = keywords.slice(3);

    if (keywords.length === 0) {
        return <>{optionalFallback}</>;
    }

    return (
        <Stack direction="row" spacing={0.5} useFlexGap sx={{ flexWrap: 'wrap', maxWidth: 260 }}>
            {visibleKeywords.map((keyword) => (
                <Chip
                    key={keyword}
                    label={keyword}
                    size="small"
                    sx={{ maxWidth: 132 }}
                />
            ))}
            {hiddenKeywords.length > 0 && (
                <Tooltip title={hiddenKeywords.join(', ')}>
                    <Chip
                        label={`+${hiddenKeywords.length}`}
                        size="small"
                        variant="outlined"
                    />
                </Tooltip>
            )}
        </Stack>
    );
}

function RowStatus({ row }: { row: AcademicianImportPreviewRow }) {
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

function RowMessages({ row }: { row: AcademicianImportPreviewRow }) {
    const messages = [
        ...row.errors.map((message) => ({ message, color: 'error.main' })),
        ...row.warnings.map((message) => ({ message, color: 'warning.main' })),
    ];
    const visibleMessages = messages.slice(0, 2);
    const hiddenCount = messages.length - visibleMessages.length;

    if (messages.length === 0) {
        return <Typography color="text.secondary">{optionalFallback}</Typography>;
    }

    return (
        <Stack spacing={0.25} sx={{ maxWidth: 280 }}>
            {visibleMessages.map(({ message, color }) => (
                <Typography
                    key={`${color}-${message}`}
                    variant="body2"
                    sx={{
                        color,
                        display: '-webkit-box',
                        overflow: 'hidden',
                        WebkitBoxOrient: 'vertical',
                        WebkitLineClamp: 2,
                    }}
                >
                    {message}
                </Typography>
            ))}
            {hiddenCount > 0 && (
                <Typography variant="caption" color="text.secondary">
                    +{hiddenCount} mesaj
                </Typography>
            )}
        </Stack>
    );
}

function PreviewSummary({ preview }: { preview: AcademicianImportPreviewResponse }) {
    return (
        <Stack spacing={2}>
            <Stack direction="row" spacing={1.25} useFlexGap sx={{ flexWrap: 'wrap' }}>
                <SummaryMetric label="Dosya adı" value={preview.fileName} />
                <SummaryMetric label="Sayfa sayısı" value={preview.sheetCount} />
                <SummaryMetric label="Toplam kayıt" value={preview.totalDetectedRows} />
                <SummaryMetric
                    label="Geçerli kayıt"
                    value={preview.validRowCount}
                    color="success.main"
                />
                <SummaryMetric
                    label="Hatalı kayıt"
                    value={preview.invalidRowCount}
                    color="error.main"
                />
                <SummaryMetric
                    label="Uyarı sayısı"
                    value={preview.warningCount}
                    color="warning.main"
                />
            </Stack>

            {preview.generalWarnings.length > 0 && (
                <Alert severity="warning">
                    <Stack component="ul" sx={{ m: 0, pl: 2 }}>
                        {preview.generalWarnings.map((warning) => (
                            <li key={warning}>{warning}</li>
                        ))}
                    </Stack>
                </Alert>
            )}

            {hasProjectsHeader(preview.ignoredHeaders) && (
                <Alert severity="info">
                    Projeler sütunu bu aktarımda kullanılmayacak.
                </Alert>
            )}

            <Stack direction={{ xs: 'column', md: 'row' }} spacing={2}>
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

function getImportErrorMessage(error: unknown) {
    const message = getApiErrorMessage(error, 'Aktarım tamamlanamadı');

    if (axios.isAxiosError(error) && error.response?.status === 400) {
        return `${revalidationErrorMessage} ${message}`;
    }

    return message;
}

function ResultRowLine({ row }: { row: AcademicianImportResultRow }) {
    const messages = row.messages.length > 0
        ? row.messages.join(', ')
        : 'Tekrar eden kayıt atlandı';

    return (
        <Stack spacing={0.25}>
            <Typography variant="body2" sx={{ fontWeight: 700 }}>
                {displayOptional(row.sheetName)} / Excel satırı {row.rowNumber}
            </Typography>
            <Typography variant="body2" color="text.secondary">
                {displayOptional(row.firstName)} {displayOptional(row.lastName)}
                {' · '}
                {displayOptional(row.title)}
                {' · '}
                {displayOptional(row.email)}
            </Typography>
            <Typography variant="caption" color="text.secondary">
                {messages}
            </Typography>
        </Stack>
    );
}

function ImportResultSummary({
    result,
    resultSectionRef,
}: {
    result: AcademicianImportResultResponse;
    resultSectionRef: RefObject<HTMLDivElement | null>;
}) {
    const failedCount = Math.max(
        result.totalDetectedRows - result.createdCount - result.skippedDuplicateCount,
        0,
    );

    return (
        <Paper
            ref={resultSectionRef}
            tabIndex={-1}
            elevation={0}
            sx={{
                border: '1px solid',
                borderColor: 'divider',
                borderRadius: '8px',
                outline: 'none',
                overflow: 'hidden',
            }}
        >
            <Stack spacing={2} sx={{ p: 1.5 }}>
                <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>
                    Aktarım sonucu
                </Typography>
                <Stack direction="row" spacing={1.25} useFlexGap sx={{ flexWrap: 'wrap' }}>
                    <SummaryMetric
                        label="Oluşturulan kayıt"
                        value={result.createdCount}
                        color="success.main"
                    />
                    <SummaryMetric
                        label="Atlanan duplicate"
                        value={result.skippedDuplicateCount}
                        color="warning.main"
                    />
                    <SummaryMetric
                        label="Başarısız kayıt"
                        value={failedCount}
                        color={failedCount > 0 ? 'error.main' : 'text.primary'}
                    />
                    <SummaryMetric
                        label="Uyarı sayısı"
                        value={result.warningCount}
                        color="warning.main"
                    />
                </Stack>

                {result.generalWarnings.length > 0 && (
                    <Alert severity="warning">
                        <Stack component="ul" sx={{ m: 0, pl: 2 }}>
                            {result.generalWarnings.map((warning) => (
                                <li key={warning}>{warning}</li>
                            ))}
                        </Stack>
                    </Alert>
                )}

                {result.skippedRows.length > 0 && (
                    <Stack spacing={1}>
                        <Typography variant="body2" sx={{ fontWeight: 700 }}>
                            Atlanan kayıtlar
                        </Typography>
                        <Paper
                            elevation={0}
                            sx={{
                                border: '1px solid',
                                borderColor: 'divider',
                                maxHeight: 220,
                                overflowY: 'auto',
                                p: 1.25,
                            }}
                        >
                            <Stack divider={<Divider flexItem />} spacing={1}>
                                {result.skippedRows.map((row) => (
                                    <ResultRowLine
                                        key={`${row.sheetName}-${row.rowNumber}-${row.status}`}
                                        row={row}
                                    />
                                ))}
                            </Stack>
                        </Paper>
                    </Stack>
                )}
            </Stack>
        </Paper>
    );
}

function PreviewTable({
    preview,
}: {
    preview: AcademicianImportPreviewResponse;
}) {
    const [filter, setFilter] = useState<RowFilter>('all');
    const [page, setPage] = useState(0);
    const [rowsPerPage, setRowsPerPage] = useState(defaultRowsPerPage);

    const filteredRows = useMemo(() => {
        if (filter === 'invalid') {
            return preview.rows.filter((row) => !row.valid);
        }

        if (filter === 'warning') {
            return preview.rows.filter((row) => row.valid && row.warnings.length > 0);
        }

        return preview.rows;
    }, [filter, preview.rows]);

    const safePage = Math.min(
        page,
        Math.max(Math.ceil(filteredRows.length / rowsPerPage) - 1, 0),
    );
    const visibleRows = filteredRows.slice(
        safePage * rowsPerPage,
        safePage * rowsPerPage + rowsPerPage,
    );

    return (
        <Paper
            elevation={0}
            sx={{
                border: '1px solid',
                borderColor: 'divider',
                borderRadius: '8px',
                overflow: 'hidden',
            }}
        >
            <Stack
                direction={{ xs: 'column', sm: 'row' }}
                spacing={1.5}
                sx={{
                    alignItems: { xs: 'stretch', sm: 'center' },
                    justifyContent: 'space-between',
                    p: 1.5,
                }}
            >
                <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>
                    Satır önizleme
                </Typography>
                <ToggleButtonGroup
                    exclusive
                    size="small"
                    value={filter}
                    onChange={(_, nextFilter: RowFilter | null) => {
                        if (nextFilter) {
                            setFilter(nextFilter);
                            setPage(0);
                        }
                    }}
                    sx={{
                        alignSelf: { xs: 'stretch', sm: 'center' },
                        '& .MuiToggleButton-root': { flex: { xs: 1, sm: 'initial' } },
                    }}
                >
                    <ToggleButton value="all">Tümü</ToggleButton>
                    <ToggleButton value="invalid">Hatalılar</ToggleButton>
                    <ToggleButton value="warning">Uyarılılar</ToggleButton>
                </ToggleButtonGroup>
            </Stack>
            <Divider />

            <TableContainer sx={{ maxWidth: '100%', overflowX: 'auto' }}>
                <Table
                    size="small"
                    sx={{ minWidth: 1240, tableLayout: 'fixed' }}
                    aria-label="Excel önizleme tablosu"
                >
                    <TableHead>
                        <TableRow>
                            <TableCell sx={{ width: 132 }}>Durum</TableCell>
                            <TableCell sx={{ width: 156 }}>Kaynak</TableCell>
                            <TableCell sx={{ width: 140 }}>Ad</TableCell>
                            <TableCell sx={{ width: 140 }}>Soyad</TableCell>
                            <TableCell sx={{ width: 148 }}>Ünvan</TableCell>
                            <TableCell sx={{ width: 210 }}>E-posta</TableCell>
                            <TableCell sx={{ width: 136 }}>Telefon</TableCell>
                            <TableCell sx={{ width: 222 }}>Anahtar Kelimeler</TableCell>
                            <TableCell sx={{ width: 280 }}>Uyarı/Hata</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {visibleRows.map((row, index) => (
                            <TableRow
                                key={`${row.sheetName}-${row.rowNumber}-${index}`}
                                hover
                                sx={(theme) => ({
                                    bgcolor: !row.valid
                                        ? alpha(theme.palette.error.main, 0.045)
                                        : row.warnings.length > 0
                                            ? alpha(theme.palette.warning.main, 0.055)
                                            : alpha(theme.palette.success.main, 0.035),
                                })}
                            >
                                <TableCell>
                                    <RowStatus row={row} />
                                </TableCell>
                                <TableCell>
                                    <Stack spacing={0.25} sx={{ minWidth: 132 }}>
                                        <Typography variant="body2" sx={{ fontWeight: 600 }} noWrap>
                                            {displayOptional(row.sheetName)}
                                        </Typography>
                                        <Typography variant="caption" color="text.secondary">
                                            Excel satırı {row.rowNumber}
                                        </Typography>
                                    </Stack>
                                </TableCell>
                                <TableCell>
                                    <Typography sx={{ fontWeight: 600 }} noWrap>
                                        {displayOptional(row.firstName)}
                                    </Typography>
                                </TableCell>
                                <TableCell>
                                    <Typography sx={{ fontWeight: 600 }} noWrap>
                                        {displayOptional(row.lastName)}
                                    </Typography>
                                </TableCell>
                                <TableCell>
                                    <Typography noWrap sx={{ maxWidth: 160 }}>
                                        {displayOptional(row.title)}
                                    </Typography>
                                </TableCell>
                                <TableCell>
                                    <Typography noWrap sx={{ maxWidth: 220 }}>
                                        {displayOptional(row.email)}
                                    </Typography>
                                </TableCell>
                                <TableCell>
                                    <Typography noWrap sx={{ maxWidth: 140 }}>
                                        {displayOptional(row.phone)}
                                    </Typography>
                                </TableCell>
                                <TableCell>
                                    <KeywordPreview keywords={row.keywords} />
                                </TableCell>
                                <TableCell>
                                    <RowMessages row={row} />
                                </TableCell>
                            </TableRow>
                        ))}
                        {visibleRows.length === 0 && (
                            <TableRow>
                                <TableCell colSpan={9}>
                                    <Typography color="text.secondary" sx={{ py: 2 }}>
                                        Bu filtrede gösterilecek satır yok.
                                    </Typography>
                                </TableCell>
                            </TableRow>
                        )}
                    </TableBody>
                </Table>
            </TableContainer>

            <TablePagination
                component="div"
                count={filteredRows.length}
                page={safePage}
                rowsPerPage={rowsPerPage}
                rowsPerPageOptions={[25, 50, 100]}
                labelRowsPerPage="Sayfa başına satır"
                labelDisplayedRows={({ from, to, count }) =>
                    `${from}-${to} / ${count}`}
                onPageChange={(_, nextPage) => setPage(nextPage)}
                onRowsPerPageChange={(event) => {
                    setRowsPerPage(Number(event.target.value));
                    setPage(0);
                }}
            />
        </Paper>
    );
}

export function AcademicianImportDialog({
    open,
    onClose,
}: AcademicianImportDialogProps) {
    const theme = useTheme();
    const queryClient = useQueryClient();
    const fullScreen = useMediaQuery(theme.breakpoints.down('sm'));
    const inputRef = useRef<HTMLInputElement | null>(null);
    const resultSectionRef = useRef<HTMLDivElement | null>(null);
    const [selectedFile, setSelectedFile] = useState<File | null>(null);
    const [previewedFile, setPreviewedFile] = useState<File | null>(null);
    const [preview, setPreview] =
        useState<AcademicianImportPreviewResponse | null>(null);
    const [importResult, setImportResult] =
        useState<AcademicianImportResultResponse | null>(null);
    const [fileError, setFileError] = useState<string | null>(null);
    const [importError, setImportError] = useState<string | null>(null);
    const [isConfirmOpen, setConfirmOpen] = useState(false);

    const previewMutation = useMutation({
        mutationFn: previewAcademicianImport,
        onSuccess: (response, file) => {
            setPreview(response);
            setPreviewedFile(file);
            setImportResult(null);
            setFileError(null);
            setImportError(null);
        },
        onError: (error) => {
            setPreview(null);
            setPreviewedFile(null);
            setImportResult(null);
            setFileError(getApiErrorMessage(error, 'Excel dosyası kontrol edilemedi'));
        },
    });

    const importMutation = useMutation({
        mutationFn: importAcademicians,
        onSuccess: async (response) => {
            setImportResult(response);
            setImportError(null);
            setConfirmOpen(false);
            await queryClient.invalidateQueries({
                queryKey: academicianQueryKeys.all,
            });
        },
        onError: (error) => {
            setImportError(getImportErrorMessage(error));
            setConfirmOpen(false);
        },
    });

    const isBusy = previewMutation.isPending || importMutation.isPending;
    const canImport = Boolean(
        preview
        && selectedFile
        && previewedFile
        && selectedFile === previewedFile
        && preview.validRowCount > 0
        && preview.invalidRowCount === 0
        && !isBusy,
    );

    useEffect(() => {
        if (!importResult) {
            return undefined;
        }

        const animationFrameId = window.requestAnimationFrame(() => {
            resultSectionRef.current?.scrollIntoView({
                behavior: 'smooth',
                block: 'start',
            });
            resultSectionRef.current?.focus({ preventScroll: true });
        });

        return () => window.cancelAnimationFrame(animationFrameId);
    }, [importResult]);

    function resetState() {
        setSelectedFile(null);
        setPreviewedFile(null);
        setPreview(null);
        setImportResult(null);
        setFileError(null);
        setImportError(null);
        setConfirmOpen(false);
        previewMutation.reset();
        importMutation.reset();
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
        setPreviewedFile(null);
        setImportResult(null);
        setImportError(null);
        setConfirmOpen(false);
        previewMutation.reset();
        importMutation.reset();

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

        setImportResult(null);
        setImportError(null);
        setConfirmOpen(false);
        previewMutation.mutate(selectedFile);
    }

    function handleImportConfirm() {
        if (!canImport || !previewedFile) {
            return;
        }

        importMutation.mutate(previewedFile);
    }

    return (
        <Dialog
            open={open}
            fullWidth
            fullScreen={fullScreen}
            maxWidth="xl"
            onClose={isBusy ? undefined : handleClose}
        >
            <DialogTitle>Akademisyenleri Excel'den Aktar</DialogTitle>
            <DialogContent
                dividers
                sx={{
                    maxHeight: fullScreen ? 'none' : 'calc(100vh - 180px)',
                    overflowY: 'auto',
                }}
            >
                <Stack spacing={2.5}>
                    <Typography color="text.secondary">
                        Excel dosyanızı yükleyerek kayıtları aktarmadan önce kontrol edebilirsiniz.
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

                    {fileError && (
                        <Alert severity="error">{fileError}</Alert>
                    )}

                    {importError && (
                        <Alert severity="error">{importError}</Alert>
                    )}

                    {preview && preview.invalidRowCount > 0 && (
                        <Alert severity="error">
                            Dosyada hatalı satırlar bulunduğu için aktarım başlatılamaz.
                        </Alert>
                    )}

                    {previewMutation.isPending && (
                        <Stack spacing={1}>
                            <LinearProgress />
                            <Typography variant="body2" color="text.secondary">
                                Dosya kontrol ediliyor...
                            </Typography>
                        </Stack>
                    )}

                    {importMutation.isPending && (
                        <Stack spacing={1}>
                            <LinearProgress />
                            <Typography variant="body2" color="text.secondary">
                                Aktarım yapılıyor...
                            </Typography>
                        </Stack>
                    )}

                    {preview && (
                        <>
                            <PreviewSummary preview={preview} />
                            <PreviewTable preview={preview} />
                            {!importResult && (
                                <Alert severity="info">
                                    Bu aşamada veritabanına kayıt yapılmaz.
                                </Alert>
                            )}
                        </>
                    )}

                    {importResult && (
                        <ImportResultSummary
                            result={importResult}
                            resultSectionRef={resultSectionRef}
                        />
                    )}
                </Stack>
            </DialogContent>
            <DialogActions>
                {importResult ? (
                    <Button onClick={handleClose}>
                        Kapat
                    </Button>
                ) : (
                    <>
                        <Button onClick={handleClose} disabled={isBusy}>
                            Vazgeç
                        </Button>
                        <Button
                            variant={preview ? 'outlined' : 'contained'}
                            loading={previewMutation.isPending}
                            disabled={!selectedFile || isBusy}
                            onClick={handlePreview}
                        >
                            {preview ? 'Yeniden Kontrol Et' : 'Dosyayı Kontrol Et'}
                        </Button>
                        {preview && (
                            <Button
                                variant="contained"
                                loading={importMutation.isPending}
                                disabled={!canImport}
                                onClick={() => setConfirmOpen(true)}
                            >
                                {importMutation.isPending
                                    ? 'Aktarılıyor...'
                                    : `${preview.validRowCount} Geçerli Kaydı Aktar`}
                            </Button>
                        )}
                    </>
                )}
            </DialogActions>
            <Dialog
                open={isConfirmOpen}
                onClose={importMutation.isPending
                    ? undefined
                    : () => setConfirmOpen(false)}
            >
                <DialogTitle>Aktarımı başlat</DialogTitle>
                <DialogContent dividers>
                    <Typography color="text.secondary">
                        Dosyadaki {preview?.validRowCount ?? 0} geçerli kayıt
                        aktarım için gönderilecek. Sistemde veya dosya içinde
                        tekrar eden kayıtlar atlanacaktır. Projeler sütunu
                        kaydedilmeyecektir. Devam etmek istiyor musunuz?
                    </Typography>
                </DialogContent>
                <DialogActions>
                    <Button
                        onClick={() => setConfirmOpen(false)}
                        disabled={importMutation.isPending}
                    >
                        Vazgeç
                    </Button>
                    <Button
                        variant="contained"
                        loading={importMutation.isPending}
                        disabled={!canImport}
                        onClick={handleImportConfirm}
                    >
                        Aktarımı Başlat
                    </Button>
                </DialogActions>
            </Dialog>
        </Dialog>
    );
}
