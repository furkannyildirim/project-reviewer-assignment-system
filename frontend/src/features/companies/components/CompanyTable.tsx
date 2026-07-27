import { useState } from 'react';
import {
    Avatar,
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
import BusinessOutlinedIcon from '@mui/icons-material/BusinessOutlined';
import DeleteOutlineOutlinedIcon from '@mui/icons-material/DeleteOutlineOutlined';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import type { PageResponse } from '../../../types/api';
import type { Company, CompanyListItem } from '../company.types';

type CompanyTableProps = {
    data: PageResponse<CompanyListItem> | undefined;
    page: number;
    size: number;
    isLoading: boolean;
    isAdmin: boolean;
    isMutating: boolean;
    onPageChange: (page: number) => void;
    onEdit: (company: Company) => void;
    onDelete: (company: Company) => void;
};

function displayOptional(value: string | null) {
    return value?.trim() ? value : '—';
}

function CompanyContactSummary({ company }: { company: CompanyListItem }) {
    const contact = company.primaryContact;

    if (!contact) {
        return (
            <Typography color="text.secondary">
                Yetkili kişi eklenmemiş
            </Typography>
        );
    }

    const extraContactCount = Math.max(company.contactCount - 1, 0);

    return (
        <Stack spacing={0.75} sx={{ alignItems: 'flex-start' }}>
            <Box>
                <Typography sx={{ fontWeight: 600 }}>
                    {contact.fullName}
                </Typography>
                {contact.position && (
                    <Typography
                        variant="body2"
                        color="text.secondary"
                        sx={{ maxWidth: 220 }}
                        noWrap
                    >
                        {contact.position}
                    </Typography>
                )}
            </Box>
            <Stack direction="row" spacing={0.75} sx={{ flexWrap: 'wrap' }}>
                <Chip
                    label="Ana Yetkili"
                    color="primary"
                    size="small"
                    variant="outlined"
                />
                {extraContactCount > 0 && (
                    <Chip
                        label={`+${extraContactCount} kişi`}
                        size="small"
                        variant="outlined"
                    />
                )}
            </Stack>
        </Stack>
    );
}

function CompanyCommunication({ company }: { company: CompanyListItem }) {
    const hasEmail = Boolean(company.email?.trim());
    const hasPhone = Boolean(company.phone?.trim());

    if (!hasEmail && !hasPhone) {
        return <Typography color="text.secondary">—</Typography>;
    }

    return (
        <Stack spacing={0.25}>
            {hasEmail && (
                <Typography>{company.email}</Typography>
            )}
            {hasPhone && (
                <Typography>{company.phone}</Typography>
            )}
        </Stack>
    );
}

function CompanyLogo({ company }: { company: Company }) {
    const [hasError, setHasError] = useState(false);
    const logoUrl = company.logoUrl?.trim();
    const firstLetter = company.name.trim().charAt(0).toUpperCase();

    if (!logoUrl || hasError) {
        return (
            <Avatar sx={{ width: 40, height: 40 }}>
                {firstLetter || <BusinessOutlinedIcon />}
            </Avatar>
        );
    }

    return (
        <Avatar
            src={logoUrl}
            alt={`${company.name} logosu`}
            slotProps={{
                img: {
                    onError: () => setHasError(true),
                },
            }}
            sx={{ width: 40, height: 40, bgcolor: 'background.default' }}
        >
            {firstLetter || <BusinessOutlinedIcon />}
        </Avatar>
    );
}

function LoadingRows() {
    return (
        <>
            {Array.from({ length: 5 }).map((_, index) => (
                <TableRow key={index}>
                    <TableCell>
                        <Skeleton variant="circular" width={40} height={40} />
                    </TableCell>
                    <TableCell><Skeleton width="80%" /></TableCell>
                    <TableCell><Skeleton width="70%" /></TableCell>
                    <TableCell><Skeleton width="50%" /></TableCell>
                    <TableCell><Skeleton width="75%" /></TableCell>
                    <TableCell align="right"><Skeleton width={72} /></TableCell>
                </TableRow>
            ))}
        </>
    );
}

export function CompanyTable({
    data,
    page,
    isLoading,
    isAdmin,
    isMutating,
    onPageChange,
    onEdit,
    onDelete,
}: CompanyTableProps) {
    const companies = data?.content ?? [];
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
                <Table sx={{ minWidth: 860 }} aria-label="Şirket tablosu">
                    <TableHead>
                        <TableRow>
                            <TableCell>Logo</TableCell>
                            <TableCell>Şirket adı</TableCell>
                            <TableCell>Yetkili Kişi</TableCell>
                            <TableCell>Şirket iletişimi</TableCell>
                            <TableCell>Faaliyet alanı</TableCell>
                            <TableCell align="right">İşlemler</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {isLoading && companies.length === 0 ? (
                            <LoadingRows />
                        ) : (
                            companies.map((company) => (
                                <TableRow key={company.id} hover>
                                    <TableCell>
                                        <CompanyLogo company={company} />
                                    </TableCell>
                                    <TableCell>
                                        <Typography sx={{ fontWeight: 600 }}>
                                            {company.name}
                                        </Typography>
                                    </TableCell>
                                    <TableCell>
                                        <CompanyContactSummary company={company} />
                                    </TableCell>
                                    <TableCell>
                                        <CompanyCommunication company={company} />
                                    </TableCell>
                                    <TableCell>
                                        <Box sx={{ maxWidth: 260 }}>
                                            <Typography noWrap>
                                                {displayOptional(company.activityArea)}
                                            </Typography>
                                        </Box>
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
                                                        aria-label="Şirketi düzenle"
                                                        disabled={isMutating}
                                                        onClick={() => onEdit(company)}
                                                    >
                                                        <EditOutlinedIcon />
                                                    </IconButton>
                                                </span>
                                            </Tooltip>
                                            {isAdmin && (
                                                <Tooltip title="Sil">
                                                    <span>
                                                        <IconButton
                                                            aria-label="Şirketi sil"
                                                            color="error"
                                                            disabled={isMutating}
                                                            onClick={() => onDelete(company)}
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
