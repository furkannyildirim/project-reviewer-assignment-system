import { useQuery } from '@tanstack/react-query';
import {
    Alert,
    Box,
    Button,
    Chip,
    CircularProgress,
    Divider,
    Link,
    Popover,
    Stack,
    Typography,
} from '@mui/material';
import { getApiErrorMessage } from '../../../utils/getApiErrorMessage';
import { getCompanyById } from '../companyApi';
import { companyQueryKeys } from '../companyQueryKeys';
import type { CompanyContact } from '../company.types';

type CompanyContactsPopoverProps = {
    companyId: number;
    anchorEl: HTMLElement | null;
    onClose: () => void;
};

function sortContacts(contacts: CompanyContact[]) {
    return [...contacts].sort((firstContact, secondContact) => {
        if (firstContact.primary !== secondContact.primary) {
            return firstContact.primary ? -1 : 1;
        }

        return firstContact.id - secondContact.id;
    });
}

export function CompanyContactsPopover({
    companyId,
    anchorEl,
    onClose,
}: CompanyContactsPopoverProps) {
    const open = Boolean(anchorEl);
    const companyDetailQuery = useQuery({
        queryKey: companyQueryKeys.detail(companyId),
        queryFn: () => getCompanyById(companyId),
        enabled: open,
    });

    const contacts = sortContacts(companyDetailQuery.data?.contacts ?? []);

    return (
        <Popover
            open={open}
            anchorEl={anchorEl}
            onClose={onClose}
            anchorOrigin={{
                vertical: 'bottom',
                horizontal: 'left',
            }}
            transformOrigin={{
                vertical: 'top',
                horizontal: 'left',
            }}
            slotProps={{
                paper: {
                    sx: {
                        width: { xs: 320, sm: 400 },
                        maxWidth: 'calc(100vw - 32px)',
                        maxHeight: 420,
                        overflowY: 'auto',
                        p: 2,
                    },
                },
            }}
        >
            <Stack spacing={1.5}>
                <Stack
                    direction="row"
                    spacing={1}
                    sx={{ alignItems: 'center', justifyContent: 'space-between' }}
                >
                    <Typography variant="h6">
                        Yetkili Kişiler
                    </Typography>
                    {companyDetailQuery.data && (
                        <Chip
                            label={`${contacts.length} kişi`}
                            size="small"
                            variant="outlined"
                        />
                    )}
                </Stack>

                {companyDetailQuery.isLoading && (
                    <Stack
                        direction="row"
                        spacing={1}
                        sx={{ alignItems: 'center', py: 2 }}
                    >
                        <CircularProgress size={20} />
                        <Typography color="text.secondary">
                            Yetkili kişiler yükleniyor...
                        </Typography>
                    </Stack>
                )}

                {companyDetailQuery.isError && (
                    <Alert
                        severity="error"
                        action={(
                            <Button
                                color="inherit"
                                size="small"
                                onClick={() => void companyDetailQuery.refetch()}
                            >
                                Tekrar dene
                            </Button>
                        )}
                    >
                        {getApiErrorMessage(
                            companyDetailQuery.error,
                            'Yetkili kişiler alınamadı',
                        )}
                    </Alert>
                )}

                {companyDetailQuery.isSuccess && contacts.map((contact, index) => (
                    <Box key={contact.id}>
                        {index > 0 && <Divider sx={{ mb: 1.5 }} />}

                        <Stack spacing={0.5}>
                            <Stack
                                direction="row"
                                spacing={1}
                                sx={{
                                    alignItems: 'flex-start',
                                    justifyContent: 'space-between',
                                }}
                            >
                                <Typography sx={{ fontWeight: 600 }}>
                                    {contact.fullName}
                                </Typography>
                                {contact.primary && (
                                    <Chip
                                        label="Ana Yetkili"
                                        color="primary"
                                        size="small"
                                        variant="outlined"
                                    />
                                )}
                            </Stack>

                            {contact.position && (
                                <Typography color="text.secondary">
                                    {contact.position}
                                </Typography>
                            )}

                            {contact.email && (
                                <Link
                                    href={`mailto:${contact.email}`}
                                    aria-label={`${contact.fullName} e-posta adresine ileti gönder`}
                                    underline="hover"
                                >
                                    {contact.email}
                                </Link>
                            )}

                            {contact.phone && (
                                <Link
                                    href={`tel:${contact.phone}`}
                                    aria-label={`${contact.fullName} telefon numarasını ara`}
                                    underline="hover"
                                >
                                    {contact.phone}
                                </Link>
                            )}
                        </Stack>
                    </Box>
                ))}
            </Stack>
        </Popover>
    );
}
