import { Box, Stack, Typography } from '@mui/material';
import type { ReactNode } from 'react';

type PageHeaderProps = {
    title: string;
    description: string;
    action?: ReactNode;
};

export function PageHeader({ title, description, action }: PageHeaderProps) {
    return (
        <Stack
            direction={{ xs: 'column', sm: 'row' }}
            spacing={2}
            sx={{
                alignItems: { xs: 'stretch', sm: 'flex-start' },
                justifyContent: 'space-between',
            }}
        >
            <Box sx={{ minWidth: 0 }}>
                <Typography variant="h4" component="h1">
                    {title}
                </Typography>
                <Typography color="text.secondary" sx={{ mt: 0.75 }}>
                    {description}
                </Typography>
            </Box>
            {action}
        </Stack>
    );
}
