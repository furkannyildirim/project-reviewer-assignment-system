import { useMemo, useState } from 'react';
import axios from 'axios';
import {
    Alert,
    Button,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    Stack,
    TextField,
} from '@mui/material';
import type { ApiErrorResponse, FieldErrors } from '../../../types/api';
import type {
    Company,
    CompanyFormValues,
    CreateCompanyRequest,
} from '../company.types';

type CompanyFormDialogProps = {
    open: boolean;
    company: Company | null;
    isSaving: boolean;
    onClose: () => void;
    onSubmit: (request: CreateCompanyRequest) => Promise<void>;
};

type FormErrors = Partial<Record<keyof CompanyFormValues, string>>;

const emptyValues: CompanyFormValues = {
    name: '',
    email: '',
    phone: '',
    activityArea: '',
    logoUrl: '',
};

const fieldLabels: Record<keyof CompanyFormValues, string> = {
    name: 'Şirket adı',
    email: 'E-posta',
    phone: 'Telefon',
    activityArea: 'Faaliyet alanı',
    logoUrl: 'Logo URL',
};

function toFormValues(company: Company | null): CompanyFormValues {
    if (!company) {
        return emptyValues;
    }

    return {
        name: company.name,
        email: company.email ?? '',
        phone: company.phone ?? '',
        activityArea: company.activityArea ?? '',
        logoUrl: company.logoUrl ?? '',
    };
}

function normalizeOptional(value: string): string | null {
    const normalizedValue = value.trim();

    return normalizedValue ? normalizedValue : null;
}

function buildRequest(values: CompanyFormValues): CreateCompanyRequest {
    return {
        name: values.name.trim(),
        email: normalizeOptional(values.email),
        phone: normalizeOptional(values.phone),
        activityArea: normalizeOptional(values.activityArea),
        logoUrl: normalizeOptional(values.logoUrl),
    };
}

function validate(values: CompanyFormValues): FormErrors {
    const errors: FormErrors = {};
    const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

    if (!values.name.trim()) {
        errors.name = 'Şirket adı zorunludur';
    } else if (values.name.trim().length > 200) {
        errors.name = 'Şirket adı en fazla 200 karakter olabilir';
    }

    if (values.email.trim()) {
        if (values.email.trim().length > 255) {
            errors.email = 'E-posta en fazla 255 karakter olabilir';
        } else if (!emailPattern.test(values.email.trim())) {
            errors.email = 'Geçerli bir e-posta adresi girilmelidir';
        }
    }

    if (values.phone.trim().length > 30) {
        errors.phone = 'Telefon en fazla 30 karakter olabilir';
    }

    if (values.activityArea.trim().length > 255) {
        errors.activityArea = 'Faaliyet alanı en fazla 255 karakter olabilir';
    }

    if (values.logoUrl.trim().length > 500) {
        errors.logoUrl = 'Logo adresi en fazla 500 karakter olabilir';
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

export function CompanyFormDialog({
    open,
    company,
    isSaving,
    onClose,
    onSubmit,
}: CompanyFormDialogProps) {
    const [values, setValues] = useState<CompanyFormValues>(() =>
        toFormValues(company),
    );
    const [errors, setErrors] = useState<FormErrors>({});
    const [generalError, setGeneralError] = useState<string | null>(null);
    const title = company ? 'Şirketi Düzenle' : 'Yeni Şirket';

    const helperTexts = useMemo(() => ({
        name: errors.name,
        email: errors.email,
        phone: errors.phone,
        activityArea: errors.activityArea,
        logoUrl: errors.logoUrl,
    }), [errors]);

    function handleChange(field: keyof CompanyFormValues, value: string) {
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
        const validationErrors = validate(values);

        if (Object.keys(validationErrors).length > 0) {
            setErrors(validationErrors);
            return;
        }

        try {
            await onSubmit(buildRequest(values));
        } catch (error) {
            const apiFieldErrors = getFieldErrors(error);
            const nextErrors: FormErrors = {};

            for (const field of Object.keys(fieldLabels) as Array<keyof CompanyFormValues>) {
                if (apiFieldErrors[field]) {
                    nextErrors[field] = apiFieldErrors[field];
                }
            }

            setErrors(nextErrors);
            setGeneralError(
                axios.isAxiosError(error) && isApiErrorResponse(error.response?.data)
                    ? error.response.data.message
                    : 'Şirket kaydedilemedi',
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
                        label={fieldLabels.name}
                        value={values.name}
                        onChange={(event) => handleChange('name', event.target.value)}
                        error={Boolean(helperTexts.name)}
                        helperText={helperTexts.name}
                        required
                        fullWidth
                        slotProps={{
                            htmlInput: { maxLength: 200 },
                        }}
                    />

                    <TextField
                        label={fieldLabels.email}
                        type="email"
                        value={values.email}
                        onChange={(event) => handleChange('email', event.target.value)}
                        error={Boolean(helperTexts.email)}
                        helperText={helperTexts.email}
                        fullWidth
                        slotProps={{
                            htmlInput: { maxLength: 255 },
                        }}
                    />

                    <TextField
                        label={fieldLabels.phone}
                        value={values.phone}
                        onChange={(event) => handleChange('phone', event.target.value)}
                        error={Boolean(helperTexts.phone)}
                        helperText={helperTexts.phone}
                        fullWidth
                        slotProps={{
                            htmlInput: { maxLength: 30 },
                        }}
                    />

                    <TextField
                        label={fieldLabels.activityArea}
                        value={values.activityArea}
                        onChange={(event) => handleChange('activityArea', event.target.value)}
                        error={Boolean(helperTexts.activityArea)}
                        helperText={helperTexts.activityArea}
                        fullWidth
                        slotProps={{
                            htmlInput: { maxLength: 255 },
                        }}
                    />

                    <TextField
                        label={fieldLabels.logoUrl}
                        value={values.logoUrl}
                        onChange={(event) => handleChange('logoUrl', event.target.value)}
                        error={Boolean(helperTexts.logoUrl)}
                        helperText={helperTexts.logoUrl}
                        fullWidth
                        slotProps={{
                            htmlInput: { maxLength: 500 },
                        }}
                    />
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
