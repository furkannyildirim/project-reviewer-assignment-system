import {
    Box,
    Chip,
    Paper,
    Skeleton,
    Stack,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TablePagination,
    TableRow,
    Tooltip,
    Typography,
    IconButton,
} from '@mui/material';
import VisibilityOutlinedIcon from '@mui/icons-material/VisibilityOutlined';
import DeleteOutlineOutlinedIcon from '@mui/icons-material/DeleteOutlineOutlined';
import type { PageResponse } from '../../../types/api';
import type { ProjectListItem, ProjectStatus } from '../project.types';

type ProjectTableProps = {
    data: PageResponse<ProjectListItem> | undefined;
    page: number;
    size: number;
    isLoading: boolean;
    onPageChange: (page: number) => void;
    onView: (project: ProjectListItem) => void;
    onDelete: (project: ProjectListItem) => void;
    isDeleting: boolean;
};

const statusPresentation: Record<ProjectStatus, { label: string; sx: object }> = {
    AWAITING_REVIEWER: {
        label: 'Hakem bekliyor',
        sx: { bgcolor: '#FFF4D6', color: '#9A6700' },
    },
    ACTIVE: { label: 'Aktif', sx: { bgcolor: '#EAF7FB', color: '#187A9C' } },
    COMPLETED: { label: 'Tamamlandı', sx: { bgcolor: '#EAF7EF', color: '#237A47' } },
};

function formatDate(value: string | null): string {
    if (!value) {
        return '-';
    }
    const date = new Date(`${value}T00:00:00`);
    if (Number.isNaN(date.getTime())) {
        return '-';
    }
    return new Intl.DateTimeFormat('tr-TR', {
        day: '2-digit',
        month: 'short',
        year: 'numeric',
    }).format(date);
}

function activeReviewerLabel(project: ProjectListItem): string {
    if (project.activeReviewerCount > 0) {
        return `${project.activeReviewerCount} hakem`;
    }

    if (project.status === 'ACTIVE') {
        return 'Aktif hakem bulunmuyor';
    }

    if (project.status === 'COMPLETED') {
        return 'Aktif hakem yok';
    }

    return 'Atanmadı';
}

function LoadingRows() {
    return Array.from({ length: 5 }).map((_, index) => (
        <TableRow key={index}>
            {Array.from({ length: 6 }).map((__, cellIndex) => (
                <TableCell key={cellIndex}><Skeleton width="70%" /></TableCell>
            ))}
        </TableRow>
    ));
}

function CompanyCell({ project }: { project: ProjectListItem }) {
    return (
        <Stack spacing={0.75} sx={{ minWidth: 160 }}>
            <Typography sx={{ fontWeight: 600 }}>{project.companyName}</Typography>
            <Chip
                label={project.companyType === 'TGB' ? 'TGB Şirketi' : 'Dış Şirket'}
                size="small"
                sx={{
                    alignSelf: 'flex-start',
                    bgcolor: '#F2F7FA',
                    color: '#526273',
                    fontSize: 12,
                    height: 24,
                }}
            />
        </Stack>
    );
}

export function ProjectTable({
    data,
    page,
    size,
    isLoading,
    onPageChange,
    onView,
    onDelete,
    isDeleting,
}: ProjectTableProps) {
    const projects = data?.content ?? [];
    const totalElements = data?.totalElements ?? 0;
    const totalPages = data?.totalPages ?? 0;
    const safePage = totalPages > 0 ? Math.min(page, totalPages - 1) : 0;

    return (
        <Paper
            elevation={0}
            sx={{ border: '1px solid #E3EAF0', borderRadius: '8px', overflow: 'hidden' }}
        >
            <TableContainer sx={{ overflowX: 'auto' }}>
                <Table sx={{ minWidth: 980 }} aria-label="Proje tablosu">
                    <TableHead sx={{
                        bgcolor: '#F7FAFC',
                        '& .MuiTableCell-head': {
                            borderBottom: '1px solid #E3EAF0',
                            color: '#0A1026',
                            fontWeight: 600,
                        },
                    }}>
                        <TableRow>
                            <TableCell>Proje adı</TableCell>
                            <TableCell>Şirket</TableCell>
                            <TableCell>Kategori / Sektör</TableCell>
                            <TableCell>Başlangıç – Bitiş</TableCell>
                            <TableCell>Durum</TableCell>
                            <TableCell>Aktif hakem</TableCell>
                            <TableCell align="right">İşlemler</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {isLoading && projects.length === 0 ? <LoadingRows /> : projects.map((project) => {
                            const status = statusPresentation[project.status];
                            const hasClassification = project.category || project.sector;
                            const canDelete = project.status !== 'COMPLETED';
                            return (
                                <TableRow hover key={project.id}>
                                    <TableCell sx={{ maxWidth: 230 }}>
                                        <Tooltip title={project.name} placement="top-start">
                                            <Typography noWrap sx={{ fontWeight: 600 }}>
                                                {project.name}
                                            </Typography>
                                        </Tooltip>
                                    </TableCell>
                                    <TableCell><CompanyCell project={project} /></TableCell>
                                    <TableCell>
                                        {hasClassification ? (
                                            <Stack spacing={0.25}>
                                                {project.category && <Typography>{project.category}</Typography>}
                                                {project.sector && (
                                                    <Typography color="text.secondary" variant="body2">
                                                        {project.sector}
                                                    </Typography>
                                                )}
                                            </Stack>
                                        ) : (
                                            <Typography color="text.secondary">Bilgi girilmemiş</Typography>
                                        )}
                                    </TableCell>
                                    <TableCell>
                                        <Stack spacing={0.25}>
                                            <Typography>{formatDate(project.startDate)}</Typography>
                                            <Typography>
                                                {formatDate(project.endDate)}
                                            </Typography>
                                        </Stack>
                                    </TableCell>
                                    <TableCell>
                                        <Chip label={status.label} size="small" sx={{
                                            fontWeight: 600,
                                            ...status.sx,
                                        }} />
                                    </TableCell>
                                    <TableCell>
                                        {activeReviewerLabel(project)}
                                    </TableCell>
                                    <TableCell align="right">
                                        <Tooltip title="Görüntüle">
                                            <IconButton
                                                aria-label={`${project.name} projesini görüntüle`}
                                                onClick={() => onView(project)}
                                                size="small"
                                                sx={{
                                                    color: '#526273',
                                                    '&:hover': { bgcolor: '#F2F7FA', color: '#2A2B61' },
                                                }}
                                            >
                                                <VisibilityOutlinedIcon fontSize="small" />
                                            </IconButton>
                                        </Tooltip>
                                        {canDelete && (
                                            <Tooltip title="Projeyi Sil">
                                                <span>
                                                    <IconButton
                                                        aria-label={`${project.name} projesini sil`}
                                                        disabled={isDeleting}
                                                        onClick={() => onDelete(project)}
                                                        size="small"
                                                        sx={{
                                                            color: '#526273',
                                                            '&:hover': {
                                                                bgcolor: 'rgba(180, 35, 24, 0.08)',
                                                                color: '#B42318',
                                                            },
                                                        }}
                                                    >
                                                        <DeleteOutlineOutlinedIcon fontSize="small" />
                                                    </IconButton>
                                                </span>
                                            </Tooltip>
                                        )}
                                    </TableCell>
                                </TableRow>
                            );
                        })}
                    </TableBody>
                </Table>
            </TableContainer>
            <Box sx={{ borderTop: '1px solid #E3EAF0' }}>
                <TablePagination
                    component="div"
                    count={totalElements}
                    page={safePage}
                    rowsPerPage={size}
                    rowsPerPageOptions={[]}
                    labelDisplayedRows={({ from, to, count }) => `${from}-${to} / ${count}`}
                    onPageChange={(_, nextPage) => onPageChange(nextPage)}
                    sx={{
                        '& .MuiTablePagination-toolbar': {
                            justifyContent: 'flex-end',
                            px: { xs: 1, sm: 2 },
                        },
                        '& .MuiTablePagination-spacer': { display: 'none' },
                    }}
                />
            </Box>
        </Paper>
    );
}
