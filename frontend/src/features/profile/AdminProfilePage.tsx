import { useEffect, useMemo, useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import LockResetOutlinedIcon from '@mui/icons-material/LockResetOutlined';
import VisibilityOffOutlinedIcon from '@mui/icons-material/VisibilityOffOutlined';
import VisibilityOutlinedIcon from '@mui/icons-material/VisibilityOutlined';
import {
    Alert,
    Avatar,
    Box,
    Button,
    CircularProgress,
    IconButton,
    InputAdornment,
    Paper,
    Snackbar,
    Stack,
    TextField,
    Typography,
} from '@mui/material';
import { useNavigate } from 'react-router';
import { getApiErrorMessage } from '../../utils/getApiErrorMessage';
import { useAuth } from '../auth/useAuth';
import {
    changeAdminPassword,
    getAdminProfile,
    updateAdminProfile,
} from './profileApi';
import { profileQueryKeys } from './profileQueryKeys';
import type { UpdateAdminProfileRequest } from './profile.types';

type ProfileForm = UpdateAdminProfileRequest;

type PasswordForm = {
    currentPassword: string;
    newPassword: string;
    confirmPassword: string;
};

type ProfileErrors = Partial<Record<keyof ProfileForm, string>>;
type PasswordErrors = Partial<Record<keyof PasswordForm, string>>;

const emptyProfileForm: ProfileForm = {
    firstName: '',
    lastName: '',
    phone: '',
};

const emptyPasswordForm: PasswordForm = {
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
};

const cardSx = {
    border: '1px solid #E3EAF0',
    borderRadius: '14px',
    boxShadow: '0 8px 28px rgba(23, 35, 58, 0.05)',
    p: { xs: 2, sm: 3 },
} as const;

const formFieldSx = {
    '& .MuiInputLabel-root.Mui-focused': {
        color: '#2A2B61',
    },
    '& .MuiOutlinedInput-root': {
        '& .MuiOutlinedInput-notchedOutline': {
            borderColor: '#D9E2EA',
        },
        '&:hover .MuiOutlinedInput-notchedOutline': {
            borderColor: '#B8C7D4',
        },
        '&.Mui-focused': {
            boxShadow: '0 0 0 3px rgba(42, 43, 97, 0.09)',
        },
        '&.Mui-focused .MuiOutlinedInput-notchedOutline': {
            borderColor: '#2A2B61',
            borderWidth: 1,
        },
    },
} as const;

function validateProfile(values: ProfileForm): ProfileErrors {
    const errors: ProfileErrors = {};

    if (!values.firstName.trim()) errors.firstName = 'Ad boş bırakılamaz';
    else if (values.firstName.length > 100) errors.firstName = 'Ad en fazla 100 karakter olabilir';

    if (!values.lastName.trim()) errors.lastName = 'Soyad boş bırakılamaz';
    else if (values.lastName.length > 100) errors.lastName = 'Soyad en fazla 100 karakter olabilir';

    if (!values.phone.trim()) errors.phone = 'Telefon boş bırakılamaz';
    else if (values.phone.length > 30) errors.phone = 'Telefon en fazla 30 karakter olabilir';

    return errors;
}

function validatePassword(values: PasswordForm): PasswordErrors {
    const errors: PasswordErrors = {};

    if (!values.currentPassword) {
        errors.currentPassword = 'Mevcut parola boş bırakılamaz';
    }

    if (!values.newPassword) {
        errors.newPassword = 'Yeni parola boş bırakılamaz';
    } else if (values.newPassword.length < 8 || values.newPassword.length > 72) {
        errors.newPassword = 'Yeni parola 8 ile 72 karakter arasında olmalıdır';
    } else if (values.newPassword === values.currentPassword) {
        errors.newPassword = 'Yeni parola mevcut paroladan farklı olmalıdır.';
    }

    if (!values.confirmPassword) {
        errors.confirmPassword = 'Yeni parola tekrarı boş bırakılamaz';
    } else if (values.newPassword !== values.confirmPassword) {
        errors.confirmPassword = 'Yeni parolalar eşleşmiyor';
    }

    return errors;
}

export function AdminProfilePage() {
    const queryClient = useQueryClient();
    const navigate = useNavigate();
    const { logout } = useAuth();
    const [profileForm, setProfileForm] = useState(emptyProfileForm);
    const [profileErrors, setProfileErrors] = useState<ProfileErrors>({});
    const [passwordForm, setPasswordForm] = useState(emptyPasswordForm);
    const [passwordErrors, setPasswordErrors] = useState<PasswordErrors>({});
    const [showCurrentPassword, setShowCurrentPassword] = useState(false);
    const [showNewPassword, setShowNewPassword] = useState(false);
    const [showConfirmPassword, setShowConfirmPassword] = useState(false);
    const [successMessage, setSuccessMessage] = useState<string | null>(null);

    const profileQuery = useQuery({
        queryKey: profileQueryKeys.detail(),
        queryFn: getAdminProfile,
    });

    useEffect(() => {
        if (!profileQuery.data) return;

        setProfileForm({
            firstName: profileQuery.data.firstName,
            lastName: profileQuery.data.lastName,
            phone: profileQuery.data.phone,
        });
    }, [profileQuery.data]);

    const initials = useMemo(() => {
        const first = profileQuery.data?.firstName.trim().charAt(0) ?? '';
        const last = profileQuery.data?.lastName.trim().charAt(0) ?? '';
        return `${first}${last}`.toLocaleUpperCase('tr-TR') || 'Y';
    }, [profileQuery.data]);

    const profileMutation = useMutation({
        mutationFn: updateAdminProfile,
        onSuccess: (profile) => {
            queryClient.setQueryData(profileQueryKeys.detail(), profile);
            setProfileErrors({});
            setSuccessMessage('Profil bilgileri başarıyla güncellendi');
        },
    });

    const passwordMutation = useMutation({
        mutationFn: changeAdminPassword,
        onSuccess: () => {
            setPasswordForm(emptyPasswordForm);
            logout();
            navigate('/login', { replace: true });
        },
    });

    function handleProfileSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        const errors = validateProfile(profileForm);
        setProfileErrors(errors);

        if (Object.keys(errors).length > 0) return;

        profileMutation.mutate({
            firstName: profileForm.firstName.trim(),
            lastName: profileForm.lastName.trim(),
            phone: profileForm.phone.trim(),
        });
    }

    function handlePasswordSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        const errors = validatePassword(passwordForm);
        setPasswordErrors(errors);

        if (Object.keys(errors).length > 0) return;

        passwordMutation.mutate({
            currentPassword: passwordForm.currentPassword,
            newPassword: passwordForm.newPassword,
        });
    }

    const passwordVisibilityButton = (
        visible: boolean,
        onToggle: () => void,
        label: string,
    ) => (
        <InputAdornment position="end">
            <IconButton
                aria-label={label}
                edge="end"
                onClick={onToggle}
            >
                {visible
                    ? <VisibilityOffOutlinedIcon />
                    : <VisibilityOutlinedIcon />}
            </IconButton>
        </InputAdornment>
    );

    return (
        <Stack spacing={3} sx={{ pt: { xs: 2.5, sm: 3, lg: 4 } }}>
            <Stack direction="row" spacing={1.75} sx={{ alignItems: 'center' }}>
                <Box
                    aria-hidden="true"
                    sx={{
                        bgcolor: '#2EA8D3',
                        borderRadius: '4px',
                        flex: '0 0 auto',
                        height: 52,
                        width: 4,
                    }}
                />
                <Box sx={{ minWidth: 0 }}>
                    <Typography variant="h4" component="h1">
                        Yönetici Profili
                    </Typography>
                    <Typography color="text.secondary" sx={{ mt: 0.75 }}>
                        Hesap bilgilerinizi ve parolanızı güvenle yönetin.
                    </Typography>
                </Box>
            </Stack>

            {profileQuery.isLoading && (
                <Paper sx={cardSx}>
                    <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center' }}>
                        <CircularProgress size={24} />
                        <Typography color="text.secondary">
                            Profil bilgileri yükleniyor...
                        </Typography>
                    </Stack>
                </Paper>
            )}

            {profileQuery.isError && (
                <Alert
                    severity="error"
                    action={(
                        <Button
                            color="inherit"
                            size="small"
                            onClick={() => void profileQuery.refetch()}
                        >
                            Tekrar dene
                        </Button>
                    )}
                >
                    {getApiErrorMessage(
                        profileQuery.error,
                        'Profil bilgileri alınamadı',
                    )}
                </Alert>
            )}

            {profileQuery.data && (
                <Box
                    sx={{
                        display: 'grid',
                        gap: 3,
                        gridTemplateColumns: { xs: '1fr', lg: 'minmax(0, 1.15fr) minmax(360px, 0.85fr)' },
                    }}
                >
                    <Paper component="section" sx={cardSx}>
                        <Stack spacing={3}>
                            <Stack
                                direction="row"
                                spacing={1.5}
                                sx={{ alignItems: 'center' }}
                            >
                                <Avatar
                                    sx={{
                                        bgcolor: '#E8F5FA',
                                        color: '#2A2B61',
                                        fontWeight: 800,
                                        height: 52,
                                        width: 52,
                                    }}
                                >
                                    {initials}
                                </Avatar>
                                <Box>
                                    <Typography variant="h6" sx={{ fontWeight: 700 }}>
                                        Hesap Bilgileri
                                    </Typography>
                                    <Typography color="text.secondary" variant="body2">
                                        Temel iletişim bilgilerinizi güncelleyin.
                                    </Typography>
                                </Box>
                            </Stack>

                            {profileMutation.isError && (
                                <Alert severity="error">
                                    {getApiErrorMessage(
                                        profileMutation.error,
                                        'Profil güncellenemedi',
                                    )}
                                </Alert>
                            )}

                            <Box component="form" onSubmit={handleProfileSubmit} noValidate>
                                <Stack spacing={2.25}>
                                    <Box
                                        sx={{
                                            display: 'grid',
                                            gap: 2,
                                            gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' },
                                        }}
                                    >
                                        <TextField
                                            label="Ad"
                                            sx={formFieldSx}
                                            value={profileForm.firstName}
                                            error={Boolean(profileErrors.firstName)}
                                            helperText={profileErrors.firstName}
                                            onChange={(event) => setProfileForm((current) => ({
                                                ...current,
                                                firstName: event.target.value,
                                            }))}
                                            slotProps={{ htmlInput: { maxLength: 100 } }}
                                        />
                                        <TextField
                                            label="Soyad"
                                            sx={formFieldSx}
                                            value={profileForm.lastName}
                                            error={Boolean(profileErrors.lastName)}
                                            helperText={profileErrors.lastName}
                                            onChange={(event) => setProfileForm((current) => ({
                                                ...current,
                                                lastName: event.target.value,
                                            }))}
                                            slotProps={{ htmlInput: { maxLength: 100 } }}
                                        />
                                    </Box>
                                    <TextField
                                        label="Telefon"
                                        sx={formFieldSx}
                                        value={profileForm.phone}
                                        error={Boolean(profileErrors.phone)}
                                        helperText={profileErrors.phone}
                                        onChange={(event) => setProfileForm((current) => ({
                                            ...current,
                                            phone: event.target.value,
                                        }))}
                                        slotProps={{ htmlInput: { maxLength: 30 } }}
                                    />
                                    <TextField
                                        label="E-posta"
                                        sx={formFieldSx}
                                        value={profileQuery.data.email}
                                        slotProps={{ input: { readOnly: true } }}
                                    />
                                    <TextField
                                        label="Rol"
                                        sx={formFieldSx}
                                        value="Yönetici"
                                        slotProps={{ input: { readOnly: true } }}
                                    />
                                    <Button
                                        type="submit"
                                        variant="contained"
                                        disabled={profileMutation.isPending}
                                        sx={{
                                            alignSelf: { xs: 'stretch', sm: 'flex-end' },
                                            bgcolor: '#2A2B61',
                                            minWidth: 150,
                                            '&:hover': { bgcolor: '#202153' },
                                        }}
                                    >
                                        {profileMutation.isPending
                                            ? <CircularProgress color="inherit" size={22} />
                                            : 'Kaydet'}
                                    </Button>
                                </Stack>
                            </Box>
                        </Stack>
                    </Paper>

                    <Paper component="section" sx={cardSx}>
                        <Stack spacing={3}>
                            <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center' }}>
                                <Avatar sx={{ bgcolor: '#F0F0F7', color: '#2A2B61' }}>
                                    <LockResetOutlinedIcon />
                                </Avatar>
                                <Box>
                                    <Typography variant="h6" sx={{ fontWeight: 700 }}>
                                        Parola Değiştir
                                    </Typography>
                                    <Typography color="text.secondary" variant="body2">
                                        En az 8 karakterli yeni bir parola belirleyin.
                                    </Typography>
                                </Box>
                            </Stack>

                            {passwordMutation.isError && (
                                <Alert severity="error">
                                    {getApiErrorMessage(
                                        passwordMutation.error,
                                        'Parola değiştirilemedi',
                                    )}
                                </Alert>
                            )}

                            <Box component="form" onSubmit={handlePasswordSubmit} noValidate>
                                <Stack spacing={2.25}>
                                    <TextField
                                        label="Mevcut parola"
                                        sx={formFieldSx}
                                        type={showCurrentPassword ? 'text' : 'password'}
                                        autoComplete="current-password"
                                        value={passwordForm.currentPassword}
                                        error={Boolean(passwordErrors.currentPassword)}
                                        helperText={passwordErrors.currentPassword}
                                        onChange={(event) => setPasswordForm((current) => ({
                                            ...current,
                                            currentPassword: event.target.value,
                                        }))}
                                        slotProps={{
                                            input: {
                                                endAdornment: passwordVisibilityButton(
                                                    showCurrentPassword,
                                                    () => setShowCurrentPassword((value) => !value),
                                                    'Mevcut parola görünürlüğünü değiştir',
                                                ),
                                            },
                                        }}
                                    />
                                    <TextField
                                        label="Yeni parola"
                                        sx={formFieldSx}
                                        type={showNewPassword ? 'text' : 'password'}
                                        autoComplete="new-password"
                                        value={passwordForm.newPassword}
                                        error={Boolean(passwordErrors.newPassword)}
                                        helperText={passwordErrors.newPassword}
                                        onChange={(event) => setPasswordForm((current) => ({
                                            ...current,
                                            newPassword: event.target.value,
                                        }))}
                                        slotProps={{
                                            htmlInput: { maxLength: 72 },
                                            input: {
                                                endAdornment: passwordVisibilityButton(
                                                    showNewPassword,
                                                    () => setShowNewPassword((value) => !value),
                                                    'Yeni parola görünürlüğünü değiştir',
                                                ),
                                            },
                                        }}
                                    />
                                    <TextField
                                        label="Yeni parola tekrar"
                                        sx={formFieldSx}
                                        type={showConfirmPassword ? 'text' : 'password'}
                                        autoComplete="new-password"
                                        value={passwordForm.confirmPassword}
                                        error={Boolean(passwordErrors.confirmPassword)}
                                        helperText={passwordErrors.confirmPassword}
                                        onChange={(event) => setPasswordForm((current) => ({
                                            ...current,
                                            confirmPassword: event.target.value,
                                        }))}
                                        slotProps={{
                                            htmlInput: { maxLength: 72 },
                                            input: {
                                                endAdornment: passwordVisibilityButton(
                                                    showConfirmPassword,
                                                    () => setShowConfirmPassword((value) => !value),
                                                    'Yeni parola tekrar görünürlüğünü değiştir',
                                                ),
                                            },
                                        }}
                                    />
                                    <Alert severity="info">
                                        Parola değişikliğinden sonra güvenliğiniz için yeniden giriş yapmanız gerekir.
                                    </Alert>
                                    <Button
                                        type="submit"
                                        variant="outlined"
                                        disabled={passwordMutation.isPending}
                                        sx={{
                                            alignSelf: { xs: 'stretch', sm: 'flex-end' },
                                            borderColor: '#2A2B61',
                                            color: '#2A2B61',
                                            minWidth: 170,
                                            '&:hover': {
                                                bgcolor: 'rgba(42, 43, 97, 0.06)',
                                                borderColor: '#2A2B61',
                                            },
                                        }}
                                    >
                                        {passwordMutation.isPending
                                            ? <CircularProgress color="inherit" size={22} />
                                            : 'Parolayı Değiştir'}
                                    </Button>
                                </Stack>
                            </Box>
                        </Stack>
                    </Paper>
                </Box>
            )}

            <Snackbar
                open={Boolean(successMessage)}
                autoHideDuration={4000}
                onClose={() => setSuccessMessage(null)}
                anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
            >
                <Alert
                    severity="success"
                    variant="filled"
                    onClose={() => setSuccessMessage(null)}
                >
                    {successMessage}
                </Alert>
            </Snackbar>
        </Stack>
    );
}
