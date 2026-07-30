import { useEffect, useMemo, useRef, useState, type RefObject } from 'react';
import { useMutation } from '@tanstack/react-query';
import CheckCircleOutlineOutlinedIcon from '@mui/icons-material/CheckCircleOutlineOutlined';
import ErrorOutlineOutlinedIcon from '@mui/icons-material/ErrorOutlineOutlined';
import InsertDriveFileOutlinedIcon from '@mui/icons-material/InsertDriveFileOutlined';
import UploadFileOutlinedIcon from '@mui/icons-material/UploadFileOutlined';
import WarningAmberOutlinedIcon from '@mui/icons-material/WarningAmberOutlined';
import {
    Accordion,
    AccordionDetails,
    AccordionSummary,
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
    Typography,
    alpha,
    useMediaQuery,
    useTheme,
} from '@mui/material';
import { ConfirmDialog } from '../../../components/common/ConfirmDialog';
import { importCompaniesFromExcel, previewCompanyImport } from '../companyApi';
import type {
    CompanyImportPreviewResponse,
    CompanyImportPreviewRow,
    CompanyImportResultResponse,
    CompanyImportResultRow,
} from '../company.types';
import { getApiErrorMessage } from '../../../utils/getApiErrorMessage';

type CompanyImportPreviewDialogProps = {
    open: boolean;
    onClose: () => void;
    onImported: () => void;
};

const optionalFallback = '—';
const defaultRowsPerPage = 25;

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
    color = 'text.primary',
    tone = 'default',
}: {
    label: string;
    value: number | string;
    color?: string;
    tone?: 'default' | 'success' | 'error' | 'warning' | 'info';
}) {
    return (
        <Paper
            elevation={0}
            sx={(theme) => {
                const toneColors = {
                    default: theme.palette.grey[500],
                    success: theme.palette.success.main,
                    error: theme.palette.error.main,
                    warning: theme.palette.warning.main,
                    info: theme.palette.info.main,
                };
                const accentColor = toneColors[tone];

                return {
                    position: 'relative',
                    border: '1px solid',
                    borderColor: alpha(accentColor, tone === 'default' ? 0.18 : 0.28),
                    borderRadius: '8px',
                    minWidth: 132,
                    maxWidth: label === 'Dosya adı' ? 360 : undefined,
                    overflow: 'hidden',
                    px: 1.75,
                    py: 1.45,
                    bgcolor: alpha(accentColor, tone === 'default' ? 0.025 : 0.06),
                    boxShadow: `inset 0 1px 0 ${alpha(theme.palette.common.white, 0.75)}`,
                    '&::before': {
                        bgcolor: accentColor,
                        bottom: 0,
                        content: '""',
                        left: 0,
                        position: 'absolute',
                        top: 0,
                        width: 4,
                    },
                };
            }}
        >
            <Typography
                variant="caption"
                color="text.secondary"
                sx={{ display: 'block', fontWeight: 700 }}
            >
                {label}
            </Typography>
            <Typography
                variant="h6"
                sx={{
                    color,
                    fontWeight: 800,
                    lineHeight: 1.2,
                    mt: 0.35,
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                    whiteSpace: 'nowrap',
                }}
            >
                {value}
            </Typography>
        </Paper>
    );
}

function HeaderChips({
    title,
    headers,
    color = 'primary',
}: {
    title: string;
    headers: string[];
    color?: 'primary' | 'warning';
}) {
    return (
        <Paper
            elevation={0}
            sx={(theme) => ({
                border: '1px solid',
                borderColor: 'divider',
                borderRadius: '8px',
                flex: 1,
                minWidth: 0,
                p: 1.5,
                bgcolor: alpha(theme.palette.background.default, 0.62),
            })}
        >
            <Stack
                direction="row"
                spacing={1}
                sx={{ alignItems: 'center', justifyContent: 'space-between', mb: 1 }}
            >
                <Typography variant="body2" sx={{ fontWeight: 800 }}>
                    {title}
                </Typography>
                <Typography variant="caption" color="text.secondary">
                    {headers.length}
                </Typography>
            </Stack>
            {headers.length > 0 ? (
                <Stack direction="row" spacing={0.75} useFlexGap sx={{ flexWrap: 'wrap' }}>
                    {headers.map((header) => (
                        <Chip
                            key={header}
                            label={header}
                            size="small"
                            color={color}
                            variant="outlined"
                            sx={{
                                bgcolor: 'background.paper',
                                fontWeight: 700,
                            }}
                        />
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

function PreviewNoticePanel({
    preview,
    showNoSaveInfo,
}: {
    preview: CompanyImportPreviewResponse;
    showNoSaveInfo: boolean;
}) {
    const hasNotices = preview.invalidRowCount > 0
        || preview.generalWarnings.length > 0
        || preview.generalInformationMessages.length > 0
        || showNoSaveInfo;

    if (!hasNotices) {
        return null;
    }

    return (
        <Paper
            elevation={0}
            sx={{
                border: '1px solid',
                borderColor: 'divider',
                borderRadius: '8px',
                flex: { lg: '0 0 380px' },
                minWidth: 0,
                overflow: 'hidden',
            }}
        >
            <Stack
                direction="row"
                sx={{
                    alignItems: 'center',
                    bgcolor: 'background.default',
                    borderBottom: '1px solid',
                    borderBottomColor: 'divider',
                    justifyContent: 'space-between',
                    px: 1.5,
                    py: 1.1,
                }}
            >
                <Typography variant="body2" sx={{ fontWeight: 800 }}>
                    Önizleme notları
                </Typography>
                <Chip
                    label="Kontrol"
                    size="small"
                    variant="outlined"
                    sx={{ bgcolor: 'background.paper', fontWeight: 700 }}
                />
            </Stack>
            <Stack spacing={1} sx={{ p: 1.25 }}>
                {preview.invalidRowCount > 0 && (
                    <Alert severity="warning" sx={{ borderRadius: '8px', py: 0.5 }}>
                        {preview.invalidRowCount} hatalı kayıt aktarılmayacak.
                        {' '}
                        {preview.validRowCount} geçerli kayıt aktarılabilir.
                    </Alert>
                )}

                {preview.generalWarnings.map((warning) => (
                    <Alert
                        key={warning}
                        severity="warning"
                        sx={{ borderRadius: '8px', py: 0.5 }}
                    >
                        {warning}
                    </Alert>
                ))}

                {preview.generalInformationMessages.map((message) => (
                    <Alert
                        key={message}
                        severity="info"
                        sx={{ borderRadius: '8px', py: 0.5 }}
                    >
                        {message}
                    </Alert>
                ))}

                {showNoSaveInfo && (
                    <Alert severity="info" sx={{ borderRadius: '8px', py: 0.5 }}>
                        Bu aşamada veritabanına kayıt yapılmaz.
                    </Alert>
                )}
            </Stack>
        </Paper>
    );
}

function PreviewSummary({
    preview,
    showNoSaveInfo,
}: {
    preview: CompanyImportPreviewResponse;
    showNoSaveInfo: boolean;
}) {
    return (
        <Stack spacing={2}>
            <Stack
                direction={{ xs: 'column', lg: 'row' }}
                spacing={1.5}
                sx={{ alignItems: 'flex-start' }}
            >
                <Stack spacing={2} sx={{ flex: 1, minWidth: 0 }}>
                    <Stack
                        sx={{
                            display: 'grid',
                            gap: 1.25,
                            gridTemplateColumns: {
                                xs: '1fr',
                                sm: 'repeat(2, minmax(0, 1fr))',
                                md: 'repeat(3, minmax(0, 1fr))',
                            },
                        }}
                    >
                        <SummaryMetric label="Dosya adı" value={preview.fileName} tone="info" />
                        <SummaryMetric label="Sayfa sayısı" value={preview.sheetCount} />
                        <SummaryMetric label="Toplam kayıt" value={preview.totalRowCount} />
                        <SummaryMetric
                            label="Geçerli kayıt"
                            value={preview.validRowCount}
                            color="success.main"
                            tone="success"
                        />
                        <SummaryMetric
                            label="Hatalı kayıt"
                            value={preview.invalidRowCount}
                            color="error.main"
                            tone="error"
                        />
                    </Stack>

                    <Accordion
                        disableGutters
                        elevation={0}
                        sx={{
                            border: '1px solid',
                            borderColor: 'divider',
                            borderRadius: '8px',
                            '&:before': { display: 'none' },
                        }}
                    >
                        <AccordionSummary>
                            <Typography variant="body2" sx={{ fontWeight: 800 }}>
                                Excel sütunları
                            </Typography>
                        </AccordionSummary>
                        <AccordionDetails>
                            <Stack direction={{ xs: 'column', md: 'row' }} spacing={1.5}>
                                <HeaderChips
                                    title="Algılanan sütunlar"
                                    headers={preview.detectedHeaders}
                                />
                                <Divider flexItem orientation="vertical" />
                                <HeaderChips
                                    title="Kullanılmayan sütunlar"
                                    headers={preview.ignoredHeaders}
                                    color="warning"
                                />
                            </Stack>
                        </AccordionDetails>
                    </Accordion>
                </Stack>
                <PreviewNoticePanel
                    preview={preview}
                    showNoSaveInfo={showNoSaveInfo}
                />
            </Stack>
        </Stack>
    );
}

function PreviewList({
    preview,
}: {
    preview: CompanyImportPreviewResponse;
}) {
    const [page, setPage] = useState(0);
    const [rowsPerPage, setRowsPerPage] = useState(defaultRowsPerPage);

    const hiddenValidRowCount = useMemo(
        () => preview.rows.filter((row) =>
            row.valid && row.warnings.length === 0).length,
        [preview.rows],
    );
    const problemRows = useMemo(
        () => preview.rows.filter((row) =>
            !row.valid || row.warnings.length > 0),
        [preview.rows],
    );
    const rowCountText = problemRows.length === preview.invalidRowCount
        ? `${problemRows.length} hatalı kayıt gösteriliyor`
        : `${problemRows.length} problemli kayıt gösteriliyor`;
    const safePage = Math.min(
        page,
        Math.max(Math.ceil(problemRows.length / rowsPerPage) - 1, 0),
    );
    const visibleRows = problemRows.slice(
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
                boxShadow: '0 10px 30px rgba(15, 23, 42, 0.06)',
            }}
        >
            <Stack
                spacing={1.5}
                sx={{
                    bgcolor: 'background.paper',
                    p: 1.75,
                }}
            >
                <Stack spacing={0.25}>
                    <Typography variant="subtitle1" sx={{ fontWeight: 800 }}>
                        Satır önizleme
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                        {rowCountText}
                    </Typography>
                </Stack>
            </Stack>
            <Divider />

            <TableContainer sx={{ maxWidth: '100%', overflowX: 'auto' }}>
                <Table
                    size="small"
                    sx={{ minWidth: 1040, tableLayout: 'fixed' }}
                    aria-label="Şirket Excel önizleme tablosu"
                >
                    <TableHead>
                        <TableRow
                            sx={(theme) => ({
                                '& .MuiTableCell-head': {
                                    bgcolor: alpha(theme.palette.primary.main, 0.04),
                                    borderBottomColor: alpha(theme.palette.primary.main, 0.16),
                                    color: 'text.secondary',
                                    fontSize: 12,
                                    fontWeight: 800,
                                    textTransform: 'uppercase',
                                },
                            })}
                        >
                            <TableCell sx={{ width: 132 }}>Durum</TableCell>
                            <TableCell sx={{ width: 156 }}>Kaynak</TableCell>
                            <TableCell sx={{ width: 280 }}>Kaydedilecek Şirket</TableCell>
                            <TableCell sx={{ width: 280 }}>İletişim</TableCell>
                            <TableCell sx={{ width: 220 }}>Faaliyet Alanı</TableCell>
                            <TableCell sx={{ width: 300 }}>Uyarı/Hata</TableCell>
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
                                    <Typography sx={{ fontWeight: 800 }} noWrap>
                                        {displayOptional(row.name)}
                                    </Typography>
                                </TableCell>
                                <TableCell>
                                    <Stack spacing={0.35} sx={{ minWidth: 0 }}>
                                        <Typography variant="body2" noWrap>
                                            {displayOptional(row.email)}
                                        </Typography>
                                        <Typography variant="caption" color="text.secondary" noWrap>
                                            {displayOptional(row.phone)}
                                        </Typography>
                                        {row.secondaryPhone?.trim() && (
                                            <Typography
                                                variant="caption"
                                                color="text.secondary"
                                                noWrap
                                            >
                                                Tel 2: {row.secondaryPhone}
                                            </Typography>
                                        )}
                                    </Stack>
                                </TableCell>
                                <TableCell>
                                    <Typography variant="body2" noWrap>
                                        {displayOptional(row.activityArea)}
                                    </Typography>
                                </TableCell>
                                <TableCell>
                                    <RowMessages row={row} />
                                </TableCell>
                            </TableRow>
                        ))}
                        {visibleRows.length === 0 && (
                            <TableRow>
                                <TableCell colSpan={6}>
                                    <Typography color="text.secondary" sx={{ py: 2 }}>
                                        {problemRows.length === 0
                                            ? `Tüm kayıtlar geçerli. ${hiddenValidRowCount} kayıt aktarılmaya hazır.`
                                            : 'Gösterilecek problemli kayıt yok.'}
                                    </Typography>
                                </TableCell>
                            </TableRow>
                        )}
                    </TableBody>
                </Table>
            </TableContainer>

            <TablePagination
                component="div"
                count={problemRows.length}
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

function ResultStatus({ row }: { row: CompanyImportResultRow }) {
    if (row.status === 'IMPORTED') {
        return (
            <Chip
                color="success"
                icon={<CheckCircleOutlineOutlinedIcon />}
                label="Eklendi"
                size="small"
                variant="outlined"
            />
        );
    }

    if (row.status === 'DUPLICATE') {
        return (
            <Chip
                color="warning"
                icon={<WarningAmberOutlinedIcon />}
                label="Tekrarlanan"
                size="small"
                variant="outlined"
            />
        );
    }

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

function ImportResultCard({ row }: { row: CompanyImportResultRow }) {
    const tone = row.status === 'IMPORTED'
        ? 'success'
        : row.status === 'DUPLICATE'
            ? 'warning'
            : 'error';

    return (
        <Paper
            elevation={0}
            sx={(theme) => {
                const accent = tone === 'error'
                    ? theme.palette.error.main
                    : tone === 'warning'
                        ? theme.palette.warning.main
                        : theme.palette.success.main;

                return {
                    border: '1px solid',
                    borderColor: alpha(accent, 0.22),
                    borderRadius: '8px',
                    bgcolor: alpha(accent, 0.035),
                    p: 1.25,
                };
            }}
        >
            <Stack spacing={0.75}>
                <Stack
                    direction="row"
                    spacing={1}
                    sx={{ alignItems: 'center', justifyContent: 'space-between' }}
                >
                    <Stack
                        direction="row"
                        spacing={1}
                        sx={{ alignItems: 'center', minWidth: 0 }}
                    >
                        <ResultStatus row={row} />
                        <Typography sx={{ fontWeight: 800, overflowWrap: 'anywhere' }}>
                            {displayOptional(row.name)}
                        </Typography>
                    </Stack>
                    <Chip
                        label={`Excel satırı ${row.rowNumber}`}
                        size="small"
                        color={tone}
                        variant="outlined"
                        sx={{ bgcolor: 'background.paper', fontWeight: 700 }}
                    />
                </Stack>

                <Typography variant="body2" color="text.secondary" noWrap>
                    {displayOptional(row.sheetName)}
                    {' · '}
                    {displayOptional(row.activityArea)}
                    {' · '}
                    {displayOptional(row.email)}
                </Typography>

                {row.messages.length > 0 && (
                    <Typography
                        variant="caption"
                        sx={{
                            color: tone === 'error'
                                ? 'error.main'
                                : tone === 'warning'
                                    ? 'warning.dark'
                                    : 'success.dark',
                            display: '-webkit-box',
                            overflow: 'hidden',
                            WebkitBoxOrient: 'vertical',
                            WebkitLineClamp: 2,
                        }}
                    >
                        {row.messages.join(', ')}
                    </Typography>
                )}
            </Stack>
        </Paper>
    );
}

function ResultRowsPanel({
    title,
    rows,
    tone,
}: {
    title: string;
    rows: CompanyImportResultRow[];
    tone: 'success' | 'warning' | 'error';
}) {
    if (rows.length === 0) {
        return null;
    }

    return (
        <Paper
            elevation={0}
            sx={(theme) => {
                const accent = tone === 'error'
                    ? theme.palette.error.main
                    : tone === 'warning'
                        ? theme.palette.warning.main
                        : theme.palette.success.main;

                return {
                    border: '1px solid',
                    borderColor: alpha(accent, 0.20),
                    borderRadius: '8px',
                    minWidth: 0,
                    overflow: 'hidden',
                    bgcolor: 'background.paper',
                };
            }}
        >
            <Stack
                direction="row"
                spacing={1}
                sx={(theme) => {
                    const accent = tone === 'error'
                        ? theme.palette.error.main
                        : tone === 'warning'
                            ? theme.palette.warning.main
                            : theme.palette.success.main;

                    return {
                        alignItems: 'center',
                        bgcolor: alpha(accent, 0.07),
                        borderBottom: '1px solid',
                        borderBottomColor: alpha(accent, 0.16),
                        justifyContent: 'space-between',
                        px: 1.5,
                        py: 1.25,
                    };
                }}
            >
                <Typography variant="body2" sx={{ fontWeight: 800 }}>
                    {title}
                </Typography>
                <Chip
                    label={rows.length}
                    size="small"
                    color={tone}
                    sx={{ fontWeight: 800, minWidth: 34 }}
                />
            </Stack>
            <Box
                sx={{
                    maxHeight: 360,
                    overflowY: 'auto',
                    p: 1.25,
                }}
            >
                <Stack spacing={1}>
                    {rows.map((row) => (
                        <ImportResultCard
                            key={`${row.sheetName}-${row.rowNumber}-${row.status}`}
                            row={row}
                        />
                    ))}
                </Stack>
            </Box>
        </Paper>
    );
}

function ImportResultSummary({
    result,
    resultSectionRef,
}: {
    result: CompanyImportResultResponse;
    resultSectionRef: RefObject<HTMLDivElement | null>;
}) {
    const duplicateRows = result.rows.filter((row) => row.status === 'DUPLICATE');
    const invalidRows = result.rows.filter((row) => row.status === 'INVALID');

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
                        label="Eklenen"
                        value={result.importedCount}
                        color="success.main"
                        tone="success"
                    />
                    <SummaryMetric
                        label="Tekrarlanan"
                        value={result.duplicateCount}
                        color="warning.main"
                        tone="warning"
                    />
                    <SummaryMetric
                        label="Hatalı"
                        value={result.invalidCount}
                        color={result.invalidCount > 0 ? 'error.main' : 'text.primary'}
                        tone={result.invalidCount > 0 ? 'error' : 'default'}
                    />
                </Stack>

                {duplicateRows.length === 0 && invalidRows.length === 0 && (
                    <Alert severity="success" sx={{ borderRadius: '8px' }}>
                        Tüm geçerli kayıtlar başarıyla aktarıldı.
                    </Alert>
                )}

                {(duplicateRows.length > 0 || invalidRows.length > 0) && (
                    <Box
                        sx={{
                            display: 'grid',
                            gap: 1.5,
                            gridTemplateColumns: {
                                xs: '1fr',
                                md: 'repeat(2, minmax(0, 1fr))',
                            },
                        }}
                    >
                        <ResultRowsPanel
                            title="Tekrarlanan kayıtlar"
                            rows={duplicateRows}
                            tone="warning"
                        />
                        <ResultRowsPanel
                            title="Hatalı olduğu için aktarılmayan kayıtlar"
                            rows={invalidRows}
                            tone="error"
                        />
                    </Box>
                )}
            </Stack>
        </Paper>
    );
}

export function CompanyImportPreviewDialog({
    open,
    onClose,
    onImported,
}: CompanyImportPreviewDialogProps) {
    const theme = useTheme();
    const fullScreen = useMediaQuery(theme.breakpoints.down('sm'));
    const inputRef = useRef<HTMLInputElement | null>(null);
    const contentRef = useRef<HTMLDivElement | null>(null);
    const resultRef = useRef<HTMLDivElement | null>(null);
    const [selectedFile, setSelectedFile] = useState<File | null>(null);
    const [previewedFile, setPreviewedFile] = useState<File | null>(null);
    const [preview, setPreview] = useState<CompanyImportPreviewResponse | null>(null);
    const [importResult, setImportResult] =
        useState<CompanyImportResultResponse | null>(null);
    const [isConfirmOpen, setConfirmOpen] = useState(false);
    const [fileError, setFileError] = useState<string | null>(null);

    const previewMutation = useMutation({
        mutationFn: previewCompanyImport,
        onSuccess: (response, file) => {
            setPreview(response);
            setPreviewedFile(file);
            setImportResult(null);
            setFileError(null);
        },
        onError: (error) => {
            setPreview(null);
            setPreviewedFile(null);
            setFileError(getApiErrorMessage(error, 'Excel dosyası kontrol edilemedi'));
        },
    });

    const importMutation = useMutation({
        mutationFn: importCompaniesFromExcel,
        onSuccess: (response) => {
            setImportResult(response);
            setConfirmOpen(false);
            setFileError(null);
            onImported();
        },
        onError: (error) => {
            setConfirmOpen(false);
            setFileError(getApiErrorMessage(error, 'Excel aktarımı tamamlanamadı'));
        },
    });

    const isBusy = previewMutation.isPending || importMutation.isPending;
    const canImport = Boolean(
        preview
        && selectedFile
        && previewedFile
        && selectedFile === previewedFile
        && preview.validRowCount > 0
        && !isBusy,
    );

    useEffect(() => {
        if (!importResult) {
            return undefined;
        }

        const frameId = window.requestAnimationFrame(() => {
            const result = resultRef.current;

            if (!result) {
                return;
            }

            result.scrollIntoView({
                behavior: 'smooth',
                block: 'start',
            });
            result.focus({ preventScroll: true });
        });

        return () => window.cancelAnimationFrame(frameId);
    }, [importResult]);

    function resetState() {
        setSelectedFile(null);
        setPreviewedFile(null);
        setPreview(null);
        setImportResult(null);
        setConfirmOpen(false);
        setFileError(null);
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
            <DialogTitle>TGB şirketlerini Excel'den önizle</DialogTitle>
            <DialogContent
                ref={contentRef}
                dividers
                sx={{
                    maxHeight: fullScreen ? 'none' : 'calc(100vh - 180px)',
                    overflowY: 'auto',
                }}
            >
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
                            <PreviewSummary
                                preview={preview}
                                showNoSaveInfo={!importResult}
                            />
                            <PreviewList
                                preview={preview}
                            />
                        </>
                    )}

                    {importResult && (
                        <ImportResultSummary
                            result={importResult}
                            resultSectionRef={resultRef}
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
            <ConfirmDialog
                open={isConfirmOpen}
                title="Geçerli kayıtları aktar"
                description="Geçerli şirket kayıtları veritabanına aktarılacak. Hatalı ve tekrarlanan kayıtlar atlanacaktır."
                confirmLabel="Aktar"
                isConfirming={importMutation.isPending}
                onClose={() => {
                    if (!importMutation.isPending) {
                        setConfirmOpen(false);
                    }
                }}
                onConfirm={handleImportConfirm}
            />
        </Dialog>
    );
}
