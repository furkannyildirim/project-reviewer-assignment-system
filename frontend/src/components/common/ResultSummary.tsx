import { Box } from '@mui/material';

type ResultSummaryProps = {
    filteredCount: number;
    totalCount: number;
    hasActiveFilter: boolean;
    entityLabel: string;
};

export function ResultSummary({
    filteredCount,
    totalCount,
    hasActiveFilter,
    entityLabel,
}: ResultSummaryProps) {
    return (
        <Box
            aria-live="polite"
            sx={{
                alignItems: 'center',
                bgcolor: '#FFFFFF',
                border: '1px solid #E3EAF0',
                borderRadius: 999,
                color: '#59677A',
                display: 'flex',
                flexShrink: 0,
                fontSize: 14,
                gap: 0.75,
                justifyContent: { xs: 'center', md: 'flex-start' },
                lineHeight: 1.4,
                maxWidth: '100%',
                minHeight: 40,
                ml: { md: 'auto' },
                px: 2,
                whiteSpace: 'nowrap',
                width: { xs: '100%', md: 'auto' },
            }}
        >
            {hasActiveFilter ? (
                <>
                    <Box component="span" sx={{ color: '#2A2B61', fontWeight: 700 }}>
                        {filteredCount}
                    </Box>
                    <Box component="span">sonuç</Box>
                    <Box
                        component="span"
                        aria-hidden="true"
                        sx={{ color: '#2EA8D3', fontWeight: 700 }}
                    >
                        ·
                    </Box>
                    <Box component="span" sx={{ color: '#2A2B61', fontWeight: 700 }}>
                        {totalCount}
                    </Box>
                    <Box component="span">toplam</Box>
                </>
            ) : (
                <>
                    <Box component="span" sx={{ color: '#2A2B61', fontWeight: 700 }}>
                        {totalCount}
                    </Box>
                    <Box component="span">{entityLabel}</Box>
                </>
            )}
        </Box>
    );
}
