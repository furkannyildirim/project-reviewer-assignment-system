import {
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
import type { User } from '../user.types';

type UserTableProps = {
    users: User[];
    page: number;
    totalPages: number;
    isLoading: boolean;
    isMutating: boolean;
    onPageChange: (page: number) => void;
    onEdit: (user: User) => void;
    onDelete: (user: User) => void;
};

function LoadingRows() {
    return (
        <>
            {Array.from({ length: 5 }).map((_, index) => (
                <TableRow key={index}>
                    <TableCell><Skeleton width="70%" /></TableCell>
                    <TableCell><Skeleton width="70%" /></TableCell>
                    <TableCell><Skeleton width="80%" /></TableCell>
                    <TableCell><Skeleton width="55%" /></TableCell>
                    <TableCell><Skeleton width={72} /></TableCell>
                    <TableCell align="right"><Skeleton width={72} /></TableCell>
                </TableRow>
            ))}
        </>
    );
}

export function UserTable({
    users,
    page,
    totalPages,
    isLoading,
    isMutating,
    onPageChange,
    onEdit,
    onDelete,
}: UserTableProps) {
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
                <Table sx={{ minWidth: 860 }} aria-label="Kullanıcı tablosu">
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
                            <TableCell>Ad</TableCell>
                            <TableCell>Soyad</TableCell>
                            <TableCell>E-posta</TableCell>
                            <TableCell>Telefon</TableCell>
                            <TableCell>Durum</TableCell>
                            <TableCell align="right">İşlemler</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {isLoading && users.length === 0 ? (
                            <LoadingRows />
                        ) : (
                            users.map((user) => (
                                <TableRow key={user.id} hover>
                                    <TableCell>
                                        <Typography sx={{ fontWeight: 600 }}>
                                            {user.firstName}
                                        </Typography>
                                    </TableCell>
                                    <TableCell>{user.lastName}</TableCell>
                                    <TableCell>{user.email}</TableCell>
                                    <TableCell>{user.phone}</TableCell>
                                    <TableCell>
                                        <Chip
                                            label={user.active ? 'Aktif' : 'Pasif'}
                                            color={user.active ? 'success' : 'default'}
                                            size="small"
                                            variant={user.active ? 'filled' : 'outlined'}
                                            sx={user.active ? {
                                                bgcolor: '#EAF7FB',
                                                border: '1px solid #C9EAF4',
                                                color: '#187A9C',
                                                fontWeight: 600,
                                            } : undefined}
                                        />
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
                                                        aria-label="Kullanıcıyı düzenle"
                                                        disabled={isMutating}
                                                        onClick={() => onEdit(user)}
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
                                            <Tooltip title="Sil">
                                                <span>
                                                    <IconButton
                                                        aria-label="Kullanıcıyı sil"
                                                        color="error"
                                                        disabled={isMutating}
                                                        onClick={() => onDelete(user)}
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
