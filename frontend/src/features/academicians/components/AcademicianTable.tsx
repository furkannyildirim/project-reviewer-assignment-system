import {
    Box,
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
    onPageChange: (page: number) => void;
    onEdit: (academician: Academician) => void;
    onDelete: (academician: Academician) => void;
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
    onPageChange,
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
                    <TableHead>
                        <TableRow>
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
