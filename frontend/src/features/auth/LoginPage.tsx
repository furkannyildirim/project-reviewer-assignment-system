import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import {
    Alert,
    Box,
    Button,
    Container,
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
                minHeight: '100vh',
                display: 'flex',
                alignItems: 'center',
                bgcolor: 'background.default',
                py: 4,
            }}
        >
            <Container maxWidth="xs">
                <Paper
                    elevation={0}
                    sx={{
                        p: { xs: 3, sm: 4 },
                        border: '1px solid',
                        borderColor: 'divider',
                        borderRadius: '8px',
                    }}
                >
                    <Stack
                        component="form"
                        spacing={3}
                        onSubmit={handleSubmit}
                        noValidate
                    >
                        <Stack spacing={1}>
                            <Typography variant="h4" component="h1">
                                Yönetim Girişi
                            </Typography>
                            <Typography color="text.secondary">
                                Hakem Atama Yönetim Sistemi
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
                        >
                            Giriş yap
                        </Button>
                    </Stack>
                </Paper>
            </Container>
        </Box>
    );
}
