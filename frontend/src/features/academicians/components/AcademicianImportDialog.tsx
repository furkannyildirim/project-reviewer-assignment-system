import { useEffect, useMemo, useRef, useState, type MouseEvent, type RefObject } from 'react';
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
    Tooltip,
    ToggleButton,
    ToggleButtonGroup,
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

type PreviewRowFilter = 'all' | 'NEW' | 'UPDATE' | 'DUPLICATE' | 'INVALID';

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
    const statusConfig = {
        NEW: {
            color: 'success',
            icon: <CheckCircleOutlineOutlinedIcon />,
            label: 'Yeni',
        },
        UPDATE: {
            color: 'info',
            icon: <CheckCircleOutlineOutlinedIcon />,
            label: 'Guncellenecek',
        },
        DUPLICATE: {
            color: 'warning',
            icon: <WarningAmberOutlinedIcon />,
            label: 'Tekrarli',
        },
        CONFLICT: {
            color: 'error',
            icon: <ErrorOutlineOutlinedIcon />,
            label: 'Cakisma',
        },
        INVALID: {
            color: 'error',
            icon: <ErrorOutlineOutlinedIcon />,
            label: 'Hatali',
        },
    } as const;
    const config = statusConfig[row.status];

    return (
        <Chip
            color={config.color}
            icon={config.icon}
            label={config.label}
            size="small"
            variant="outlined"
        />
    );
}

function RowMessages({ row }: { row: AcademicianImportPreviewRow }) {
    const messages = [
        ...row.errors.map((message) => ({ message, color: 'error.main' })),
        ...row.messages.map((message) => ({
            message,
            color: row.status === 'CONFLICT' || row.status === 'INVALID'
                ? 'error.main'
                : row.status === 'DUPLICATE'
                    ? 'warning.main'
                    : 'success.main',
        })),
        ...row.warnings.map((message) => ({ message, color: 'warning.main' })),
    ];
    if (messages.length === 0) {
        return <Typography color="text.secondary">{optionalFallback}</Typography>;
    }

    return (
        <Stack spacing={0.25} sx={{ maxWidth: 280, minWidth: 0 }}>
            {messages.map(({ message, color }, index) => (
                <Typography
                    key={`${color}-${index}-${message}`}
                    variant="body2"
                    sx={{
                        color,
                        whiteSpace: 'normal',
                        overflowWrap: 'anywhere',
                    }}
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
    preview: AcademicianImportPreviewResponse;
    showNoSaveInfo: boolean;
}) {
    const hasNotices = preview.invalidRowCount > 0
        || preview.generalWarnings.length > 0
        || preview.generalInformationMessages.length > 0
        || hasProjectsHeader(preview.ignoredHeaders)
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

                {hasProjectsHeader(preview.ignoredHeaders) && (
                    <Alert severity="info" sx={{ borderRadius: '8px', py: 0.5 }}>
                        Projeler sütunu bu aktarımda kullanılmayacak.
                    </Alert>
                )}

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
    preview: AcademicianImportPreviewResponse;
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
                        <SummaryMetric label="Toplam kayıt" value={preview.totalDetectedRows} />
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
                        <SummaryMetric
                            label="Uyarı sayısı"
                            value={preview.warningCount}
                            color="warning.main"
                            tone="warning"
                        />
                    </Stack>

                    <Stack direction={{ xs: 'column', md: 'row' }} spacing={1.5}>
                        <HeaderChips
                            title="Algılanan sütunlar"
                            headers={preview.detectedHeaders}
                        />
                        <HeaderChips
                            title="Kullanılmayan sütunlar"
                            headers={preview.ignoredHeaders}
                            color="warning"
                        />
                    </Stack>
                </Stack>
                <PreviewNoticePanel
                    preview={preview}
                    showNoSaveInfo={showNoSaveInfo}
                />
            </Stack>

            {showNoSaveInfo && (
                <Alert severity="info" sx={{ borderRadius: '8px', py: 0.5 }}>
                    Aktarım sırasında kayıtlar yeniden doğrulanır. Bu nedenle aktarım sonucu önizlemeden farklı olabilir.
                </Alert>
            )}
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

function ResultRowLine({
    row,
    tone,
}: {
    row: AcademicianImportResultRow;
    tone: 'warning' | 'error';
}) {
    const messages = row.messages.length > 0
        ? row.messages.join(', ')
        : row.status === 'INVALID'
            ? 'Hatalı kayıt aktarılmadı'
            : row.status === 'FAILED'
                ? 'Kayıt veritabanına yazılamadı'
                : 'Tekrar eden kayıt atlandı';

    return (
        <Paper
            elevation={0}
            sx={(theme) => {
                const accent = tone === 'error'
                    ? theme.palette.error.main
                    : theme.palette.warning.main;

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
                    <Typography variant="body2" sx={{ fontWeight: 800 }} noWrap>
                        {displayOptional(row.sheetName)}
                    </Typography>
                    <Chip
                        label={`Excel satırı ${row.rowNumber}`}
                        size="small"
                        color={tone}
                        variant="outlined"
                        sx={{ bgcolor: 'background.paper', fontWeight: 700 }}
                    />
                </Stack>
                <Typography variant="body2" color="text.secondary" noWrap>
                    {displayOptional(row.firstName)} {displayOptional(row.lastName)}
                    {' · '}
                    {displayOptional(row.title)}
                    {' · '}
                    {displayOptional(row.email)}
                </Typography>
                <Typography
                    variant="caption"
                    sx={{
                        color: tone === 'error' ? 'error.main' : 'warning.dark',
                        display: '-webkit-box',
                        overflow: 'hidden',
                        WebkitBoxOrient: 'vertical',
                        WebkitLineClamp: 2,
                    }}
                >
                    {messages}
                </Typography>
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
    rows: AcademicianImportResultRow[];
    tone: 'warning' | 'error';
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
                    : theme.palette.warning.main;

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
                        : theme.palette.warning.main;

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
                        <ResultRowLine
                            key={`${row.sheetName}-${row.rowNumber}-${row.status}`}
                            row={row}
                            tone={tone}
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
    result: AcademicianImportResultResponse;
    resultSectionRef: RefObject<HTMLDivElement | null>;
}) {
    const invalidOrConflictCount = result.invalidCount + result.conflictCount;

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
                        label="Guncellenen kayit"
                        value={result.updatedCount}
                        color="info.main"
                    />
                    <SummaryMetric
                        label="Atlanan duplicate"
                        value={result.duplicateCount}
                        color="warning.main"
                    />
                    <SummaryMetric
                        label="Hatalı / aktarılamayan kayıt"
                        value={invalidOrConflictCount}
                        color={invalidOrConflictCount > 0 ? 'error.main' : 'text.primary'}
                    />
                    <SummaryMetric
                        label="Başarısız kayıt"
                        value={result.failedCount}
                        color={result.failedCount > 0 ? 'error.main' : 'text.primary'}
                    />
                    <SummaryMetric
                        label="Uyarı sayısı"
                        value={result.warningCount}
                        color="warning.main"
                    />
                </Stack>

                {result.duplicateRows.length === 0
                    && result.conflictRows.length === 0
                    && result.invalidRows.length === 0
                    && result.failedRows.length === 0 && (
                    <Alert severity="success" sx={{ borderRadius: '8px' }}>
                        Tüm geçerli kayıtlar başarıyla aktarıldı.
                    </Alert>
                )}

                {(result.updatedRows.length > 0
                    || result.duplicateRows.length > 0
                    || result.conflictRows.length > 0
                    || result.invalidRows.length > 0
                    || result.failedRows.length > 0) && (
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
                            title="Guncellenen kayitlar"
                            rows={result.updatedRows}
                            tone="warning"
                        />
                        <ResultRowsPanel
                            title="Hata nedeniyle aktarılmayan kayıtlar"
                            rows={result.conflictRows}
                            tone="error"
                        />
                        <ResultRowsPanel
                            title="Duplicate olduğu için atlanan kayıtlar"
                            rows={result.duplicateRows}
                            tone="warning"
                        />
                        <ResultRowsPanel
                            title="Hatalı olduğu için aktarılmayan kayıtlar"
                            rows={result.invalidRows}
                            tone="error"
                        />
                        <ResultRowsPanel
                            title="Veritabanı hatası nedeniyle aktarılamayan kayıtlar"
                            rows={result.failedRows}
                            tone="error"
                        />
                    </Box>
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
    const [page, setPage] = useState(0);
    const [rowsPerPage, setRowsPerPage] = useState(defaultRowsPerPage);
    const [rowFilter, setRowFilter] = useState<PreviewRowFilter>('INVALID');

    const filteredRows = useMemo(
        () => preview.rows.filter((row) => {
            if (rowFilter === 'all') {
                return true;
            }

            if (rowFilter === 'INVALID') {
                return row.status === 'INVALID' || row.status === 'CONFLICT';
            }

            return row.status === rowFilter;
        }),
        [preview.rows, rowFilter],
    );

    const rowCountText = `${filteredRows.length} kayıt gösteriliyor`;

    const safePage = Math.min(
        page,
        Math.max(Math.ceil(filteredRows.length / rowsPerPage) - 1, 0),
    );
    const visibleRows = filteredRows.slice(
        safePage * rowsPerPage,
        safePage * rowsPerPage + rowsPerPage,
    );
    const emptyMessage = 'Gosterilecek kayit yok.';

    function handleRowFilterChange(
        _: MouseEvent<HTMLElement>,
        nextFilter: PreviewRowFilter | null,
    ) {
        if (!nextFilter) {
            return;
        }

        setRowFilter(nextFilter);
        setPage(0);
    }

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
                <Stack
                    direction={{ xs: 'column', sm: 'row' }}
                    spacing={1.5}
                    sx={{
                        alignItems: { xs: 'flex-start', sm: 'center' },
                        justifyContent: 'space-between',
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
                    <ToggleButtonGroup
                        exclusive
                        size="small"
                        value={rowFilter}
                        onChange={handleRowFilterChange}
                        sx={{
                            display: 'grid',
                            gridTemplateColumns: {
                                xs: 'repeat(2, minmax(0, 1fr))',
                                sm: 'repeat(5, minmax(0, 1fr))',
                            },
                            width: { xs: '100%', sm: 560 },
                            '& .MuiToggleButton-root': {
                                minHeight: 34,
                                px: 1.25,
                                whiteSpace: 'nowrap',
                            },
                        }}
                    >
                        <ToggleButton value="all">Tumu</ToggleButton>
                        <ToggleButton value="NEW">Yeni</ToggleButton>
                        <ToggleButton value="UPDATE">Guncellenecek</ToggleButton>
                        <ToggleButton value="DUPLICATE">Tekrarli</ToggleButton>
                        <ToggleButton value="INVALID">Hatali</ToggleButton>
                    </ToggleButtonGroup>
                </Stack>
            </Stack>
            <Divider />

            <TableContainer sx={{ maxWidth: '100%', overflowX: 'auto' }}>
                <Table
                    size="small"
                    sx={{ minWidth: 1040, tableLayout: 'fixed' }}
                    aria-label="Excel önizleme tablosu"
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
                            <TableCell sx={{ width: 260 }}>Kaydedilecek Akademisyen</TableCell>
                            <TableCell sx={{ width: 260 }}>İletişim</TableCell>
                            <TableCell sx={{ width: 230 }}>Anahtar Kelimeler</TableCell>
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
                                    <Stack spacing={0.35} sx={{ minWidth: 0 }}>
                                        <Typography sx={{ fontWeight: 800 }} noWrap>
                                            {displayOptional(row.firstName)}
                                            {' '}
                                            {displayOptional(row.lastName)}
                                        </Typography>
                                        <Typography variant="caption" color="text.secondary" noWrap>
                                            {displayOptional(row.title)}
                                        </Typography>
                                    </Stack>
                                </TableCell>
                                <TableCell>
                                    <Stack spacing={0.35} sx={{ minWidth: 0 }}>
                                        <Typography variant="body2" noWrap>
                                            {displayOptional(row.email)}
                                        </Typography>
                                        <Typography variant="caption" color="text.secondary" noWrap>
                                            {displayOptional(row.phone)}
                                        </Typography>
                                    </Stack>
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
                                <TableCell colSpan={6}>
                                    <Typography color="text.secondary" sx={{ py: 2 }}>
                                        {emptyMessage}
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
                            <PreviewTable
                                key={`${preview.fileName}-${preview.totalDetectedRows}`}
                                preview={preview}
                            />
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
                        aktarım için gönderilecek. {preview?.invalidRowCount ?? 0}
                        {' '}
                        hatalı kayıt aktarılmayacaktır. Sistemde veya dosya
                        içinde tekrar eden kayıtlar ayrıca atlanacaktır. Projeler
                        sütunu kaydedilmeyecektir. Devam etmek istiyor musunuz?
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
