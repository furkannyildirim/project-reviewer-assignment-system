import { useEffect, useMemo, useRef, useState, type MouseEvent, type RefObject } from 'react';
import axios from 'axios';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import CheckCircleOutlineOutlinedIcon from '@mui/icons-material/CheckCircleOutlineOutlined';
import ErrorOutlineOutlinedIcon from '@mui/icons-material/ErrorOutlineOutlined';
import InsertDriveFileOutlinedIcon from '@mui/icons-material/InsertDriveFileOutlined';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
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

type PreviewRowFilter = 'all' | 'NEW' | 'DUPLICATE' | 'CONFLICT' | 'INVALID';

const defaultRowsPerPage = 25;
const optionalFallback = '—';
const brandNavy = '#2A2B61';
const brandNavyHover = '#222352';
const softBlue = '#2EA8D3';
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
            sx={(theme) => ({
                border: '1px solid',
                borderColor: 'divider',
                borderRadius: '8px',
                minWidth: 0,
                px: 1.5,
                py: 1.35,
                bgcolor: 'background.paper',
                boxShadow: `0 1px 2px ${alpha(theme.palette.common.black, 0.025)}`,
            })}
        >
            <Stack direction="row" spacing={1.25} sx={{ alignItems: 'center' }}>
                <Box
                    sx={(theme) => {
                        const toneColors = {
                            default: theme.palette.grey[500],
                            success: theme.palette.success.main,
                            error: theme.palette.error.main,
                            warning: theme.palette.warning.main,
                            info: theme.palette.info.main,
                        };

                        return {
                            bgcolor: alpha(toneColors[tone], tone === 'default' ? 0.35 : 0.72),
                            borderRadius: '6px',
                            flex: '0 0 auto',
                            height: 32,
                            width: 4,
                        };
                    }}
                />
                <Box sx={{ minWidth: 0 }}>
                    <Typography
                        variant="caption"
                        color="text.secondary"
                        sx={{ display: 'block', fontWeight: 600, lineHeight: 1.3 }}
                    >
                        {label}
                    </Typography>
                    <Typography
                        variant="h6"
                        sx={{
                            color,
                            fontSize: 20,
                            fontWeight: 750,
                            lineHeight: 1.3,
                            mt: 0.15,
                            overflow: 'hidden',
                            textOverflow: 'ellipsis',
                            whiteSpace: 'nowrap',
                        }}
                    >
                        {value}
                    </Typography>
                </Box>
            </Stack>
        </Paper>
    );
}

function HeaderChips({
    title,
    headers,
    color,
}: {
    title: string;
    headers: string[];
    color?: 'warning';
}) {
    return (
        <Box sx={{ flex: 1, minWidth: 0 }}>
            <Stack
                direction="row"
                spacing={1}
                sx={{ alignItems: 'center', justifyContent: 'space-between', mb: 0.9 }}
            >
                <Typography variant="body2" sx={{ fontWeight: 700 }}>
                    {title}
                </Typography>
                <Typography
                    variant="caption"
                    color="text.secondary"
                    sx={{ bgcolor: 'background.default', borderRadius: 10, px: 0.8, py: 0.15 }}
                >
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
                            variant="outlined"
                            sx={(theme) => ({
                                '&&': {
                                    bgcolor: color === 'warning'
                                        ? alpha(theme.palette.warning.main, 0.035)
                                        : '#F7F8FA',
                                    borderColor: color === 'warning'
                                        ? alpha(theme.palette.warning.main, 0.28)
                                        : '#E2E5EA',
                                    color: color === 'warning'
                                        ? 'text.secondary'
                                        : brandNavy,
                                    fontWeight: 600,
                                    height: 27,
                                },
                            })}
                        />
                    ))}
                </Stack>
            ) : (
                <Typography variant="body2" color="text.secondary">
                    {optionalFallback}
                </Typography>
            )}
        </Box>
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
        DUPLICATE: {
            color: 'warning',
            icon: <WarningAmberOutlinedIcon />,
            label: 'Tekrarlı',
        },
        CONFLICT: {
            color: 'warning',
            icon: <WarningAmberOutlinedIcon />,
            label: 'İncelenecek',
        },
        INVALID: {
            color: 'error',
            icon: <ErrorOutlineOutlinedIcon />,
            label: 'Hatalı',
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
            sx={row.status === 'CONFLICT'
                ? {
                    flexShrink: 0,
                    maxWidth: 'none',
                    '& .MuiChip-label': {
                        overflow: 'visible',
                        textOverflow: 'clip',
                    },
                }
                : undefined}
        />
    );
}

function RowMessages({ row }: { row: AcademicianImportPreviewRow }) {
    const messages = [
        ...row.errors.map((message) => ({ message, color: 'error.main' })),
        ...row.messages.map((message) => ({
            message,
            color: row.status === 'INVALID'
                ? 'error.main'
                : row.status === 'CONFLICT'
                    ? 'warning.main'
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

    const notices = [
        ...(preview.invalidRowCount > 0
            ? [{
                severity: 'warning' as const,
                text: `${preview.invalidRowCount} hatalı kayıt aktarılmayacak. ${preview.newRowCount} yeni kayıt aktarılabilir.`,
            }]
            : []),
        ...preview.generalWarnings.map((text) => ({ severity: 'warning' as const, text })),
        ...preview.generalInformationMessages.map((text) => ({ severity: 'info' as const, text })),
        ...(hasProjectsHeader(preview.ignoredHeaders)
            ? [{ severity: 'info' as const, text: 'Projeler sütunu bu aktarımda kullanılmayacak.' }]
            : []),
        ...(showNoSaveInfo
            ? [{ severity: 'info' as const, text: 'Bu aşamada veritabanına kayıt yapılmaz.' }]
            : []),
    ];

    return (
        <Paper
            elevation={0}
            sx={{
                border: '1px solid',
                borderColor: 'divider',
                borderRadius: '8px',
                height: '100%',
                minWidth: 0,
                overflow: 'hidden',
            }}
        >
            <Stack
                direction="row"
                sx={{
                    alignItems: 'center',
                    bgcolor: '#FAFBFC',
                    borderBottom: '1px solid',
                    borderBottomColor: 'divider',
                    justifyContent: 'space-between',
                    px: 1.75,
                    py: 1.25,
                }}
            >
                <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                    <InfoOutlinedIcon sx={{ color: brandNavy, fontSize: 19 }} />
                    <Typography variant="body2" sx={{ fontWeight: 700 }}>
                    Önizleme notları
                    </Typography>
                </Stack>
                <Typography variant="caption" color="text.secondary">
                    {notices.length} bilgi
                </Typography>
            </Stack>
            <Stack divider={<Divider flexItem />} sx={{ px: 1.75 }}>
                {notices.map((notice, index) => (
                    <Stack
                        key={`${notice.severity}-${index}-${notice.text}`}
                        direction="row"
                        spacing={1.1}
                        sx={{ alignItems: 'flex-start', py: 1.25 }}
                    >
                        <Box
                            sx={(theme) => ({
                                bgcolor: notice.severity === 'warning'
                                    ? alpha(theme.palette.warning.main, 0.82)
                                    : alpha(softBlue, 0.82),
                                borderRadius: '50%',
                                flex: '0 0 auto',
                                height: 7,
                                mt: '7px',
                                width: 7,
                            })}
                        />
                        <Typography variant="body2" color="text.secondary" sx={{ lineHeight: 1.55 }}>
                            {notice.text}
                        </Typography>
                    </Stack>
                ))}
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
        <Stack spacing={1.5}>
            <Paper
                elevation={0}
                sx={{
                    alignItems: { xs: 'flex-start', sm: 'center' },
                    border: '1px solid',
                    borderColor: 'divider',
                    borderRadius: '8px',
                    display: 'flex',
                    flexDirection: { xs: 'column', sm: 'row' },
                    gap: 1.5,
                    justifyContent: 'space-between',
                    minWidth: 0,
                    px: 1.75,
                    py: 1.5,
                }}
            >
                <Stack direction="row" spacing={1.25} sx={{ alignItems: 'center', minWidth: 0 }}>
                    <Box
                        sx={{
                            alignItems: 'center',
                            bgcolor: alpha(brandNavy, 0.07),
                            borderRadius: '8px',
                            color: brandNavy,
                            display: 'flex',
                            flex: '0 0 auto',
                            height: 38,
                            justifyContent: 'center',
                            width: 38,
                        }}
                    >
                        <InsertDriveFileOutlinedIcon fontSize="small" />
                    </Box>
                    <Box sx={{ minWidth: 0 }}>
                        <Typography sx={{ fontWeight: 700 }} noWrap>
                            {preview.fileName}
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                            Excel çalışma kitabı doğrulandı
                        </Typography>
                    </Box>
                </Stack>
                <Stack direction="row" spacing={1.5} useFlexGap sx={{ flexWrap: 'wrap' }}>
                    <Typography variant="body2" color="text.secondary">
                        <Box component="span" sx={{ color: 'text.primary', fontWeight: 700 }}>
                            {preview.sheetCount}
                        </Box>
                        {' '}sayfa
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                        <Box component="span" sx={{ color: 'text.primary', fontWeight: 700 }}>
                            {preview.totalDetectedRows}
                        </Box>
                        {' '}kayıt
                    </Typography>
                </Stack>
            </Paper>

            <Box
                sx={{
                    display: 'grid',
                    gap: 1.5,
                    gridTemplateColumns: { xs: '1fr', lg: 'minmax(0, 1.55fr) minmax(320px, 0.75fr)' },
                }}
            >
                <Stack spacing={1.5} sx={{ minWidth: 0 }}>
                    <Stack
                        sx={{
                            display: 'grid',
                            gap: 1,
                            gridTemplateColumns: {
                                xs: 'repeat(2, minmax(0, 1fr))',
                                sm: 'repeat(2, minmax(0, 1fr))',
                                md: 'repeat(4, minmax(0, 1fr))',
                            },
                        }}
                    >
                        <SummaryMetric label="Toplam kayıt" value={preview.totalDetectedRows} />
                        <SummaryMetric
                            label="Geçerli kayıt"
                            value={preview.validRowCount}
                            color="success.main"
                            tone="success"
                        />
                        <SummaryMetric
                            label="Yeni kayıt"
                            value={preview.newRowCount}
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

                    <Paper
                        elevation={0}
                        sx={{ border: '1px solid', borderColor: 'divider', borderRadius: '8px', p: 1.5 }}
                    >
                        <Typography variant="overline" color="text.secondary" sx={{ fontWeight: 700 }}>
                            Sütun eşleştirmesi
                        </Typography>
                        <Stack
                            direction={{ xs: 'column', md: 'row' }}
                            divider={<Divider flexItem orientation="vertical" />}
                            spacing={1.5}
                            sx={{ mt: 0.5 }}
                        >
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
                    </Paper>
                </Stack>
                <PreviewNoticePanel
                    preview={preview}
                    showNoSaveInfo={showNoSaveInfo}
                />
            </Box>

            {showNoSaveInfo && (
                <Stack
                    direction="row"
                    spacing={1}
                    sx={{
                        alignItems: 'flex-start',
                        bgcolor: '#F8FBFC',
                        border: '1px solid #DCEBF0',
                        borderRadius: '8px',
                        color: 'text.secondary',
                        px: 1.5,
                        py: 1.1,
                    }}
                >
                    <InfoOutlinedIcon sx={{ color: softBlue, flex: '0 0 auto', fontSize: 18, mt: '2px' }} />
                    <Typography variant="body2">
                        Aktarım sırasında kayıtlar yeniden doğrulanır. Bu nedenle aktarım sonucu önizlemeden farklı olabilir.
                    </Typography>
                </Stack>
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

type ResultTone = 'success' | 'warning' | 'error';

function getResultRowFallbackMessage(row: AcademicianImportResultRow) {
    switch (row.status) {
        case 'CREATED':
            return 'Kayıt başarıyla oluşturuldu';
        case 'INVALID':
            return 'Hatalı kayıt aktarılmadı';
        case 'FAILED':
            return 'Kayıt veritabanına yazılamadı';
        case 'CONFLICT':
            return 'Kayıt manuel kontrol için atlandı';
        case 'DUPLICATE':
            return 'Tekrar eden kayıt atlandı';
    }
}

function getResultRowTone(row: AcademicianImportResultRow): ResultTone {
    if (row.status === 'CREATED') {
        return 'success';
    }

    if (row.status === 'INVALID' || row.status === 'FAILED') {
        return 'error';
    }

    return 'warning';
}

function ResultRowLine({
    row,
    tone,
}: {
    row: AcademicianImportResultRow;
    tone: ResultTone;
}) {
    const messages = row.messages.length > 0
        ? row.messages.join(', ')
        : getResultRowFallbackMessage(row);

    return (
        <Box sx={{ px: 1.5, py: 1.25 }}>
            <Stack spacing={0.65}>
                <Stack
                    direction={{ xs: 'column', sm: 'row' }}
                    spacing={1}
                    sx={{
                        alignItems: { xs: 'flex-start', sm: 'center' },
                        justifyContent: 'space-between',
                    }}
                >
                    <Typography variant="body2" sx={{ fontWeight: 700 }}>
                        {displayOptional(row.sheetName)}
                    </Typography>
                    <Chip
                        label={`Excel satırı ${row.rowNumber}`}
                        size="small"
                        variant="outlined"
                        sx={(theme) => {
                            const accent = tone === 'success'
                                ? theme.palette.success.main
                                : tone === 'error'
                                    ? theme.palette.error.main
                                    : theme.palette.warning.main;

                            return {
                                bgcolor: 'background.paper',
                                borderColor: alpha(accent, 0.26),
                                color: 'text.secondary',
                                fontWeight: 600,
                                height: 24,
                            };
                        }}
                    />
                </Stack>
                <Typography
                    variant="body2"
                    color="text.secondary"
                    sx={{ overflowWrap: 'anywhere' }}
                >
                    {displayOptional(row.firstName)} {displayOptional(row.lastName)}
                    {' · '}
                    {displayOptional(row.title)}
                    {' · '}
                    {displayOptional(row.email)}
                </Typography>
                <Typography
                    variant="caption"
                    sx={{
                        color: tone === 'success'
                            ? 'success.dark'
                            : tone === 'error'
                                ? 'error.main'
                                : 'warning.dark',
                        lineHeight: 1.45,
                        overflowWrap: 'anywhere',
                    }}
                >
                    {messages}
                </Typography>
            </Stack>
        </Box>
    );
}

function ResultRowsPanel({
    title,
    rows,
    tone,
    getRowTone,
}: {
    title: string;
    rows: AcademicianImportResultRow[];
    tone: ResultTone;
    getRowTone?: (row: AcademicianImportResultRow) => ResultTone;
}) {
    if (rows.length === 0) {
        return null;
    }

    return (
        <Paper
            elevation={0}
            sx={{
                border: '1px solid',
                borderColor: 'divider',
                borderRadius: '8px',
                height: '100%',
                minWidth: 0,
                overflow: 'hidden',
                bgcolor: 'background.paper',
            }}
        >
            <Stack
                direction="row"
                spacing={1}
                sx={{
                    alignItems: 'center',
                    bgcolor: '#FAFBFC',
                    borderBottom: '1px solid',
                    borderBottomColor: 'divider',
                    justifyContent: 'space-between',
                    px: 1.75,
                    py: 1.35,
                }}
            >
                <Stack direction="row" spacing={1} sx={{ alignItems: 'center', minWidth: 0 }}>
                    <Box
                        sx={(theme) => {
                            const accent = tone === 'success'
                                ? theme.palette.success.main
                                : tone === 'error'
                                    ? theme.palette.error.main
                                    : theme.palette.warning.main;

                            return {
                                bgcolor: accent,
                                borderRadius: '50%',
                                flex: '0 0 auto',
                                height: 7,
                                width: 7,
                            };
                        }}
                    />
                    <Typography variant="body2" sx={{ fontWeight: 700 }}>
                        {title}
                    </Typography>
                </Stack>
                <Chip
                    label={rows.length}
                    size="small"
                    variant="outlined"
                    sx={(theme) => {
                        const accent = tone === 'success'
                            ? theme.palette.success.main
                            : tone === 'error'
                                ? theme.palette.error.main
                                : theme.palette.warning.main;

                        return {
                            bgcolor: 'background.paper',
                            borderColor: alpha(accent, 0.28),
                            color: tone === 'success'
                                ? 'success.dark'
                                : tone === 'error'
                                    ? 'error.main'
                                    : 'warning.dark',
                            fontWeight: 700,
                            height: 25,
                            minWidth: 31,
                        };
                    }}
                />
            </Stack>
            <Box
                sx={{
                    maxHeight: 320,
                    overflowY: 'auto',
                }}
            >
                <Stack divider={<Divider flexItem />}>
                    {rows.map((row) => (
                        <ResultRowLine
                            key={`${row.sheetName}-${row.rowNumber}-${row.status}`}
                            row={row}
                            tone={getRowTone?.(row) ?? tone}
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
    const skippedRows = [
        ...result.duplicateRows,
        ...result.conflictRows,
        ...result.invalidRows,
        ...result.failedRows,
    ];

    return (
        <Paper
            ref={resultSectionRef}
            tabIndex={-1}
            elevation={0}
            sx={{
                border: '1px solid',
                borderColor: 'divider',
                borderRadius: '8px',
                bgcolor: '#FCFCFD',
                outline: 'none',
                overflow: 'hidden',
            }}
        >
            <Stack spacing={1.75} sx={{ p: { xs: 1.25, sm: 1.75 } }}>
                <Stack
                    direction={{ xs: 'column', sm: 'row' }}
                    spacing={1.5}
                    sx={{ alignItems: { xs: 'flex-start', sm: 'center' }, justifyContent: 'space-between' }}
                >
                    <Stack direction="row" spacing={1.25} sx={{ alignItems: 'center' }}>
                        <Box
                            sx={{
                                alignItems: 'center',
                                bgcolor: (theme) => alpha(theme.palette.success.main, 0.1),
                                borderRadius: '50%',
                                color: 'success.main',
                                display: 'flex',
                                height: 40,
                                justifyContent: 'center',
                                width: 40,
                            }}
                        >
                            <CheckCircleOutlineOutlinedIcon />
                        </Box>
                        <Box>
                            <Typography variant="subtitle1" sx={{ fontWeight: 750 }}>
                                Aktarım tamamlandı
                            </Typography>
                            <Typography variant="body2" color="text.secondary">
                                {result.createdCount} yeni kayıt oluşturuldu; ayrıntılar aşağıda.
                            </Typography>
                        </Box>
                    </Stack>
                    <Chip
                        label="Aktarım sonucu"
                        variant="outlined"
                        sx={{ bgcolor: 'background.paper', borderColor: '#D9DCE3', color: brandNavy }}
                    />
                </Stack>
                <Box
                    sx={{
                        display: 'grid',
                        gap: 1,
                        gridTemplateColumns: {
                            xs: 'repeat(2, minmax(0, 1fr))',
                            md: 'repeat(5, minmax(0, 1fr))',
                        },
                    }}
                >
                    <SummaryMetric
                        label="Oluşturulan kayıt"
                        value={result.createdCount}
                        color="success.main"
                        tone="success"
                    />
                    <SummaryMetric
                        label="Atlanan duplicate"
                        value={result.duplicateCount}
                        color="warning.main"
                        tone="warning"
                    />
                    <SummaryMetric
                        label="Hatalı / aktarılamayan kayıt"
                        value={invalidOrConflictCount}
                        color={invalidOrConflictCount > 0 ? 'error.main' : 'text.primary'}
                        tone={invalidOrConflictCount > 0 ? 'error' : 'default'}
                    />
                    <SummaryMetric
                        label="Başarısız kayıt"
                        value={result.failedCount}
                        color={result.failedCount > 0 ? 'error.main' : 'text.primary'}
                        tone={result.failedCount > 0 ? 'error' : 'default'}
                    />
                    <SummaryMetric
                        label="Uyarı sayısı"
                        value={result.warningCount}
                        color="warning.main"
                        tone="warning"
                    />
                </Box>

                {result.duplicateRows.length === 0
                    && result.conflictRows.length === 0
                    && result.invalidRows.length === 0
                    && result.failedRows.length === 0 && (
                    <Alert
                        severity="success"
                        variant="outlined"
                        sx={{ bgcolor: 'background.paper', borderRadius: '8px' }}
                    >
                        Tüm geçerli kayıtlar başarıyla aktarıldı.
                    </Alert>
                )}

                {(result.createdRows.length > 0 || skippedRows.length > 0) && (
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
                            title="Başarılı İşlemler"
                            rows={result.createdRows}
                            tone="success"
                        />
                        <ResultRowsPanel
                            title="Aktarılmayan Kayıtlar"
                            rows={skippedRows}
                            tone="warning"
                            getRowTone={getResultRowTone}
                        />
                    </Box>
                )}
            </Stack>
        </Paper>
    );
}

function PreviewMobileRow({ row }: { row: AcademicianImportPreviewRow }) {
    return (
        <Box sx={{ p: 1.5 }}>
            <Stack spacing={1.25}>
                <Stack
                    direction="row"
                    spacing={1}
                    sx={{ alignItems: 'flex-start', justifyContent: 'space-between' }}
                >
                    <Box sx={{ minWidth: 0 }}>
                        <Typography sx={{ fontWeight: 700, overflowWrap: 'anywhere' }}>
                            {displayOptional(row.firstName)} {displayOptional(row.lastName)}
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                            {displayOptional(row.title)}
                        </Typography>
                    </Box>
                    <RowStatus row={row} />
                </Stack>
                <Stack direction="row" spacing={1.5} useFlexGap sx={{ flexWrap: 'wrap' }}>
                    <Typography variant="caption" color="text.secondary">
                        <Box component="span" sx={{ color: 'text.primary', fontWeight: 600 }}>
                            {displayOptional(row.sheetName)}
                        </Box>
                        {' · '}Satır {row.rowNumber}
                    </Typography>
                    <Typography variant="caption" color="text.secondary" sx={{ overflowWrap: 'anywhere' }}>
                        {displayOptional(row.email)}
                    </Typography>
                </Stack>
                {row.keywords.length > 0 && <KeywordPreview keywords={row.keywords} />}
                <RowMessages row={row} />
            </Stack>
        </Box>
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

    const filterCounts = useMemo(() => {
        const counts: Record<PreviewRowFilter, number> = {
            all: preview.rows.length,
            NEW: 0,
            DUPLICATE: 0,
            CONFLICT: 0,
            INVALID: 0,
        };

        preview.rows.forEach((row) => {
            counts[row.status] += 1;
        });

        return counts;
    }, [preview.rows]);
    const filteredRows = useMemo(
        () => rowFilter === 'all'
            ? preview.rows
            : preview.rows.filter((row) => row.status === rowFilter),
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
    const emptyMessage = 'Gösterilecek kayıt yok.';

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
                boxShadow: '0 8px 24px rgba(15, 23, 42, 0.045)',
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
                        <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>
                            Satır önizleme
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                            {rowCountText}
                        </Typography>
                    </Stack>
                    <Box
                        sx={{
                            display: 'flex',
                            flex: 1,
                            justifyContent: { xs: 'flex-start', sm: 'flex-end' },
                            maxWidth: '100%',
                            minWidth: 0,
                            overflowX: 'auto',
                            width: '100%',
                        }}
                    >
                        <ToggleButtonGroup
                            exclusive
                            size="small"
                            value={rowFilter}
                            onChange={handleRowFilterChange}
                            sx={{
                                display: 'flex',
                                height: 42,
                                minWidth: 760,
                                width: 760,
                                '& .MuiToggleButton-root': {
                                    borderColor: '#D9DDE4',
                                    color: 'text.secondary',
                                    flex: '1 1 0',
                                    fontWeight: 600,
                                    minWidth: 0,
                                    px: 0.5,
                                    whiteSpace: 'nowrap',
                                    '&.Mui-selected': {
                                        bgcolor: brandNavy,
                                        color: '#FFFFFF',
                                        '&:hover': { bgcolor: brandNavyHover },
                                    },
                                    '&:focus-visible': {
                                        outline: `2px solid ${alpha(brandNavy, 0.18)}`,
                                        outlineOffset: -2,
                                    },
                                },
                            }}
                        >
                            {([
                                ['all', 'Tümü'],
                                ['NEW', 'Yeni'],
                                ['DUPLICATE', 'Sistemde mevcut'],
                                ['CONFLICT', 'İncelenecek'],
                                ['INVALID', 'Hatalı'],
                            ] as const).map(([value, label]) => (
                                <ToggleButton key={value} value={value}>
                                    <Stack
                                        component="span"
                                        direction="row"
                                        spacing={0.75}
                                        sx={{ alignItems: 'center', justifyContent: 'center', minWidth: 0 }}
                                    >
                                        <Box component="span">{label}</Box>
                                        <Box
                                            component="span"
                                            sx={{
                                                bgcolor: 'rgba(42, 43, 97, 0.07)',
                                                borderRadius: '999px',
                                                flex: '0 0 auto',
                                                fontSize: '0.72rem',
                                                fontWeight: 700,
                                                lineHeight: 1,
                                                px: 0.65,
                                                py: 0.4,
                                            }}
                                        >
                                            {filterCounts[value]}
                                        </Box>
                                    </Stack>
                                </ToggleButton>
                            ))}
                        </ToggleButtonGroup>
                    </Box>
                </Stack>
            </Stack>
            <Divider />

            <Stack
                divider={<Divider flexItem />}
                sx={{ display: { xs: 'flex', sm: 'none' } }}
            >
                {visibleRows.map((row, index) => (
                    <PreviewMobileRow
                        key={`${row.sheetName}-${row.rowNumber}-${index}`}
                        row={row}
                    />
                ))}
                {visibleRows.length === 0 && (
                    <Typography color="text.secondary" sx={{ px: 1.5, py: 3 }}>
                        {emptyMessage}
                    </Typography>
                )}
            </Stack>

            <TableContainer sx={{ display: { xs: 'none', sm: 'block' }, maxWidth: '100%', overflowX: 'auto' }}>
                <Table
                    size="small"
                    sx={{ minWidth: 1100, tableLayout: 'fixed' }}
                    aria-label="Excel önizleme tablosu"
                >
                    <TableHead>
                        <TableRow
                            sx={{
                                '& .MuiTableCell-head': {
                                    bgcolor: '#F7FAFC',
                                    borderBottomColor: '#E3EAF0',
                                    color: '#344054',
                                    fontSize: 12,
                                    fontWeight: 650,
                                },
                            }}
                        >
                            <TableCell sx={{ width: 118 }}>Durum</TableCell>
                            <TableCell sx={{ width: 140 }}>Kaynak</TableCell>
                            <TableCell sx={{ width: 220 }}>Kaydedilecek Akademisyen</TableCell>
                            <TableCell sx={{ width: 200 }}>İletişim</TableCell>
                            <TableCell sx={{ width: 190 }}>Anahtar Kelimeler</TableCell>
                            <TableCell sx={{ width: 270 }}>Uyarı/Hata</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {visibleRows.map((row, index) => (
                            <TableRow
                                key={`${row.sheetName}-${row.rowNumber}-${index}`}
                                hover
                                sx={(theme) => ({
                                    '& td:first-of-type': {
                                        borderLeft: '3px solid',
                                        borderLeftColor: !row.valid
                                            ? alpha(theme.palette.error.main, 0.72)
                                            : row.warnings.length > 0
                                                ? alpha(theme.palette.warning.main, 0.72)
                                                : alpha(theme.palette.success.main, 0.62),
                                    },
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
                                        <Typography sx={{ fontWeight: 700 }} noWrap>
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
                sx={{
                    overflow: 'hidden',
                    '& .MuiTablePagination-toolbar': {
                        flexWrap: { xs: 'wrap', sm: 'nowrap' },
                        justifyContent: 'flex-end',
                        minHeight: { xs: 72, sm: 52 },
                        px: { xs: 1, sm: 2 },
                    },
                    '& .MuiTablePagination-spacer': { display: { xs: 'none', sm: 'block' } },
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
        && preview.newRowCount > 0
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
            sx={{
                '& .MuiDialog-paper': {
                    bgcolor: '#F7F8FA',
                    border: fullScreen ? 0 : '1px solid #E3E6EB',
                    borderRadius: fullScreen ? 0 : '12px',
                    maxHeight: fullScreen ? '100%' : 'calc(100% - 48px)',
                    overflow: 'hidden',
                },
            }}
        >
            <DialogTitle
                component="div"
                sx={{
                    bgcolor: 'background.paper',
                    borderBottom: '1px solid',
                    borderBottomColor: 'divider',
                    px: { xs: 2, sm: 2.5 },
                    py: 1.75,
                    zIndex: 1,
                }}
            >
                <Typography variant="h6" sx={{ color: '#172033', fontWeight: 700 }}>
                    Akademisyenleri Excel'den Aktar
                </Typography>
                <Typography variant="body2" color="text.secondary" sx={{ mt: 0.35 }}>
                    Dosyayı doğrulayın, kayıtları inceleyin ve aktarım sonucunu tek akışta takip edin.
                </Typography>
            </DialogTitle>
            <DialogContent
                sx={{
                    bgcolor: '#F7F8FA',
                    overflowY: 'auto',
                    overflowX: 'hidden',
                    p: { xs: 1.5, sm: 2 },
                }}
            >
                <Stack spacing={2}>
                    <Paper
                        elevation={0}
                        sx={{
                            alignItems: { xs: 'stretch', sm: 'center' },
                            border: '1px solid',
                            borderColor: 'divider',
                            borderRadius: '8px',
                            display: 'flex',
                            flexDirection: { xs: 'column', sm: 'row' },
                            gap: 1.25,
                            justifyContent: 'space-between',
                            minWidth: 0,
                            p: 1.5,
                        }}
                    >
                        <Stack direction="row" spacing={1.25} sx={{ alignItems: 'center', minWidth: 0 }}>
                            <Box
                                sx={{
                                    alignItems: 'center',
                                    borderRadius: '8px',
                                    bgcolor: alpha(softBlue, 0.09),
                                    color: '#187A9C',
                                    display: 'flex',
                                    flex: '0 0 auto',
                                    height: 38,
                                    justifyContent: 'center',
                                    width: 38,
                                }}
                            >
                                {selectedFile
                                    ? <InsertDriveFileOutlinedIcon fontSize="small" />
                                    : <UploadFileOutlinedIcon fontSize="small" />}
                            </Box>
                            <Box sx={{ minWidth: 0 }}>
                                <Typography sx={{ fontWeight: 700 }} noWrap>
                                    {selectedFile?.name ?? 'Excel dosyası seçin'}
                                </Typography>
                                <Typography variant="caption" color="text.secondary">
                                    {selectedFile
                                        ? `${formatFileSize(selectedFile.size)} · .xlsx`
                                        : 'Yalnızca .xlsx formatı desteklenir'}
                                </Typography>
                            </Box>
                        </Stack>
                        <Button
                            component="label"
                            variant="outlined"
                            startIcon={<UploadFileOutlinedIcon />}
                            disabled={isBusy}
                            sx={{
                                bgcolor: '#FFFFFF',
                                borderColor: '#CDD3DB',
                                color: brandNavy,
                                flex: '0 0 auto',
                                '&:hover': {
                                    bgcolor: alpha(softBlue, 0.055),
                                    borderColor: alpha(softBlue, 0.6),
                                },
                                '&:focus-visible': {
                                    outline: `3px solid ${alpha(brandNavy, 0.18)}`,
                                },
                            }}
                        >
                            {selectedFile ? 'Dosyayı Değiştir' : 'Dosya Seç'}
                            <input
                                ref={inputRef}
                                hidden
                                type="file"
                                accept=".xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                onChange={(event) =>
                                    handleFileChange(event.target.files?.[0])}
                            />
                        </Button>
                    </Paper>

                    {fileError && (
                        <Alert severity="error">{fileError}</Alert>
                    )}

                    {importError && (
                        <Alert severity="error">{importError}</Alert>
                    )}

                    {previewMutation.isPending && (
                        <Stack spacing={1} sx={{ px: 0.5 }}>
                            <LinearProgress sx={{ '& .MuiLinearProgress-bar': { bgcolor: brandNavy } }} />
                            <Typography variant="body2" color="text.secondary">
                                Dosya kontrol ediliyor...
                            </Typography>
                        </Stack>
                    )}

                    {importMutation.isPending && (
                        <Stack spacing={1} sx={{ px: 0.5 }}>
                            <LinearProgress sx={{ '& .MuiLinearProgress-bar': { bgcolor: brandNavy } }} />
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
            <DialogActions
                sx={{
                    bgcolor: 'background.paper',
                    borderTop: '1px solid',
                    borderTopColor: 'divider',
                    flexWrap: 'wrap',
                    gap: 1,
                    justifyContent: 'flex-end',
                    px: { xs: 1.5, sm: 2.5 },
                    py: 1.25,
                    '& > :not(style) ~ :not(style)': { ml: 0 },
                    '& .MuiButton-root': { flex: { xs: '1 1 auto', sm: '0 0 auto' } },
                }}
            >
                {importResult ? (
                    <Button
                        variant="contained"
                        onClick={handleClose}
                        sx={{
                            '&&': {
                                bgcolor: brandNavy,
                                color: '#FFFFFF',
                            },
                            '&&:hover': {
                                bgcolor: brandNavyHover,
                                color: '#FFFFFF',
                            },
                            '&&:focus-visible': {
                                outline: '3px solid rgba(42, 43, 97, 0.18)',
                            },
                        }}
                    >
                        Kapat
                    </Button>
                ) : (
                    <>
                        <Button
                            onClick={handleClose}
                            disabled={isBusy}
                            sx={{
                                '&&:not(.Mui-disabled)': {
                                    bgcolor: '#F1F3F5',
                                    color: '#4B5563',
                                },
                                '&&:not(.Mui-disabled):hover': {
                                    bgcolor: '#E9EBEF',
                                    color: '#4B5563',
                                },
                                '&:focus-visible': {
                                    outline: '3px solid rgba(42, 43, 97, 0.18)',
                                },
                            }}
                        >
                            Vazgeç
                        </Button>
                        <Button
                            variant={preview ? 'outlined' : 'contained'}
                            loading={previewMutation.isPending}
                            disabled={!selectedFile || isBusy}
                            onClick={handlePreview}
                            sx={!preview
                                ? {
                                    bgcolor: brandNavy,
                                    color: '#FFFFFF',
                                    '&:hover': {
                                        bgcolor: brandNavyHover,
                                    },
                                    '&:focus-visible': {
                                        outline: '3px solid rgba(42, 43, 97, 0.18)',
                                    },
                                }
                                : {
                                    bgcolor: '#FFFFFF',
                                    borderColor: '#C9DEE5',
                                    color: '#187A9C',
                                    '&:hover': {
                                        bgcolor: alpha(softBlue, 0.07),
                                        borderColor: alpha(softBlue, 0.55),
                                    },
                                    '&:focus-visible': {
                                        outline: '3px solid rgba(24, 122, 156, 0.18)',
                                    },
                                }}
                        >
                            {preview ? 'Yeniden Kontrol Et' : 'Dosyayı Kontrol Et'}
                        </Button>
                        {preview && (
                            <Button
                                variant="contained"
                                loading={importMutation.isPending}
                                disabled={!canImport}
                                onClick={() => setConfirmOpen(true)}
                                sx={{
                                    bgcolor: brandNavy,
                                    color: '#FFFFFF',
                                    '&:hover': {
                                        bgcolor: brandNavyHover,
                                    },
                                    '&:focus-visible': {
                                        outline: '3px solid rgba(42, 43, 97, 0.18)',
                                    },
                                }}
                            >
                                {importMutation.isPending
                                    ? 'Aktarılıyor...'
                                    : `${preview.newRowCount} Yeni Kaydı Aktar`}
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
                fullWidth
                maxWidth="sm"
                sx={{ '& .MuiDialog-paper': { borderRadius: '10px' } }}
            >
                <DialogTitle sx={{ pb: 1 }}>Aktarımı başlat</DialogTitle>
                <DialogContent>
                    <Stack spacing={1.75}>
                        <Typography color="text.secondary">
                            Dosyadaki kayıtlar son kez doğrulanacak ve uygun olanlar sisteme eklenecek.
                        </Typography>
                        <Box
                            sx={{
                                bgcolor: '#F7F8FA',
                                border: '1px solid',
                                borderColor: 'divider',
                                borderRadius: '8px',
                                display: 'grid',
                                gap: 1,
                                gridTemplateColumns: 'repeat(2, minmax(0, 1fr))',
                                p: 1.5,
                            }}
                        >
                            <Box>
                                <Typography variant="caption" color="text.secondary">
                                    Aktarıma gönderilecek yeni kayıt
                                </Typography>
                                <Typography sx={{ color: brandNavy, fontSize: 20, fontWeight: 750 }}>
                                    {preview?.newRowCount ?? 0}
                                </Typography>
                            </Box>
                            <Box>
                                <Typography variant="caption" color="text.secondary">
                                    Aktarılmayacak hatalı
                                </Typography>
                                <Typography sx={{ color: 'error.main', fontSize: 20, fontWeight: 750 }}>
                                    {preview?.invalidRowCount ?? 0}
                                </Typography>
                            </Box>
                        </Box>
                        <Typography variant="body2" color="text.secondary">
                            Sistemde veya dosya içinde tekrar eden kayıtlar ayrıca atlanacaktır.
                            Projeler sütunu kaydedilmeyecektir. Devam etmek istiyor musunuz?
                        </Typography>
                    </Stack>
                </DialogContent>
                <DialogActions sx={{ px: 3, pb: 2.5, pt: 1.5 }}>
                    <Button
                        onClick={() => setConfirmOpen(false)}
                        disabled={importMutation.isPending}
                        sx={{
                            '&&:not(.Mui-disabled)': {
                                bgcolor: '#F3F4F6',
                                color: '#4B5563',
                            },
                            '&&:not(.Mui-disabled):hover': {
                                bgcolor: '#E9EBEF',
                                color: '#4B5563',
                            },
                            '&:focus-visible': {
                                outline: '3px solid rgba(42, 43, 97, 0.18)',
                            },
                        }}
                    >
                        Vazgeç
                    </Button>
                    <Button
                        variant="contained"
                        loading={importMutation.isPending}
                        disabled={!canImport}
                        onClick={handleImportConfirm}
                        sx={{
                            '&&:not(.Mui-disabled)': {
                                bgcolor: brandNavy,
                                color: '#FFFFFF',
                            },
                            '&&:not(.Mui-disabled):hover': {
                                bgcolor: brandNavyHover,
                                color: '#FFFFFF',
                            },
                            '&:focus-visible': {
                                outline: '3px solid rgba(42, 43, 97, 0.18)',
                            },
                        }}
                    >
                        Aktarımı Başlat
                    </Button>
                </DialogActions>
            </Dialog>
        </Dialog>
    );
}
