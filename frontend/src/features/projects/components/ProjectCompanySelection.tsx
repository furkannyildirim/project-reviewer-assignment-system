import { useEffect, useMemo, useState } from 'react';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import SearchOutlinedIcon from '@mui/icons-material/SearchOutlined';
import {
    Autocomplete,
    Box,
    Button,
    Chip,
    Paper,
    Stack,
    TextField,
    Typography,
} from '@mui/material';
import { useDebouncedValue } from '../../../hooks/useDebouncedValue';
import { getApiErrorMessage } from '../../../utils/getApiErrorMessage';
import { getCompanies } from '../../companies/companyApi';
import type { CompanyListItem, CompanyType } from '../../companies/company.types';
import type { ProjectCompanyCandidate, ProjectCompanyMatch } from '../project.types';

type ProjectCompanySelectionProps = {
    match: ProjectCompanyMatch;
    selectedCompanyId: number | null;
    onSelect: (companyId: number | null) => void;
    error?: string;
    disabled: boolean;
};

function typeLabel(type: CompanyType) {
    return type === 'TGB' ? 'TGB Şirketi' : 'Dış Şirket';
}

function candidateToCompany(candidate: ProjectCompanyCandidate): CompanyListItem {
    return {
        id: candidate.id,
        name: candidate.name,
        companyType: candidate.companyType,
        activityArea: null,
        contactMethods: [],
        primaryContact: null,
        contactCount: 0,
    };
}

export function ProjectCompanySelection({
    match,
    selectedCompanyId,
    onSelect,
    error,
    disabled,
}: ProjectCompanySelectionProps) {
    const [searchOpen, setSearchOpen] = useState(false);
    const [search, setSearch] = useState('');
    const [selectedCompanySnapshot, setSelectedCompanySnapshot] =
        useState<CompanyListItem | null>(null);
    const debouncedSearch = useDebouncedValue(search.trim(), 400);
    const queryEnabled = searchOpen && debouncedSearch.length > 0;
    const queryTgb = useQuery({
        queryKey: ['project-company-selection', 'TGB', debouncedSearch],
        queryFn: () => getCompanies({ companyType: 'TGB', search: debouncedSearch, page: 0, size: 10 }),
        enabled: queryEnabled,
        placeholderData: keepPreviousData,
    });
    const queryExternal = useQuery({
        queryKey: ['project-company-selection', 'EXTERNAL', debouncedSearch],
        queryFn: () => getCompanies({ companyType: 'EXTERNAL', search: debouncedSearch, page: 0, size: 10 }),
        enabled: queryEnabled,
        placeholderData: keepPreviousData,
    });
    const options = useMemo(() => {
        const byId = new Map<number, CompanyListItem>();
        [...(queryTgb.data?.content ?? []), ...(queryExternal.data?.content ?? [])]
            .forEach((company) => byId.set(company.id, company));
        return [...byId.values()].slice(0, 8);
    }, [queryExternal.data?.content, queryTgb.data?.content]);
    const initialCandidates = match.candidates.length > 0
        ? match.candidates.map(candidateToCompany)
        : match.matchedCompany ? [candidateToCompany(match.matchedCompany)] : [];
    const knownCompanies = [...initialCandidates, ...options];
    const resolvedSelectedCompany = knownCompanies
        .find((company) => company.id === selectedCompanyId) ?? null;
    const selectedCompany = resolvedSelectedCompany
        ?? (selectedCompanySnapshot?.id === selectedCompanyId
            ? selectedCompanySnapshot
            : null);
    const isSearching = queryTgb.isFetching || queryExternal.isFetching;
    const searchError = queryTgb.error ?? queryExternal.error;

    useEffect(() => {
        if (selectedCompanyId === null) {
            setSelectedCompanySnapshot(null);
            return;
        }
        if (resolvedSelectedCompany) {
            setSelectedCompanySnapshot(resolvedSelectedCompany);
        }
    }, [resolvedSelectedCompany, selectedCompanyId]);

    function openSearch() {
        setSearch('');
        setSearchOpen(true);
    }

    function cancelSearch() {
        setSearch('');
        setSearchOpen(false);
    }

    function selectCompany(company: CompanyListItem | null) {
        onSelect(company?.id ?? null);
        setSelectedCompanySnapshot(company);
        setSearch('');
        setSearchOpen(false);
    }

    return (
        <Stack spacing={1.25}>
            {selectedCompany && (
                <Paper elevation={0} sx={{ bgcolor: '#F2F9FC', border: '1px solid #BFE5F0', borderRadius: '8px', p: 1.25 }}>
                    <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} sx={{ alignItems: { sm: 'center' }, justifyContent: 'space-between' }}>
                        <Box>
                            <Typography sx={{ fontWeight: 700 }}>{selectedCompany.name}</Typography>
                            <Typography color="text.secondary" variant="body2">{typeLabel(selectedCompany.companyType)}</Typography>
                        </Box>
                        <Stack direction="row" spacing={0.75} sx={{ alignItems: 'center' }}>
                            <Chip label="Seçili şirket" size="small" sx={{ bgcolor: '#DDF3FA', color: '#187A9C' }} />
                            <Button disabled={disabled} onClick={openSearch} size="small" sx={{ color: '#526273', minWidth: 0 }}>
                                Değiştir
                            </Button>
                        </Stack>
                    </Stack>
                </Paper>
            )}
            {!selectedCompany && initialCandidates.length > 0 && !searchOpen && (
                <Stack spacing={0.75}>
                    <Typography color="text.secondary" variant="body2">
                        {match.status === 'AMBIGUOUS' ? 'Bir şirket seçin' : 'Eşleşen şirket'}
                    </Typography>
                    {initialCandidates.map((candidate) => (
                        <Button
                            key={candidate.id}
                            disabled={disabled}
                            onClick={() => selectCompany(candidate)}
                            sx={{
                                alignItems: 'center',
                                bgcolor: selectedCompanyId === candidate.id ? '#F2F9FC' : '#FFFFFF',
                                border: '1px solid',
                                borderColor: selectedCompanyId === candidate.id ? '#8FD3E5' : '#E3EAF0',
                                color: '#344054',
                                justifyContent: 'space-between',
                                px: 1.5,
                                py: 1,
                                textAlign: 'left',
                                textTransform: 'none',
                                '&:hover': { bgcolor: '#F2F9FC', borderColor: '#8FD3E5' },
                            }}
                            variant="outlined"
                        >
                            <Box>
                                <Typography sx={{ fontWeight: 600 }}>{candidate.name}</Typography>
                                <Typography color="text.secondary" variant="body2">{typeLabel(candidate.companyType)}</Typography>
                            </Box>
                            {selectedCompanyId === candidate.id && <Chip label="Seçili" size="small" sx={{ bgcolor: '#DDF3FA', color: '#187A9C' }} />}
                        </Button>
                    ))}
                </Stack>
            )}
            {!selectedCompany && !searchOpen && (
                <Button
                    disabled={disabled}
                    onClick={openSearch}
                    startIcon={<SearchOutlinedIcon />}
                    sx={{ alignSelf: 'flex-start', color: '#2A2B61' }}
                >
                    Farklı şirket seç
                </Button>
            )}
            {searchOpen && (
                <Autocomplete
                    disabled={disabled}
                    filterOptions={(values) => values}
                    getOptionLabel={(option) => option.name}
                    id="project-preview-companyId"
                    inputValue={search}
                    isOptionEqualToValue={(option, value) => option.id === value.id}
                    loading={isSearching}
                    noOptionsText={debouncedSearch ? 'Şirket bulunamadı' : 'Şirket aramak için yazın'}
                    onChange={(_, value) => selectCompany(value)}
                    onInputChange={(_, newInputValue, reason) => {
                        if (reason === 'input' || reason === 'clear') {
                            setSearch(newInputValue);
                        }
                    }}
                    options={options}
                    renderInput={(params) => (
                        <TextField
                            {...params}
                            error={Boolean(searchError)}
                            helperText={searchError ? getApiErrorMessage(searchError, 'Şirketler alınamadı.') : undefined}
                            placeholder="Şirket adı yazın"
                            sx={{
                                '& .MuiInputLabel-root.Mui-focused:not(.Mui-error)': {
                                    color: '#2A2B61',
                                },
                                '& .MuiOutlinedInput-root.Mui-focused:not(.Mui-error) .MuiOutlinedInput-notchedOutline': {
                                    borderColor: '#2A2B61',
                                    borderWidth: '1.5px',
                                },
                            }}
                        />
                    )}
                    renderOption={(props, option) => (
                        <li {...props} key={option.id}>
                            <Stack>
                                <Typography sx={{ fontWeight: 600 }}>{option.name}</Typography>
                                <Typography color="text.secondary" variant="body2">{typeLabel(option.companyType)}</Typography>
                            </Stack>
                        </li>
                    )}
                    value={searchOpen ? null : selectedCompany}
                />
            )}
            {searchOpen && (
                <Button disabled={disabled} onClick={cancelSearch} size="small" sx={{ alignSelf: 'flex-start', color: '#526273' }}>
                    İptal
                </Button>
            )}
            {error && <Typography color="error" id="project-preview-companyId" variant="body2">{error}</Typography>}
        </Stack>
    );
}
