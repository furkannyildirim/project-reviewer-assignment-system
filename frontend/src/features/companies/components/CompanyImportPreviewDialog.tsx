import { useEffect, useMemo, useRef, useState, type MouseEvent, type RefObject } from 'react';
import { useMutation } from '@tanstack/react-query';
import CheckCircleOutlineOutlinedIcon from '@mui/icons-material/CheckCircleOutlineOutlined';
import ErrorOutlineOutlinedIcon from '@mui/icons-material/ErrorOutlineOutlined';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
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
    Typography,
    alpha,
    useMediaQuery,
    useTheme,
} from '@mui/material';
import { importCompaniesFromExcel, previewCompanyImport } from '../companyApi';
import type {
    CompanyImportPreviewResponse,
    CompanyImportPreviewRow,
    CompanyImportResultResponse,
    CompanyImportResultRow,
    CompanyType,
} from '../company.types';
import { getApiErrorMessage } from '../../../utils/getApiErrorMessage';

type CompanyImportPreviewDialogProps = {
    open: boolean;
    companyType: CompanyType;
    onClose: () => void;
    onImported: () => void;
};

type PreviewRowCategory = 'new' | 'existing' | 'duplicate' | 'review' | 'invalid';
type PreviewRowFilter = 'all' | PreviewRowCategory;

const optionalFallback = '—';
const defaultRowsPerPage = 25;
const brandNavy = '#2A2B61';
const brandNavyHover = '#222352';
const softBlue = '#2EA8D3';

function displayOptional(value: string | null) {
    return value?.trim() ? value : optionalFallback;
}

function displayOptionalList(values: string[]) {
    const displayValues = values.filter((value) => value.trim());

    if (displayValues.length === 0) {
        return <Typography color="text.secondary">{optionalFallback}</Typography>;
    }

    return (
        <Stack spacing={0.25}>
            {displayValues.map((value) => (
                <Typography key={value} variant="body2" noWrap>
                    {value}
                </Typography>
            ))}
        </Stack>
    );
}

function countImportableRows(preview: CompanyImportPreviewResponse) {
    return preview.rows.filter((row) => row.valid && row.status === 'NEW').length;
}

function getPreviewRowCategory(row: CompanyImportPreviewRow): PreviewRowCategory {
    if (!row.valid || row.errors.length > 0 || row.status === 'INVALID') {
        return 'invalid';
    }

    if (row.status === 'DUPLICATE') {
        return 'duplicate';
    }

    if (row.status === 'EXISTING') {
        return 'existing';
    }

    if (row.status === 'WARNING') {
        return 'review';
    }

    return 'new';
}

function displayInlineList(values: string[]) {
    const displayValues = values.filter((value) => value.trim());
    return displayValues.length > 0 ? displayValues.join(', ') : optionalFallback;
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
                                    color: color === 'warning' ? 'text.secondary' : brandNavy,
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

function RowStatus({ row }: { row: CompanyImportPreviewRow }) {
    if (!row.valid || row.status === 'INVALID') {
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

    if (row.status === 'DUPLICATE') {
        return (
            <Chip
                color="warning"
                icon={<WarningAmberOutlinedIcon />}
                label="Tekrarlı"
                size="small"
                variant="outlined"
            />
        );
    }

    if (row.status === 'WARNING' || row.locationWarnings.length > 0) {
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

    if (row.status === 'EXISTING') {
        return (
            <Chip
                color="default"
                icon={<CheckCircleOutlineOutlinedIcon />}
                label="Mevcut"
                size="small"
                variant="outlined"
            />
        );
    }

    return (
        <Chip
            color="success"
            icon={<CheckCircleOutlineOutlinedIcon />}
            label="Yeni"
            size="small"
            variant="outlined"
        />
    );
}

function RowMessages({ row }: { row: CompanyImportPreviewRow }) {
    const seenMessages = new Set<string>();
    const uniqueMessages = (
        messages: string[],
        color: string,
        type: string,
    ) => messages
        .filter((message) => {
            if (seenMessages.has(message)) {
                return false;
            }

            seenMessages.add(message);
            return true;
        })
        .map((message) => ({
            color,
            message,
            type,
        }));
    const messages = [
        ...uniqueMessages(row.errors, 'error.main', 'error'),
        ...uniqueMessages(row.locationWarnings, 'warning.main', 'location-warning'),
        ...uniqueMessages(row.warnings, 'warning.main', 'warning'),
    ];

    if (messages.length === 0) {
        return <Typography color="text.secondary">{optionalFallback}</Typography>;
    }

    return (
        <Stack spacing={0.25}>
            {messages.map(({ color, message, type }, index) => (
                <Typography
                    key={`${type}-${index}-${message}`}
                    variant="body2"
                    color={color}
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
    const importableRowCount = countImportableRows(preview);
    const hasNotices = preview.invalidRowCount > 0
        || preview.generalWarnings.length > 0
        || preview.generalInformationMessages.length > 0
        || showNoSaveInfo;

    if (!hasNotices) {
        return null;
    }

    const notices = [
        ...(preview.invalidRowCount > 0
            ? [{
                severity: 'warning' as const,
                text: `${preview.invalidRowCount} hatalı kayıt aktarılmayacak. ${preview.validRowCount} satır doğrulamadan geçti; ${importableRowCount} yeni kayıt otomatik aktarılabilir.`,
            }]
            : []),
        ...preview.generalWarnings.map((text) => ({ severity: 'warning' as const, text })),
        ...preview.generalInformationMessages.map((text) => ({ severity: 'info' as const, text })),
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
    preview: CompanyImportPreviewResponse;
    showNoSaveInfo: boolean;
}) {
    const importableRowCount = countImportableRows(preview);

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
                            {preview.totalRowCount}
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
                        <SummaryMetric label="Toplam kayıt" value={preview.totalRowCount} />
                        <SummaryMetric
                            label="Geçerli kayıt"
                            value={preview.validRowCount}
                            color="success.main"
                            tone="success"
                        />
                        <SummaryMetric
                            label="Yeni kayıt"
                            value={importableRowCount}
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
        </Stack>
    );
}

function PreviewList({
    preview,
    companyType,
}: {
    preview: CompanyImportPreviewResponse;
    companyType: CompanyType;
}) {
    const [page, setPage] = useState(0);
    const [rowsPerPage, setRowsPerPage] = useState(defaultRowsPerPage);
    const [rowFilter, setRowFilter] = useState<PreviewRowFilter>('all');

    const categoryCounts = useMemo(() => {
        const counts: Record<PreviewRowFilter, number> = {
            all: preview.rows.length,
            new: 0,
            existing: 0,
            duplicate: 0,
            review: 0,
            invalid: 0,
        };

        preview.rows.forEach((row) => {
            counts[getPreviewRowCategory(row)] += 1;
        });

        return counts;
    }, [preview.rows]);
    const filteredRows = useMemo(
        () => rowFilter === 'all'
            ? preview.rows
            : preview.rows.filter((row) => getPreviewRowCategory(row) === rowFilter),
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
    const emptyMessages: Record<PreviewRowFilter, string> = {
        all: 'Gösterilecek kayıt yok.',
        new: 'Yeni kayıt bulunmuyor.',
        existing: 'Mevcut kayıt bulunmuyor.',
        duplicate: 'Tekrarlı kayıt bulunmuyor.',
        review: 'İncelenecek kayıt bulunmuyor.',
        invalid: 'Hatalı kayıt bulunmuyor.',
    };
    const emptyMessage = emptyMessages[rowFilter];

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
                    sx={{ alignItems: { xs: 'flex-start', sm: 'center' }, justifyContent: 'space-between' }}
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
                                    flex: '1 0 96px',
                                    fontWeight: 600,
                                    minWidth: 0,
                                    px: 0.5,
                                    whiteSpace: 'nowrap',
                                    '&:nth-of-type(1), &:nth-of-type(2), &:nth-of-type(6)': {
                                        flexBasis: 76,
                                    },
                                    '&:nth-of-type(3), &:nth-of-type(4)': {
                                        flexBasis: 154,
                                    },
                                    '&:nth-of-type(5)': {
                                        flexBasis: 112,
                                    },
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
                                ['new', 'Yeni'],
                                ['existing', 'Sistemde mevcut'],
                                ['duplicate', 'Dosya içi tekrar'],
                                ['review', 'İncelenecek'],
                                ['invalid', 'Hatalı'],
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
                                            {categoryCounts[value]}
                                        </Box>
                                    </Stack>
                                </ToggleButton>
                            ))}
                        </ToggleButtonGroup>
                    </Box>
                </Stack>
            </Stack>
            <Divider />

            <TableContainer sx={{ maxWidth: '100%', overflowX: 'auto' }}>
                <Table
                    size="small"
                    sx={{
                        minWidth: companyType === 'EXTERNAL' ? 1320 : 1040,
                        tableLayout: 'fixed',
                    }}
                    aria-label="Şirket Excel önizleme tablosu"
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
                            <TableCell sx={{ width: 132 }}>Durum</TableCell>
                            <TableCell sx={{ width: 156 }}>Kaynak</TableCell>
                            <TableCell sx={{ width: 280 }}>Kaydedilecek Şirket</TableCell>
                            {companyType === 'EXTERNAL' && (
                                <TableCell sx={{ width: 260 }}>Faaliyet Alanı</TableCell>
                            )}
                            <TableCell sx={{ width: 280 }}>
                                {companyType === 'EXTERNAL' ? 'Şirket İletişimi' : 'İletişim'}
                            </TableCell>
                            {companyType === 'EXTERNAL' ? (
                                <>
                                    <TableCell sx={{ width: 240 }}>Yetkili Kişi</TableCell>
                                    <TableCell sx={{ width: 280 }}>Yetkili İletişimi</TableCell>
                                </>
                            ) : (
                                <TableCell sx={{ width: 220 }}>Faaliyet Alanı</TableCell>
                            )}
                            <TableCell sx={{ width: 300 }}>Uyarı/Hata</TableCell>
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
                                    <Typography sx={{ fontWeight: 800 }} noWrap>
                                        {displayOptional(row.name)}
                                    </Typography>
                                </TableCell>
                                {companyType === 'EXTERNAL' && (
                                    <TableCell>
                                        <Typography variant="body2" noWrap>
                                            {displayOptional(row.activityArea)}
                                        </Typography>
                                    </TableCell>
                                )}
                                <TableCell>
                                    <Stack spacing={0.35} sx={{ minWidth: 0 }}>
                                        {displayOptionalList(row.companyPhones)}
                                        <Typography variant="caption" color="text.secondary" noWrap>
                                            {displayInlineList(row.companyEmails)}
                                        </Typography>
                                    </Stack>
                                </TableCell>
                                {companyType === 'EXTERNAL' ? (
                                    <>
                                        <TableCell>
                                            <Stack spacing={0.35} sx={{ minWidth: 0 }}>
                                                <Typography
                                                    variant="body2"
                                                    sx={{ fontWeight: 700 }}
                                                    noWrap
                                                >
                                                    {displayOptional(row.contactFullName)}
                                                </Typography>
                                                <Typography
                                                    variant="caption"
                                                    color="text.secondary"
                                                    noWrap
                                                >
                                                    {displayOptional(row.contactPosition)}
                                                </Typography>
                                            </Stack>
                                        </TableCell>
                                        <TableCell>
                                            <Stack spacing={0.35} sx={{ minWidth: 0 }}>
                                                <Typography variant="body2" noWrap>
                                                    {displayOptional(row.contactPhone)}
                                                </Typography>
                                                <Typography
                                                    variant="caption"
                                                    color="text.secondary"
                                                    noWrap
                                                >
                                                    {displayOptional(row.contactEmail)}
                                                </Typography>
                                            </Stack>
                                        </TableCell>
                                    </>
                                ) : (
                                    <TableCell>
                                        <Typography variant="body2" noWrap>
                                            {displayOptional(row.activityArea)}
                                        </Typography>
                                    </TableCell>
                                )}
                                <TableCell>
                                    <RowMessages row={row} />
                                </TableCell>
                            </TableRow>
                        ))}
                        {visibleRows.length === 0 && (
                            <TableRow>
                                <TableCell colSpan={companyType === 'EXTERNAL' ? 8 : 6}>
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
                label="Tekrarlı"
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
                    <Stack
                        direction={{ xs: 'column', sm: 'row' }}
                        spacing={1}
                        sx={{ alignItems: { xs: 'flex-start', sm: 'center' }, minWidth: 0 }}
                    >
                        <ResultStatus row={row} />
                        <Typography sx={{ fontWeight: 700, overflowWrap: 'anywhere' }}>
                            {displayOptional(row.name)}
                        </Typography>
                    </Stack>
                    <Chip
                        label={`Excel satırı ${row.rowNumber}`}
                        size="small"
                        variant="outlined"
                        sx={(theme) => {
                            const accent = tone === 'error'
                                ? theme.palette.error.main
                                : tone === 'warning'
                                    ? theme.palette.warning.main
                                    : theme.palette.success.main;

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

                <Typography variant="body2" color="text.secondary" sx={{ overflowWrap: 'anywhere' }}>
                    {displayOptional(row.sheetName)}
                    {' · '}
                    {displayOptional(row.activityArea)}
                    {' · '}
                    {displayInlineList(row.companyEmails)}
                </Typography>

                {row.messages.length > 0 && (
                    <Stack spacing={0.25}>
                        {row.messages.map((message, index) => (
                            <Typography
                                key={`${row.sheetName}-${row.rowNumber}-${index}-${message}`}
                                variant="caption"
                                sx={{
                                    color: tone === 'error'
                                        ? 'error.main'
                                        : tone === 'warning'
                                            ? 'warning.dark'
                                            : 'success.dark',
                                    lineHeight: 1.45,
                                    overflowWrap: 'anywhere',
                                }}
                            >
                                {message}
                            </Typography>
                        ))}
                    </Stack>
                )}
            </Stack>
        </Box>
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
            sx={{
                border: '1px solid',
                borderColor: 'divider',
                borderRadius: '8px',
                bgcolor: 'background.paper',
                height: '100%',
                minWidth: 0,
                overflow: 'hidden',
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
                        sx={(theme) => ({
                            bgcolor: tone === 'error'
                                ? theme.palette.error.main
                                : tone === 'warning'
                                    ? theme.palette.warning.main
                                    : theme.palette.success.main,
                            borderRadius: '50%',
                            flex: '0 0 auto',
                            height: 7,
                            width: 7,
                        })}
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
                        const accent = tone === 'error'
                            ? theme.palette.error.main
                            : tone === 'warning'
                                ? theme.palette.warning.main
                                : theme.palette.success.main;

                        return {
                            bgcolor: 'background.paper',
                            borderColor: alpha(accent, 0.28),
                            color: tone === 'error'
                                ? 'error.main'
                                : tone === 'warning'
                                    ? 'warning.dark'
                                    : 'success.dark',
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
    const successfulRows = result.rows.filter((row) => row.status === 'IMPORTED');
    const skippedRows = result.rows.filter((row) => row.status !== 'IMPORTED');
    const skippedCount = result.duplicateCount + result.invalidCount;

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
                            sx={(theme) => ({
                                alignItems: 'center',
                                bgcolor: alpha(theme.palette.success.main, 0.1),
                                borderRadius: '50%',
                                color: 'success.main',
                                display: 'flex',
                                height: 40,
                                justifyContent: 'center',
                                width: 40,
                            })}
                        >
                            <CheckCircleOutlineOutlinedIcon />
                        </Box>
                        <Box>
                            <Typography variant="subtitle1" sx={{ fontWeight: 750 }}>
                                Aktarım tamamlandı
                            </Typography>
                            <Typography variant="body2" color="text.secondary">
                                {result.importedCount} yeni kayıt eklendi; ayrıntılar aşağıda.
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
                            md: 'repeat(3, minmax(0, 1fr))',
                        },
                    }}
                >
                    <SummaryMetric
                        label="Eklenen"
                        value={result.importedCount}
                        color="success.main"
                        tone="success"
                    />
                    <SummaryMetric
                        label="Tekrarlı"
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
                </Box>

                {skippedCount === 0 && (
                    <Alert
                        severity="success"
                        variant="outlined"
                        sx={{ bgcolor: 'background.paper', borderRadius: '8px' }}
                    >
                        Tüm geçerli kayıtlar başarıyla aktarıldı.
                    </Alert>
                )}

                {(successfulRows.length > 0 || skippedRows.length > 0) && (
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
                            rows={successfulRows}
                            tone="success"
                        />
                        <ResultRowsPanel
                            title="Aktarılmayan Kayıtlar"
                            rows={skippedRows}
                            tone="warning"
                        />
                    </Box>
                )}
            </Stack>
        </Paper>
    );
}

export function CompanyImportPreviewDialog({
    open,
    companyType,
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
    const importableRowCount = preview
        ? countImportableRows(preview)
        : 0;
    const importTitle = companyType === 'EXTERNAL'
        ? 'Dış şirketleri Excelden önizle'
        : "TGB şirketlerini Excel'den önizle";

    const previewMutation = useMutation({
        mutationFn: (file: File) => previewCompanyImport(file, companyType),
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
        mutationFn: (file: File) => importCompaniesFromExcel(file, companyType),
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
        && importableRowCount > 0
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
                    {importTitle}
                </Typography>
                <Typography variant="body2" color="text.secondary" sx={{ mt: 0.35 }}>
                    Dosyayı doğrulayın, şirket kayıtlarını inceleyin ve aktarım sonucunu tek akışta takip edin.
                </Typography>
            </DialogTitle>
            <DialogContent
                ref={contentRef}
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

                    {fileError && <Alert severity="error">{fileError}</Alert>}

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
                            <PreviewList
                                key={`${preview.fileName}-${preview.totalRowCount}`}
                                preview={preview}
                                companyType={companyType}
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
                            '&&': { bgcolor: brandNavy, color: '#FFFFFF' },
                            '&&:hover': { bgcolor: brandNavyHover, color: '#FFFFFF' },
                            '&&:focus-visible': { outline: `3px solid ${alpha(brandNavy, 0.18)}` },
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
                                '&&:not(.Mui-disabled)': { bgcolor: '#F1F3F5', color: '#4B5563' },
                                '&&:not(.Mui-disabled):hover': { bgcolor: '#E9EBEF', color: '#4B5563' },
                                '&:focus-visible': { outline: `3px solid ${alpha(brandNavy, 0.18)}` },
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
                                    '&:hover': { bgcolor: brandNavyHover },
                                    '&:focus-visible': { outline: `3px solid ${alpha(brandNavy, 0.18)}` },
                                }
                                : {
                                    bgcolor: '#FFFFFF',
                                    borderColor: '#C9DEE5',
                                    color: '#187A9C',
                                    '&:hover': {
                                        bgcolor: alpha(softBlue, 0.07),
                                        borderColor: alpha(softBlue, 0.55),
                                    },
                                    '&:focus-visible': { outline: '3px solid rgba(24, 122, 156, 0.18)' },
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
                                    '&:hover': { bgcolor: brandNavyHover },
                                    '&:focus-visible': { outline: `3px solid ${alpha(brandNavy, 0.18)}` },
                                }}
                            >
                                {importMutation.isPending
                                    ? 'Aktarılıyor...'
                                    : `${importableRowCount} Yeni Kaydı Aktar`}
                            </Button>
                        )}
                    </>
                )}
            </DialogActions>
            <Dialog
                open={isConfirmOpen}
                onClose={() => {
                    if (!importMutation.isPending) {
                        setConfirmOpen(false);
                    }
                }}
                fullWidth
                maxWidth="sm"
                sx={{ '& .MuiDialog-paper': { borderRadius: '10px' } }}
            >
                <DialogTitle sx={{ pb: 1 }}>Yeni kayıtları aktar</DialogTitle>
                <DialogContent>
                    <Stack spacing={1.75}>
                        <Typography color="text.secondary">
                            Dosyadaki kayıtlar son kez doğrulanacak ve uygun olan yeni şirketler sisteme eklenecek.
                        </Typography>
                        <Box
                            sx={{
                                bgcolor: '#F7F8FA',
                                border: '1px solid',
                                borderColor: 'divider',
                                borderRadius: '8px',
                                p: 1.5,
                            }}
                        >
                            <Typography variant="caption" color="text.secondary">
                                Eklenecek yeni kayıt
                            </Typography>
                            <Typography sx={{ color: brandNavy, fontSize: 20, fontWeight: 750 }}>
                                {importableRowCount}
                            </Typography>
                        </Box>
                        <Typography variant="body2" color="text.secondary">
                            Mevcut, uyarılı, hatalı ve tekrarlanan kayıtlar atlanacaktır.
                        </Typography>
                    </Stack>
                </DialogContent>
                <DialogActions sx={{ px: 3, pb: 2.5, pt: 1.5 }}>
                    <Button
                        onClick={() => setConfirmOpen(false)}
                        disabled={importMutation.isPending}
                        sx={{
                            '&&:not(.Mui-disabled)': { bgcolor: '#F3F4F6', color: '#4B5563' },
                            '&&:not(.Mui-disabled):hover': { bgcolor: '#E9EBEF', color: '#4B5563' },
                            '&:focus-visible': { outline: `3px solid ${alpha(brandNavy, 0.18)}` },
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
                            '&&:not(.Mui-disabled)': { bgcolor: brandNavy, color: '#FFFFFF' },
                            '&&:not(.Mui-disabled):hover': { bgcolor: brandNavyHover, color: '#FFFFFF' },
                            '&:focus-visible': { outline: `3px solid ${alpha(brandNavy, 0.18)}` },
                        }}
                    >
                        Aktar
                    </Button>
                </DialogActions>
            </Dialog>
        </Dialog>
    );
}
