import { useMemo, useState } from 'react';
import axios from 'axios';
import {
    Alert,
    Box,
    Button,
    Card,
    CardContent,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    Divider,
    FormControlLabel,
    Radio,
    Stack,
    TextField,
    Typography,
} from '@mui/material';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import DeleteOutlineOutlinedIcon from '@mui/icons-material/DeleteOutlineOutlined';
import type { ApiErrorResponse, FieldErrors } from '../../../types/api';
import type {
    Company,
    CompanyContactFormValues,
    CompanyDetail,
    CompanyFormValues,
    CreateCompanyRequest,
    UpdateCompanyRequest,
} from '../company.types';

type CompanyFormDialogProps = {
    open: boolean;
    company: Company | null;
    companyDetail: CompanyDetail | null;
    isDetailLoading: boolean;
    detailError: string | null;
    isSaving: boolean;
    onClose: () => void;
    onSubmit: (
        request: CreateCompanyRequest | UpdateCompanyRequest,
    ) => Promise<void>;
};

type FormErrors = Partial<Record<string, string>>;

const emptyContact: CompanyContactFormValues = {
    id: null,
    fullName: '',
    position: '',
    email: '',
    phone: '',
    primary: true,
};

const emptyValues: CompanyFormValues = {
    name: '',
    email: '',
    phone: '',
    activityArea: '',
    logoUrl: '',
    contacts: [emptyContact],
};

const fieldLabels = {
    name: 'Şirket adı',
    email: 'E-posta',
    phone: 'Telefon',
    activityArea: 'Faaliyet alanı',
    logoUrl: 'Logo URL',
} satisfies Record<Exclude<keyof CompanyFormValues, 'contacts'>, string>;

function createEmptyContact(primary = false): CompanyContactFormValues {
    return {
        ...emptyContact,
        primary,
    };
}

function toFormValues(company: CompanyDetail | null): CompanyFormValues {
    if (!company) {
        return {
            ...emptyValues,
            contacts: [createEmptyContact(true)],
        };
    }

    const contacts = company.contacts.length > 0
        ? company.contacts.map((contact) => ({
            id: contact.id,
            fullName: contact.fullName,
            position: contact.position ?? '',
            email: contact.email ?? '',
            phone: contact.phone ?? '',
            primary: contact.primary,
        }))
        : [createEmptyContact(true)];

    return {
        name: company.name,
        email: company.email ?? '',
        phone: company.phone ?? '',
        activityArea: company.activityArea ?? '',
        logoUrl: company.logoUrl ?? '',
        contacts,
    };
}

function normalizeOptional(value: string): string | null {
    const normalizedValue = value.trim();

    return normalizedValue ? normalizedValue : null;
}

function buildRequest(
    values: CompanyFormValues,
    isEdit: boolean,
): CreateCompanyRequest | UpdateCompanyRequest {
    const companyFields = {
        name: values.name.trim(),
        email: normalizeOptional(values.email),
        phone: normalizeOptional(values.phone),
        activityArea: normalizeOptional(values.activityArea),
        logoUrl: normalizeOptional(values.logoUrl),
    };

    if (isEdit) {
        return {
            ...companyFields,
            contacts: values.contacts.map((contact) => ({
                id: contact.id,
                fullName: contact.fullName.trim(),
                position: normalizeOptional(contact.position),
                email: normalizeOptional(contact.email),
                phone: normalizeOptional(contact.phone),
                primary: contact.primary,
            })),
        };
    }

    return {
        ...companyFields,
        contacts: values.contacts.map((contact) => ({
            fullName: contact.fullName.trim(),
            position: normalizeOptional(contact.position),
            email: normalizeOptional(contact.email),
            phone: normalizeOptional(contact.phone),
            primary: contact.primary,
        })),
    };
}

function contactField(index: number, field: keyof CompanyContactFormValues) {
    return `contacts[${index}].${field}`;
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

    if (values.contacts.length === 0) {
        errors.contacts = 'En az bir muhatap kişi eklenmelidir';
    }

    const primaryCount = values.contacts.filter((contact) => contact.primary)
        .length;

    if (primaryCount !== 1) {
        errors.contacts = 'Tam olarak bir muhatap kişi birincil olarak seçilmelidir';
    }

    values.contacts.forEach((contact, index) => {
        const fullName = contact.fullName.trim();
        const position = contact.position.trim();
        const email = contact.email.trim();
        const phone = contact.phone.trim();

        if (!fullName) {
            errors[contactField(index, 'fullName')] =
                'Muhatap adı soyadı boş bırakılamaz';
        } else if (fullName.length > 200) {
            errors[contactField(index, 'fullName')] =
                'Muhatap adı soyadı en fazla 200 karakter olabilir';
        }

        if (position.length > 150) {
            errors[contactField(index, 'position')] =
                'Pozisyon en fazla 150 karakter olabilir';
        }

        if (email) {
            if (email.length > 255) {
                errors[contactField(index, 'email')] =
                    'Muhatap e-postası en fazla 255 karakter olabilir';
            } else if (!emailPattern.test(email)) {
                errors[contactField(index, 'email')] =
                    'Geçerli bir muhatap e-posta adresi girilmelidir';
            }
        }

        if (phone.length > 30) {
            errors[contactField(index, 'phone')] =
                'Muhatap telefonu en fazla 30 karakter olabilir';
        }

        if (!email && !phone) {
            const message =
                'Muhatap kişi için e-posta veya telefon bilgilerinden en az biri girilmelidir';

            errors[contactField(index, 'email')] ??= message;
            errors[contactField(index, 'phone')] ??= message;
        }
    });

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

function getBusinessError(error: unknown): string {
    if (axios.isAxiosError(error) && isApiErrorResponse(error.response?.data)) {
        return error.response.data.message;
    }

    return 'Şirket kaydedilemedi';
}

export function CompanyFormDialog({
    open,
    company,
    companyDetail,
    isDetailLoading,
    detailError,
    isSaving,
    onClose,
    onSubmit,
}: CompanyFormDialogProps) {
    const [values, setValues] = useState<CompanyFormValues>(() =>
        toFormValues(companyDetail),
    );
    const [errors, setErrors] = useState<FormErrors>({});
    const [generalError, setGeneralError] = useState<string | null>(null);
    const isEdit = Boolean(company);
    const title = isEdit ? 'Şirketi Düzenle' : 'Yeni Şirket';
    const detailReady = !isEdit || Boolean(companyDetail);
    const isFormDisabled = isSaving || isDetailLoading || !detailReady || Boolean(detailError);

    const helperTexts = useMemo(() => (values.contacts.map((_, index) => ({
        fullName: errors[contactField(index, 'fullName')],
        position: errors[contactField(index, 'position')],
        email: errors[contactField(index, 'email')],
        phone: errors[contactField(index, 'phone')],
    }))), [errors, values.contacts]);

    function handleChange(field: Exclude<keyof CompanyFormValues, 'contacts'>, value: string) {
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

    function handleContactChange(
        index: number,
        field: Exclude<keyof CompanyContactFormValues, 'id' | 'primary'>,
        value: string,
    ) {
        setValues((currentValues) => ({
            ...currentValues,
            contacts: currentValues.contacts.map((contact, contactIndex) =>
                contactIndex === index
                    ? { ...contact, [field]: value }
                    : contact,
            ),
        }));
        setErrors((currentErrors) => ({
            ...currentErrors,
            [contactField(index, field)]: undefined,
        }));
        setGeneralError(null);
    }

    function handleAddContact() {
        setValues((currentValues) => ({
            ...currentValues,
            contacts: [
                ...currentValues.contacts,
                createEmptyContact(false),
            ],
        }));
        setErrors((currentErrors) => ({
            ...currentErrors,
            contacts: undefined,
        }));
        setGeneralError(null);
    }

    function handleRemoveContact(index: number) {
        setValues((currentValues) => {
            if (currentValues.contacts.length === 1) {
                return currentValues;
            }

            const removedContact = currentValues.contacts[index];
            const contacts = currentValues.contacts.filter((_, contactIndex) =>
                contactIndex !== index,
            );

            if (removedContact.primary && contacts.length > 0) {
                return {
                    ...currentValues,
                    contacts: contacts.map((contact, contactIndex) => ({
                        ...contact,
                        primary: contactIndex === 0,
                    })),
                };
            }

            return {
                ...currentValues,
                contacts,
            };
        });
        setErrors({});
        setGeneralError(null);
    }

    function handlePrimaryChange(index: number) {
        setValues((currentValues) => ({
            ...currentValues,
            contacts: currentValues.contacts.map((contact, contactIndex) => ({
                ...contact,
                primary: contactIndex === index,
            })),
        }));
        setErrors((currentErrors) => ({
            ...currentErrors,
            contacts: undefined,
        }));
        setGeneralError(null);
    }

    async function handleSubmit() {
        if (isFormDisabled) {
            return;
        }

        const validationErrors = validate(values);

        if (Object.keys(validationErrors).length > 0) {
            setErrors(validationErrors);
            return;
        }

        try {
            await onSubmit(buildRequest(values, isEdit));
        } catch (error) {
            const apiFieldErrors = getFieldErrors(error);

            setErrors(apiFieldErrors);
            setGeneralError(getBusinessError(error));
        }
    }

    return (
        <Dialog
            open={open}
            onClose={isSaving ? undefined : onClose}
            fullWidth
            maxWidth="md"
        >
            <DialogTitle>{title}</DialogTitle>
            <DialogContent dividers>
                <Stack spacing={2.5} sx={{ pt: 1 }}>
                    {generalError && (
                        <Alert severity="error">{generalError}</Alert>
                    )}

                    {detailError && (
                        <Alert severity="error">{detailError}</Alert>
                    )}

                    {isDetailLoading && (
                        <Alert severity="info">Şirket detayı yükleniyor...</Alert>
                    )}

                    <TextField
                        label={fieldLabels.name}
                        value={values.name}
                        onChange={(event) => handleChange('name', event.target.value)}
                        error={Boolean(errors.name)}
                        helperText={errors.name}
                        required
                        fullWidth
                        disabled={isFormDisabled}
                        slotProps={{
                            htmlInput: { maxLength: 200 },
                        }}
                    />

                    <Stack direction={{ xs: 'column', md: 'row' }} spacing={2}>
                        <TextField
                            label={fieldLabels.email}
                            type="email"
                            value={values.email}
                            onChange={(event) => handleChange('email', event.target.value)}
                            error={Boolean(errors.email)}
                            helperText={errors.email}
                            fullWidth
                            disabled={isFormDisabled}
                            slotProps={{
                                htmlInput: { maxLength: 255 },
                            }}
                        />

                        <TextField
                            label={fieldLabels.phone}
                            value={values.phone}
                            onChange={(event) => handleChange('phone', event.target.value)}
                            error={Boolean(errors.phone)}
                            helperText={errors.phone}
                            fullWidth
                            disabled={isFormDisabled}
                            slotProps={{
                                htmlInput: { maxLength: 30 },
                            }}
                        />
                    </Stack>

                    <TextField
                        label={fieldLabels.activityArea}
                        value={values.activityArea}
                        onChange={(event) => handleChange('activityArea', event.target.value)}
                        error={Boolean(errors.activityArea)}
                        helperText={errors.activityArea}
                        fullWidth
                        disabled={isFormDisabled}
                        slotProps={{
                            htmlInput: { maxLength: 255 },
                        }}
                    />

                    <TextField
                        label={fieldLabels.logoUrl}
                        value={values.logoUrl}
                        onChange={(event) => handleChange('logoUrl', event.target.value)}
                        error={Boolean(errors.logoUrl)}
                        helperText={errors.logoUrl}
                        fullWidth
                        disabled={isFormDisabled}
                        slotProps={{
                            htmlInput: { maxLength: 500 },
                        }}
                    />

                    <Divider />

                    <Stack spacing={1.5}>
                        <Stack
                            direction={{ xs: 'column', sm: 'row' }}
                            spacing={1}
                            sx={{
                                alignItems: { xs: 'stretch', sm: 'center' },
                                justifyContent: 'space-between',
                            }}
                        >
                            <Box>
                                <Typography variant="h6">
                                    Yetkili Kişiler
                                </Typography>
                                {errors.contacts && (
                                    <Typography color="error" variant="body2">
                                        {errors.contacts}
                                    </Typography>
                                )}
                            </Box>

                            <Button
                                variant="outlined"
                                startIcon={<AddOutlinedIcon />}
                                onClick={handleAddContact}
                                disabled={isFormDisabled}
                            >
                                Yetkili Kişi Ekle
                            </Button>
                        </Stack>

                        {values.contacts.map((contact, index) => (
                            <Card
                                key={`${contact.id ?? 'new'}-${index}`}
                                variant="outlined"
                                sx={{ borderRadius: '8px' }}
                            >
                                <CardContent>
                                    <Stack spacing={2}>
                                        <Stack
                                            direction={{ xs: 'column', sm: 'row' }}
                                            spacing={1}
                                            sx={{
                                                alignItems: {
                                                    xs: 'stretch',
                                                    sm: 'center',
                                                },
                                                justifyContent: 'space-between',
                                            }}
                                        >
                                            <Typography sx={{ fontWeight: 600 }}>
                                                {values.contacts.length > 1
                                                    ? `${index + 1}. Yetkili Kişi`
                                                    : 'Yetkili Kişi'}
                                            </Typography>

                                            <Stack
                                                direction="row"
                                                spacing={1}
                                                sx={{
                                                    alignItems: 'center',
                                                    justifyContent: 'space-between',
                                                }}
                                            >
                                                <FormControlLabel
                                                    control={(
                                                        <Radio
                                                            checked={contact.primary}
                                                            onChange={() =>
                                                                handlePrimaryChange(index)}
                                                            disabled={isFormDisabled}
                                                        />
                                                    )}
                                                    label="Ana yetkili kişi"
                                                />
                                                {values.contacts.length > 1 && (
                                                    <Button
                                                        color="error"
                                                        startIcon={<DeleteOutlineOutlinedIcon />}
                                                        onClick={() =>
                                                            handleRemoveContact(index)}
                                                        disabled={isFormDisabled}
                                                    >
                                                        Sil
                                                    </Button>
                                                )}
                                            </Stack>
                                        </Stack>

                                        <TextField
                                            label="Ad soyad"
                                            value={contact.fullName}
                                            onChange={(event) =>
                                                handleContactChange(
                                                    index,
                                                    'fullName',
                                                    event.target.value,
                                                )}
                                            error={Boolean(helperTexts[index]?.fullName)}
                                            helperText={helperTexts[index]?.fullName}
                                            required
                                            fullWidth
                                            disabled={isFormDisabled}
                                            slotProps={{
                                                htmlInput: { maxLength: 200 },
                                            }}
                                        />

                                        <TextField
                                            label="Pozisyon"
                                            value={contact.position}
                                            onChange={(event) =>
                                                handleContactChange(
                                                    index,
                                                    'position',
                                                    event.target.value,
                                                )}
                                            error={Boolean(helperTexts[index]?.position)}
                                            helperText={helperTexts[index]?.position}
                                            fullWidth
                                            disabled={isFormDisabled}
                                            slotProps={{
                                                htmlInput: { maxLength: 150 },
                                            }}
                                        />

                                        <Stack
                                            direction={{ xs: 'column', md: 'row' }}
                                            spacing={2}
                                        >
                                            <TextField
                                                label="E-posta"
                                                type="email"
                                                value={contact.email}
                                                onChange={(event) =>
                                                    handleContactChange(
                                                        index,
                                                        'email',
                                                        event.target.value,
                                                    )}
                                                error={Boolean(helperTexts[index]?.email)}
                                                helperText={helperTexts[index]?.email}
                                                fullWidth
                                                disabled={isFormDisabled}
                                                slotProps={{
                                                    htmlInput: { maxLength: 255 },
                                                }}
                                            />

                                            <TextField
                                                label="Telefon"
                                                value={contact.phone}
                                                onChange={(event) =>
                                                    handleContactChange(
                                                        index,
                                                        'phone',
                                                        event.target.value,
                                                    )}
                                                error={Boolean(helperTexts[index]?.phone)}
                                                helperText={helperTexts[index]?.phone}
                                                fullWidth
                                                disabled={isFormDisabled}
                                                slotProps={{
                                                    htmlInput: { maxLength: 30 },
                                                }}
                                            />
                                        </Stack>
                                    </Stack>
                                </CardContent>
                            </Card>
                        ))}
                    </Stack>
                </Stack>
            </DialogContent>
            <DialogActions>
                <Button onClick={onClose} disabled={isSaving}>
                    Vazgeç
                </Button>
                <Button
                    variant="contained"
                    loading={isSaving}
                    disabled={isFormDisabled}
                    onClick={handleSubmit}
                >
                    Kaydet
                </Button>
            </DialogActions>
        </Dialog>
    );
}
