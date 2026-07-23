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
                    <TableHead>
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
