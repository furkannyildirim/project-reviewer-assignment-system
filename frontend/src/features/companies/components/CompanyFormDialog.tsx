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
    CompanyContactMethodFormValues,
    CompanyContactMethodType,
    CompanyDetail,
    CompanyFormValues,
    CompanyType,
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
    companyType: CompanyType;
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
    activityArea: '',
    logoUrl: '',
    contactMethods: [],
    contacts: [emptyContact],
};

function createEmptyContact(primary = false): CompanyContactFormValues {
    return {
        ...emptyContact,
        primary,
    };
}

function createEmptyContactMethod(
    type: CompanyContactMethodType,
    primary = false,
): CompanyContactMethodFormValues {
    return {
        id: null,
        type,
        value: '',
        label: '',
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
        activityArea: company.activityArea ?? '',
        logoUrl: company.logoUrl ?? '',
        contactMethods: company.contactMethods.map((contactMethod) => ({
            id: contactMethod.id,
            type: contactMethod.type,
            value: contactMethod.value,
            label: contactMethod.label ?? '',
            primary: contactMethod.primary,
        })),
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
    companyType: CompanyType,
): CreateCompanyRequest | UpdateCompanyRequest {
    const companyFields = {
        name: values.name.trim(),
        activityArea: normalizeOptional(values.activityArea),
        city: null,
        district: null,
        osb: null,
        address: null,
        building: null,
        logoUrl: normalizeOptional(values.logoUrl),
        contactMethods: values.contactMethods.map((contactMethod) => ({
            id: contactMethod.id,
            type: contactMethod.type,
            value: contactMethod.value.trim(),
            label: normalizeOptional(contactMethod.label),
            primary: contactMethod.primary,
        })),
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
        companyType,
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

function contactMethodField(
    index: number,
    field: keyof CompanyContactMethodFormValues,
) {
    return `contactMethods[${index}].${field}`;
}

function validate(values: CompanyFormValues): FormErrors {
    const errors: FormErrors = {};
    const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

    if (!values.name.trim()) {
        errors.name = 'Sirket adi zorunludur';
    } else if (values.name.trim().length > 200) {
        errors.name = 'Sirket adi en fazla 200 karakter olabilir';
    }

    if (values.activityArea.trim().length > 2000) {
        errors.activityArea = 'Faaliyet alani en fazla 2000 karakter olabilir';
    }

    if (values.logoUrl.trim().length > 500) {
        errors.logoUrl = 'Logo adresi en fazla 500 karakter olabilir';
    }

    values.contactMethods.forEach((contactMethod, index) => {
        const value = contactMethod.value.trim();
        const label = contactMethod.label.trim();

        if (!value) {
            errors[contactMethodField(index, 'value')] =
                'Iletisim degeri bos birakilamaz';
        } else if (contactMethod.type === 'EMAIL') {
            if (value.length > 255) {
                errors[contactMethodField(index, 'value')] =
                    'E-posta en fazla 255 karakter olabilir';
            } else if (!emailPattern.test(value)) {
                errors[contactMethodField(index, 'value')] =
                    'Gecerli bir e-posta adresi girilmelidir';
            }
        } else if (value.length > 30) {
            errors[contactMethodField(index, 'value')] =
                'Telefon en fazla 30 karakter olabilir';
        }

        if (label.length > 100) {
            errors[contactMethodField(index, 'label')] =
                'Etiket en fazla 100 karakter olabilir';
        }
    });

    if (values.contacts.length === 0) {
        errors.contacts = 'En az bir muhatap kisi eklenmelidir';
    }

    const primaryCount = values.contacts.filter((contact) => contact.primary)
        .length;

    if (primaryCount !== 1) {
        errors.contacts = 'Tam olarak bir muhatap kisi birincil olarak secilmelidir';
    }

    values.contacts.forEach((contact, index) => {
        const fullName = contact.fullName.trim();
        const position = contact.position.trim();
        const email = contact.email.trim();
        const phone = contact.phone.trim();

        if (!fullName) {
            errors[contactField(index, 'fullName')] =
                'Muhatap adi soyadi bos birakilamaz';
        } else if (fullName.length > 200) {
            errors[contactField(index, 'fullName')] =
                'Muhatap adi soyadi en fazla 200 karakter olabilir';
        }

        if (position.length > 150) {
            errors[contactField(index, 'position')] =
                'Pozisyon en fazla 150 karakter olabilir';
        }

        if (email) {
            if (email.length > 255) {
                errors[contactField(index, 'email')] =
                    'Muhatap e-postasi en fazla 255 karakter olabilir';
            } else if (!emailPattern.test(email)) {
                errors[contactField(index, 'email')] =
                    'Gecerli bir muhatap e-posta adresi girilmelidir';
            }
        }

        if (phone.length > 30) {
            errors[contactField(index, 'phone')] =
                'Muhatap telefonu en fazla 30 karakter olabilir';
        }

        if (!email && !phone) {
            const message =
                'Muhatap kisi icin e-posta veya telefon bilgilerinden en az biri girilmelidir';

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

    return 'Sirket kaydedilemedi';
}

function methodTypeLabel(type: CompanyContactMethodType) {
    return type === 'PHONE' ? 'Telefon' : 'E-posta';
}

function methodGroupTitle(type: CompanyContactMethodType) {
    return type === 'PHONE' ? 'Telefonlar' : 'E-postalar';
}

export function CompanyFormDialog({
    open,
    company,
    companyDetail,
    isDetailLoading,
    detailError,
    isSaving,
    companyType,
    onClose,
    onSubmit,
}: CompanyFormDialogProps) {
    const [values, setValues] = useState<CompanyFormValues>(() =>
        toFormValues(companyDetail),
    );
    const [errors, setErrors] = useState<FormErrors>({});
    const [generalError, setGeneralError] = useState<string | null>(null);
    const isEdit = Boolean(company);
    const title = isEdit ? 'Sirketi Duzenle' : 'Yeni Sirket';
    const createTypeMessage = companyType === 'TGB'
        ? 'Bu kayit TGB sirketi olarak olusturulacaktir.'
        : 'Bu kayit dis sirket olarak olusturulacaktir.';
    const detailReady = !isEdit || Boolean(companyDetail);
    const isFormDisabled = isSaving || isDetailLoading || !detailReady || Boolean(detailError);

    const contactHelperTexts = useMemo(() => (values.contacts.map((_, index) => ({
        fullName: errors[contactField(index, 'fullName')],
        position: errors[contactField(index, 'position')],
        email: errors[contactField(index, 'email')],
        phone: errors[contactField(index, 'phone')],
    }))), [errors, values.contacts]);

    const contactMethodHelperTexts = useMemo(
        () => values.contactMethods.map((_, index) => ({
            value: errors[contactMethodField(index, 'value')],
            label: errors[contactMethodField(index, 'label')],
        })),
        [errors, values.contactMethods],
    );

    function clearTopLevelError(field: keyof CompanyFormValues) {
        setErrors((currentErrors) => ({
            ...currentErrors,
            [field]: undefined,
        }));
        setGeneralError(null);
    }

    function handleChange(field: Exclude<keyof CompanyFormValues, 'contacts' | 'contactMethods'>, value: string) {
        setValues((currentValues) => ({
            ...currentValues,
            [field]: value,
        }));
        clearTopLevelError(field);
    }

    function handleContactMethodChange(
        index: number,
        field: Exclude<keyof CompanyContactMethodFormValues, 'id' | 'type' | 'primary'>,
        value: string,
    ) {
        setValues((currentValues) => ({
            ...currentValues,
            contactMethods: currentValues.contactMethods.map((contactMethod, methodIndex) =>
                methodIndex === index
                    ? { ...contactMethod, [field]: value }
                    : contactMethod,
            ),
        }));
        setErrors((currentErrors) => ({
            ...currentErrors,
            [contactMethodField(index, field)]: undefined,
        }));
        setGeneralError(null);
    }

    function handleAddContactMethod(type: CompanyContactMethodType) {
        setValues((currentValues) => {
            const hasMethodOfType = currentValues.contactMethods
                .some((contactMethod) => contactMethod.type === type);

            return {
                ...currentValues,
                contactMethods: [
                    ...currentValues.contactMethods,
                    createEmptyContactMethod(type, !hasMethodOfType),
                ],
            };
        });
        setGeneralError(null);
    }

    function handleRemoveContactMethod(index: number) {
        setValues((currentValues) => {
            const removedMethod = currentValues.contactMethods[index];
            const contactMethods = currentValues.contactMethods.filter((_, methodIndex) =>
                methodIndex !== index,
            );

            if (removedMethod.primary) {
                const firstSameTypeIndex = contactMethods.findIndex(
                    (contactMethod) => contactMethod.type === removedMethod.type,
                );

                if (firstSameTypeIndex >= 0) {
                    return {
                        ...currentValues,
                        contactMethods: contactMethods.map((contactMethod, methodIndex) => ({
                            ...contactMethod,
                            primary: contactMethod.type === removedMethod.type
                                ? methodIndex === firstSameTypeIndex
                                : contactMethod.primary,
                        })),
                    };
                }
            }

            return {
                ...currentValues,
                contactMethods,
            };
        });
        setErrors({});
        setGeneralError(null);
    }

    function handleContactMethodPrimaryChange(index: number) {
        setValues((currentValues) => {
            const selectedType = currentValues.contactMethods[index].type;

            return {
                ...currentValues,
                contactMethods: currentValues.contactMethods.map((contactMethod, methodIndex) => ({
                    ...contactMethod,
                    primary: contactMethod.type === selectedType
                        ? methodIndex === index
                        : contactMethod.primary,
                })),
            };
        });
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
            await onSubmit(buildRequest(values, isEdit, companyType));
        } catch (error) {
            const apiFieldErrors = getFieldErrors(error);

            setErrors(apiFieldErrors);
            setGeneralError(getBusinessError(error));
        }
    }

    function renderContactMethodGroup(type: CompanyContactMethodType) {
        const methodsWithIndexes = values.contactMethods
            .map((contactMethod, index) => ({ contactMethod, index }))
            .filter(({ contactMethod }) => contactMethod.type === type);

        return (
            <Stack spacing={1.25}>
                <Stack
                    direction={{ xs: 'column', sm: 'row' }}
                    spacing={1}
                    sx={{
                        alignItems: { xs: 'stretch', sm: 'center' },
                        justifyContent: 'space-between',
                    }}
                >
                    <Typography sx={{ fontWeight: 600 }}>
                        {methodGroupTitle(type)}
                    </Typography>
                    <Button
                        variant="outlined"
                        size="small"
                        startIcon={<AddOutlinedIcon />}
                        onClick={() => handleAddContactMethod(type)}
                        disabled={isFormDisabled}
                    >
                        {`${methodTypeLabel(type)} Ekle`}
                    </Button>
                </Stack>

                {methodsWithIndexes.length === 0 ? (
                    <Typography variant="body2" color="text.secondary">
                        Kayit yok
                    </Typography>
                ) : methodsWithIndexes.map(({ contactMethod, index }) => (
                    <Stack
                        key={`${contactMethod.id ?? 'new'}-${index}`}
                        direction={{ xs: 'column', md: 'row' }}
                        spacing={1}
                        sx={{ alignItems: { xs: 'stretch', md: 'flex-start' } }}
                    >
                        <TextField
                            label={methodTypeLabel(type)}
                            type={type === 'EMAIL' ? 'email' : undefined}
                            value={contactMethod.value}
                            onChange={(event) =>
                                handleContactMethodChange(index, 'value', event.target.value)}
                            error={Boolean(contactMethodHelperTexts[index]?.value)}
                            helperText={contactMethodHelperTexts[index]?.value}
                            fullWidth
                            disabled={isFormDisabled}
                            slotProps={{
                                htmlInput: { maxLength: type === 'PHONE' ? 30 : 255 },
                            }}
                        />
                        <TextField
                            label="Etiket"
                            value={contactMethod.label}
                            onChange={(event) =>
                                handleContactMethodChange(index, 'label', event.target.value)}
                            error={Boolean(contactMethodHelperTexts[index]?.label)}
                            helperText={contactMethodHelperTexts[index]?.label}
                            fullWidth
                            disabled={isFormDisabled}
                            slotProps={{
                                htmlInput: { maxLength: 100 },
                            }}
                        />
                        <Stack
                            direction="row"
                            spacing={1}
                            sx={{
                                alignItems: 'center',
                                justifyContent: { xs: 'space-between', md: 'flex-start' },
                                minWidth: { md: 190 },
                                pt: { md: 0.75 },
                            }}
                        >
                            <FormControlLabel
                                control={(
                                    <Radio
                                        checked={contactMethod.primary}
                                        onChange={() =>
                                            handleContactMethodPrimaryChange(index)}
                                        disabled={isFormDisabled}
                                    />
                                )}
                                label="Ana"
                            />
                            <Button
                                color="error"
                                startIcon={<DeleteOutlineOutlinedIcon />}
                                onClick={() => handleRemoveContactMethod(index)}
                                disabled={isFormDisabled}
                            >
                                Sil
                            </Button>
                        </Stack>
                    </Stack>
                ))}
            </Stack>
        );
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
                        <Alert severity="info">Sirket detayi yukleniyor...</Alert>
                    )}

                    {!isEdit && (
                        <Alert severity="info">{createTypeMessage}</Alert>
                    )}

                    <TextField
                        label="Sirket adi"
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

                    <TextField
                        label="Faaliyet alani"
                        value={values.activityArea}
                        onChange={(event) => handleChange('activityArea', event.target.value)}
                        error={Boolean(errors.activityArea)}
                        helperText={errors.activityArea}
                        fullWidth
                        multiline
                        minRows={3}
                        maxRows={6}
                        disabled={isFormDisabled}
                        slotProps={{
                            htmlInput: { maxLength: 2000 },
                        }}
                    />

                    <TextField
                        label="Logo URL"
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
                        <Box>
                            <Typography variant="h6">
                                Sirket Iletisim Bilgileri
                            </Typography>
                        </Box>
                        {renderContactMethodGroup('PHONE')}
                        {renderContactMethodGroup('EMAIL')}
                    </Stack>

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
                                    Yetkili Kisiler
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
                                Yetkili Kisi Ekle
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
                                                    ? `${index + 1}. Yetkili Kisi`
                                                    : 'Yetkili Kisi'}
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
                                                    label="Ana yetkili kisi"
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
                                            error={Boolean(contactHelperTexts[index]?.fullName)}
                                            helperText={contactHelperTexts[index]?.fullName}
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
                                            error={Boolean(contactHelperTexts[index]?.position)}
                                            helperText={contactHelperTexts[index]?.position}
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
                                                error={Boolean(contactHelperTexts[index]?.email)}
                                                helperText={contactHelperTexts[index]?.email}
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
                                                error={Boolean(contactHelperTexts[index]?.phone)}
                                                helperText={contactHelperTexts[index]?.phone}
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
                    Vazgec
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
