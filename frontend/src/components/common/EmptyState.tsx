import { Paper, Stack, Typography } from '@mui/material';

type EmptyStateProps = {
    title: string;
    description: string;
};

export function EmptyState({ title, description }: EmptyStateProps) {
    return (
        <Paper
            elevation={0}
            sx={{
                p: 4,
                border: '1px solid',
                borderColor: 'divider',
                borderRadius: '8px',
                textAlign: 'center',
            }}
        >
            <Stack spacing={1}>
                <Typography variant="h6">{title}</Typography>
                <Typography color="text.secondary">{description}</Typography>
            </Stack>
        </Paper>
    );
}
