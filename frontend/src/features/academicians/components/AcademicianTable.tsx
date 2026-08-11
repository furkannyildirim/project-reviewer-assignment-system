import {
    Box,
    Checkbox,
    Chip,
    IconButton,
    Pagination,
    Paper,
    Skeleton,
    Stack,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    Tooltip,
    Typography,
} from '@mui/material';
import DeleteOutlineOutlinedIcon from '@mui/icons-material/DeleteOutlineOutlined';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import type { PageResponse } from '../../../types/api';
import type { Academician } from '../academician.types';

type AcademicianTableProps = {
    data: PageResponse<Academician> | undefined;
    page: number;
    size: number;
    isLoading: boolean;
    isAdmin: boolean;
    isMutating: boolean;
    selectedIds: Set<number>;
    isAllSelected: boolean;
    isSelectionIndeterminate: boolean;
    isSelectionLoading: boolean;
    onPageChange: (page: number) => void;
    onToggleAll: () => void;
    onToggleOne: (id: number) => void;
    onEdit: (academician: Academician) => void;
    onDelete: (academician: Academician) => void;
};

const selectionCheckboxSx = {
    '&.Mui-checked, &.MuiCheckbox-indeterminate': {
        color: '#2A2B61',
    },
    '&:hover': {
        bgcolor: 'rgba(42, 43, 97, 0.08)',
    },
    '&.Mui-focusVisible': {
        bgcolor: 'rgba(42, 43, 97, 0.08)',
        outline: '2px solid rgba(42, 43, 97, 0.18)',
        outlineOffset: 1,
    },
};

function displayOptional(value: string | null) {
    return value?.trim() ? value : '—';
}

function KeywordChips({ keywords }: { keywords: string[] }) {
    const visibleKeywords = keywords.slice(0, 4);
    const hiddenKeywords = keywords.slice(4);

    if (keywords.length === 0) {
        return <>—</>;
    }

    return (
        <Stack
            direction="row"
            spacing={0.75}
            useFlexGap
            sx={{ flexWrap: 'wrap', maxWidth: 360 }}
        >
            {visibleKeywords.map((keyword) => (
                <Chip key={keyword} label={keyword} size="small" />
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

function LoadingRows() {
    return (
        <>
            {Array.from({ length: 5 }).map((_, index) => (
                <TableRow key={index}>
                    <TableCell padding="checkbox">
                        <Skeleton variant="rectangular" width={24} height={24} />
                    </TableCell>
                    <TableCell><Skeleton width="70%" /></TableCell>
                    <TableCell><Skeleton width="70%" /></TableCell>
                    <TableCell><Skeleton width="80%" /></TableCell>
                    <TableCell><Skeleton width="55%" /></TableCell>
                    <TableCell><Skeleton width="60%" /></TableCell>
                    <TableCell><Skeleton width="90%" /></TableCell>
                    <TableCell align="right"><Skeleton width={72} /></TableCell>
                </TableRow>
            ))}
        </>
    );
}

export function AcademicianTable({
    data,
    page,
    isLoading,
    isAdmin,
    isMutating,
    selectedIds,
    isAllSelected,
    isSelectionIndeterminate,
    isSelectionLoading,
    onPageChange,
    onToggleAll,
    onToggleOne,
    onEdit,
    onDelete,
}: AcademicianTableProps) {
    const academicians = data?.content ?? [];
    const totalPages = Math.max(data?.totalPages ?? 1, 1);

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
            <TableContainer sx={{ overflowX: 'auto' }}>
                <Table sx={{ minWidth: 980 }} aria-label="Akademisyen tablosu">
                    <TableHead
                        sx={{
                            bgcolor: '#F7FAFC',
                            '& .MuiTableCell-head': {
                                borderBottom: '1px solid #E3EAF0',
                                color: '#0A1026',
                                fontWeight: 600,
                            },
                        }}
                    >
                        <TableRow>
                            <TableCell padding="checkbox">
                                <Tooltip title="Filtreye uyan tüm kayıtları seç">
                                    <span>
                                        <Checkbox
                                            aria-label="Tum akademisyenleri sec"
                                            checked={isAllSelected}
                                            disabled={isSelectionLoading}
                                            indeterminate={isSelectionIndeterminate}
                                            onChange={onToggleAll}
                                            sx={selectionCheckboxSx}
                                        />
                                    </span>
                                </Tooltip>
                            </TableCell>
                            <TableCell>Ad</TableCell>
                            <TableCell>Soyad</TableCell>
                            <TableCell>E-posta</TableCell>
                            <TableCell>Telefon</TableCell>
                            <TableCell>Ünvan</TableCell>
                            <TableCell>Anahtar kelimeler</TableCell>
                            <TableCell align="right">İşlemler</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {isLoading && academicians.length === 0 ? (
                            <LoadingRows />
                        ) : (
                            academicians.map((academician) => (
                                <TableRow key={academician.id} hover>
                                    <TableCell padding="checkbox">
                                        <Checkbox
                                            aria-label="Akademisyen sec"
                                            checked={selectedIds.has(academician.id)}
                                            onClick={(event) => event.stopPropagation()}
                                            onChange={() => onToggleOne(academician.id)}
                                            sx={selectionCheckboxSx}
                                        />
                                    </TableCell>
                                    <TableCell>
                                        <Typography sx={{ fontWeight: 600 }}>
                                            {academician.firstName}
                                        </Typography>
                                    </TableCell>
                                    <TableCell>{academician.lastName}</TableCell>
                                    <TableCell>
                                        {displayOptional(academician.email)}
                                    </TableCell>
                                    <TableCell>
                                        {displayOptional(academician.phone)}
                                    </TableCell>
                                    <TableCell>
                                        <Box sx={{ maxWidth: 180 }}>
                                            <Typography noWrap>
                                                {displayOptional(academician.title)}
                                            </Typography>
                                        </Box>
                                    </TableCell>
                                    <TableCell>
                                        <KeywordChips keywords={academician.keywords} />
                                    </TableCell>
                                    <TableCell align="right">
                                        <Stack
                                            direction="row"
                                            spacing={0.5}
                                            sx={{ justifyContent: 'flex-end' }}
                                        >
                                            <Tooltip title="Düzenle">
                                                <span>
                                                    <IconButton
                                                        aria-label="Akademisyeni düzenle"
                                                        disabled={isMutating}
                                                        onClick={() => onEdit(academician)}
                                                        sx={{
                                                            color: 'text.secondary',
                                                            '&:hover': {
                                                                bgcolor: 'rgba(42, 43, 97, 0.08)',
                                                                color: '#2A2B61',
                                                            },
                                                        }}
                                                    >
                                                        <EditOutlinedIcon />
                                                    </IconButton>
                                                </span>
                                            </Tooltip>
                                            {isAdmin && (
                                                <Tooltip title="Sil">
                                                    <span>
                                                        <IconButton
                                                            aria-label="Akademisyeni sil"
                                                            color="error"
                                                            disabled={isMutating}
                                                            onClick={() => onDelete(academician)}
                                                            sx={{
                                                                color: 'text.secondary',
                                                                '&:hover': {
                                                                    bgcolor: 'rgba(180, 35, 24, 0.08)',
                                                                    color: '#B42318',
                                                                },
                                                            }}
                                                        >
                                                            <DeleteOutlineOutlinedIcon />
                                                        </IconButton>
                                                    </span>
                                                </Tooltip>
                                            )}
                                        </Stack>
                                    </TableCell>
                                </TableRow>
                            ))
                        )}
                    </TableBody>
                </Table>
            </TableContainer>

            <Stack
                sx={{
                    display: 'flex',
                    flexDirection: 'row',
                    alignItems: 'center',
                    justifyContent: 'flex-end',
                    px: 2,
                    py: 1.5,
                }}
            >
                <Pagination
                    count={totalPages}
                    page={page + 1}
                    onChange={(_, nextPage) => onPageChange(nextPage - 1)}
                    color="primary"
                    shape="rounded"
                    siblingCount={0}
                    boundaryCount={1}
                    size="small"
                    sx={{
                        maxWidth: '100%',
                        '& .MuiPaginationItem-root': {
                            color: '#344054',
                            '&:hover': {
                                bgcolor: 'rgba(42, 43, 97, 0.06)',
                            },
                            '&.Mui-focusVisible': {
                                bgcolor: 'rgba(42, 43, 97, 0.08)',
                                outline: '2px solid rgba(42, 43, 97, 0.14)',
                                outlineOffset: 1,
                            },
                        },
                        '& .MuiPaginationItem-root.Mui-selected': {
                            bgcolor: '#2A2B61',
                            color: '#FFFFFF',
                            '&:hover': {
                                bgcolor: '#202153',
                            },
                            '&.Mui-focusVisible': {
                                bgcolor: '#2A2B61',
                                outlineColor: 'rgba(42, 43, 97, 0.24)',
                            },
                        },
                        '& .MuiPagination-ul': {
                            justifyContent: 'flex-end',
                            flexWrap: 'wrap',
                        },
                    }}
                />
            </Stack>
        </Paper>
    );
}
