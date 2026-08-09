import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import {
    Alert,
    Box,
    Button,
    IconButton,
    InputAdornment,
    Paper,
    Stack,
    TextField,
    Typography,
} from '@mui/material';
import LoginOutlinedIcon from '@mui/icons-material/LoginOutlined';
import VisibilityOffOutlinedIcon from '@mui/icons-material/VisibilityOffOutlined';
import VisibilityOutlinedIcon from '@mui/icons-material/VisibilityOutlined';
import { useNavigate } from 'react-router';
import { useAuth } from './useAuth';
import { getApiErrorMessage } from '../../utils/getApiErrorMessage';
import sakaryaTeknokentLogo from '../../assets/sakarya-teknokent-logo-on-dark.svg';

const PANEL_BLUE = '#0A1026';
const NIGHT_BLUE = '#2A2B61';
const LIGHT_BLUE = '#2EA8D3';

export function LoginPage() {
    const { isAuthenticated, login } = useAuth();
    const navigate = useNavigate();
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [errorMessage, setErrorMessage] = useState<string | null>(null);

    useEffect(() => {
        if (isAuthenticated) {
            navigate('/academicians', { replace: true });
        }
    }, [isAuthenticated, navigate]);

    async function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();

        const trimmedEmail = email.trim();

        if (!trimmedEmail || !password) {
            setErrorMessage('E-posta ve parola alanları zorunludur');
            return;
        }

        setIsSubmitting(true);
        setErrorMessage(null);

        try {
            await login({
                email: trimmedEmail,
                password,
            });
            navigate('/academicians', { replace: true });
        } catch (error) {
            setErrorMessage(
                getApiErrorMessage(error, 'Giriş işlemi tamamlanamadı'),
            );
        } finally {
            setIsSubmitting(false);
        }
    }

    return (
        <Box
            sx={{
                minHeight: '100svh',
                display: 'grid',
                gridTemplateColumns: {
                    xs: 'minmax(0, 1fr)',
                    md: 'clamp(240px, 20vw, 288px) minmax(0, 1fr)',
                },
                bgcolor: '#F4F7FA',
            }}
        >
            <Box
                component="aside"
                sx={{
                    display: { xs: 'none', md: 'flex' },
                    minHeight: '100svh',
                    flexDirection: 'column',
                    bgcolor: PANEL_BLUE,
                    color: '#FFFFFF',
                    px: { md: 3.5, lg: 4 },
                    py: 5,
                }}
            >
                <Box
                    component="img"
                    src={sakaryaTeknokentLogo}
                    alt="Sakarya Teknokent"
                    sx={{
                        display: 'block',
                        width: '100%',
                        maxWidth: 218,
                        height: 'auto',
                    }}
                />

                <Box sx={{ mt: 'auto', pt: 4 }}>
                    <Box
                        aria-hidden="true"
                        sx={{
                            width: 32,
                            height: 3,
                            mb: 1.5,
                            bgcolor: LIGHT_BLUE,
                        }}
                    />
                    <Typography
                        sx={{
                            color: 'rgba(255, 255, 255, 0.68)',
                            fontSize: 12,
                            lineHeight: 1.6,
                        }}
                    >
                        Sakarya Teknokent
                    </Typography>
                </Box>
            </Box>

            <Box
                component="main"
                sx={{
                    minWidth: 0,
                    minHeight: '100svh',
                    display: 'flex',
                    flexDirection: 'column',
                }}
            >
                <Box
                    sx={{
                        display: { xs: 'flex', md: 'none' },
                        alignItems: 'center',
                        justifyContent: 'center',
                        minHeight: 104,
                        bgcolor: PANEL_BLUE,
                        px: 3,
                        py: 2.5,
                    }}
                >
                    <Box
                        component="img"
                        src={sakaryaTeknokentLogo}
                        alt="Sakarya Teknokent"
                        sx={{
                            display: 'block',
                            width: 'min(220px, 72vw)',
                            height: 'auto',
                        }}
                    />
                </Box>

                <Box
                    component="header"
                    sx={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: { xs: 2, sm: 2.5 },
                        minHeight: { xs: 88, md: 124 },
                        px: { xs: 3, sm: 5, lg: 7 },
                        py: { xs: 2.5, md: 3 },
                        bgcolor: 'rgba(255, 255, 255, 0.72)',
                        borderBottom: '1px solid #E1E8EF',
                    }}
                >
                    <Box
                        aria-hidden="true"
                        sx={{
                            width: 4,
                            height: { xs: 42, md: 50 },
                            flex: '0 0 auto',
                            bgcolor: LIGHT_BLUE,
                            borderRadius: 999,
                        }}
                    />
                    <Stack spacing={0.25}>
                        <Typography
                            component="p"
                            sx={{
                                color: NIGHT_BLUE,
                                fontSize: { xs: 17, sm: 20 },
                                fontWeight: 700,
                                lineHeight: 1.35,
                                letterSpacing: '-0.01em',
                            }}
                        >
                            Hakem Atama Yönetim Sistemi
                        </Typography>
                        <Typography
                            sx={{
                                color: '#687386',
                                fontSize: { xs: 13, sm: 14 },
                                lineHeight: 1.5,
                            }}
                        >
                            Proje değerlendirme ve hakem atama süreçleri için yönetim platformu.
                        </Typography>
                    </Stack>
                </Box>

                <Box
                    sx={{
                        flex: 1,
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        px: { xs: 2, sm: 4 },
                        py: { xs: 4, md: 5 },
                    }}
                >
                    <Paper
                        elevation={0}
                        sx={{
                            width: '100%',
                            maxWidth: { xs: 432, md: 480 },
                            p: { xs: 3, sm: 4.5, md: 5 },
                            border: '1px solid #DEE6EE',
                            borderRadius: 3,
                            boxShadow: '0 18px 50px rgba(42, 43, 97, 0.08)',
                        }}
                    >
                        <Stack
                            component="form"
                            spacing={{ xs: 2.5, md: 2.75 }}
                            onSubmit={handleSubmit}
                            noValidate
                            sx={{
                                '& .MuiInputLabel-root.Mui-focused': {
                                    color: NIGHT_BLUE,
                                },
                                '& .MuiOutlinedInput-root': {
                                    borderRadius: 2,
                                    minHeight: { md: 44 },
                                    '&.Mui-focused': {
                                        boxShadow: '0 0 0 3px rgba(46, 168, 211, 0.14)',
                                    },
                                    '&.Mui-focused .MuiOutlinedInput-notchedOutline': {
                                        borderColor: NIGHT_BLUE,
                                    },
                                },
                            }}
                        >
                        <Stack spacing={0.75} sx={{ mb: 0.5 }}>
                            <Box
                                aria-hidden="true"
                                sx={{
                                    width: 36,
                                    height: 3,
                                    mb: 1,
                                    bgcolor: LIGHT_BLUE,
                                }}
                            />
                            <Typography
                                component="h1"
                                sx={{
                                    color: NIGHT_BLUE,
                                    fontSize: { xs: 28, sm: 30, md: 32 },
                                    fontWeight: 700,
                                    lineHeight: 1.2,
                                    letterSpacing: '-0.02em',
                                }}
                            >
                                Giriş Yap
                            </Typography>
                            <Typography
                                sx={{
                                    color: '#687386',
                                    fontSize: { xs: 14, md: 14.5 },
                                    lineHeight: 1.6,
                                }}
                            >
                                Devam etmek için hesabınızla giriş yapın.
                            </Typography>
                        </Stack>

                        {errorMessage && (
                            <Alert severity="error">
                                {errorMessage}
                            </Alert>
                        )}

                        <TextField
                            label="E-posta"
                            type="email"
                            value={email}
                            onChange={(event) => setEmail(event.target.value)}
                            autoComplete="email"
                            required
                            fullWidth
                        />

                        <TextField
                            label="Parola"
                            type={showPassword ? 'text' : 'password'}
                            value={password}
                            onChange={(event) => setPassword(event.target.value)}
                            autoComplete="current-password"
                            required
                            fullWidth
                            slotProps={{
                                input: {
                                    endAdornment: (
                                        <InputAdornment position="end">
                                            <IconButton
                                                aria-label={
                                                    showPassword
                                                        ? 'Parolayı gizle'
                                                        : 'Parolayı göster'
                                                }
                                                edge="end"
                                                sx={{
                                                    '&:hover': {
                                                        color: NIGHT_BLUE,
                                                        bgcolor: 'rgba(46, 168, 211, 0.09)',
                                                    },
                                                    '&:focus-visible': {
                                                        outline: `3px solid rgba(46, 168, 211, 0.24)`,
                                                        outlineOffset: 2,
                                                    },
                                                }}
                                                onClick={() => {
                                                    setShowPassword(
                                                        (current) => !current,
                                                    );
                                                }}
                                            >
                                                {showPassword
                                                    ? <VisibilityOffOutlinedIcon />
                                                    : <VisibilityOutlinedIcon />}
                                            </IconButton>
                                        </InputAdornment>
                                    ),
                                },
                            }}
                        />

                        <Button
                            type="submit"
                            variant="contained"
                            size="large"
                            loading={isSubmitting}
                            startIcon={<LoginOutlinedIcon />}
                            sx={{
                                minHeight: { xs: 48, md: 50 },
                                mt: 0.5,
                                bgcolor: NIGHT_BLUE,
                                borderRadius: 2,
                                '&:hover': {
                                    bgcolor: '#202153',
                                },
                                '&:focus-visible': {
                                    outline: `3px solid rgba(46, 168, 211, 0.28)`,
                                    outlineOffset: 2,
                                },
                            }}
                        >
                            Giriş Yap
                        </Button>

                        <Typography
                            sx={{
                                pt: { xs: 2, md: 2.25 },
                                borderTop: '1px solid #E8EDF2',
                                color: '#7A8495',
                                fontSize: { xs: 12.5, md: 13.5 },
                                lineHeight: { xs: 1.6, md: 1.65 },
                                textAlign: 'center',
                            }}
                        >
                            Hesap oluşturma işlemleri sistem yöneticileri
                            tarafından yapılır.
                        </Typography>
                        </Stack>
                    </Paper>
                </Box>
            </Box>
        </Box>
    );
}
