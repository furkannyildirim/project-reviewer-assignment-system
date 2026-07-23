import { useMemo, useState } from 'react';
import axios from 'axios';
import {
    Alert,
    Button,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    FormControlLabel,
    IconButton,
    InputAdornment,
    Stack,
    Switch,
    TextField,
} from '@mui/material';
import VisibilityOffOutlinedIcon from '@mui/icons-material/VisibilityOffOutlined';
import VisibilityOutlinedIcon from '@mui/icons-material/VisibilityOutlined';
import type { ApiErrorResponse, FieldErrors } from '../../../types/api';
import type {
    CreateUserRequest,
    UpdateUserRequest,
    User,
    UserFormValues,
} from '../user.types';

type UserFormDialogProps = {
    open: boolean;
    user: User | null;
    isSaving: boolean;
    onClose: () => void;
    onSubmit: (
        request: CreateUserRequest | UpdateUserRequest,
        mode: 'create' | 'edit',
    ) => Promise<void>;
};

type FormErrors = Partial<Record<keyof UserFormValues, string>>;

const emptyValues: UserFormValues = {
    firstName: '',
    lastName: '',
    email: '',
    phone: '',
    password: '',
    active: true,
};

const fieldLabels: Record<keyof UserFormValues, string> = {
    firstName: 'Ad',
    lastName: 'Soyad',
    email: 'E-posta',
    phone: 'Telefon',
    password: 'Parola',
    active: 'Aktif kullanıcı',
};

function toFormValues(user: User | null): UserFormValues {
    if (!user) {
        return emptyValues;
    }

    return {
        firstName: user.firstName,
        lastName: user.lastName,
        email: user.email,
        phone: user.phone,
        password: '',
        active: user.active,
    };
}

function buildCreateRequest(values: UserFormValues): CreateUserRequest {
    return {
        firstName: values.firstName.trim(),
        lastName: values.lastName.trim(),
        email: values.email.trim(),
        phone: values.phone.trim(),
        password: values.password,
    };
}

function buildUpdateRequest(values: UserFormValues): UpdateUserRequest {
    return {
        firstName: values.firstName.trim(),
        lastName: values.lastName.trim(),
        email: values.email.trim(),
        phone: values.phone.trim(),
        active: values.active,
    };
}

function validate(values: UserFormValues, mode: 'create' | 'edit'): FormErrors {
    const errors: FormErrors = {};
    const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

    if (!values.firstName.trim()) {
        errors.firstName = 'Ad zorunludur';
    } else if (values.firstName.trim().length > 100) {
        errors.firstName = 'Ad en fazla 100 karakter olabilir';
    }

    if (!values.lastName.trim()) {
        errors.lastName = 'Soyad zorunludur';
    } else if (values.lastName.trim().length > 100) {
        errors.lastName = 'Soyad en fazla 100 karakter olabilir';
    }

    if (!values.email.trim()) {
        errors.email = 'E-posta zorunludur';
    } else if (values.email.trim().length > 255) {
        errors.email = 'E-posta en fazla 255 karakter olabilir';
    } else if (!emailPattern.test(values.email.trim())) {
        errors.email = 'Geçerli bir e-posta adresi girilmelidir';
    }

    if (!values.phone.trim()) {
        errors.phone = 'Telefon zorunludur';
    } else if (values.phone.trim().length > 30) {
        errors.phone = 'Telefon en fazla 30 karakter olabilir';
    }

    if (mode === 'create') {
        if (!values.password) {
            errors.password = 'Parola zorunludur';
        } else if (values.password.length < 8 || values.password.length > 72) {
            errors.password = 'Parola 8 ile 72 karakter arasında olmalıdır';
        }
    }

    return errors;
}

function isApiErrorResponse(value: unknown): value is ApiErrorResponse {
    return Boolean(
        value
        && typeof value === 'object'
        && 'message' in value
        && typeof value.message === 'string',
    );
}

function getFieldErrors(error: unknown): FieldErrors {
    if (axios.isAxiosError(error) && isApiErrorResponse(error.response?.data)) {
        return error.response.data.fieldErrors ?? {};
    }

    return {};
}

export function UserFormDialog({
    open,
    user,
    isSaving,
    onClose,
    onSubmit,
}: UserFormDialogProps) {
    const mode = user ? 'edit' : 'create';
    const [values, setValues] = useState<UserFormValues>(() =>
        toFormValues(user),
    );
    const [errors, setErrors] = useState<FormErrors>({});
    const [generalError, setGeneralError] = useState<string | null>(null);
    const [showPassword, setShowPassword] = useState(false);
    const title = user ? 'Kullanıcıyı Düzenle' : 'Yeni Kullanıcı';

    const helperTexts = useMemo(() => ({
        firstName: errors.firstName,
        lastName: errors.lastName,
        email: errors.email,
        phone: errors.phone,
        password: errors.password,
    }), [errors]);

    function handleChange(field: keyof UserFormValues, value: string | boolean) {
        setValues((currentValues) => ({
            ...currentValues,
            [field]: value,
        }));
        setErrors((currentErrors) => ({
            ...currentErrors,
            [field]: undefined,
        }));
        setGeneralError(null);
    }

    async function handleSubmit() {
        const validationErrors = validate(values, mode);

        if (Object.keys(validationErrors).length > 0) {
            setErrors(validationErrors);
            return;
        }

        try {
            await onSubmit(
                mode === 'create'
                    ? buildCreateRequest(values)
                    : buildUpdateRequest(values),
                mode,
            );
        } catch (error) {
            const apiFieldErrors = getFieldErrors(error);
            const nextErrors: FormErrors = {};

            for (const field of Object.keys(fieldLabels) as Array<keyof UserFormValues>) {
                if (apiFieldErrors[field]) {
                    nextErrors[field] = apiFieldErrors[field];
                }
            }

            setErrors(nextErrors);
            setGeneralError(
                axios.isAxiosError(error) && isApiErrorResponse(error.response?.data)
                    ? error.response.data.message
                    : 'Kullanıcı kaydedilemedi',
            );
        }
    }

    return (
        <Dialog open={open} onClose={isSaving ? undefined : onClose} fullWidth maxWidth="sm">
            <DialogTitle>{title}</DialogTitle>
            <DialogContent>
                <Stack spacing={2.5} sx={{ pt: 1 }}>
                    {generalError && (
                        <Alert severity="error">{generalError}</Alert>
                    )}

                    <TextField
                        label={fieldLabels.firstName}
                        value={values.firstName}
                        onChange={(event) => handleChange('firstName', event.target.value)}
                        error={Boolean(helperTexts.firstName)}
                        helperText={helperTexts.firstName}
                        required
                        fullWidth
                        slotProps={{ htmlInput: { maxLength: 100 } }}
                    />

                    <TextField
                        label={fieldLabels.lastName}
                        value={values.lastName}
                        onChange={(event) => handleChange('lastName', event.target.value)}
                        error={Boolean(helperTexts.lastName)}
                        helperText={helperTexts.lastName}
                        required
                        fullWidth
                        slotProps={{ htmlInput: { maxLength: 100 } }}
                    />

                    <TextField
                        label={fieldLabels.email}
                        type="email"
                        value={values.email}
                        onChange={(event) => handleChange('email', event.target.value)}
                        error={Boolean(helperTexts.email)}
                        helperText={helperTexts.email}
                        required
                        fullWidth
                        slotProps={{ htmlInput: { maxLength: 255 } }}
                    />

                    <TextField
                        label={fieldLabels.phone}
                        value={values.phone}
                        onChange={(event) => handleChange('phone', event.target.value)}
                        error={Boolean(helperTexts.phone)}
                        helperText={helperTexts.phone}
                        required
                        fullWidth
                        slotProps={{ htmlInput: { maxLength: 30 } }}
                    />

                    {mode === 'create' ? (
                        <TextField
                            label={fieldLabels.password}
                            type={showPassword ? 'text' : 'password'}
                            value={values.password}
                            onChange={(event) => handleChange('password', event.target.value)}
                            error={Boolean(helperTexts.password)}
                            helperText={helperTexts.password}
                            required
                            fullWidth
                            slotProps={{
                                htmlInput: { maxLength: 72 },
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
                                                    setShowPassword((current) => !current);
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
                    ) : (
                        <FormControlLabel
                            control={(
                                <Switch
                                    checked={values.active}
                                    onChange={(event) => handleChange(
                                        'active',
                                        event.target.checked,
                                    )}
                                />
                            )}
                            label={fieldLabels.active}
                        />
                    )}
                </Stack>
            </DialogContent>
            <DialogActions>
                <Button onClick={onClose} disabled={isSaving}>
                    Vazgeç
                </Button>
                <Button
                    variant="contained"
                    loading={isSaving}
                    onClick={handleSubmit}
                >
                    Kaydet
                </Button>
            </DialogActions>
        </Dialog>
    );
}
