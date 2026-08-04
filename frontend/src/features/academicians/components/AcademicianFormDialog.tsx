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
    Academician,
    AcademicianFormValues,
    CreateAcademicianRequest,
    UpdateAcademicianRequest,
} from '../academician.types';

type AcademicianFormDialogProps = {
    open: boolean;
    academician: Academician | null;
    isSaving: boolean;
    onClose: () => void;
    onSubmit: (
        request: CreateAcademicianRequest | UpdateAcademicianRequest,
    ) => Promise<void>;
};

type FormErrors = Partial<Record<keyof AcademicianFormValues, string>>;

const emptyValues: AcademicianFormValues = {
    firstName: '',
    lastName: '',
    email: '',
    phone: '',
    title: '',
    keywordsText: '',
};

const fieldLabels: Record<keyof AcademicianFormValues, string> = {
    firstName: 'Ad',
    lastName: 'Soyad',
    email: 'E-posta',
    phone: 'Telefon',
    title: 'Ünvan',
    keywordsText: 'Anahtar kelimeler',
};

function toFormValues(academician: Academician | null): AcademicianFormValues {
    if (!academician) {
        return emptyValues;
    }

    return {
        firstName: academician.firstName,
        lastName: academician.lastName,
        email: academician.email ?? '',
        phone: academician.phone ?? '',
        title: academician.title ?? '',
        keywordsText: academician.keywords.join(', '),
    };
}

function normalizeOptional(value: string): string | null {
    const normalizedValue = value.trim();

    return normalizedValue ? normalizedValue : null;
}

function parseKeywords(value: string): string[] {
    return value
        .split(/[,;\n\r]+/)
        .map((keyword) => keyword.trim().replace(/\s+/g, ' '))
        .filter(Boolean);
}

function normalizeKeywords(value: string): string[] {
    const uniqueKeywords = new Map<string, string>();

    for (const keyword of parseKeywords(value)) {
        const comparisonValue = keyword.toLocaleLowerCase('tr-TR');

        if (!uniqueKeywords.has(comparisonValue)) {
            uniqueKeywords.set(comparisonValue, keyword);
        }
    }

    return [...uniqueKeywords.values()];
}

function buildRequest(
    values: AcademicianFormValues,
    academician: Academician | null,
): CreateAcademicianRequest | UpdateAcademicianRequest {
    const request = {
        firstName: values.firstName.trim(),
        lastName: values.lastName.trim(),
        email: normalizeOptional(values.email),
        phone: normalizeOptional(values.phone),
        title: normalizeOptional(values.title),
        keywords: normalizeKeywords(values.keywordsText),
    };

    return academician
        ? {
            ...request,
            version: academician.version,
        }
        : request;
}

function validate(values: AcademicianFormValues): FormErrors {
    const errors: FormErrors = {};
    const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    const keywords = normalizeKeywords(values.keywordsText);

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

    if (values.title.trim().length > 100) {
        errors.title = 'Ünvan en fazla 100 karakter olabilir';
    }

    if (keywords.some((keyword) => keyword.length > 255)) {
        errors.keywordsText = 'Anahtar kelime en fazla 255 karakter olabilir';
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

function getApiFieldError(
    apiFieldErrors: FieldErrors,
    field: keyof AcademicianFormValues,
): string | undefined {
    if (apiFieldErrors[field]) {
        return apiFieldErrors[field];
    }

    if (field === 'keywordsText') {
        const keywordField = Object.keys(apiFieldErrors).find((key) =>
            key.startsWith('keywords'),
        );

        return keywordField ? apiFieldErrors[keywordField] : undefined;
    }

    return undefined;
}

export function AcademicianFormDialog({
    open,
    academician,
    isSaving,
    onClose,
    onSubmit,
}: AcademicianFormDialogProps) {
    const [values, setValues] = useState<AcademicianFormValues>(() =>
        toFormValues(academician),
    );
    const [errors, setErrors] = useState<FormErrors>({});
    const [generalError, setGeneralError] = useState<string | null>(null);
    const title = academician ? 'Akademisyeni Düzenle' : 'Yeni Akademisyen';

    const helperTexts = useMemo(() => ({
        firstName: errors.firstName,
        lastName: errors.lastName,
        email: errors.email,
        phone: errors.phone,
        title: errors.title,
        keywordsText: errors.keywordsText,
    }), [errors]);

    function handleChange(field: keyof AcademicianFormValues, value: string) {
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
            await onSubmit(buildRequest(values, academician));
        } catch (error) {
            const apiFieldErrors = getFieldErrors(error);
            const nextErrors: FormErrors = {};

            for (const field of Object.keys(fieldLabels) as Array<keyof AcademicianFormValues>) {
                const fieldError = getApiFieldError(apiFieldErrors, field);

                if (fieldError) {
                    nextErrors[field] = fieldError;
                }
            }

            setErrors(nextErrors);
            setGeneralError(
                axios.isAxiosError(error) && isApiErrorResponse(error.response?.data)
                    ? error.response.data.message
                    : 'Akademisyen kaydedilemedi',
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
                        slotProps={{
                            htmlInput: { maxLength: 100 },
                        }}
                    />

                    <TextField
                        label={fieldLabels.lastName}
                        value={values.lastName}
                        onChange={(event) => handleChange('lastName', event.target.value)}
                        error={Boolean(helperTexts.lastName)}
                        helperText={helperTexts.lastName}
                        required
                        fullWidth
                        slotProps={{
                            htmlInput: { maxLength: 100 },
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
                        label={fieldLabels.title}
                        value={values.title}
                        onChange={(event) => handleChange('title', event.target.value)}
                        error={Boolean(helperTexts.title)}
                        helperText={helperTexts.title}
                        fullWidth
                        slotProps={{
                            htmlInput: { maxLength: 100 },
                        }}
                    />

                    <TextField
                        label={fieldLabels.keywordsText}
                        value={values.keywordsText}
                        onChange={(event) => handleChange('keywordsText', event.target.value)}
                        error={Boolean(helperTexts.keywordsText)}
                        helperText={
                            helperTexts.keywordsText
                            ?? 'Anahtar kelimeleri virgül, noktalı virgül veya yeni satır ile ayırabilirsiniz.'
                        }
                        multiline
                        rows={3}
                        fullWidth
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
