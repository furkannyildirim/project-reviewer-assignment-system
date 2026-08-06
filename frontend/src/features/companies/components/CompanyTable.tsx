import { useState } from 'react';
import {
    Avatar,
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
import BusinessOutlinedIcon from '@mui/icons-material/BusinessOutlined';
import DeleteOutlineOutlinedIcon from '@mui/icons-material/DeleteOutlineOutlined';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import EmailOutlinedIcon from '@mui/icons-material/EmailOutlined';
import PhoneOutlinedIcon from '@mui/icons-material/PhoneOutlined';
import type { PageResponse } from '../../../types/api';
import type { Company, CompanyContactMethod, CompanyListItem } from '../company.types';
import { CompanyContactsPopover } from './CompanyContactsPopover';

type CompanyTableProps = {
    data: PageResponse<CompanyListItem> | undefined;
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
    onEdit: (company: Company) => void;
    onDelete: (company: Company) => void;
};

function displayOptional(value: string | null) {
    return value?.trim() ? value : '-';
}

function contactMethodLabel(contactMethod: CompanyContactMethod) {
    if (contactMethod.label?.trim()) {
        return `${contactMethod.label}: ${contactMethod.value}`;
    }

    return contactMethod.value;
}

function CompanyContactSummary({ company }: { company: CompanyListItem }) {
    const [anchorEl, setAnchorEl] = useState<HTMLElement | null>(null);
    const [isTooltipOpen, setIsTooltipOpen] = useState(false);
    const contact = company.primaryContact;

    if (!contact) {
        return (
            <Typography color="text.secondary">
                Yetkili kisi eklenmemis
            </Typography>
        );
    }

    const extraContactCount = Math.max(company.contactCount - 1, 0);
    const hasQuickContact = Boolean(contact.email?.trim() || contact.phone?.trim());

    function handleOpenDetails(event: React.MouseEvent<HTMLElement>) {
        setIsTooltipOpen(false);
        setAnchorEl(event.currentTarget);
    }

    function handleKeyDown(event: React.KeyboardEvent<HTMLElement>) {
        if (event.key === 'Enter' || event.key === ' ') {
            event.preventDefault();
            setIsTooltipOpen(false);
            setAnchorEl(event.currentTarget);
        }
    }

    const summary = (
        <Box
            role="button"
            tabIndex={0}
            aria-label="Yetkili iletisim bilgilerini goruntule"
            onClick={handleOpenDetails}
            onKeyDown={handleKeyDown}
            sx={{
                display: 'inline-block',
                borderRadius: 1,
                cursor: 'pointer',
                outline: 'none',
                '&:focus-visible': {
                    boxShadow: (theme) =>
                        `0 0 0 2px ${theme.palette.primary.main}`,
                },
            }}
        >
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
                            label={`+${extraContactCount} kisi`}
                            size="small"
                            variant="outlined"
                            sx={{
                                cursor: 'pointer',
                                '&:hover': {
                                    bgcolor: 'action.hover',
                                },
                            }}
                        />
                    )}
                </Stack>
            </Stack>
        </Box>
    );

    const tooltipContent = (
        <Stack spacing={0.5}>
            {contact.email && (
                <Stack
                    direction="row"
                    spacing={0.75}
                    sx={{ alignItems: 'center' }}
                >
                    <EmailOutlinedIcon sx={{ fontSize: 16 }} />
                    <Typography
                        variant="body2"
                        sx={{
                            maxWidth: 240,
                            overflow: 'hidden',
                            textOverflow: 'ellipsis',
                            whiteSpace: 'nowrap',
                        }}
                    >
                        {contact.email}
                    </Typography>
                </Stack>
            )}
            {contact.phone && (
                <Stack
                    direction="row"
                    spacing={0.75}
                    sx={{ alignItems: 'center' }}
                >
                    <PhoneOutlinedIcon sx={{ fontSize: 16 }} />
                    <Typography variant="body2">
                        {contact.phone}
                    </Typography>
                </Stack>
            )}
        </Stack>
    );

    return (
        <>
            {hasQuickContact ? (
                <Tooltip
                    title={tooltipContent}
                    open={isTooltipOpen}
                    enterDelay={300}
                    placement="right-start"
                    onOpen={() => setIsTooltipOpen(true)}
                    onClose={() => setIsTooltipOpen(false)}
                    arrow
                >
                    {summary}
                </Tooltip>
            ) : summary}

            <CompanyContactsPopover
                companyId={company.id}
                anchorEl={anchorEl}
                onClose={() => setAnchorEl(null)}
            />
        </>
    );
}

function CompanyCommunication({ company }: { company: CompanyListItem }) {
    const emails = company.contactMethods
        .filter((contactMethod) => contactMethod.type === 'EMAIL');
    const phones = company.contactMethods
        .filter((contactMethod) => contactMethod.type === 'PHONE');
    const primaryEmail = emails.find((contactMethod) => contactMethod.primary)
        ?? emails[0];
    const primaryPhone = phones.find((contactMethod) => contactMethod.primary)
        ?? phones[0];
    const displayedCount = Number(Boolean(primaryEmail)) + Number(Boolean(primaryPhone));
    const extraCount = Math.max(company.contactMethods.length - displayedCount, 0);

    if (!primaryEmail && !primaryPhone) {
        return <Typography color="text.secondary">-</Typography>;
    }

    const tooltipContent = (
        <Stack spacing={0.75}>
            {emails.length > 0 && (
                <Stack spacing={0.25}>
                    <Typography variant="caption" color="text.secondary">
                        E-postalar
                    </Typography>
                    {emails.map((email) => (
                        <Typography key={email.id} variant="body2">
                            {contactMethodLabel(email)}
                        </Typography>
                    ))}
                </Stack>
            )}
            {phones.length > 0 && (
                <Stack spacing={0.25}>
                    <Typography variant="caption" color="text.secondary">
                        Telefonlar
                    </Typography>
                    {phones.map((phone) => (
                        <Typography key={phone.id} variant="body2">
                            {contactMethodLabel(phone)}
                        </Typography>
                    ))}
                </Stack>
            )}
        </Stack>
    );

    return (
        <Tooltip title={tooltipContent} placement="right-start" arrow>
            <Stack spacing={0.25} sx={{ display: 'inline-flex' }}>
                {primaryEmail && (
                    <Typography>{primaryEmail.value}</Typography>
                )}
                {primaryPhone && (
                    <Typography>{primaryPhone.value}</Typography>
                )}
                {extraCount > 0 && (
                    <Typography variant="body2" color="text.secondary">
                        {`+${extraCount} diger`}
                    </Typography>
                )}
            </Stack>
        </Tooltip>
    );
}

function ActivityAreaCell({ activityArea }: { activityArea: string | null }) {
    const activityAreaText = activityArea?.trim();

    if (!activityAreaText) {
        return <Typography color="text.secondary">{displayOptional(activityArea)}</Typography>;
    }

    return (
        <Typography
            sx={{
                WebkitBoxOrient: 'vertical',
                WebkitLineClamp: 2,
                display: '-webkit-box',
                lineHeight: 1.45,
                maxHeight: '2.9em',
                maxWidth: 330,
                overflow: 'hidden',
                overflowWrap: 'anywhere',
            }}
        >
            {activityAreaText}
        </Typography>
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
                    <TableCell padding="checkbox">
                        <Skeleton variant="rectangular" width={24} height={24} />
                    </TableCell>
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
    selectedIds,
    isAllSelected,
    isSelectionIndeterminate,
    isSelectionLoading,
    onPageChange,
    onToggleAll,
    onToggleOne,
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
                <Table sx={{ minWidth: 860 }} aria-label="Sirket tablosu">
                    <TableHead>
                        <TableRow>
                            <TableCell padding="checkbox">
                                <Tooltip title="Filtreye uyan tüm kayıtları seç">
                                    <span>
                                        <Checkbox
                                            aria-label="Tum firmalari sec"
                                            checked={isAllSelected}
                                            disabled={isSelectionLoading}
                                            indeterminate={isSelectionIndeterminate}
                                            onChange={onToggleAll}
                                        />
                                    </span>
                                </Tooltip>
                            </TableCell>
                            <TableCell>Logo</TableCell>
                            <TableCell>Sirket adi</TableCell>
                            <TableCell>Yetkili Kisi</TableCell>
                            <TableCell>Sirket iletisimi</TableCell>
                            <TableCell>Faaliyet alani</TableCell>
                            <TableCell align="right">Islemler</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {isLoading && companies.length === 0 ? (
                            <LoadingRows />
                        ) : (
                            companies.map((company) => (
                                <TableRow key={company.id} hover>
                                    <TableCell padding="checkbox">
                                        <Checkbox
                                            aria-label="Firma sec"
                                            checked={selectedIds.has(company.id)}
                                            onClick={(event) => event.stopPropagation()}
                                            onChange={() => onToggleOne(company.id)}
                                        />
                                    </TableCell>
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
                                        <Box sx={{ maxWidth: 340, minWidth: 300 }}>
                                            <ActivityAreaCell
                                                activityArea={company.activityArea}
                                            />
                                        </Box>
                                    </TableCell>
                                    <TableCell align="right">
                                        <Stack
                                            direction="row"
                                            spacing={0.5}
                                            sx={{ justifyContent: 'flex-end' }}
                                        >
                                            <Tooltip title="Duzenle">
                                                <span>
                                                    <IconButton
                                                        aria-label="Sirketi duzenle"
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
                                                            aria-label="Sirketi sil"
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
